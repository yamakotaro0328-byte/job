package com.yamakotaro.ecojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlacedBlockTrackerTest {
   @Test
   void everyPositionInAChunkColumnPacksUniquely() {
      Set<Integer> seen = new HashSet<>();
      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            for (int y = -64; y < 320; y++) {
               seen.add(PlacedBlockTracker.pack(x, y, z));
            }
         }
      }

      assertEquals(16 * 16 * 384, seen.size());
   }

   @Test
   void negativeWorldCoordinatesMapToChunkLocal() {
      // Block -1 is x=15 of chunk -1, same local slot as block 15 of chunk 0.
      assertEquals(PlacedBlockTracker.pack(15, 70, 15), PlacedBlockTracker.pack(-1, 70, -1));
      assertNotEquals(PlacedBlockTracker.pack(0, 70, 0), PlacedBlockTracker.pack(0, 71, 0));
   }
}
