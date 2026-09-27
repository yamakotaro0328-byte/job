package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class JobsMenuHolder implements InventoryHolder {
   public static final int SUMMARY_SLOT = 4;
   public static final int BACK_SLOT = 45;
   public static final int CLOSE_SLOT = 49;
   private static final int PROGRESS_BAR_LENGTH = 10;
   static final Map<String, Material> ICONS = Map.ofEntries(
      Map.entry("miner", Material.IRON_PICKAXE),
      Map.entry("digger", Material.IRON_SHOVEL),
      Map.entry("woodcutter", Material.DIAMOND_AXE),
      Map.entry("farmer", Material.WHEAT),
      Map.entry("builder", Material.BRICKS),
      Map.entry("fisherman", Material.FISHING_ROD),
      Map.entry("treasurehunter", Material.CHEST),
      Map.entry("hunter", Material.IRON_SWORD),
      Map.entry("archer", Material.BOW),
      Map.entry("slayer", Material.NETHER_STAR),
      Map.entry("warrior", Material.SHIELD),
      Map.entry("breeder", Material.EGG),
      Map.entry("tamer", Material.BONE),
      Map.entry("shearer", Material.SHEARS),
      Map.entry("beekeeper", Material.HONEYCOMB),
      Map.entry("enchanter", Material.ENCHANTING_TABLE),
      Map.entry("smelter", Material.FURNACE),
      Map.entry("crafter", Material.CRAFTING_TABLE),
      Map.entry("merchant", Material.EMERALD),
      Map.entry("explorer", Material.COMPASS)
   );
   private static final List<Integer> JOB_SLOTS = MenuUtil.interiorSlots();
   private final Messages messages;
   private final Inventory inventory;
   private final Map<Integer, String> slotToJobId = new HashMap<>();

   public JobsMenuHolder(Messages messages) {
      this.messages = messages;
      this.inventory = Bukkit.createInventory(this, 54, messages.get("menu.title", Map.of()));
   }

   public String jobIdAt(int slot) {
      return this.slotToJobId.get(slot);
   }

   public void render(JobManager jobManager, PlayerJobManager playerJobManager, JobOverrides jobOverrides, Player viewer) {
      this.inventory.clear();
      this.slotToJobId.clear();
      MenuUtil.fillBorder(this.inventory);
      Map<String, PlayerJobProgress> allProgress = playerJobManager.allProgress(viewer.getUniqueId());
      boolean canViewLeaderboard = viewer.hasPermission("ecojobs.top");
      this.inventory.setItem(4, this.summaryItem(viewer, jobManager, playerJobManager, allProgress));
      int index = 0;

      for (String jobId : jobManager.all().keySet()) {
         if (index >= JOB_SLOTS.size()) {
            break;
         }

         boolean active = playerJobManager.isJoined(viewer.getUniqueId(), jobId);
         boolean enabled = jobOverrides.isEnabled(jobId);
         int slot = JOB_SLOTS.get(index++);
         this.inventory.setItem(slot, this.buildJobItem(jobId, allProgress.get(jobId), active, enabled, canViewLeaderboard, jobManager, playerJobManager));
         this.slotToJobId.put(slot, jobId);
      }

      this.inventory.setItem(45, MenuUtil.backItem(this.messages));
      this.inventory.setItem(49, MenuUtil.closeItem(this.messages));
   }

   private ItemStack summaryItem(Player viewer, JobManager jobManager, PlayerJobManager playerJobManager, Map<String, PlayerJobProgress> allProgress) {
      ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
      if (stack.getItemMeta() instanceof SkullMeta skullMeta) {
         skullMeta.setOwningPlayer(viewer);
         skullMeta.displayName(this.messages.get("menu.summary-title", Map.of("player", viewer.getName())));
         int totalLevel = 0;
         int joinedCount = 0;

         for (String jobId : jobManager.all().keySet()) {
            PlayerJobProgress progress = allProgress.get(jobId);
            if (progress != null) {
               totalLevel += progress.getLevel();
            }

            if (playerJobManager.isJoined(viewer.getUniqueId(), jobId)) {
               joinedCount++;
            }
         }

         skullMeta.lore(
            List.of(
               this.messages
                  .get(
                     "menu.summary-lore",
                     Map.of(
                        "total_level", String.valueOf(totalLevel), "joined", String.valueOf(joinedCount), "max", String.valueOf(jobManager.maxConcurrentJobs())
                     )
                  )
            )
         );
         stack.setItemMeta(skullMeta);
      }

      return stack;
   }

   private ItemStack buildJobItem(
      String jobId,
      PlayerJobProgress progress,
      boolean active,
      boolean enabled,
      boolean canViewLeaderboard,
      JobManager jobManager,
      PlayerJobManager playerJobManager
   ) {
      Material material = ICONS.getOrDefault(jobId, Material.PAPER);
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get("menu.job-title", Map.of("job", this.messages.jobName(jobId))));
         List<Component> lore = new ArrayList<>();
         boolean maxed = false;
         if (progress != null) {
            lore.add(
               this.messages
                  .get(
                     "menu.lore-level",
                     Map.of(
                        "level",
                        String.valueOf(progress.getLevel()),
                        "prestige",
                        String.valueOf(progress.getPrestige()),
                        "xp",
                        String.format("%.0f", progress.getXp()),
                        "next_xp",
                        String.format("%.0f", playerJobManager.xpToNextLevel(progress.getLevel()))
                     )
                  )
            );
            maxed = progress.getLevel() >= jobManager.maxLevel();
            if (!maxed) {
               double nextXp = playerJobManager.xpToNextLevel(progress.getLevel());
               lore.add(
                  this.messages
                     .get(
                        "menu.lore-progress",
                        Map.of("bar", progressBar(progress.getXp(), nextXp), "percent", String.valueOf(progressPercent(progress.getXp(), nextXp)))
                     )
               );
            }
         } else {
            lore.add(this.messages.get("menu.lore-not-joined", Map.of()));
         }

         if (!enabled) {
            lore.add(this.messages.get("menu.lore-disabled", Map.of()));
         } else if (progress != null) {
            lore.add(this.messages.get(active ? "menu.lore-click-leave" : "menu.lore-click-rejoin", Map.of()));
         } else {
            lore.add(this.messages.get("menu.lore-click-join", Map.of()));
         }

         lore.add(this.messages.get("menu.lore-click-info", Map.of()));
         if (canViewLeaderboard) {
            lore.add(this.messages.get("menu.lore-click-leaderboard", Map.of()));
         }

         meta.lore(lore);
         meta.setEnchantmentGlintOverride(active);
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private static String progressBar(double xp, double nextXp) {
      double ratio = nextXp > 0.0 ? Math.min(1.0, xp / nextXp) : 0.0;
      int filled = (int)Math.round(10.0 * ratio);
      return "&a" + "■".repeat(filled) + "&7" + "□".repeat(10 - filled);
   }

   private static int progressPercent(double xp, double nextXp) {
      double ratio = nextXp > 0.0 ? Math.min(1.0, xp / nextXp) : 0.0;
      return (int)Math.round(ratio * 100.0);
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
