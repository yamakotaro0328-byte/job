package com.yamakotaro.ecojobs;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.block.Block;

public class PlacedBlockTracker {
   private final Set<PlacedBlockTracker.BlockKey> placed = new HashSet<>();
   private final Set<PlacedBlockTracker.BlockKey> buildPaid = new HashSet<>();

   public void markPlaced(Block block) {
      this.placed.add(PlacedBlockTracker.BlockKey.of(block));
   }

   public boolean markBuildPaid(Block block) {
      return this.buildPaid.add(PlacedBlockTracker.BlockKey.of(block));
   }

   public boolean wasPlaced(Block block) {
      return this.placed.remove(PlacedBlockTracker.BlockKey.of(block));
   }

   public void clear() {
      this.placed.clear();
      this.buildPaid.clear();
   }

   private record BlockKey(UUID world, int x, int y, int z) {
      private static PlacedBlockTracker.BlockKey of(Block block) {
         return new PlacedBlockTracker.BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
      }
   }
}
