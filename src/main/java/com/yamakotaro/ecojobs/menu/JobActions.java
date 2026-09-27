package com.yamakotaro.ecojobs.menu;

import java.util.Map;
import org.bukkit.entity.Player;

/** Join/leave with the same feedback messages as /jobs join|leave. */
final class JobActions {
   private JobActions() {
   }

   static void toggleJoin(MenuContext ctx, Player player, String jobId) {
      var messages = ctx.messages();
      if (!player.hasPermission("ecojobs.use")) {
         player.sendMessage(messages.get("general.no-permission", Map.of()));
         ctx.deny(player);
      } else if (ctx.playerJobManager().isJoined(player.getUniqueId(), jobId)) {
         ctx.playerJobManager().leave(player.getUniqueId(), jobId);
         player.sendMessage(messages.get("jobs.left", Map.of("job", ctx.jobName(jobId))));
         ctx.click(player);
      } else {
         switch (ctx.playerJobManager().join(player, jobId)) {
            case SUCCESS -> {
               player.sendMessage(messages.get("jobs.joined", Map.of("job", ctx.jobName(jobId))));
               ctx.success(player);
            }
            case MAX_JOBS_REACHED -> {
               player.sendMessage(messages.get("jobs.max-jobs-reached", Map.of("max", String.valueOf(ctx.jobManager().maxConcurrentJobs()))));
               ctx.deny(player);
            }
            case JOB_DISABLED -> {
               player.sendMessage(messages.get("jobs.job-disabled", Map.of("job", ctx.jobName(jobId))));
               ctx.deny(player);
            }
            default -> ctx.deny(player);
         }
      }
   }
}
