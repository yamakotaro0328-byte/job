package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.PlayerJobManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps the stored player name current (renamed accounts) and drops per-session state on quit. */
public class PlayerSessionListener implements Listener {
   private final PlayerJobManager jobs;

   public PlayerSessionListener(PlayerJobManager jobs) {
      this.jobs = jobs;
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.jobs.refreshName(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.jobs.forgetSession(event.getPlayer().getUniqueId());
   }
}
