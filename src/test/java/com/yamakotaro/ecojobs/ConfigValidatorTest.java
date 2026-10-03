package com.yamakotaro.ecojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigValidatorTest {
   @Test
   void acceptsRealMaterialsAndDefault() {
      List<String> problems = new ArrayList<>();
      ConfigValidator.validateAction("jobs.miner.actions.break-block", "break-block", Set.of("DIAMOND_ORE", "deepslate_iron_ore", "default"), problems);
      assertEquals(List.of(), problems);
   }

   @Test
   void flagsMisspelledMaterial() {
      List<String> problems = new ArrayList<>();
      ConfigValidator.validateAction("jobs.miner.actions.break-block", "break-block", Set.of("DIAMOND_OR"), problems);
      assertEquals(1, problems.size());
      assertTrue(problems.get(0).contains("DIAMOND_OR"));
   }

   @Test
   void flagsUnknownActionTypeAndNonDefaultKeys() {
      List<String> problems = new ArrayList<>();
      ConfigValidator.validateAction("jobs.x.actions.chop-wood", "chop-wood", Set.of("OAK_LOG"), problems);
      ConfigValidator.validateAction("jobs.x.actions.enchant-item", "enchant-item", Set.of("DIAMOND_SWORD"), problems);
      assertEquals(2, problems.size());
   }

   @Test
   void flagsBadPerks() {
      List<String> problems = new ArrayList<>();
      ConfigValidator.validatePerks("jobs.miner.perks", List.of(
         Map.of("level", 10, "type", "pay-bonus", "value", 5),
         Map.of("level", "ten", "type", "speed-boost")
      ), problems);
      assertEquals(2, problems.size(), String.join("\n", problems));
   }
}
