package com.yamakotaro.ecojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TitlesTest {
   private static final Map<String, String> TITLES = Map.of("1", "Novice", "10", "Apprentice", "50", "Master", "oops", "ignored");

   @Test
   void picksHighestThresholdAtOrBelowLevel() {
      assertEquals("Novice", PlayerJobManager.pickTitle(TITLES, 1));
      assertEquals("Novice", PlayerJobManager.pickTitle(TITLES, 9));
      assertEquals("Apprentice", PlayerJobManager.pickTitle(TITLES, 10));
      assertEquals("Master", PlayerJobManager.pickTitle(TITLES, 100));
      assertEquals("", PlayerJobManager.pickTitle(TITLES, 0));
   }
}
