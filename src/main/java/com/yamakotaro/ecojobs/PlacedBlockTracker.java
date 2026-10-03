package com.yamakotaro.ecojobs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Remembers which blocks were put there by players (or generators) so breaking them doesn't pay
 * mining jobs. Positions are stored in each chunk's persistent data, so the protection survives
 * restarts and never expires - previously an in-memory set was wiped every 30 minutes, after
 * which re-mining your own placed ore paid out again.
 */
public class PlacedBlockTracker {
   private final NamespacedKey key;
   /** Builder anti-repeat: in memory only (cleared periodically), same as before. */
   private final Set<PlacedBlockTracker.BlockKey> buildPaid = new HashSet<>();

   public PlacedBlockTracker(EcoJobsPlugin plugin) {
      this.key = new NamespacedKey(plugin, "placed");
   }

   public void markPlaced(Block block) {
      PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
      int packed = pack(block);
      int[] current = pdc.getOrDefault(this.key, PersistentDataType.INTEGER_ARRAY, new int[0]);
      for (int value : current) {
         if (value == packed) {
            return;
         }
      }

      int[] next = Arrays.copyOf(current, current.length + 1);
      next[current.length] = packed;
      pdc.set(this.key, PersistentDataType.INTEGER_ARRAY, next);
   }

   public boolean markBuildPaid(Block block) {
      return this.buildPaid.add(PlacedBlockTracker.BlockKey.of(block));
   }

   /** True if the block was player-placed; also forgets it (the block is being broken). */
   public boolean wasPlaced(Block block) {
      PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
      int[] current = pdc.get(this.key, PersistentDataType.INTEGER_ARRAY);
      if (current == null) {
         return false;
      }

      int packed = pack(block);
      for (int i = 0; i < current.length; i++) {
         if (current[i] == packed) {
            if (current.length == 1) {
               pdc.remove(this.key);
            } else {
               int[] next = new int[current.length - 1];
               System.arraycopy(current, 0, next, 0, i);
               System.arraycopy(current, i + 1, next, i, current.length - i - 1);
               pdc.set(this.key, PersistentDataType.INTEGER_ARRAY, next);
            }

            return true;
         }
      }

      return false;
   }

   public void clear() {
      this.buildPaid.clear();
   }

   /** Chunk-local x/z (4 bits each) + y offset (supports y from -2048 to 2047). */
   private static int pack(Block block) {
      return pack(block.getX(), block.getY(), block.getZ());
   }

   static int pack(int x, int y, int z) {
      return (x & 15) | (z & 15) << 4 | (y + 2048) << 8;
   }

   private record BlockKey(UUID world, int x, int y, int z) {
      private static PlacedBlockTracker.BlockKey of(Block block) {
         return new PlacedBlockTracker.BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
      }
   }
}
