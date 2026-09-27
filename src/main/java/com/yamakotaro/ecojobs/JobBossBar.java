package com.yamakotaro.ecojobs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Shows a boss bar with the job's xp progress whenever a player earns job xp; hides it after a few idle seconds. */
public class JobBossBar {
   private final EcoJobsPlugin plugin;
   private final Messages messages;
   private final Map<UUID, Shown> shown = new HashMap<>();

   public JobBossBar(EcoJobsPlugin plugin, Messages messages) {
      this.plugin = plugin;
      this.messages = messages;
   }

   public void show(Player player, String jobId, int level, double xp, double required, boolean maxed) {
      if (!this.plugin.config().getBoolean("bossbar.enabled", true)) {
         return;
      }
      float progress = maxed || required <= 0.0 ? 1.0F : (float)Math.max(0.0, Math.min(1.0, xp / required));
      Map<String, String> placeholders = Map.of(
         "job", this.messages.jobName(jobId),
         "level", String.valueOf(level),
         "xp", String.format("%.0f", xp),
         "required", String.format("%.0f", required),
         "percent", String.valueOf(Math.round(progress * 100.0F))
      );
      var title = this.messages.get(maxed ? "bossbar.max-level" : "bossbar.title", placeholders);
      Shown current = this.shown.get(player.getUniqueId());
      if (current == null) {
         BossBar bar = BossBar.bossBar(title, progress, this.color(), BossBar.Overlay.PROGRESS);
         player.showBossBar(bar);
         current = new Shown(bar);
         this.shown.put(player.getUniqueId(), current);
      } else {
         current.bar.name(title);
         current.bar.progress(progress);
         current.bar.color(this.color());
      }
      current.lastUpdate = System.currentTimeMillis();
   }

   private BossBar.Color color() {
      try {
         return BossBar.Color.valueOf(this.plugin.config().getString("bossbar.color", "GREEN").toUpperCase());
      } catch (IllegalArgumentException var2) {
         return BossBar.Color.GREEN;
      }
   }

   /** Runs every second: hides bars that haven't changed for bossbar.hide-after-seconds. */
   public void tick() {
      long hideAfter = Math.max(1L, this.plugin.config().getLong("bossbar.hide-after-seconds", 4L)) * 1000L;
      long now = System.currentTimeMillis();
      Iterator<Entry<UUID, Shown>> iterator = this.shown.entrySet().iterator();
      while (iterator.hasNext()) {
         Entry<UUID, Shown> entry = iterator.next();
         if (now - entry.getValue().lastUpdate >= hideAfter) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
               player.hideBossBar(entry.getValue().bar);
            }
            iterator.remove();
         }
      }
   }

   public void hideAll() {
      for (Entry<UUID, Shown> entry : this.shown.entrySet()) {
         Player player = Bukkit.getPlayer(entry.getKey());
         if (player != null) {
            player.hideBossBar(entry.getValue().bar);
         }
      }
      this.shown.clear();
   }

   private static final class Shown {
      final BossBar bar;
      long lastUpdate;

      Shown(BossBar bar) {
         this.bar = bar;
      }
   }
}
