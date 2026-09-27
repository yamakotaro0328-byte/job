package com.yamakotaro.ecojobs.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** Fluent GUI item builder: no italics, no vanilla tooltip noise (attack damage, potion effects...). */
public final class Icon {
   private final Material material;
   private Component name;
   private final List<Component> lore = new ArrayList<>();
   private boolean glow;
   private int amount = 1;
   private OfflinePlayer skullOwner;
   private boolean hideTooltip;

   private Icon(Material material) {
      this.material = material;
   }

   public static Icon of(Material material) {
      return new Icon(material == null || !material.isItem() ? Material.PAPER : material);
   }

   /** A pane with no tooltip at all, for frames and dividers. */
   public static ItemStack pane(Material material) {
      Icon icon = of(material);
      icon.hideTooltip = true;
      icon.name = Component.empty();
      return icon.build();
   }

   public Icon name(Component name) {
      this.name = name;
      return this;
   }

   public Icon lore(Component line) {
      this.lore.add(line);
      return this;
   }

   public Icon lore(List<Component> lines) {
      this.lore.addAll(lines);
      return this;
   }

   public Icon blank() {
      this.lore.add(Component.empty());
      return this;
   }

   public Icon glow(boolean glow) {
      this.glow = glow;
      return this;
   }

   public Icon amount(int amount) {
      this.amount = Math.max(1, Math.min(99, amount));
      return this;
   }

   public Icon skull(OfflinePlayer owner) {
      this.skullOwner = owner;
      return this;
   }

   public ItemStack build() {
      ItemStack stack = new ItemStack(this.material, this.amount);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         if (this.name != null) {
            meta.displayName(this.name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
         }

         if (!this.lore.isEmpty()) {
            List<Component> fixed = new ArrayList<>(this.lore.size());
            for (Component line : this.lore) {
               fixed.add(line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }

            meta.lore(fixed);
         }

         if (this.glow) {
            meta.setEnchantmentGlintOverride(true);
         }

         if (this.skullOwner != null && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(this.skullOwner);
         }

         meta.addItemFlags(ItemFlag.values());
         stack.setItemMeta(meta);
      }

      try {
         TooltipDisplay.Builder display = TooltipDisplay.tooltipDisplay().hideTooltip(this.hideTooltip);
         display.addHiddenComponents(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.STORED_ENCHANTMENTS,
            DataComponentTypes.POTION_CONTENTS,
            DataComponentTypes.UNBREAKABLE,
            DataComponentTypes.DYED_COLOR,
            DataComponentTypes.TRIM,
            DataComponentTypes.JUKEBOX_PLAYABLE,
            DataComponentTypes.BANNER_PATTERNS,
            DataComponentTypes.FIREWORK_EXPLOSION,
            DataComponentTypes.CONTAINER_LOOT,
            DataComponentTypes.BUNDLE_CONTENTS
         );
         stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, display.build());
      } catch (RuntimeException | LinkageError var6) {
         // Older/newer server without this data component API: the ItemFlags above still apply.
      }

      return stack;
   }
}
