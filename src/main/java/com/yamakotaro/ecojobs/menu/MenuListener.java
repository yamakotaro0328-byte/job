package com.yamakotaro.ecojobs.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder(false) instanceof Menu menu) {
         // Cancel everything (including shift-clicks and number-key swaps from the player's own
         // inventory) so items can't be moved into or out of a menu.
         event.setCancelled(true);
         if (event.getClickedInventory() == event.getInventory()) {
            menu.click(event.getSlot(), event.getClick());
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder(false) instanceof Menu) {
         event.setCancelled(true);
      }
   }
}
