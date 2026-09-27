package com.yamakotaro.ecojobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PerkManager {
   private final EcoJobsPlugin plugin;
   private final JobManager jobManager;
   private final Map<Material, Material> smeltMap = new EnumMap<>(Material.class);
   private boolean badEffectWarned;

   public PerkManager(EcoJobsPlugin plugin, JobManager jobManager) {
      this.plugin = plugin;
      this.jobManager = jobManager;
      this.load();
   }

   public void load() {
      this.smeltMap.clear();
      ConfigurationSection section = this.plugin.config().getConfigurationSection("auto-smelt-map");
      if (section != null) {
         for (String key : section.getKeys(false)) {
            Material ore = Material.matchMaterial(key);
            Material result = Material.matchMaterial(section.getString(key, ""));
            if (ore != null && result != null) {
               this.smeltMap.put(ore, result);
            }
         }
      }
   }

   public int effectiveLevel(PlayerJobProgress progress) {
      return progress.getLevel() + progress.getPrestige() * this.jobManager.maxLevel();
   }

   private List<PerkDefinition> unlockedOfType(JobDefinition job, int effectiveLevel, String type) {
      List<PerkDefinition> result = new ArrayList<>();

      for (PerkDefinition perk : job.getPerks()) {
         if (perk.type().equalsIgnoreCase(type) && effectiveLevel >= perk.level()) {
            result.add(perk);
         }
      }

      return result;
   }

   public double payBonusMultiplier(JobDefinition job, int effectiveLevel) {
      double total = 0.0;

      for (PerkDefinition perk : this.unlockedOfType(job, effectiveLevel, "pay-bonus")) {
         total += perk.value() / 100.0;
      }

      return total;
   }

   public int xpOrbBonus(JobDefinition job, int effectiveLevel) {
      int total = 0;

      for (PerkDefinition perk : this.unlockedOfType(job, effectiveLevel, "xp-orb-bonus")) {
         total += (int)perk.value();
      }

      return total;
   }

   public void applyPotionPerks(Player player, JobDefinition job, int effectiveLevel, int durationTicks) {
      for (PerkDefinition perk : this.unlockedOfType(job, effectiveLevel, "potion")) {
         PotionEffectType type = perk.effect() != null ? PotionEffectType.getByName(perk.effect()) : null;
         if (type == null) {
            if (!this.badEffectWarned) {
               this.badEffectWarned = true;
               this.plugin
                  .getLogger()
                  .log(
                     Level.WARNING,
                     "Unknown potion effect ''{0}'' in a perk for job ''{1}'' - skipping (further warnings suppressed).",
                     new Object[]{perk.effect(), job.getId()}
                  );
            }
         } else {
            int amplifier = Math.max(0, (int)perk.value() - 1);
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false));
         }
      }
   }

   public boolean rollDoubleDrop(JobDefinition job, int effectiveLevel) {
      for (PerkDefinition perk : this.unlockedOfType(job, effectiveLevel, "double-drop")) {
         if (ThreadLocalRandom.current().nextDouble(100.0) < perk.value()) {
            return true;
         }
      }

      return false;
   }

   public boolean hasAutoSmelt(JobDefinition job, int effectiveLevel) {
      return !this.unlockedOfType(job, effectiveLevel, "auto-smelt").isEmpty();
   }

   public Material smeltedResult(Material ore) {
      return this.smeltMap.get(ore);
   }

   public List<PerkDefinition> allPerks(JobDefinition job) {
      List<PerkDefinition> sorted = new ArrayList<>(job.getPerks());
      sorted.sort(Comparator.comparingInt(PerkDefinition::level));
      return sorted;
   }
}
