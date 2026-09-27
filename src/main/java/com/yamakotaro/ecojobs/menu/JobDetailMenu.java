package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.ActionReward;
import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.MoneyFormat;
import com.yamakotaro.ecojobs.PerkDefinition;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** One job in depth: a "rewards" tab (what pays, and what it pays *you*) and a "perks" tab. */
public class JobDetailMenu extends Menu {
   private static final List<Integer> SLOTS = Layout.interior(6);
   private final String jobId;
   private final Tab tab;
   private final int page;
   private final Runnable back;

   public JobDetailMenu(MenuContext ctx, Player viewer, String jobId, Tab tab, int page, Runnable back) {
      super(ctx, viewer, 6, MenuContext.line(ctx.raw("detail.title", Map.of("job", ctx.jobName(jobId)))));
      this.jobId = jobId;
      this.tab = tab;
      this.page = page;
      this.back = back;
   }

   @Override
   protected void render() {
      Layout.frame(this, this.tab == Tab.REWARDS ? Material.YELLOW_STAINED_GLASS_PANE : Material.PURPLE_STAINED_GLASS_PANE);
      JobDefinition job = this.ctx.jobManager().get(this.jobId);
      if (job == null) {
         this.backButton(45, this.back);
         this.closeButton(49);
         return;
      }

      PlayerJobProgress progress = this.ctx.playerJobManager().allProgress(this.viewer.getUniqueId()).get(this.jobId);
      this.set(4, this.header(job, progress));
      this.tabButton(2, Tab.REWARDS, Material.GOLD_INGOT);
      this.tabButton(6, Tab.PERKS, Material.NETHER_STAR);

      List<ItemStack> entries = this.tab == Tab.REWARDS ? this.rewardItems(job, progress) : this.perkItems(job, progress);
      int pages = Math.max(1, (entries.size() + SLOTS.size() - 1) / SLOTS.size());
      int current = Math.max(0, Math.min(this.page, pages - 1));
      for (int i = 0; i < SLOTS.size() && current * SLOTS.size() + i < entries.size(); i++) {
         this.set(SLOTS.get(i), entries.get(current * SLOTS.size() + i));
      }

      if (entries.isEmpty()) {
         this.set(22, Icon.of(Material.STRUCTURE_VOID).name(this.ctx.text(this.tab == Tab.REWARDS ? "detail.no-rewards" : "detail.no-perks")).build());
      }

      this.backButton(45, this.back);
      this.joinButton(46);
      this.pageButtons(48, 50, current, pages, p -> new JobDetailMenu(this.ctx, this.viewer, this.jobId, this.tab, p, this.back).open());
      this.closeButton(49);
      if (this.viewer.hasPermission("ecojobs.top")) {
         this.button(52, Icon.of(Material.GOLDEN_HELMET).name(this.ctx.text("detail.ranking")).lore(this.ctx.lines("detail.ranking-lore", this.rankPlaceholders())).build(), type -> {
            this.ctx.click(this.viewer);
            new LeaderboardMenu(this.ctx, this.viewer, this.jobId, () -> new JobDetailMenu(this.ctx, this.viewer, this.jobId, this.tab, this.page, this.back).open()).open();
         });
      }

      this.prestigeButton(53, progress);
   }

   private Map<String, String> rankPlaceholders() {
      int rank = this.ctx.playerJobManager().rankOf(this.jobId, this.viewer.getUniqueId());
      return Map.of("rank", rank > 0 ? "#" + rank : "-", "total", String.valueOf(this.ctx.playerJobManager().rankedCount(this.jobId)));
   }

   private ItemStack header(JobDefinition job, PlayerJobProgress progress) {
      boolean joined = this.ctx.playerJobManager().isJoined(this.viewer.getUniqueId(), this.jobId);
      Icon icon = Icon.of(JobIcons.of(this.ctx.plugin(), this.jobId)).name(this.ctx.text("card.name", Map.of("job", this.ctx.jobName(this.jobId)))).glow(joined);
      icon.lore(this.ctx.text(!this.ctx.jobOverrides().isEnabled(this.jobId) ? "card.status-disabled" : joined ? "card.status-joined" : progress != null ? "card.status-paused" : "card.status-not-joined"));
      if (progress != null) {
         double next = this.ctx.playerJobManager().xpToNextLevel(progress.getLevel());
         icon.blank().lore(this.ctx.text("card.level", Map.of(
            "level", String.valueOf(progress.getLevel()),
            "max", String.valueOf(this.ctx.jobManager().maxLevel()),
            "prestige", progress.getPrestige() > 0 ? this.ctx.raw("card.prestige-badge", Map.of("prestige", String.valueOf(progress.getPrestige()))) : ""
         )));
         if (progress.getLevel() >= this.ctx.jobManager().maxLevel()) {
            icon.lore(this.ctx.text("card.maxed"));
         } else {
            icon.lore(this.ctx.lines("card.xp", Map.of(
               "bar", Layout.bar(progress.getXp(), next, 12),
               "percent", String.valueOf(Layout.percent(progress.getXp(), next)),
               "xp", String.format("%.0f", progress.getXp()),
               "next", String.format("%.0f", next)
            )));
         }
      }

      icon.lore(this.ctx.text("card.multiplier", Map.of("multiplier", String.format("%.2f", this.ctx.playerJobManager().moneyMultiplier(job, progress)))));
      return icon.build();
   }

