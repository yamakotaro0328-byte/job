package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.Messages;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PrestigeConfirmMenuHolder implements InventoryHolder {
   public static final int CONFIRM_SLOT = 11;
   public static final int CANCEL_SLOT = 15;
   private final Messages messages;
   private final String jobId;
   private final Inventory inventory;

   public PrestigeConfirmMenuHolder(Messages messages, String jobId) {
      this.messages = messages;
      this.jobId = jobId;
      this.inventory = Bukkit.createInventory(this, 27, messages.get("menu.prestige-confirm-title", Map.of("job", messages.jobName(jobId))));
   }

   public String getJobId() {
      return this.jobId;
   }

   public void render() {
      this.inventory.clear();
      this.inventory.setItem(11, this.item(Material.LIME_CONCRETE, "menu.prestige-confirm-yes"));
      this.inventory.setItem(15, this.item(Material.RED_CONCRETE, "menu.prestige-confirm-no"));
   }

   private ItemStack item(Material material, String titleKey) {
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get(titleKey, Map.of()));
         meta.lore(List.of(this.messages.get("menu.prestige-confirm-lore", Map.of())));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
