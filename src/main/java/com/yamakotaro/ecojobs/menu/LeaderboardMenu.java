package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * A job's ranking: a podium for the top 3, then ranks 4-17 below, plus your own position.
 * <pre>
 *  row 1        [#1]
 *  row 2    [#2]    [#3]
 *  row 3-4  #4 ... #17
 * </pre>
 */
public class LeaderboardMenu extends Menu {
   private static final int[] PODIUM = {13, 21, 23};
   private static final Material[] PEDESTALS = {Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK};
   private static final int[] REST = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
   private final String jobId;
   private final Runnable back;

   public LeaderboardMenu(MenuContext ctx, Player viewer, String jobId, Runnable back) {
      super(ctx, viewer, 6, MenuContext.line(ctx.raw("ranking.title", Map.of("job", ctx.jobName(jobId)))));
      this.jobId = jobId;
      this.back = back;
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.YELLOW_STAINED_GLASS_PANE);
      this.set(4, Icon.of(JobIcons.of(this.ctx.plugin(), this.jobId)).name(this.ctx.text("ranking.header", Map.of("job", this.ctx.jobName(this.jobId)))).build());
      PlayerJobManager pjm = this.ctx.playerJobManager();
      List<PlayerJobManager.TopEntry> top = pjm.top(this.jobId, PODIUM.length + REST.length);

      for (int i = 0; i < PODIUM.length; i++) {
         if (i < top.size()) {
            this.set(PODIUM[i], this.head(top.get(i), i + 1));
         } else {
            this.set(PODIUM[i], Icon.of(Material.SKELETON_SKULL).name(this.ctx.text("ranking.empty-slot", Map.of("rank", String.valueOf(i + 1)))).build());
         }
      }

      // Gold/iron/copper blocks beside the heads mark 1st/2nd/3rd at a glance.
      this.set(12, Icon.pane(Material.YELLOW_STAINED_GLASS_PANE));
      this.set(14, Icon.pane(Material.YELLOW_STAINED_GLASS_PANE));
      this.set(22, Icon.of(PEDESTALS[0]).name(this.ctx.text("ranking.first")).build());
      this.set(20, Icon.of(PEDESTALS[1]).name(this.ctx.text("ranking.second")).build());
      this.set(24, Icon.of(PEDESTALS[2]).name(this.ctx.text("ranking.third")).build());

      for (int i = 0; i < REST.length; i++) {
         int index = PODIUM.length + i;
         if (index < top.size()) {
            this.set(REST[i], this.head(top.get(index), index + 1));
         }
      }

      int rank = pjm.rankOf(this.jobId, this.viewer.getUniqueId());
      this.set(53, Icon.of(Material.PLAYER_HEAD).skull(this.viewer)
         .name(this.ctx.text("ranking.you", Map.of("rank", rank > 0 ? "#" + rank : "-")))
         .lore(this.ctx.text("ranking.you-lore", Map.of("total", String.valueOf(pjm.rankedCount(this.jobId)))))
         .glow(rank > 0 && rank <= 3)
         .build());
      this.backButton(45, this.back);
      this.closeButton(49);
   }

   private org.bukkit.inventory.ItemStack head(PlayerJobManager.TopEntry entry, int rank) {
      String color = switch (rank) {
         case 1 -> "&6&l";
         case 2 -> "&f&l";
         case 3 -> "&c&l";
         default -> "&7";
      };
      boolean self = entry.uuid().equals(this.viewer.getUniqueId());
      return Icon.of(Material.PLAYER_HEAD)
         .skull(Bukkit.getOfflinePlayer(entry.uuid()))
         .name(this.ctx.text("ranking.entry", Map.of("color", color, "rank", String.valueOf(rank), "player", entry.name(), "you", self ? this.ctx.raw("ranking.you-tag") : "")))
         .lore(this.ctx.lines("ranking.entry-lore", Map.of(
            "level", String.valueOf(entry.level()),
            "prestige", String.valueOf(entry.prestige()),
            "xp", String.format("%.0f", entry.xp())
         )))
         .glow(self)
         .build();
   }
}
