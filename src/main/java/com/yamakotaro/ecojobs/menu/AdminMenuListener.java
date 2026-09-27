package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AdminMenuListener implements Listener {
   private final JobManager jobManager;
   private final PlayerJobManager playerJobManager;
   private final JobOverrides jobOverrides;
   private final BoosterManager boosterManager;
   private final Messages messages;

   public AdminMenuListener(
      JobManager jobManager, PlayerJobManager playerJobManager, JobOverrides jobOverrides, BoosterManager boosterManager, Messages messages
   ) {
      this.jobManager = jobManager;
      this.playerJobManager = playerJobManager;
      this.jobOverrides = jobOverrides;
      this.boosterManager = boosterManager;
      this.messages = messages;
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof LeaderboardMenuHolder holder) {
         this.handleLeaderboardClick(event, holder);
      } else if (event.getInventory().getHolder() instanceof AdminMenuHolder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() == event.getInventory()) {
               int slot = event.getSlot();
               if (slot == 53) {
                  player.closeInventory();
               } else if (slot == 45) {
                  HubMenuHolder hub = new HubMenuHolder(this.messages);
                  hub.render(true, this.boosterManager);
                  player.openInventory(hub.getInventory());
                  this.playSound(player, Sound.UI_BUTTON_CLICK, 1.2F);
               } else if (slot == 48) {
                  long durationMillis = TimeUnit.MINUTES.toMillis(30L);
                  this.boosterManager.start("all", 2.0, 2.0, durationMillis, player.getName());
                  Bukkit.getServer()
                     .sendMessage(
                        this.messages
                           .get(
                              "jobs.booster-started",
                              Map.of(
                                 "scope",
                                 this.messages.raw("jobs.booster-scope-all", Map.of()),
                                 "money",
                                 String.format("%.2f", 2.0),
                                 "xp",
                                 String.format("%.2f", 2.0),
                                 "minutes",
                                 String.valueOf(30L),
                                 "player",
                                 player.getName()
                              )
                           )
                     );
                  this.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.2F);
                  holder.render(this.jobManager, this.jobOverrides, this.boosterManager);
               } else if (slot == 50) {
                  int stopped = this.boosterManager.stopAll();
                  if (stopped > 0) {
                     Bukkit.getServer().sendMessage(this.messages.get("jobs.booster-stopped-all", Map.of("player", player.getName())));
                  }

                  this.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F);
                  holder.render(this.jobManager, this.jobOverrides, this.boosterManager);
               } else {
                  String jobId = holder.jobIdAt(slot);
                  if (jobId != null) {
                     if (event.isShiftClick() && event.isLeftClick()) {
                        LeaderboardMenuHolder leaderboard = new LeaderboardMenuHolder(this.messages, jobId, LeaderboardMenuHolder.Origin.ADMIN);
                        leaderboard.render(this.playerJobManager);
                        player.openInventory(leaderboard.getInventory());
                        this.playSound(player, Sound.UI_BUTTON_CLICK, 1.4F);
                     } else {
                        if (event.isShiftClick() && event.isRightClick()) {
                           this.jobOverrides.adjustPayMultiplier(jobId, -0.1);
                        } else if (event.isRightClick()) {
                           this.jobOverrides.adjustPayMultiplier(jobId, 0.1);
                        } else {
                           this.jobOverrides.setEnabled(jobId, !this.jobOverrides.isEnabled(jobId));
                        }

                        this.playSound(player, Sound.UI_BUTTON_CLICK, 1.4F);
                        holder.render(this.jobManager, this.jobOverrides, this.boosterManager);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleLeaderboardClick(InventoryClickEvent event, LeaderboardMenuHolder holder) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         int slot = event.getSlot();
         if (slot == 49) {
            player.closeInventory();
         } else if (slot == 45) {
            switch (holder.getOrigin()) {
               case JOBS_MENU:
                  JobsMenuHolder jobsMenu = new JobsMenuHolder(this.messages);
                  jobsMenu.render(this.jobManager, this.playerJobManager, this.jobOverrides, player);
                  player.openInventory(jobsMenu.getInventory());
                  break;
               case PICKER:
                  LeaderboardPickerMenuHolder picker = new LeaderboardPickerMenuHolder(this.messages);
                  picker.render(this.jobManager);
                  player.openInventory(picker.getInventory());
                  break;
               case ADMIN:
                  AdminMenuHolder admin = new AdminMenuHolder(this.messages);
                  admin.render(this.jobManager, this.jobOverrides, this.boosterManager);
                  player.openInventory(admin.getInventory());
            }

            this.playSound(player, Sound.UI_BUTTON_CLICK, 1.2F);
         }
      }
   }

   private void playSound(Player player, Sound sound, float pitch) {
      if (this.playerJobManager.isSoundEnabled(player)) {
         player.playSound(player.getLocation(), sound, 0.6F, pitch);
      }
   }
}
