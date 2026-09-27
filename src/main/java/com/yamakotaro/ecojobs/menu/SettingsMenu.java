package com.yamakotaro.ecojobs.menu;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SettingsMenu extends Menu {
   public SettingsMenu(MenuContext ctx, Player viewer) {
      super(ctx, viewer, 3, MenuContext.line(ctx.raw("settings.title")));
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
      boolean sound = this.ctx.playerJobManager().isSoundEnabled(this.viewer);
      boolean actionBar = this.ctx.playerJobManager().isActionBarEnabled(this.viewer);
      this.button(11, this.toggle(Material.NOTE_BLOCK, "settings.sound", sound), type -> {
         this.ctx.playerJobManager().toggleSoundEnabled(this.viewer);
         this.ctx.click(this.viewer);
         this.redraw();
      });
      this.button(15, this.toggle(Material.NAME_TAG, "settings.actionbar", actionBar), type -> {
         this.ctx.playerJobManager().toggleActionBarEnabled(this.viewer);
         this.ctx.click(this.viewer);
         this.redraw();
      });
      this.set(13, Icon.of(Material.BOOK).name(this.ctx.text("settings.info")).lore(this.ctx.lines("settings.info-lore", Map.of())).build());
      this.backButton(18, () -> new HubMenu(this.ctx, this.viewer).open());
      this.closeButton(22);
   }

   private org.bukkit.inventory.ItemStack toggle(Material material, String key, boolean on) {
      return Icon.of(material)
         .glow(on)
         .name(this.ctx.text(key))
         .lore(this.ctx.lines(key + "-lore", Map.of()))
         .blank()
         .lore(this.ctx.text(on ? "settings.state-on" : "settings.state-off"))
         .build();
   }
}
