package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.MoneyFormat;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Main menu (/jobs).
 * <pre>
 *  row 0  ▓▓▓▓[profile]▓▓▓▓
 *  row 1  ░ [jobs] [quests] [ranking] ░
 *  row 2  ── ▼ current jobs ▼ ──
 *  row 3  ░  [job] [job] [job]  ░
 *  row 4  ▓ ... [settings][close][boosters] [admin]
 * </pre>
 */
public class HubMenu extends Menu {
   public HubMenu(MenuContext ctx, Player viewer) {
      super(ctx, viewer, 5, MenuContext.line(ctx.raw("hub.title")));
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.LIME_STAINED_GLASS_PANE);
      this.set(4, this.profileItem());

      this.button(11, Icon.of(Material.BOOKSHELF).name(this.ctx.text("hub.jobs")).lore(this.ctx.lines("hub.jobs-lore", Map.of())).build(), type -> {
         this.ctx.click(this.viewer);
         new JobsListMenu(this.ctx, this.viewer, JobsListMenu.Filter.ALL, 0).open();
      });
      this.questButton(13);
      this.button(15, Icon.of(Material.GOLDEN_HELMET).name(this.ctx.text("hub.ranking")).lore(this.ctx.lines("hub.ranking-lore", Map.of())).build(), type -> {
         if (!this.viewer.hasPermission("ecojobs.top")) {
            this.ctx.deny(this.viewer);
            this.viewer.sendMessage(this.ctx.messages().get("general.no-permission", Map.of()));
            return;
         }

         this.ctx.click(this.viewer);
         new LeaderboardPickerMenu(this.ctx, this.viewer).open();
      });

      var divider = Icon.of(Material.LIME_STAINED_GLASS_PANE).name(this.ctx.text("hub.current-jobs")).build();
      for (int slot = 18; slot <= 26; slot++) {
         this.set(slot, divider);
      }

      this.currentJobs();

