package com.yamakotaro.ecojobs.menu;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Slot maths and frame drawing shared by all menus. */
final class Layout {
   static final Material FRAME = Material.BLACK_STAINED_GLASS_PANE;
   static final Material SIDE = Material.GRAY_STAINED_GLASS_PANE;

   private Layout() {
   }

   /** Top + bottom bar in black, side columns in gray, accent-coloured corners. */
   static void frame(Menu menu, Material accent) {
      int size = menu.size();
      int lastRow = size / 9 - 1;
      ItemStack frame = Icon.pane(FRAME);
      ItemStack side = Icon.pane(SIDE);
      ItemStack corner = Icon.pane(accent);

      for (int slot = 0; slot < size; slot++) {
         int row = slot / 9;
         int col = slot % 9;
         if (row == 0 || row == lastRow) {
            menu.set(slot, (col == 0 || col == 8) ? corner : frame);
         } else if (col == 0 || col == 8) {
            menu.set(slot, side);
         }
      }
   }

   /** The 7-wide interior of rows 1..rows-2. */
   static List<Integer> interior(int rows) {
      List<Integer> slots = new ArrayList<>();
      for (int row = 1; row < rows - 1; row++) {
         for (int col = 1; col <= 7; col++) {
            slots.add(row * 9 + col);
         }
      }

      return slots;
   }

   /** Up to 7 slots centred in the given row, e.g. 3 items in row 3 -> 29, 31, 33 (spaced) or packed if many. */
   static List<Integer> centered(int row, int count) {
      List<Integer> slots = new ArrayList<>();
      if (count <= 0) {
         return slots;
      }

      if (count <= 4) {
         // Spaced out: every other column, centred.
         int start = 4 - (count - 1);
         for (int i = 0; i < count; i++) {
            slots.add(row * 9 + start + i * 2);
         }
      } else {
         int n = Math.min(7, count);
         int start = 1 + (7 - n) / 2;
         for (int i = 0; i < n; i++) {
            slots.add(row * 9 + start + i);
         }
      }

      return slots;
   }

   static String bar(double current, double max, int length) {
      double ratio = max > 0.0 ? Math.max(0.0, Math.min(1.0, current / max)) : 1.0;
      int filled = (int)Math.round(ratio * length);
      return "&a" + "▌".repeat(filled) + "&8" + "▌".repeat(length - filled);
   }

   static int percent(double current, double max) {
      return max > 0.0 ? (int)Math.round(Math.max(0.0, Math.min(1.0, current / max)) * 100.0) : 100;
   }

   static String duration(long millis) {
      long minutes = Math.max(0L, millis / 60000L);
      long hours = minutes / 60L;
      return hours > 0L ? hours + "h " + minutes % 60L + "m" : minutes + "m";
   }

   static String prettify(String key) {
      StringBuilder result = new StringBuilder();
      for (String part : key.toLowerCase().split("_")) {
         if (!part.isEmpty()) {
            if (!result.isEmpty()) {
               result.append(' ');
            }

            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
         }
      }

      return result.toString();
   }
}
