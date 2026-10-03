package com.yamakotaro.ecojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QuestStreakTest {
   @Test
   void firstDayHasNoBonusAndBonusIsCapped() {
      assertEquals(1.0, QuestManager.streakMultiplier(0, 0.1, 7), 1e-9);
      assertEquals(1.0, QuestManager.streakMultiplier(1, 0.1, 7), 1e-9);
      assertEquals(1.1, QuestManager.streakMultiplier(2, 0.1, 7), 1e-9);
      assertEquals(1.7, QuestManager.streakMultiplier(8, 0.1, 7), 1e-9);
      assertEquals(1.7, QuestManager.streakMultiplier(50, 0.1, 7), 1e-9);
   }
}
