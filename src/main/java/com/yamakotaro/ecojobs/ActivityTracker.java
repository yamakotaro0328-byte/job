package com.yamakotaro.ecojobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Anti-AFK: remembers when each player last did something only a person at the keyboard does
 * (turn their camera, chat, run a command, click in an inventory). Being carried by water/minecarts
 * or an auto-clicker alone doesn't count, which is what AFK farms rely on.
 */
public class ActivityTracker implements Listener {
   private final EcoJobsPlugin plugin;
   private final Map<UUID, Long> lastActive = new HashMap<>();

   public ActivityTracker(EcoJobsPlugin plugin) {
      this.plugin = plugin;
   }

   /** True when anti-farm.afk-seconds is set and the player has been idle longer than that. */
   public boolean isAfk(Player player) {
      long limit = this.plugin.config().getLong("anti-farm.afk-seconds", 0L);
      if (limit <= 0L || player.hasPermission("ecojobs.bypass.afk")) {
         return false;
      }

      Long last = this.lastActive.get(player.getUniqueId());
      return last != null && System.currentTimeMillis() - last > limit * 1000L;
   }

   private void touch(Player player) {
      this.lastActive.put(player.getUniqueId(), System.currentTimeMillis());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      Location from = event.getFrom();
      Location to = event.getTo();
      if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
         this.touch(event.getPlayer());
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   @SuppressWarnings("deprecation")
   public void onChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      // Async event: hop to the main thread before touching the map.
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.touch(player));
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      this.touch(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         this.touch(player);
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.touch(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.lastActive.remove(event.getPlayer().getUniqueId());
   }
}
