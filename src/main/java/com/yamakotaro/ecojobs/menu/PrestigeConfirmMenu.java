package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Yes/no screen that spells out exactly what prestiging costs and gives. */
public class PrestigeConfirmMenu extends Menu {
   private final String jobId;
   private final Runnable back;

   public PrestigeConfirmMenu(MenuContext ctx, Player viewer, String jobId, Runnable back) {
      super(ctx, viewer, 3, MenuContext.line(ctx.raw("prestige.title", Map.of("job", ctx.jobName(jobId)))));
      this.jobId = jobId;
      this.back = back;
   }

   @Override
   protected void render() {
      Layout.frame(this, Material.MAGENTA_STAINED_GLASS_PANE);
      PlayerJobProgress progress = this.ctx.playerJobManager().allProgress(this.viewer.getUniqueId()).get(this.jobId);
      int prestige = progress == null ? 0 : progress.getPrestige();
      double per = this.ctx.jobManager().prestigeBonusPerPrestige() * 100.0;
      this.set(13, Icon.of(Material.NETHER_STAR).glow(true)
         .name(this.ctx.text("prestige.info", Map.of("job", this.ctx.jobName(this.jobId))))
         .lore(this.ctx.lines("prestige.info-lore", Map.of(
            "from", String.valueOf(prestige),
            "to", String.valueOf(prestige + 1),
            "bonus_from", String.format("%.0f", prestige * per),
            "bonus_to", String.format("%.0f", (prestige + 1) * per),
            "max", String.valueOf(this.ctx.jobManager().maxLevel())
         )))
         .build());
      this.button(11, Icon.of(Material.LIME_CONCRETE).name(this.ctx.text("prestige.confirm")).lore(this.ctx.lines("prestige.confirm-lore", Map.of())).build(), type -> {
         switch (this.ctx.playerJobManager().prestige(this.viewer, this.jobId)) {
            case SUCCESS -> this.ctx.success(this.viewer);
            case NOT_MAX_LEVEL -> {
               this.viewer.sendMessage(this.ctx.messages().get("jobs.prestige-not-max-level", Map.of("job", this.ctx.jobName(this.jobId), "max", String.valueOf(this.ctx.jobManager().maxLevel()))));
               this.ctx.deny(this.viewer);
            }
            case NOT_JOINED -> {
               this.viewer.sendMessage(this.ctx.messages().get("jobs.not-joined", Map.of("job", this.ctx.jobName(this.jobId))));
               this.ctx.deny(this.viewer);
            }
            default -> this.ctx.deny(this.viewer);
         }

         this.back.run();
      });
      this.button(15, Icon.of(Material.RED_CONCRETE).name(this.ctx.text("prestige.cancel")).build(), type -> {
         this.ctx.click(this.viewer);
         this.back.run();
      });
   }
}
