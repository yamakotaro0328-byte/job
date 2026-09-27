package com.yamakotaro.ecojobs;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

/**
 * "Happy hours": starts boosters automatically on configured days/times (scheduled-boosters in
 * config.yml). Each window fires at most once per day, so an admin stopping it early sticks.
 */
public class BoosterScheduler implements Runnable {
   private final EcoJobsPlugin plugin;
   private final BoosterManager boosterManager;
   private final Messages messages;
   private final Set<String> firedToday = new HashSet<>();

   public BoosterScheduler(EcoJobsPlugin plugin, BoosterManager boosterManager, Messages messages) {
      this.plugin = plugin;
      this.boosterManager = boosterManager;
      this.messages = messages;
   }

   @Override
   public void run() {
      ConfigurationSection root = this.plugin.config().getConfigurationSection("scheduled-boosters");
      if (root == null || !root.getBoolean("enabled", false)) {
         return;
      }

      ZoneId zone;
      try {
         zone = ZoneId.of(root.getString("timezone", "Asia/Tokyo"));
      } catch (Exception var14) {
         zone = ZoneId.systemDefault();
      }

      ZonedDateTime now = ZonedDateTime.now(zone);
      String date = now.toLocalDate().toString();
      this.firedToday.removeIf(key -> !key.endsWith("@" + date));
      ConfigurationSection list = root.getConfigurationSection("list");
      if (list == null) {
         return;
      }

      for (String id : list.getKeys(false)) {
         ConfigurationSection entry = list.getConfigurationSection(id);
         if (entry == null || this.firedToday.contains(id + "@" + date) || !this.dayMatches(entry.getStringList("days"), now.getDayOfWeek())) {
            continue;
         }

         LocalTime start;
         LocalTime end;
         try {
            start = LocalTime.parse(entry.getString("start", "00:00"));
            end = LocalTime.parse(entry.getString("end", "00:00"));
         } catch (Exception var13) {
            this.plugin.getLogger().warning("scheduled-boosters." + id + ": start/end must look like 20:00");
            continue;
         }

         LocalTime time = now.toLocalTime();
         if (time.isBefore(start) || !time.isBefore(end)) {
            continue;
         }

         String scope = entry.getString("scope", BoosterManager.GLOBAL_SCOPE).toLowerCase(Locale.ROOT);
         if (this.boosterManager.getActiveBooster(scope) != null) {
            // Don't clobber a booster an admin started by hand; try again once it ends.
            continue;
         }

         double money = entry.getDouble("money", 1.5);
         double xp = entry.getDouble("xp", 1.5);
         long millis = java.time.Duration.between(time, end).toMillis();
         this.firedToday.add(id + "@" + date);
         this.boosterManager.start(scope, money, xp, millis, id);
         String scopeLabel = BoosterManager.GLOBAL_SCOPE.equals(scope) ? this.messages.raw("jobs.booster-scope-all", Map.of()) : this.messages.jobName(scope);
         Bukkit.getServer().sendMessage(this.messages.get("jobs.booster-scheduled", Map.of(
            "name", entry.getString("name", id),
            "scope", scopeLabel,
            "money", String.format("%.1f", money),
            "xp", String.format("%.1f", xp),
            "end", end.toString()
         )));
      }
   }

   private boolean dayMatches(List<String> days, DayOfWeek today) {
      if (days.isEmpty()) {
         return true;
      }

      for (String day : days) {
         String upper = day.toUpperCase(Locale.ROOT);
         if (upper.equals("ALL") || upper.equals("EVERYDAY") || today.name().startsWith(upper)) {
            return true;
         }
      }

      return false;
   }
}
