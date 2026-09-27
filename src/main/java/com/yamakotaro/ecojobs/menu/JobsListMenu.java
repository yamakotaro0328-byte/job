package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Every job as a card; left-click join/leave, right-click details, shift-click ranking. */
public class JobsListMenu extends Menu {
   private static final List<Integer> SLOTS = Layout.interior(6);
   private final Filter filter;
   private final int page;

   public JobsListMenu(MenuContext ctx, Player viewer, Filter filter, int page) {
      super(ctx, viewer, 6, MenuContext.line(ctx.raw("jobs.title")));
      this.filter = filter;
      this.page = page;
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.CYAN_STAINED_GLASS_PANE);
      PlayerJobManager pjm = this.ctx.playerJobManager();
      int joined = pjm.joinedJobs(this.viewer.getUniqueId()).size();
      int max = this.ctx.jobManager().maxConcurrentJobs();
      this.set(4, Icon.of(Material.PLAYER_HEAD).skull(this.viewer)
         .name(this.ctx.text("jobs.header", Map.of("player", this.viewer.getName())))
         .lore(this.ctx.lines("jobs.header-lore", Map.of("joined", String.valueOf(joined), "max", String.valueOf(max), "bar", Layout.bar(joined, max, max))))
         .build());

      List<JobDefinition> jobs = new ArrayList<>();
      for (JobDefinition job : this.ctx.jobManager().all().values()) {
         boolean isJoined = pjm.isJoined(this.viewer.getUniqueId(), job.getId());
         if (this.filter == Filter.ALL || (this.filter == Filter.JOINED) == isJoined) {
            jobs.add(job);
         }
      }

      int pages = Math.max(1, (jobs.size() + SLOTS.size() - 1) / SLOTS.size());
      int current = Math.max(0, Math.min(this.page, pages - 1));
      for (int i = 0; i < SLOTS.size() && current * SLOTS.size() + i < jobs.size(); i++) {
         JobDefinition job = jobs.get(current * SLOTS.size() + i);
         this.button(SLOTS.get(i), JobCards.card(this.ctx, this.viewer, job, false), type -> this.onJobClick(job.getId(), type));
      }

      if (jobs.isEmpty()) {
         this.set(22, Icon.of(Material.STRUCTURE_VOID).name(this.ctx.text("jobs.empty")).build());
      }

      this.backButton(45, () -> new HubMenu(this.ctx, this.viewer).open());
      this.pageButtons(48, 50, current, pages, p -> new JobsListMenu(this.ctx, this.viewer, this.filter, p).open());
      this.closeButton(49);
      this.button(53, this.filterItem(), type -> {
         this.ctx.click(this.viewer);
         new JobsListMenu(this.ctx, this.viewer, this.filter.next(), 0).open();
      });
   }

   private org.bukkit.inventory.ItemStack filterItem() {
      Icon icon = Icon.of(Material.HOPPER).name(this.ctx.text("jobs.filter"));
      for (Filter each : Filter.values()) {
         icon.lore(this.ctx.text(each == this.filter ? "jobs.filter-selected" : "jobs.filter-option", Map.of("name", this.ctx.raw("jobs.filter-" + each.name().toLowerCase()))));
      }

      return icon.blank().lore(this.ctx.text("jobs.filter-hint")).build();
   }

   private void onJobClick(String jobId, ClickType type) {
      if (type.isShiftClick()) {
         if (!this.viewer.hasPermission("ecojobs.top")) {
            this.ctx.deny(this.viewer);
            return;
         }

         this.ctx.click(this.viewer);
         new LeaderboardMenu(this.ctx, this.viewer, jobId, () -> new JobsListMenu(this.ctx, this.viewer, this.filter, this.page).open()).open();
      } else if (type.isRightClick()) {
         this.ctx.click(this.viewer);
         new JobDetailMenu(this.ctx, this.viewer, jobId, JobDetailMenu.Tab.REWARDS, 0, () -> new JobsListMenu(this.ctx, this.viewer, this.filter, this.page).open()).open();
      } else {
         JobActions.toggleJoin(this.ctx, this.viewer, jobId);
         this.redraw();
      }
   }

   public enum Filter {
      ALL,
      JOINED,
      NOT_JOINED;

      Filter next() {
         return values()[(this.ordinal() + 1) % values().length];
      }
   }
}
