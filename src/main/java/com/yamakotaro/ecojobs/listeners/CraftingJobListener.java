package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.PlayerJobManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingJobListener implements Listener {
   private final PlayerJobManager jobs;

   public CraftingJobListener(PlayerJobManager jobs) {
      this.jobs = jobs;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onFurnaceExtract(FurnaceExtractEvent event) {
      this.jobs.reward(event.getPlayer(), "smelter", "smelt-item", event.getItemType().name(), event.getItemAmount());
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onCraftItem(CraftItemEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         ItemStack result = event.getRecipe().getResult();
         if (!result.getType().isAir()) {
            this.jobs.reward(player, "crafter", "craft-item", result.getType().name(), result.getAmount());
         }
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEnchantItem(EnchantItemEvent event) {
      this.jobs.reward(event.getEnchanter(), "enchanter", "enchant-item", "default", event.getExpLevelCost());
   }
}
