package com.yamakotaro.ecojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActionRewardTest {
   @Test
   void perLevelRewardsScaleWithEnchantCost() {
      ActionReward flat = new ActionReward(5.0, 2.0, 0.0, 0.0);
      ActionReward perLevel = new ActionReward(0.0, 0.0, 1.5, 0.5);
      assertEquals(5.0, flat.moneyFor(30.0), 1e-9);
      assertEquals(45.0, perLevel.moneyFor(30.0), 1e-9);
      assertEquals(15.0, perLevel.xpFor(30.0), 1e-9);
   }
}
