package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Per-job enable/disable + pay multiplier, and one-click global boosters. */
public class AdminMenu extends Menu {
   private static final List<Integer> SLOTS = Layout.interior(6);
   private static final double STEP = 0.1;
   private static final double QUICK_MULTIPLIER = 2.0;
   private static final long QUICK_MINUTES = 30L;
   private final int page;

   public AdminMenu(MenuContext ctx, Player viewer, int page) {
      super(ctx, viewer, 6, MenuContext.line(ctx.raw("admin.title")));
      this.page = page;
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.RED_STAINED_GLASS_PANE);
      if (!this.viewer.hasPermission("ecojobs.admin")) {
         this.closeButton(49);
         return;
      }

      this.set(4, Icon.of(Material.COMMAND_BLOCK).name(this.ctx.text("admin.header")).lore(this.ctx.lines("admin.header-lore", Map.of())).build());
      List<JobDefinition> jobs = new ArrayList<>(this.ctx.jobManager().all().values());
      int pages = Math.max(1, (jobs.size() + SLOTS.size() - 1) / SLOTS.size());
      int current = Math.max(0, Math.min(this.page, pages - 1));
      for (int i = 0; i < SLOTS.size() && current * SLOTS.size() + i < jobs.size(); i++) {
         String jobId = jobs.get(current * SLOTS.size() + i).getId();
         this.button(SLOTS.get(i), this.jobItem(jobId), type -> this.onJobClick(jobId, type));
      }

      this.backButton(45, () -> new HubMenu(this.ctx, this.viewer).open());
      this.button(47, Icon.of(Material.NETHER_STAR).name(this.ctx.text("admin.booster-start")).lore(this.ctx.lines("admin.booster-start-lore", Map.of(
         "multiplier", String.format("%.0f", QUICK_MULTIPLIER),
         "minutes", String.valueOf(QUICK_MINUTES)
      ))).build(), type -> this.startBooster());
      this.pageButtons(48, 50, current, pages, p -> new AdminMenu(this.ctx, this.viewer, p).open());
      this.closeButton(49);
      int active = this.ctx.boosterManager().active().size();
      this.button(51, Icon.of(Material.TNT).name(this.ctx.text("admin.booster-stop")).lore(this.ctx.lines("admin.booster-stop-lore", Map.of("count", String.valueOf(active)))).build(), type -> {
         if (this.ctx.boosterManager().stopAll() > 0) {
            Bukkit.getServer().sendMessage(this.ctx.messages().get("jobs.booster-stopped-all", Map.of("player", this.viewer.getName())));
         }

         this.ctx.click(this.viewer);
         this.redraw();
      });
   }

   private org.bukkit.inventory.ItemStack jobItem(String jobId) {
      boolean enabled = this.ctx.jobOverrides().isEnabled(jobId);
      double multiplier = this.ctx.jobOverrides().payMultiplier(jobId);
      Icon icon = Icon.of(enabled ? JobIcons.of(this.ctx.plugin(), jobId) : Material.GRAY_DYE)
         .name(this.ctx.text("card.name", Map.of("job", this.ctx.jobName(jobId))))
         .lore(this.ctx.text(enabled ? "admin.enabled" : "admin.disabled"))
         .lore(this.ctx.text("admin.multiplier", Map.of("multiplier", String.format("%.1f", multiplier))));
      BoosterManager.ActiveBooster booster = this.ctx.boosterManager().getActiveBooster(jobId);
      if (booster != null) {
         icon.lore(this.ctx.text("admin.job-booster", Map.of(
            "money", String.format("%.1f", booster.moneyMultiplier()),
            "xp", String.format("%.1f", booster.xpMultiplier()),
            "time", Layout.duration(booster.expiresAtMillis() - System.currentTimeMillis())
         )));
      }

      return icon.blank().lore(this.ctx.lines("admin.controls", Map.of())).build();
   }

   private void onJobClick(String jobId, ClickType type) {
      if (type == ClickType.SHIFT_LEFT) {
         this.ctx.click(this.viewer);
         new LeaderboardMenu(this.ctx, this.viewer, jobId, () -> new AdminMenu(this.ctx, this.viewer, this.page).open()).open();
         return;
      }

      if (type == ClickType.SHIFT_RIGHT) {
         this.ctx.jobOverrides().adjustPayMultiplier(jobId, -STEP);
      } else if (type.isRightClick()) {
         this.ctx.jobOverrides().adjustPayMultiplier(jobId, STEP);
      } else if (type.isLeftClick()) {
         this.ctx.jobOverrides().setEnabled(jobId, !this.ctx.jobOverrides().isEnabled(jobId));
      } else {
         return;
      }

      this.ctx.click(this.viewer);
      this.redraw();
   }

   private void startBooster() {
      this.ctx.boosterManager().start("all", QUICK_MULTIPLIER, QUICK_MULTIPLIER, TimeUnit.MINUTES.toMillis(QUICK_MINUTES), this.viewer.getName());
      Bukkit.getServer().sendMessage(this.ctx.messages().get("jobs.booster-started", Map.of(
         "scope", this.ctx.messages().raw("jobs.booster-scope-all", Map.of()),
         "money", String.format("%.2f", QUICK_MULTIPLIER),
         "xp", String.format("%.2f", QUICK_MULTIPLIER),
         "minutes", String.valueOf(QUICK_MINUTES),
         "player", this.viewer.getName()
      )));
      this.ctx.success(this.viewer);
      this.redraw();
   }
}
