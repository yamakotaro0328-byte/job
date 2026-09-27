package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.MoneyFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MenuUtil {
   private MenuUtil() {
   }

   static ItemStack closeItem(Messages messages) {
      ItemStack stack = new ItemStack(Material.BARRIER);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(messages.get("menu.close", Map.of()));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   static ItemStack backItem(Messages messages) {
      ItemStack stack = new ItemStack(Material.ARROW);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(messages.get("menu.back", Map.of()));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   static ItemStack prevPageItem(Messages messages) {
      ItemStack stack = new ItemStack(Material.SPECTRAL_ARROW);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(messages.get("menu.page-prev", Map.of()));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   static ItemStack nextPageItem(Messages messages) {
      ItemStack stack = new ItemStack(Material.SPECTRAL_ARROW);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(messages.get("menu.page-next", Map.of()));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   static void fillBorder(Inventory inventory) {
      ItemStack filler = fillerItem();
      int lastRow = inventory.getSize() / 9 - 1;

      for (int slot = 0; slot < inventory.getSize(); slot++) {
         int row = slot / 9;
         int col = slot % 9;
         if (row == 0 || row == lastRow || col == 0 || col == 8) {
            inventory.setItem(slot, filler);
         }
      }
   }

   private static ItemStack fillerItem() {
      ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.empty());
         stack.setItemMeta(meta);
      }

      return stack;
   }

   static List<Integer> interiorSlots() {
      List<Integer> slots = new ArrayList<>();

      for (int row = 1; row <= 4; row++) {
         for (int col = 1; col <= 7; col++) {
            slots.add(row * 9 + col);
         }
      }

      return slots;
   }

   static String formatReward(double flat, double perLevel) {
      return perLevel > 0.0 ? String.format("%.2f/enchant-level", perLevel) : String.format("%.2f", flat);
   }

   static String formatMoneyReward(double flat, double perLevel) {
      return perLevel > 0.0 ? MoneyFormat.format(perLevel) + "/enchant-level" : MoneyFormat.format(flat);
   }
}
