package com.yamakotaro.ecojobs;

import java.util.List;
import java.util.Map;

public class JobDefinition {
   private final String id;
   private final Map<String, Map<String, ActionReward>> actionsByType;
   private final List<PerkDefinition> perks;

   public JobDefinition(String id, Map<String, Map<String, ActionReward>> actionsByType, List<PerkDefinition> perks) {
      this.id = id;
      this.actionsByType = actionsByType;
      this.perks = perks;
   }

   public String getId() {
      return this.id;
   }

   public ActionReward getReward(String actionType, String key) {
      Map<String, ActionReward> rewards = this.actionsByType.get(actionType);
      if (rewards == null) {
         return null;
      } else {
         ActionReward reward = rewards.get(key.toUpperCase());
         return reward != null ? reward : rewards.get("DEFAULT");
      }
   }

   public Map<String, Map<String, ActionReward>> getActionsByType() {
      return this.actionsByType;
   }

   public List<PerkDefinition> getPerks() {
      return this.perks;
   }
}