   private void tabButton(int slot, Tab target, Material material) {
      boolean selected = this.tab == target;
      String key = target == Tab.REWARDS ? "detail.tab-rewards" : "detail.tab-perks";
      Icon icon = Icon.of(material).name(this.ctx.text(key)).glow(selected).lore(this.ctx.text(selected ? "detail.tab-selected" : "detail.tab-click"));
      this.button(slot, icon.build(), type -> {
         if (!selected) {
            this.ctx.pageTurn(this.viewer);
            new JobDetailMenu(this.ctx, this.viewer, this.jobId, target, 0, this.back).open();
         }
      });
   }

   private List<ItemStack> rewardItems(JobDefinition job, PlayerJobProgress progress) {
      double multiplier = this.ctx.playerJobManager().moneyMultiplier(job, progress);
      List<ItemStack> items = new ArrayList<>();
      if ("explorer".equals(this.jobId)) {
         items.add(Icon.of(Material.FILLED_MAP)
            .name(this.ctx.text("detail.explorer-name"))
            .lore(this.ctx.lines("detail.explorer-lore", Map.of(
               "distance", String.valueOf((int)this.ctx.jobManager().explorerDistancePerMilestone()),
               "money", MoneyFormat.format(this.ctx.jobManager().explorerMoneyPerMilestone()),
               "yours", MoneyFormat.format(this.ctx.jobManager().explorerMoneyPerMilestone() * multiplier),
               "xp", String.format("%.0f", this.ctx.jobManager().explorerXpPerMilestone())
            )))
            .build());
         return items;
      }

      record Row(String type, String key, ActionReward reward) {
      }

      List<Row> rows = new ArrayList<>();
      for (var byType : job.getActionsByType().entrySet()) {
         for (var byKey : byType.getValue().entrySet()) {
            rows.add(new Row(byType.getKey(), byKey.getKey(), byKey.getValue()));
         }
      }

      rows.sort(Comparator.comparingDouble((Row r) -> r.reward().money() + r.reward().moneyPerLevel()).reversed().thenComparing(Row::key));
      String any = this.ctx.raw("detail.any");
      for (Row row : rows) {
         ActionReward reward = row.reward();
         boolean perLevel = reward.moneyPerLevel() > 0.0 || reward.xpPerLevel() > 0.0;
         double baseMoney = perLevel ? reward.moneyPerLevel() : reward.money();
         double baseXp = perLevel ? reward.xpPerLevel() : reward.xp();
         String suffix = perLevel ? this.ctx.raw("detail.per-level") : "";
         Component name = JobIcons.displayName(row.key(), any).colorIfAbsent(NamedTextColor.WHITE);
         items.add(Icon.of(JobIcons.forKey(row.key()))
            .name(name)
            .lore(this.ctx.text("detail.action", Map.of("action", this.ctx.messages().raw("quests.actions." + row.type(), Map.of()))))
            .blank()
            .lore(this.ctx.lines("detail.reward-lore", Map.of(
               "money", MoneyFormat.format(baseMoney) + suffix,
               "xp", String.format("%.1f", baseXp) + suffix,
               "yours", MoneyFormat.format(baseMoney * multiplier) + suffix,
               "multiplier", String.format("%.2f", multiplier)
            )))
            .build());
      }

      return items;
   }

