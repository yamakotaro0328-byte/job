package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PerkManager;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class JobsMenuListener implements Listener {
   private final JobManager jobManager;
   private final PlayerJobManager playerJobManager;
   private final JobOverrides jobOverrides;
   private final BoosterManager boosterManager;
   private final PerkManager perkManager;
   private final Messages messages;

   public JobsMenuListener(
      JobManager jobManager,
      PlayerJobManager playerJobManager,
      JobOverrides jobOverrides,
      BoosterManager boosterManager,
      PerkManager perkManager,
      Messages messages
   ) {
      this.jobManager = jobManager;
      this.playerJobManager = playerJobManager;
      this.jobOverrides = jobOverrides;
      this.boosterManager = boosterManager;
      this.perkManager = perkManager;
      this.messages = messages;
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof JobInfoMenuHolder infoHolder) {
         this.handleJobInfoClick(event, infoHolder);
      } else if (event.getInventory().getHolder() instanceof PrestigeConfirmMenuHolder confirmHolder) {
         this.handlePrestigeConfirmClick(event, confirmHolder);
      } else if (event.getInventory().getHolder() instanceof JobsMenuHolder holder) {
         this.handleJobsMenuClick(event, holder);
      }
   }

   private void handleJobsMenuClick(InventoryClickEvent event, JobsMenuHolder holder) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player) {
         if (event.getClickedInventory() == event.getInventory()) {
            int slot = event.getSlot();
            if (slot == 49) {
               player.closeInventory();
            } else if (slot == 45) {
               HubMenuHolder hub = new HubMenuHolder(this.messages);
               hub.render(player.hasPermission("ecojobs.admin"), this.boosterManager);
               player.openInventory(hub.getInventory());
               this.playSuccess(player);
            } else {
               String jobId = holder.jobIdAt(slot);
               if (jobId != null) {
                  if (event.isShiftClick()) {
                     if (!player.hasPermission("ecojobs.top")) {
                        player.sendMessage(this.messages.get("general.no-permission", Map.of()));
                        this.playDenied(player);
                     } else {
                        LeaderboardMenuHolder leaderboard = new LeaderboardMenuHolder(this.messages, jobId, LeaderboardMenuHolder.Origin.JOBS_MENU);
                        leaderboard.render(this.playerJobManager);
                        player.openInventory(leaderboard.getInventory());
                        this.playSuccess(player);
                     }
                  } else if (event.isRightClick()) {
                     this.openJobInfo(player, jobId, 0);
                     this.playSuccess(player);
                  } else {
                     this.handleJoinLeave(player, jobId);
                     holder.render(this.jobManager, this.playerJobManager, this.jobOverrides, player);
                  }
               }
            }
         }
      }
   }

   private void handleJobInfoClick(InventoryClickEvent event, JobInfoMenuHolder holder) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         int slot = event.getSlot();
         if (slot == 49) {
            player.closeInventory();
         } else if (slot == 45) {
            JobsMenuHolder jobsMenu = new JobsMenuHolder(this.messages);
            jobsMenu.render(this.jobManager, this.playerJobManager, this.jobOverrides, player);
            player.openInventory(jobsMenu.getInventory());
            this.playSuccess(player);
         } else if (slot == 46) {
            holder.render(this.jobManager, this.playerJobManager, this.jobOverrides, this.perkManager, player, holder.getPage() - 1);
            this.playSuccess(player);
         } else if (slot == 48) {
            holder.render(this.jobManager, this.playerJobManager, this.jobOverrides, this.perkManager, player, holder.getPage() + 1);
            this.playSuccess(player);
         } else if (slot == 4) {
            String jobId = holder.getJobId();
            if (event.isShiftClick()) {
               if (!player.hasPermission("ecojobs.top")) {
                  player.sendMessage(this.messages.get("general.no-permission", Map.of()));
                  this.playDenied(player);
               } else {
                  LeaderboardMenuHolder leaderboard = new LeaderboardMenuHolder(this.messages, jobId, LeaderboardMenuHolder.Origin.JOBS_MENU);
                  leaderboard.render(this.playerJobManager);
                  player.openInventory(leaderboard.getInventory());
                  this.playSuccess(player);
               }
            } else if (event.isRightClick()) {
               this.attemptPrestige(player, jobId);
            } else {
               this.handleJoinLeave(player, jobId);
               holder.render(this.jobManager, this.playerJobManager, this.jobOverrides, this.perkManager, player, holder.getPage());
            }
         }
      }
   }

   private void handlePrestigeConfirmClick(InventoryClickEvent event, PrestigeConfirmMenuHolder holder) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         int slot = event.getSlot();
         if (slot == 11) {
            String jobId = holder.getJobId();
            switch (this.playerJobManager.prestige(player, jobId)) {
               case SUCCESS:
                  this.playSuccess(player);
                  break;
               case NOT_JOINED:
                  player.sendMessage(this.messages.get("jobs.not-joined", Map.of("job", this.messages.jobName(jobId))));
                  this.playDenied(player);
                  break;
               case NOT_MAX_LEVEL:
                  player.sendMessage(
                     this.messages
                        .get("jobs.prestige-not-max-level", Map.of("job", this.messages.jobName(jobId), "max", String.valueOf(this.jobManager.maxLevel())))
                  );
                  this.playDenied(player);
                  break;
               case UNKNOWN_JOB:
                  player.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
                  this.playDenied(player);
            }

            this.returnFromPrestigeConfirm(player, holder);
         } else if (slot == 15) {
            this.returnFromPrestigeConfirm(player, holder);
         }
      }
   }

   private void attemptPrestige(Player player, String jobId) {
      PlayerJobProgress progress = this.playerJobManager.allProgress(player.getUniqueId()).get(jobId);
      boolean active = this.playerJobManager.isJoined(player.getUniqueId(), jobId);
      if (active && progress != null && progress.getLevel() >= this.jobManager.maxLevel()) {
         PrestigeConfirmMenuHolder confirm = new PrestigeConfirmMenuHolder(this.messages, jobId);
         confirm.render();
         player.openInventory(confirm.getInventory());
         this.playSuccess(player);
      } else {
         player.sendMessage(
            this.messages
               .get(
                  active ? "jobs.prestige-not-max-level" : "jobs.not-joined",
                  Map.of("job", this.messages.jobName(jobId), "max", String.valueOf(this.jobManager.maxLevel()))
               )
         );
         this.playDenied(player);
      }
   }

   private void returnFromPrestigeConfirm(Player player, PrestigeConfirmMenuHolder holder) {
      this.openJobInfo(player, holder.getJobId(), 0);
   }

   private void openJobInfo(Player player, String jobId, int page) {
      JobInfoMenuHolder info = new JobInfoMenuHolder(this.messages, jobId);
      info.render(this.jobManager, this.playerJobManager, this.jobOverrides, this.perkManager, player, page);
      player.openInventory(info.getInventory());
   }

   private void handleJoinLeave(Player player, String jobId) {
      if (!player.hasPermission("ecojobs.use")) {
         player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         this.playDenied(player);
      } else if (this.playerJobManager.isJoined(player.getUniqueId(), jobId)) {
         this.playerJobManager.leave(player.getUniqueId(), jobId);
         player.sendMessage(this.messages.get("jobs.left", Map.of("job", this.messages.jobName(jobId))));
         this.playSuccess(player);
      } else {
         switch (this.playerJobManager.join(player, jobId)) {
            case MAX_JOBS_REACHED:
               player.sendMessage(this.messages.get("jobs.max-jobs-reached", Map.of("max", String.valueOf(this.jobManager.maxConcurrentJobs()))));
               this.playDenied(player);
               break;
            case JOB_DISABLED:
               player.sendMessage(this.messages.get("jobs.job-disabled", Map.of("job", this.messages.jobName(jobId))));
               this.playDenied(player);
               break;
            case SUCCESS:
               player.sendMessage(this.messages.get("jobs.joined", Map.of("job", this.messages.jobName(jobId))));
               this.playSuccess(player);
         }
      }
   }

   private void playSuccess(Player player) {
      if (this.playerJobManager.isSoundEnabled(player)) {
         player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.4F);
      }
   }

   private void playDenied(Player player) {
      if (this.playerJobManager.isSoundEnabled(player)) {
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
      }
   }
}
