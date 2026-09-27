package com.yamakotaro.ecojobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public class JobManager {
   private final EcoJobsPlugin plugin;
   private final Map<String, JobDefinition> jobs = new LinkedHashMap<>();
   private static final List<Integer> DEFAULT_MILESTONE_LEVELS = List.of(10, 25, 50, 75, 100);

   public JobManager(EcoJobsPlugin plugin) {
      this.plugin = plugin;
      this.load();
   }

   public void load() {
      this.jobs.clear();
      ConfigurationSection jobsSection = this.plugin.config().getConfigurationSection("jobs");
      if (jobsSection != null) {
         for (String jobId : jobsSection.getKeys(false)) {
            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
            if (jobSection != null) {
               this.jobs.put(jobId, new JobDefinition(jobId, this.loadActions(jobSection), this.loadPerks(jobSection)));
            }
         }
      }

      ConfigurationSection explorerSection = this.plugin.config().getConfigurationSection("explorer");
      this.jobs.put("explorer", new JobDefinition("explorer", Map.of(), explorerSection != null ? this.loadPerks(explorerSection) : List.of()));
   }

   private List<PerkDefinition> loadPerks(ConfigurationSection section) {
      List<PerkDefinition> perks = new ArrayList<>();

      for (Map<?, ?> raw : section.getMapList("perks")) {
         Object levelObj = raw.get("level");
         Object typeObj = raw.get("type");
         if (levelObj != null && typeObj != null) {
            int level = ((Number)levelObj).intValue();
            String type = String.valueOf(typeObj);
            double value = raw.get("value") instanceof Number number ? number.doubleValue() : 0.0;
            String effect = raw.get("effect") != null ? String.valueOf(raw.get("effect")) : null;
            perks.add(new PerkDefinition(level, type, value, effect));
         }
      }

      return perks;
   }

   private Map<String, Map<String, ActionReward>> loadActions(ConfigurationSection jobSection) {
      Map<String, Map<String, ActionReward>> actionsByType = new HashMap<>();
      ConfigurationSection actionsSection = jobSection.getConfigurationSection("actions");
      if (actionsSection == null) {
         return actionsByType;
      } else {
         for (String actionType : actionsSection.getKeys(false)) {
            ConfigurationSection entriesSection = actionsSection.getConfigurationSection(actionType);
            if (entriesSection != null) {
               Map<String, ActionReward> rewards = new HashMap<>();

               for (String key : entriesSection.getKeys(false)) {
                  ConfigurationSection entry = entriesSection.getConfigurationSection(key);
                  if (entry != null) {
                     rewards.put(
                        key.toUpperCase(),
                        new ActionReward(
                           entry.getDouble("money", 0.0),
                           entry.getDouble("xp", 0.0),
                           entry.getDouble("money-per-level", 0.0),
                           entry.getDouble("xp-per-level", 0.0)
                        )
                     );
                  }
               }

               actionsByType.put(actionType, rewards);
            }
         }

         return actionsByType;
      }
   }

   public JobDefinition get(String jobId) {
      return this.jobs.get(jobId.toLowerCase());
   }

   public Map<String, JobDefinition> all() {
      return this.jobs;
   }

   public int maxConcurrentJobs() {
      return this.plugin.config().getInt("max-concurrent-jobs", 3);
   }

   public double baseXpToLevel() {
      return this.plugin.config().getDouble("leveling.base-xp-to-level", 50.0);
   }

   public double growthExponent() {
      return this.plugin.config().getDouble("leveling.growth-exponent", 1.35);
   }

   public double payBonusPerLevel() {
      return this.plugin.config().getDouble("leveling.pay-bonus-per-level", 0.005);
   }

   public int maxLevel() {
      return this.plugin.config().getInt("leveling.max-level", 100);
   }

   public double prestigeBonusPerPrestige() {
      return this.plugin.config().getDouble("leveling.prestige-bonus-per-prestige", 0.02);
   }

   public double explorerDistancePerMilestone() {
      return this.plugin.config().getDouble("explorer.distance-per-milestone", 250.0);
   }

   public double explorerMoneyPerMilestone() {
      return this.plugin.config().getDouble("explorer.money-per-milestone", 100.0);
   }

   public double explorerXpPerMilestone() {
      return this.plugin.config().getDouble("explorer.xp-per-milestone", 60.0);
   }

   public List<Integer> milestoneLevels() {
      List<Integer> configured = this.plugin.config().getIntegerList("leveling.milestone-levels");
      return configured.isEmpty() ? DEFAULT_MILESTONE_LEVELS : configured;
   }

   public double milestoneBonusMoney() {
      return this.plugin.config().getDouble("leveling.milestone-bonus-money", 100.0);
   }

   public long playerKillCooldownMillis() {
      return Math.max(0L, this.plugin.config().getLong("anti-farm.player-kill-cooldown-minutes", 30L)) * 60000L;
   }
}