   private List<ItemStack> perkItems(JobDefinition job, PlayerJobProgress progress) {
      int effective = progress == null ? 0 : this.ctx.perkManager().effectiveLevel(progress);
      List<PerkDefinition> perks = new ArrayList<>(this.ctx.perkManager().allPerks(job));
      perks.sort(Comparator.comparingInt(PerkDefinition::level));
      List<ItemStack> items = new ArrayList<>();
      for (PerkDefinition perk : perks) {
         boolean unlocked = effective >= perk.level();
         String type = perk.type().toLowerCase(Locale.ROOT);
         Icon icon = Icon.of(unlocked ? perkIcon(type) : Material.GRAY_DYE)
            .name(this.ctx.text(unlocked ? "detail.perk-unlocked" : "detail.perk-locked", Map.of(
               "level", String.valueOf(perk.level()),
               "perk", this.ctx.raw("detail.perk-name." + type)
            )))
            .glow(unlocked)
            .lore(MenuContext.line(this.ctx.messages().raw(perkDescriptionKey(type), perkArgs(perk))));
         if (!unlocked) {
            icon.blank()
               .lore(this.ctx.text("detail.perk-progress", Map.of(
                  "bar", Layout.bar(effective, perk.level(), 12),
                  "away", String.valueOf(perk.level() - effective)
               )));
         }

         items.add(icon.build());
      }

      return items;
   }

   private void joinButton(int slot) {
      boolean enabled = this.ctx.jobOverrides().isEnabled(this.jobId);
      boolean joined = this.ctx.playerJobManager().isJoined(this.viewer.getUniqueId(), this.jobId);
      if (!enabled) {
         this.set(slot, Icon.of(Material.GRAY_DYE).name(this.ctx.text("card.status-disabled")).build());
         return;
      }

      Icon icon = joined
         ? Icon.of(Material.RED_DYE).name(this.ctx.text("detail.leave")).lore(this.ctx.lines("detail.leave-lore", Map.of()))
         : Icon.of(Material.LIME_DYE).name(this.ctx.text("detail.join")).lore(this.ctx.lines("detail.join-lore", Map.of()));
      this.button(slot, icon.build(), type -> {
         JobActions.toggleJoin(this.ctx, this.viewer, this.jobId);
         this.redraw();
      });
   }

   private void prestigeButton(int slot, PlayerJobProgress progress) {
      boolean joined = this.ctx.playerJobManager().isJoined(this.viewer.getUniqueId(), this.jobId);
      int maxLevel = this.ctx.jobManager().maxLevel();
      if (joined && progress != null && progress.getLevel() >= maxLevel) {
         this.button(slot, Icon.of(Material.NETHER_STAR).glow(true).name(this.ctx.text("detail.prestige-ready")).lore(this.ctx.lines("detail.prestige-ready-lore", Map.of())).build(), type -> {
            this.ctx.click(this.viewer);
            new PrestigeConfirmMenu(this.ctx, this.viewer, this.jobId, () -> new JobDetailMenu(this.ctx, this.viewer, this.jobId, this.tab, this.page, this.back).open()).open();
         });
      } else {
         int level = progress == null ? 0 : progress.getLevel();
         this.set(slot, Icon.of(Material.FIREWORK_STAR).name(this.ctx.text("detail.prestige-locked")).lore(this.ctx.lines("detail.prestige-locked-lore", Map.of(
            "max", String.valueOf(maxLevel),
            "bar", Layout.bar(level, maxLevel, 12),
            "away", String.valueOf(Math.max(0, maxLevel - level)),
            "bonus", String.format("%.0f", this.ctx.jobManager().prestigeBonusPerPrestige() * 100.0)
         ))).build());
      }
   }

   private static Material perkIcon(String type) {
      return switch (type) {
         case "pay-bonus" -> Material.GOLD_NUGGET;
         case "potion" -> Material.POTION;
         case "double-drop" -> Material.HOPPER;
         case "auto-smelt" -> Material.FURNACE;
         case "xp-orb-bonus" -> Material.EXPERIENCE_BOTTLE;
         default -> Material.NETHER_STAR;
      };
   }

   private static String perkDescriptionKey(String type) {
      return switch (type) {
         case "pay-bonus", "potion", "double-drop", "auto-smelt", "xp-orb-bonus" -> "menu.perk-desc-" + type;
         default -> "menu.perk-desc-unknown";
      };
   }

   private static Map<String, String> perkArgs(PerkDefinition perk) {
      return Map.of(
         "effect", perk.effect() != null ? Layout.prettify(perk.effect()) + " " + roman((int)perk.value()) : "?",
         "value", String.format("%.0f", perk.value())
      );
   }

   private static String roman(int value) {
      return switch (value) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         default -> String.valueOf(value);
      };
   }

   public enum Tab {
      REWARDS,
      PERKS;
   }
}
