package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.MoneyFormat;
import com.yamakotaro.ecojobs.QuestManager;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Today's daily quests with progress bars, the reset countdown, and the all-clear bonus. */
public class QuestMenu extends Menu {
   public QuestMenu(MenuContext ctx, Player viewer) {
      super(ctx, viewer, 5, MenuContext.line(ctx.raw("quests.title")));
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.ORANGE_STAINED_GLASS_PANE);
      QuestManager quests = this.ctx.questManager();
      List<QuestManager.Quest> list = quests.isEnabled() ? quests.questsFor(this.viewer) : List.of();
      int done = (int)list.stream().filter(q -> q.completed).count();

      this.set(4, Icon.of(Material.CLOCK)
         .name(this.ctx.text("quests.header"))
         .lore(this.ctx.lines("quests.header-lore", Map.of(
            "completed", String.valueOf(done),
            "total", String.valueOf(list.size()),
            "bar", Layout.bar(done, list.size(), 12),
            "reset", Layout.duration(quests.millisUntilReset())
         )))
         .build());

      if (!quests.isEnabled()) {
         this.set(22, Icon.of(Material.BARRIER).name(this.ctx.text("quests.disabled")).build());
      } else if (list.isEmpty()) {
         this.button(22, Icon.of(Material.OAK_SIGN).name(this.ctx.text("quests.none")).lore(this.ctx.lines("quests.none-lore", Map.of())).build(), type -> {
            this.ctx.click(this.viewer);
            new JobsListMenu(this.ctx, this.viewer, JobsListMenu.Filter.ALL, 0).open();
         });
      } else {
         List<Integer> slots = Layout.centered(2, list.size());
         for (int i = 0; i < slots.size(); i++) {
            this.set(slots.get(i), this.questItem(list.get(i)));
         }
      }

      boolean allDone = !list.isEmpty() && done == list.size();
      this.set(40, Icon.of(allDone ? Material.ENDER_CHEST : Material.CHEST)
         .glow(allDone)
         .name(this.ctx.text(allDone ? "quests.bonus-claimed" : "quests.bonus"))
         .lore(this.ctx.lines("quests.bonus-lore", Map.of("money", MoneyFormat.format(quests.allCompleteBonus()))))
         .build());
      this.backButton(36, () -> new HubMenu(this.ctx, this.viewer).open());
      this.closeButton(44);
   }

   private org.bukkit.inventory.ItemStack questItem(QuestManager.Quest quest) {
      Map<String, String> ph = this.ctx.questManager().placeholders(quest);
      Material material = JobIcons.forKey(quest.key);
      if (material == Material.PAPER) {
         material = JobIcons.of(this.ctx.plugin(), quest.jobId);
      }

      Icon icon = Icon.of(quest.completed ? Material.LIME_DYE : material)
         .glow(quest.completed)
         .name(this.ctx.text(quest.completed ? "quests.quest-done" : "quests.quest", Map.of("job", this.ctx.jobName(quest.jobId))))
         .lore(this.ctx.text("quests.objective", ph).append(JobIcons.displayName(quest.key, this.ctx.raw("detail.any")).colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE)))
         .blank()
         .lore(this.ctx.lines("quests.quest-lore", Map.of(
            "bar", Layout.bar(quest.progress, quest.target, 12),
            "progress", String.valueOf(quest.progress),
            "amount", String.valueOf(quest.target),
            "percent", String.valueOf(Layout.percent(quest.progress, quest.target)),
            "money", MoneyFormat.format(quest.money),
            "xp", String.format("%.0f", quest.xp)
         )));
      return icon.build();
   }
}
