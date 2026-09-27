package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.MoneyFormat;
import com.yamakotaro.ecojobs.PerkDefinition;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** The job "card" item shown in the hub and the jobs list. */
final class JobCards {
   private JobCards() {
   }

   static ItemStack card(MenuContext ctx, Player viewer, JobDefinition job, boolean hubStyle) {
      String jobId = job.getId();
      PlayerJobProgress progress = ctx.playerJobManager().allProgress(viewer.getUniqueId()).get(jobId);
      boolean joined = ctx.playerJobManager().isJoined(viewer.getUniqueId(), jobId);
      boolean enabled = ctx.jobOverrides().isEnabled(jobId);
      int maxLevel = ctx.jobManager().maxLevel();

      String status = !enabled ? "status-disabled" : joined ? "status-joined" : progress != null ? "status-paused" : "status-not-joined";
      Icon icon = Icon.of(JobIcons.of(ctx.plugin(), jobId))
         .name(ctx.text("card.name", Map.of("job", ctx.jobName(jobId))))
         .lore(ctx.text("card." + status))
         .glow(joined);

      if (progress != null) {
         boolean maxed = progress.getLevel() >= maxLevel;
         double next = ctx.playerJobManager().xpToNextLevel(progress.getLevel());
         icon.blank();
         icon.lore(ctx.text("card.level", Map.of(
            "level", String.valueOf(progress.getLevel()),
            "max", String.valueOf(maxLevel),
            "prestige", progress.getPrestige() > 0 ? ctx.raw("card.prestige-badge", Map.of("prestige", String.valueOf(progress.getPrestige()))) : ""
         )));
         icon.lore(ctx.text("card.title", Map.of("title", ctx.playerJobManager().titleFor(jobId, progress.getLevel()))));
         icon.lore(ctx.text("card.earned", Map.of("earned", MoneyFormat.format(progress.getEarned()), "actions", String.format("%,d", progress.getActions()))));
         if (maxed) {
            icon.lore(ctx.text("card.maxed"));
         } else {
            icon.lore(ctx.lines("card.xp", Map.of(
               "bar", Layout.bar(progress.getXp(), next, 12),
               "percent", String.valueOf(Layout.percent(progress.getXp(), next)),
               "xp", String.format("%.0f", progress.getXp()),
               "next", String.format("%.0f", next)
            )));
         }
      } else {
         icon.blank();
      }

      icon.lore(ctx.text("card.multiplier", Map.of("multiplier", String.format("%.2f", ctx.playerJobManager().moneyMultiplier(job, progress)))));
      PerkDefinition nextPerk = nextPerk(ctx, job, progress);
      if (nextPerk != null) {
         int effective = progress == null ? 0 : ctx.perkManager().effectiveLevel(progress);
         icon.lore(ctx.text("card.next-perk", Map.of("level", String.valueOf(nextPerk.level()), "away", String.valueOf(nextPerk.level() - effective))));
      }

      icon.blank();
      if (hubStyle) {
         icon.lore(ctx.text("card.hint-details"));
      } else {
         if (enabled) {
            icon.lore(ctx.text(joined ? "card.hint-leave" : progress != null ? "card.hint-resume" : "card.hint-join"));
         }

         icon.lore(ctx.text("card.hint-details-right"));
         if (viewer.hasPermission("ecojobs.top")) {
            icon.lore(ctx.text("card.hint-ranking"));
         }
      }

      return icon.build();
   }

   static PerkDefinition nextPerk(MenuContext ctx, JobDefinition job, PlayerJobProgress progress) {
      int effective = progress == null ? 0 : ctx.perkManager().effectiveLevel(progress);
      PerkDefinition best = null;
      for (PerkDefinition perk : ctx.perkManager().allPerks(job)) {
         if (perk.level() > effective && (best == null || perk.level() < best.level())) {
            best = perk;
         }
      }

      return best;
   }
}
