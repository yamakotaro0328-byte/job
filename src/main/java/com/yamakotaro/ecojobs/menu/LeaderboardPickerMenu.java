package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Pick a job's ranking; each icon previews the current #1 and your own rank. */
public class LeaderboardPickerMenu extends Menu {
   private static final List<Integer> SLOTS = Layout.interior(6);
   private final int page;

   public LeaderboardPickerMenu(MenuContext ctx, Player viewer) {
      this(ctx, viewer, 0);
   }

   private LeaderboardPickerMenu(MenuContext ctx, Player viewer, int page) {
      super(ctx, viewer, 6, MenuContext.line(ctx.raw("picker.title")));
      this.page = page;
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.YELLOW_STAINED_GLASS_PANE);
      this.set(4, Icon.of(Material.GOLDEN_HELMET).name(this.ctx.text("picker.header")).lore(this.ctx.lines("picker.header-lore", Map.of())).build());
      List<JobDefinition> jobs = new ArrayList<>(this.ctx.jobManager().all().values());
      int pages = Math.max(1, (jobs.size() + SLOTS.size() - 1) / SLOTS.size());
      int current = Math.max(0, Math.min(this.page, pages - 1));
      PlayerJobManager pjm = this.ctx.playerJobManager();

      for (int i = 0; i < SLOTS.size() && current * SLOTS.size() + i < jobs.size(); i++) {
         String jobId = jobs.get(current * SLOTS.size() + i).getId();
         List<PlayerJobManager.TopEntry> top = pjm.top(jobId, 1);
         int rank = pjm.rankOf(jobId, this.viewer.getUniqueId());
         Icon icon = Icon.of(JobIcons.of(this.ctx.plugin(), jobId)).name(this.ctx.text("card.name", Map.of("job", this.ctx.jobName(jobId))));
         if (top.isEmpty()) {
            icon.lore(this.ctx.text("picker.nobody"));
         } else {
            PlayerJobManager.TopEntry first = top.get(0);
            icon.lore(this.ctx.text("picker.first", Map.of("player", first.name(), "level", String.valueOf(first.level()), "prestige", String.valueOf(first.prestige()))));
         }

         icon.lore(this.ctx.text("picker.you", Map.of("rank", rank > 0 ? "#" + rank : "-", "total", String.valueOf(pjm.rankedCount(jobId)))))
            .blank()
            .lore(this.ctx.text("picker.hint"))
            .glow(rank == 1);
         this.button(SLOTS.get(i), icon.build(), type -> {
            this.ctx.click(this.viewer);
            new LeaderboardMenu(this.ctx, this.viewer, jobId, () -> new LeaderboardPickerMenu(this.ctx, this.viewer, current).open()).open();
         });
      }

      this.backButton(45, () -> new HubMenu(this.ctx, this.viewer).open());
      this.pageButtons(48, 50, current, pages, p -> new LeaderboardPickerMenu(this.ctx, this.viewer, p).open());
      this.closeButton(49);
   }
}