      this.button(38, Icon.of(Material.COMPARATOR).name(this.ctx.text("hub.settings")).lore(this.ctx.lines("hub.settings-lore", Map.of())).build(), type -> {
         this.ctx.click(this.viewer);
         new SettingsMenu(this.ctx, this.viewer).open();
      });
      this.closeButton(40);
      this.set(42, this.boosterItem());
      if (this.viewer.hasPermission("ecojobs.admin")) {
         this.button(44, Icon.of(Material.COMMAND_BLOCK).name(this.ctx.text("hub.admin")).lore(this.ctx.lines("hub.admin-lore", Map.of())).build(), type -> {
            this.ctx.click(this.viewer);
            new AdminMenu(this.ctx, this.viewer, 0).open();
         });
      }
   }

   private org.bukkit.inventory.ItemStack profileItem() {
      Map<String, PlayerJobProgress> all = this.ctx.playerJobManager().allProgress(this.viewer.getUniqueId());
      int totalLevel = 0;
      int prestige = 0;
      for (PlayerJobProgress progress : all.values()) {
         totalLevel += progress.getLevel();
         prestige += progress.getPrestige();
      }

      int joined = this.ctx.playerJobManager().joinedJobs(this.viewer.getUniqueId()).size();
      Icon icon = Icon.of(Material.PLAYER_HEAD)
         .skull(this.viewer)
         .name(this.ctx.text("hub.profile", Map.of("player", this.viewer.getName())))
         .lore(this.ctx.lines("hub.profile-lore", Map.of(
            "total_level", String.valueOf(totalLevel),
            "prestige", String.valueOf(prestige),
            "joined", String.valueOf(joined),
            "max", String.valueOf(this.ctx.jobManager().maxConcurrentJobs())
         )));
      if (this.ctx.questManager().isEnabled()) {
         int done = this.ctx.questManager().completedCount(this.viewer);
         int total = this.ctx.questManager().questsFor(this.viewer).size();
         icon.lore(this.ctx.text("hub.profile-quests", Map.of("completed", String.valueOf(done), "total", String.valueOf(total))));
      }

      double cap = this.ctx.plugin().config().getDouble("anti-farm.max-money-per-hour", 0.0);
      if (cap > 0.0) {
         double earned = this.ctx.playerJobManager().hourlyEarned(this.viewer.getUniqueId());
         icon.lore(this.ctx.text("hub.profile-hourly", Map.of("earned", MoneyFormat.format(earned), "cap", MoneyFormat.format(cap), "bar", Layout.bar(earned, cap, 10))));
      }

      return icon.build();
   }

   private void questButton(int slot) {
      Icon icon = Icon.of(Material.WRITABLE_BOOK).name(this.ctx.text("hub.quests"));
      if (!this.ctx.questManager().isEnabled()) {
         icon.lore(this.ctx.text("hub.quests-disabled"));
         this.set(slot, icon.build());
         return;
      }

      int done = this.ctx.questManager().completedCount(this.viewer);
      int total = this.ctx.questManager().questsFor(this.viewer).size();
      boolean allDone = total > 0 && done >= total;
      icon.lore(this.ctx.lines("hub.quests-lore", Map.of(
         "completed", String.valueOf(done),
         "total", String.valueOf(total),
         "bar", Layout.bar(done, total, 10),
         "reset", Layout.duration(this.ctx.questManager().millisUntilReset())
      )));
      icon.glow(total > 0 && !allDone).amount(Math.max(1, total - done));
      this.button(slot, icon.build(), type -> {
         this.ctx.click(this.viewer);
         new QuestMenu(this.ctx, this.viewer).open();
      });
   }

   private void currentJobs() {
      Map<String, PlayerJobProgress> joined = this.ctx.playerJobManager().joinedJobs(this.viewer.getUniqueId());
      if (joined.isEmpty()) {
         this.button(31, Icon.of(Material.OAK_SIGN).name(this.ctx.text("hub.no-jobs")).lore(this.ctx.lines("hub.no-jobs-lore", Map.of())).build(), type -> {
            this.ctx.click(this.viewer);
            new JobsListMenu(this.ctx, this.viewer, JobsListMenu.Filter.ALL, 0).open();
         });
         return;
      }

      List<String> ids = new ArrayList<>(joined.keySet());
      List<Integer> slots = Layout.centered(3, ids.size());
      for (int i = 0; i < slots.size(); i++) {
         String jobId = ids.get(i);
         JobDefinition job = this.ctx.jobManager().get(jobId);
         if (job == null) {
            continue;
         }

         this.button(slots.get(i), JobCards.card(this.ctx, this.viewer, job, true), type -> {
            this.ctx.click(this.viewer);
            new JobDetailMenu(this.ctx, this.viewer, jobId, JobDetailMenu.Tab.REWARDS, 0, () -> new HubMenu(this.ctx, this.viewer).open()).open();
         });
      }
   }

   private org.bukkit.inventory.ItemStack boosterItem() {
      Collection<BoosterManager.ActiveBooster> active = this.ctx.boosterManager().active();
      Icon icon = Icon.of(active.isEmpty() ? Material.FIREWORK_STAR : Material.NETHER_STAR).name(this.ctx.text("hub.boosters")).glow(!active.isEmpty());
      if (active.isEmpty()) {
         icon.lore(this.ctx.text("hub.boosters-none"));
      } else {
         for (BoosterManager.ActiveBooster booster : active) {
            String scope = "all".equals(booster.scope()) ? this.ctx.messages().raw("jobs.booster-scope-all", Map.of()) : this.ctx.jobName(booster.scope());
            icon.lore(this.ctx.text("hub.booster-entry", Map.of(
               "scope", scope,
               "money", String.format("%.1f", booster.moneyMultiplier()),
               "xp", String.format("%.1f", booster.xpMultiplier()),
               "time", Layout.duration(booster.expiresAtMillis() - System.currentTimeMillis())
            )));
         }
      }

      return icon.build();
   }
}
