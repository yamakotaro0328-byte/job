package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.EcoJobsPlugin;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

/** Icons for jobs (overridable via job-icons in config.yml) and client-translated names for action keys. */
public final class JobIcons {
   private static final Map<String, Material> DEFAULTS = Map.ofEntries(
      Map.entry("miner", Material.IRON_PICKAXE),
      Map.entry("digger", Material.IRON_SHOVEL),
      Map.entry("woodcutter", Material.DIAMOND_AXE),
      Map.entry("farmer", Material.WHEAT),
      Map.entry("builder", Material.BRICKS),
      Map.entry("fisherman", Material.FISHING_ROD),
      Map.entry("treasurehunter", Material.CHEST),
      Map.entry("hunter", Material.IRON_SWORD),
      Map.entry("archer", Material.BOW),
      Map.entry("slayer", Material.NETHERITE_SWORD),
      Map.entry("warrior", Material.SHIELD),
      Map.entry("breeder", Material.WHEAT_SEEDS),
      Map.entry("tamer", Material.BONE),
      Map.entry("shearer", Material.SHEARS),
      Map.entry("beekeeper", Material.HONEYCOMB),
      Map.entry("enchanter", Material.ENCHANTING_TABLE),
      Map.entry("smelter", Material.BLAST_FURNACE),
      Map.entry("crafter", Material.CRAFTING_TABLE),
      Map.entry("merchant", Material.EMERALD),
      Map.entry("explorer", Material.COMPASS)
   );

   private JobIcons() {
   }

   public static Material of(EcoJobsPlugin plugin, String jobId) {
      String configured = plugin.config().getString("job-icons." + jobId);
      if (configured != null) {
         Material material = Material.matchMaterial(configured);
         if (material != null && material.isItem()) {
            return material;
         }
      }

      return DEFAULTS.getOrDefault(jobId, Material.PAPER);
   }

   /** Item to show for an action key (a block/item name, or a mob -> its spawn egg). */
   public static Material forKey(String key) {
      Material direct = Material.matchMaterial(key);
      if (direct != null && direct.isItem()) {
         return direct;
      }

      Material egg = Material.matchMaterial(key + "_SPAWN_EGG");
      if (egg != null) {
         return egg;
      }

      // Crops/plants whose block form isn't an item (e.g. CARROTS, POTATOES, SWEET_BERRY_BUSH).
      if (direct != null) {
         Material item = switch (direct.name()) {
            case "CARROTS" -> Material.CARROT;
            case "POTATOES" -> Material.POTATO;
            case "BEETROOTS" -> Material.BEETROOT;
            case "SWEET_BERRY_BUSH" -> Material.SWEET_BERRIES;
            case "COCOA" -> Material.COCOA_BEANS;
            case "CAVE_VINES", "CAVE_VINES_PLANT" -> Material.GLOW_BERRIES;
            default -> null;
         };
         if (item != null) {
            return item;
         }
      }

      return Material.PAPER;
   }

   /**
    * Name of an action key as a translatable component, so each player sees it in their own client
    * language ("Diamond Ore" / "ダイヤモンド鉱石"). Falls back to a prettified key.
    */
   public static Component displayName(String key, String anyLabel) {
      if (key.equalsIgnoreCase("default")) {
         return Component.text(anyLabel);
      }

      Material material = Material.matchMaterial(key);
      if (material != null) {
         return Component.translatable(material.translationKey());
      }

      EntityType entity = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
      if (entity != null) {
         return Component.translatable(entity.translationKey());
      }

      return Component.text(Layout.prettify(key));
   }
}
