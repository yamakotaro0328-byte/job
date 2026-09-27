package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HubMenuListener implements Listener {
   private final JobManager jobManager;
   private final PlayerJobManager playerJobManager;
   private final JobOverrides jobOverrides;
   private final BoosterManager boosterManager;
   private final Messages messages;

   public HubMenuListener(JobManager jobManager, PlayerJobManager playerJobManager, JobOverrides jobOverrides, BoosterManager boosterManager, Messages messages) {
      this.jobManager = jobManager;
      this.playerJobManager = playerJobManager;
      this.jobOverrides = jobOverrides;
      this.boosterManager = boosterManager;
      this.messages = messages;
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof HubMenuHolder) {
         this.handleHubClick(event);
      } else if (event.getInventory().getHolder() instanceof SettingsMenuHolder) {
         this.handleSettingsClick(event);
      } else if (event.getInventory().getHolder() instanceof LeaderboardPickerMenuHolder holder) {
         this.handlePickerClick(event, holder);
      }
   }

   private void handleHubClick(InventoryClickEvent event) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         int slot = event.getSlot();
         if (slot == 26) {
            player.closeInventory();
         } else {
            if (slot == 11) {
               JobsMenuHolder holder = new JobsMenuHolder(this.messages);
               holder.render(this.jobManager, this.playerJobManager, this.jobOverrides, player);
               player.openInventory(holder.getInventory());
               this.playClick(player);
            } else if (slot == 13) {
               if (!player.hasPermission("ecojobs.top")) {
                  player.sendMessage(this.messages.get("general.no-permission", Map.of()));
                  return;
               }

               LeaderboardPickerMenuHolder holder = new LeaderboardPickerMenuHolder(this.messages);
               holder.render(this.jobManager);
               player.openInventory(holder.getInventory());
               this.playClick(player);
            } else if (slot == 15) {
               SettingsMenuHolder holder = new SettingsMenuHolder(this.messages);
               holder.render(this.playerJobManager, player);
               player.openInventory(holder.getInventory());
               this.playClick(player);
            } else if (slot == 22) {
               if (!player.hasPermission("ecojobs.admin")) {
                  player.sendMessage(this.messages.get("general.no-permission", Map.of()));
                  return;
               }

               AdminMenuHolder holder = new AdminMenuHolder(this.messages);
               holder.render(this.jobManager, this.jobOverrides, this.boosterManager);
               player.openInventory(holder.getInventory());
               this.playClick(player);
            }
         }
      }
   }

   private void handleSettingsClick(InventoryClickEvent event) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         int slot = event.getSlot();
         if (slot == 22) {
            player.closeInventory();
         } else if (slot == 18) {
            this.openHub(player);
         } else {
            if (slot == 11) {
               this.playerJobManager.toggleSoundEnabled(player);
            } else {
               if (slot != 15) {
                  return;
               }

               this.playerJobManager.toggleActionBarEnabled(player);
            }

            this.playClick(player);
            ((SettingsMenuHolder)event.getInventory().getHolder()).render(this.playerJobManager, player);
         }
      }
   }

   private void handlePickerClick(InventoryClickEvent event, LeaderboardPickerMenuHolder holder) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getInventory()) {
         if (event.getSlot() == 49) {
            player.closeInventory();
         } else if (event.getSlot() == 45) {
            this.openHub(player);
         } else {
            String jobId = holder.jobIdAt(event.getSlot());
            if (jobId != null) {
               LeaderboardMenuHolder leaderboard = new LeaderboardMenuHolder(this.messages, jobId, LeaderboardMenuHolder.Origin.PICKER);
               leaderboard.render(this.playerJobManager);
               player.openInventory(leaderboard.getInventory());
               this.playClick(player);
            }
         }
      }
   }

   private void openHub(Player player) {
      HubMenuHolder holder = new HubMenuHolder(this.messages);
      holder.render(player.hasPermission("ecojobs.admin"), this.boosterManager);
      player.openInventory(holder.getInventory());
      this.playClick(player);
   }

   private void playClick(Player player) {
      if (this.playerJobManager.isSoundEnabled(player)) {
         player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
      }
   }
}
