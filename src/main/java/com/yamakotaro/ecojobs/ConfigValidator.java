package com.yamakotaro.ecojobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Checks config.yml for the mistakes that otherwise fail silently: a misspelled material or mob
 * name in a job's action table simply never pays, and nobody notices until a player complains.
 * Pure (no plugin state) so it can be unit-tested; {@link #validate} returns one line per problem.
 */
public final class ConfigValidator {
   static final Set<String> MATERIAL_ACTIONS = Set.of(
      "break-block", "place-block", "harvest-crop", "harvest-tall-plant", "harvest-block", "craft-item", "smelt-item", "catch-fish", "catch-treasure"
   );
   static final Set<String> ENTITY_ACTIONS = Set.of("kill-mob", "kill-mob-ranged", "kill-boss", "breed-entity", "tame-entity", "shear-entity");
   static final Set<String> DEFAULT_ONLY_ACTIONS = Set.of("enchant-item", "kill-player", "trade-villager");
   static final Set<String> FREE_ACTIONS = Set.of("catch-emf-fish");
   static final Set<String> PERK_TYPES = Set.of(PerkDefinition.PAY_BONUS, PerkDefinition.POTION, PerkDefinition.DOUBLE_DROP, PerkDefinition.AUTO_SMELT, PerkDefinition.XP_ORB_BONUS);

   private ConfigValidator() {
   }

   public static List<String> validate(ConfigurationSection config) {
      List<String> problems = new ArrayList<>();
      ConfigurationSection jobs = config.getConfigurationSection("jobs");
      if (jobs != null) {
         for (String jobId : jobs.getKeys(false)) {
            ConfigurationSection job = jobs.getConfigurationSection(jobId);
            if (job == null) {
               continue;
            }

            ConfigurationSection actions = job.getConfigurationSection("actions");
            if (actions != null) {
               for (String actionType : actions.getKeys(false)) {
                  ConfigurationSection entries = actions.getConfigurationSection(actionType);
                  if (entries == null) {
                     continue;
                  }

                  String where = "jobs." + jobId + ".actions." + actionType;
                  validateAction(where, actionType, entries.getKeys(false), problems);
                  for (String key : entries.getKeys(false)) {
                     ConfigurationSection entry = entries.getConfigurationSection(key);
                     if (entry == null) {
                        problems.add(where + "." + key + ": expected { money: .., xp: .. }");
                     } else if (entry.getDouble("money", 0.0) < 0.0 || entry.getDouble("xp", 0.0) < 0.0) {
                        problems.add(where + "." + key + ": money/xp must not be negative");
                     }
                  }
               }
            }

            validatePerks("jobs." + jobId + ".perks", job.getMapList("perks"), problems);
         }
      }

      validatePerks("explorer.perks", config.getMapList("explorer.perks"), problems);
      ConfigurationSection smelt = config.getConfigurationSection("auto-smelt-map");
      if (smelt != null) {
         for (String key : smelt.getKeys(false)) {
            if (!isMaterial(key)) {
               problems.add("auto-smelt-map." + key + ": unknown block");
            } else if (!isMaterial(smelt.getString(key, ""))) {
               problems.add("auto-smelt-map." + key + ": unknown result item '" + smelt.getString(key) + "'");
            }
         }
      }

      ConfigurationSection icons = config.getConfigurationSection("job-icons");
      if (icons != null) {
         for (String key : icons.getKeys(false)) {
            if (!isMaterial(icons.getString(key, ""))) {
               problems.add("job-icons." + key + ": unknown item '" + icons.getString(key) + "'");
            }
         }
      }

      if (config.getDouble("leveling.base-xp-to-level", 1.0) <= 0.0) {
         problems.add("leveling.base-xp-to-level must be > 0");
      }

      if (config.getInt("leveling.max-level", 1) < 1) {
         problems.add("leveling.max-level must be >= 1");
      }

      return problems;
   }

   static void validateAction(String where, String actionType, Set<String> keys, List<String> problems) {
      if (MATERIAL_ACTIONS.contains(actionType)) {
         for (String key : keys) {
            if (!isDefault(key) && !isMaterial(key)) {
               problems.add(where + "." + key + ": unknown material (check the Material enum)");
            }
         }
      } else if (ENTITY_ACTIONS.contains(actionType)) {
         for (String key : keys) {
            if (!isDefault(key) && !isEntity(key)) {
               problems.add(where + "." + key + ": unknown mob (check the EntityType enum)");
            }
         }
      } else if (DEFAULT_ONLY_ACTIONS.contains(actionType)) {
         for (String key : keys) {
            if (!isDefault(key)) {
               problems.add(where + "." + key + ": this action only supports 'default'");
            }
         }
      } else if (!FREE_ACTIONS.contains(actionType)) {
         problems.add(where + ": unknown action type (nothing will ever trigger it)");
      }
   }

   static void validatePerks(String where, List<Map<?, ?>> perks, List<String> problems) {
      int index = 0;
      for (Map<?, ?> perk : perks) {
         String at = where + "[" + index++ + "]";
         Object type = perk.get("type");
         if (!(perk.get("level") instanceof Number)) {
            problems.add(at + ": missing numeric 'level'");
         }

         if (type == null || !PERK_TYPES.contains(String.valueOf(type).toLowerCase(Locale.ROOT))) {
            problems.add(at + ": unknown perk type '" + type + "' (" + String.join(", ", PERK_TYPES) + ")");
         } else if (PerkDefinition.POTION.equals(String.valueOf(type).toLowerCase(Locale.ROOT))) {
            Object effect = perk.get("effect");
            if (effect == null || !isPotionEffect(String.valueOf(effect))) {
               problems.add(at + ": unknown potion effect '" + effect + "' (check the PotionEffectType names)");
            }
         }
      }
   }

   static boolean isDefault(String key) {
      return key.equalsIgnoreCase("default");
   }

   static boolean isMaterial(String key) {
      return key != null && Material.matchMaterial(key) != null;
   }

   static boolean isEntity(String key) {
      NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
      return namespaced != null && Registry.ENTITY_TYPE.get(namespaced) != null;
   }

   @SuppressWarnings("deprecation")
   static boolean isPotionEffect(String name) {
      if (org.bukkit.potion.PotionEffectType.getByName(name) != null) {
         return true;
      }

      NamespacedKey namespaced = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
      return namespaced != null && Registry.EFFECT.get(namespaced) != null;
   }
}
