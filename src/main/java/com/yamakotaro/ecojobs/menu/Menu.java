package com.yamakotaro.ecojobs.menu;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for every EcoJobs GUI. Subclasses draw themselves in {@link #render()} and attach a
 * click handler per slot with {@link #button}; {@link MenuListener} cancels every click/drag on a
 * Menu and routes top-inventory clicks here, so no menu can ever leak or accept items.
 */
public abstract class Menu implements InventoryHolder {
   protected final MenuContext ctx;
   protected final Player viewer;
   private final Inventory inventory;
   private final Map<Integer, ClickHandler> handlers = new HashMap<>();

   protected Menu(MenuContext ctx, Player viewer, int rows, Component title) {
      this.ctx = ctx;
      this.viewer = viewer;
      this.inventory = Bukkit.createInventory(this, rows * 9, title);
   }

   protected abstract void render();

   public final void open() {
      this.redraw();
      this.viewer.openInventory(this.inventory);
   }

   /** Re-renders in place (keeps the window open, no cursor reset). */
   public final void redraw() {
      this.handlers.clear();
      this.inventory.clear();
      this.render();
   }

   protected final void set(int slot, ItemStack item) {
      this.inventory.setItem(slot, item);
   }

   protected final void button(int slot, ItemStack item, ClickHandler handler) {
      this.inventory.setItem(slot, item);
      this.handlers.put(slot, handler);
   }

   protected final int size() {
      return this.inventory.getSize();
   }

   protected final void closeButton(int slot) {
      this.button(slot, Icon.of(Material.BARRIER).name(this.ctx.text("close")).build(), type -> this.viewer.closeInventory());
   }

   protected final void backButton(int slot, Runnable action) {
      this.button(slot, Icon.of(Material.ARROW).name(this.ctx.text("back")).build(), type -> {
         this.ctx.click(this.viewer);
         action.run();
      });
   }

   /** Previous/next page arrows; only drawn when there is a page to go to. */
   protected final void pageButtons(int prevSlot, int nextSlot, int page, int pageCount, java.util.function.IntConsumer goTo) {
      if (page > 0) {
         this.button(prevSlot, Icon.of(Material.SPECTRAL_ARROW).name(this.ctx.text("page-prev", Map.of("page", String.valueOf(page), "pages", String.valueOf(pageCount)))).build(), type -> {
            this.ctx.pageTurn(this.viewer);
            goTo.accept(page - 1);
         });
      }

      if (page < pageCount - 1) {
         this.button(nextSlot, Icon.of(Material.SPECTRAL_ARROW).name(this.ctx.text("page-next", Map.of("page", String.valueOf(page + 2), "pages", String.valueOf(pageCount)))).build(), type -> {
            this.ctx.pageTurn(this.viewer);
            goTo.accept(page + 1);
         });
      }
   }

   final void click(int slot, ClickType type) {
      ClickHandler handler = this.handlers.get(slot);
      if (handler != null) {
         handler.onClick(type);
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inventory;
   }

   @FunctionalInterface
   public interface ClickHandler {
      void onClick(ClickType type);
   }
}
