package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.PerkManager;
import com.yamakotaro.ecojobs.PlacedBlockTracker;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.ItemStack;

public class BlockJobListener implements Listener {
   private static final List<String> MINING_JOB_IDS = List.of("miner", "digger", "woodcutter");
   private final PlayerJobManager jobs;
   private final PlacedBlockTracker placedBlocks;
   private final JobManager jobManager;
   private final PerkManager perkManager;

   public BlockJobListener(PlayerJobManager jobs, PlacedBlockTracker placedBlocks, JobManager jobManager, PerkManager perkManager) {
      this.jobs = jobs;
      this.placedBlocks = placedBlocks;
      this.jobManager = jobManager;
      this.perkManager = perkManager;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      this.placedBlocks.markPlaced(event.getBlock());
      if (this.placedBlocks.markBuildPaid(event.getBlock())) {
         this.jobs.reward(event.getPlayer(), "builder", "place-block", event.getBlock().getType().name(), 1.0);
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      Material type = block.getType();
      if (this.isFullyGrownCrop(block)) {
         this.applyDoubleDrop(event, player, block, "farmer");
         this.jobs.reward(player, "farmer", "harvest-crop", type.name(), 1.0);
      } else if (type != Material.MELON && type != Material.PUMPKIN) {
         if (!this.placedBlocks.wasPlaced(block)) {
            this.applyMiningPerks(event, player, block, type);
            this.jobs.reward(player, "miner", "break-block", type.name(), 1.0);
            this.jobs.reward(player, "digger", "break-block", type.name(), 1.0);
            this.jobs.reward(player, "woodcutter", "break-block", type.name(), 1.0);
         }
      } else {
         if (!this.placedBlocks.wasPlaced(block)) {
            this.applyDoubleDrop(event, player, block, "farmer");
            this.jobs.reward(player, "farmer", "harvest-tall-plant", type.name(), 1.0);
         }
      }
   }

   /** Blocks pushed by pistons keep (and gain) the placed mark at their new position. */
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPistonExtend(BlockPistonExtendEvent event) {
      this.markMoved(event.getBlocks(), event.getDirection());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPistonRetract(BlockPistonRetractEvent event) {
      this.markMoved(event.getBlocks(), event.getDirection());
   }

   private void markMoved(List<Block> blocks, BlockFace direction) {
      for (Block block : blocks) {
         this.placedBlocks.wasPlaced(block);
      }

      for (Block block : blocks) {
         this.placedBlocks.markPlaced(block.getRelative(direction));
      }
   }

   /** Cobblestone/stone/basalt generators: blocks formed by lava+water count as placed. */
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockForm(BlockFormEvent event) {
      Material formed = event.getNewState().getType();
      if (formed == Material.COBBLESTONE || formed == Material.STONE || formed == Material.BASALT || formed == Material.OBSIDIAN) {
         this.placedBlocks.markPlaced(event.getBlock());
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onHarvestBlock(PlayerHarvestBlockEvent event) {
      int amount = event.getItemsHarvested().stream().mapToInt(ItemStack::getAmount).sum();
      if (amount > 0) {
         this.jobs.reward(event.getPlayer(), "beekeeper", "harvest-block", event.getHarvestedBlock().getType().name(), amount);
      }
   }

   private boolean isFullyGrownCrop(Block block) {
      return block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
   }

   private void applyMiningPerks(BlockBreakEvent event, Player player, Block block, Material type) {
      Map<String, PlayerJobProgress> joined = this.jobs.joinedJobs(player.getUniqueId());
      boolean doubleDrop = false;
      Material smelted = null;

      for (String jobId : MINING_JOB_IDS) {
         PlayerJobProgress progress = joined.get(jobId);
         JobDefinition job = progress != null ? this.jobManager.get(jobId) : null;
         if (job != null) {
            int effectiveLevel = this.perkManager.effectiveLevel(progress);
            if (!doubleDrop && this.perkManager.rollDoubleDrop(job, effectiveLevel)) {
               doubleDrop = true;
            }

            if (smelted == null && "miner".equals(jobId) && this.perkManager.hasAutoSmelt(job, effectiveLevel)) {
               smelted = this.perkManager.smeltedResult(type);
            }
         }
      }

      if (doubleDrop || smelted != null) {
         Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
         if (!drops.isEmpty()) {
            event.setDropItems(false);
            if (smelted != null) {
               int totalAmount = drops.stream().mapToInt(ItemStack::getAmount).sum();
               this.dropStack(block, new ItemStack(smelted, totalAmount), doubleDrop);
            } else {
               for (ItemStack drop : drops) {
                  this.dropStack(block, drop, doubleDrop);
               }
            }
         }
      }
   }

   private void applyDoubleDrop(BlockBreakEvent event, Player player, Block block, String jobId) {
      PlayerJobProgress progress = this.jobs.joinedJobs(player.getUniqueId()).get(jobId);
      JobDefinition job = progress != null ? this.jobManager.get(jobId) : null;
      if (job != null && this.perkManager.rollDoubleDrop(job, this.perkManager.effectiveLevel(progress))) {
         Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
         if (!drops.isEmpty()) {
            event.setDropItems(false);

            for (ItemStack drop : drops) {
               this.dropStack(block, drop, true);
            }
         }
      }
   }

   private void dropStack(Block block, ItemStack stack, boolean doubled) {
      block.getWorld().dropItemNaturally(block.getLocation(), stack);
      if (doubled) {
         block.getWorld().dropItemNaturally(block.getLocation(), stack.clone());
      }
   }
}
