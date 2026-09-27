package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.Messages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HubMenuHolder implements InventoryHolder {
   public static final int MY_JOBS_SLOT = 11;
   public static final int LEADERBOARDS_SLOT = 13;
   public static final int SETTINGS_SLOT = 15;
   public static final int BOOSTER_SLOT = 17;
   public static final int ADMIN_SLOT = 22;
   public static final int CLOSE_SLOT = 26;
   private final Messages messages;
   private final Inventory inventory;

   public HubMenuHolder(Messages messages) {
      this.messages = messages;
      this.inventory = Bukkit.createInventory(this, 27, messages.get("menu.hub-title", Map.of()));
   }

   public void render(boolean showAdmin, BoosterManager boosterManager) {
      this.inventory.clear();
      this.inventory.setItem(11, this.item(Material.WRITABLE_BOOK, "menu.hub-my-jobs", "menu.hub-my-jobs-lore"));
      this.inventory.setItem(13, this.item(Material.GOLD_INGOT, "menu.hub-leaderboards", "menu.hub-leaderboards-lore"));
      this.inventory.setItem(15, this.item(Material.COMPARATOR, "menu.hub-settings", "menu.hub-settings-lore"));
      this.inventory.setItem(17, this.boosterItem(boosterManager));
      if (showAdmin) {
         this.inventory.setItem(22, this.item(Material.COMMAND_BLOCK, "menu.hub-admin", "menu.hub-admin-lore"));
      }

      this.inventory.setItem(26, MenuUtil.closeItem(this.messages));
   }

   private ItemStack item(Material material, String titleKey, String loreKey) {
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get(titleKey, Map.of()));
         meta.lore(List.of(this.messages.get(loreKey, Map.of())));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private ItemStack boosterItem(BoosterManager boosterManager) {
      ItemStack stack = new ItemStack(Material.NETHER_STAR);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get("menu.hub-boosters", Map.of()));
         List<Component> lore = new ArrayList<>();
         Collection<BoosterManager.ActiveBooster> active = boosterManager.active();
         if (active.isEmpty()) {
            lore.add(this.messages.get("jobs.booster-list-empty", Map.of()));
         } else {
            for (BoosterManager.ActiveBooster booster : active) {
               long minutesLeft = Math.max(0L, (booster.expiresAtMillis() - System.currentTimeMillis()) / 60000L);
               String scopeLabel = "all".equals(booster.scope())
                  ? this.messages.raw("jobs.booster-scope-all", Map.of())
                  : this.messages.jobName(booster.scope());
               lore.add(
                  this.messages
                     .get(
                        "jobs.booster-list-entry",
                        Map.of(
                           "scope",
                           scopeLabel,
                           "money",
                           String.format("%.2f", booster.moneyMultiplier()),
                           "xp",
                           String.format("%.2f", booster.xpMultiplier()),
                           "minutes",
                           String.valueOf(minutesLeft)
                        )
                     )
               );
            }
         }

         meta.lore(lore);
         stack.setItemMeta(meta);
      }

      return stack;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
