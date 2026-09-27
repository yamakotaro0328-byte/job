package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.PlayerJobManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;

public class TradeJobListener implements Listener {
   private static final int RESULT_SLOT = 2;
   private final PlayerJobManager jobs;

   public TradeJobListener(PlayerJobManager jobs) {
      this.jobs = jobs;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getClickedInventory() instanceof MerchantInventory) {
         if (event.getSlot() == 2) {
            if (event.getWhoClicked() instanceof Player player) {
               ItemStack result = event.getCurrentItem();
               if (result != null && !result.getType().isAir()) {
                  this.jobs.reward(player, "merchant", "trade-villager", "default", 1.0);
               }
            }
         }
      }
   }
}
