package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.PlayerJobManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ExplorerListener implements Listener {
   private final PlayerJobManager jobs;

   public ExplorerListener(PlayerJobManager jobs) {
      this.jobs = jobs;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onMove(PlayerMoveEvent event) {
      if (!(event instanceof PlayerTeleportEvent)) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ())) {
            Player player = event.getPlayer();
            if (!player.isGliding() && !player.isInsideVehicle()) {
               if (this.jobs.isJoined(player.getUniqueId(), "explorer")) {
                  Location spawn = player.getWorld().getSpawnLocation();
                  double dx = to.getX() - spawn.getX();
                  double dz = to.getZ() - spawn.getZ();
                  this.jobs.checkExplorerMilestones(player, player.getWorld().getName(), Math.sqrt(dx * dx + dz * dz));
               }
            }
         }
      }
   }
}
