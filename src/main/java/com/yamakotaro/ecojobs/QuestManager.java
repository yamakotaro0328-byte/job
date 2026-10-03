package com.yamakotaro.ecojobs;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Daily quests: every player gets a fresh set of tasks each day, drawn from the action tables of
 * the jobs they have joined ("break 64 COAL_ORE as Miner"). Completing one pays a bonus on top of
 * the normal per-action pay. Stored in quests.yml regardless of storage.type, since quests are
 * short-lived (they reset daily).
 */
public class QuestManager {
   /** Actions whose reward "scale" is an item count rather than a per-level modifier. */
   private static final List<String> COUNT_SCALED_ACTIONS = List.of("craft-item", "smelt-item", "harvest-block");
   private final EcoJobsPlugin plugin;
   private final JobManager jobManager;
   private final Messages messages;
   private final File file;
   private final Map<UUID, DailyQuests> quests = new HashMap<>();
   private final Map<UUID, Streak> streaks = new HashMap<>();
   private PlayerJobManager playerJobManager;
   private boolean dirty;

   public QuestManager(EcoJobsPlugin plugin, JobManager jobManager, Messages messages) {
      this.plugin = plugin;
      this.jobManager = jobManager;
      this.messages = messages;
      this.file = new File(plugin.getDataFolder(), "quests.yml");
      this.load();
   }

   public void setPlayerJobManager(PlayerJobManager playerJobManager) {
      this.playerJobManager = playerJobManager;
   }

   private ConfigurationSection cfg() {
      ConfigurationSection section = this.plugin.config().getConfigurationSection("quests");
      return section != null ? section : new YamlConfiguration();
   }

   public boolean isEnabled() {
      return this.cfg().getBoolean("enabled", true);
   }

   private String today() {
      String zone = this.cfg().getString("reset-timezone", "Asia/Tokyo");
      try {
         return LocalDate.now(ZoneId.of(zone)).toString();
      } catch (Exception var3) {
         return LocalDate.now().toString();
      }
   }

   /** Milliseconds until the daily quests roll over. */
   public long millisUntilReset() {
      ZoneId zone;
      try {
         zone = ZoneId.of(this.cfg().getString("reset-timezone", "Asia/Tokyo"));
      } catch (Exception var3) {
         zone = ZoneId.systemDefault();
      }

      long next = LocalDate.now(zone).plusDays(1L).atStartOfDay(zone).toInstant().toEpochMilli();
      return Math.max(0L, next - System.currentTimeMillis());
   }

   /** Days in a row the player has cleared every quest (0 once a day is missed). */
   public int currentStreak(UUID uuid) {
      Streak streak = this.streaks.get(uuid);
      if (streak == null) {
         return 0;
      }

      String today = this.today();
      return streak.lastDate.equals(today) || streak.lastDate.equals(LocalDate.parse(today).minusDays(1L).toString()) ? streak.days : 0;
   }

   /** Called when the player clears today's set; returns the new streak length. */
   private int advanceStreak(UUID uuid) {
      String today = this.today();
      int days = this.currentStreak(uuid);
      Streak existing = this.streaks.get(uuid);
      if (existing == null || !existing.lastDate.equals(today)) {
         days++;
      }

      this.streaks.put(uuid, new Streak(Math.max(1, days), today));
      this.dirty = true;
      return Math.max(1, days);
   }

   /** All-clear bonus multiplier: +streak-bonus-per-day for each consecutive day after the first, capped. */
   public double streakMultiplier(int streak) {
      return streakMultiplier(streak, this.cfg().getDouble("streak-bonus-per-day", 0.1), this.cfg().getInt("streak-max-days", 7));
   }

   static double streakMultiplier(int streak, double bonusPerDay, int maxDays) {
      int counted = Math.min(Math.max(0, streak - 1), Math.max(0, maxDays));
      return 1.0 + counted * bonusPerDay;
   }

   public double allCompleteBonus() {
      return this.cfg().getDouble("all-complete-bonus-money", 500.0);
   }

   /** Returns today's quests for the player, generating a new set if the day rolled over. */
   public List<Quest> questsFor(Player player) {
      if (!this.isEnabled()) {
         return List.of();
      }
      String today = this.today();
      DailyQuests daily = this.quests.get(player.getUniqueId());
      if (daily == null || !daily.date.equals(today) || daily.quests.isEmpty()) {
         daily = new DailyQuests(today, this.generate(player));
         this.quests.put(player.getUniqueId(), daily);
         this.dirty = true;
      }
      return daily.quests;
   }

   private List<Quest> generate(Player player) {
      ConfigurationSection cfg = this.cfg();
      List<String> excluded = cfg.getStringList("excluded-actions");
      List<Quest> candidates = new ArrayList<>();

      for (String jobId : this.playerJobManager.joinedJobs(player.getUniqueId()).keySet()) {
         JobDefinition job = this.jobManager.get(jobId);
         if (job == null) {
            continue;
         }
         for (Entry<String, Map<String, ActionReward>> byType : job.getActionsByType().entrySet()) {
            if (excluded.contains(byType.getKey())) {
               continue;
            }
            for (Entry<String, ActionReward> byKey : byType.getValue().entrySet()) {
               ActionReward reward = byKey.getValue();
               double unitMoney = reward.moneyFor(byType.getKey().equals("enchant-item") ? 30.0 : 1.0);
               double unitXp = reward.xpFor(byType.getKey().equals("enchant-item") ? 30.0 : 1.0);
               if (unitMoney <= 0.0 && unitXp <= 0.0) {
                  continue;
               }
               int target = this.targetFor(unitMoney, unitXp);
               double multiplier = cfg.getDouble("reward-multiplier", 2.0);
               candidates.add(
                  new Quest(jobId, byType.getKey(), byKey.getKey(), target, 0, target * unitMoney * multiplier, target * unitXp * multiplier, false)
               );
            }
         }
      }

      Collections.shuffle(candidates);
      int perDay = Math.max(1, cfg.getInt("per-day", 3));
      List<Quest> picked = new ArrayList<>();
      List<String> usedJobs = new ArrayList<>();
      // First pass spreads quests across the player's jobs; second pass fills any remaining slots.
      for (Quest quest : candidates) {
         if (picked.size() < perDay && !usedJobs.contains(quest.jobId)) {
            picked.add(quest);
            usedJobs.add(quest.jobId);
         }
      }
      for (Quest quest : candidates) {
         if (picked.size() < perDay && !picked.contains(quest)) {
            picked.add(quest);
         }
      }
      return picked;
   }

   private int targetFor(double unitMoney, double unitXp) {
      ConfigurationSection cfg = this.cfg();
      int min = Math.max(1, cfg.getInt("min-amount", 5));
      int max = Math.max(min, cfg.getInt("max-amount", 128));
      double value = unitMoney > 0.0 ? unitMoney : unitXp;
      double base = cfg.getDouble("target-value", 400.0) / value;
      double jitter = 0.75 + ThreadLocalRandom.current().nextDouble() * 0.5;
      return (int)Math.max(min, Math.min(max, Math.round(base * jitter)));
   }

   /** Called for every paid job action; advances any matching quest. */
   public void onAction(Player player, String jobId, String actionType, String key, double scale) {
      if (!this.isEnabled() || this.playerJobManager == null) {
         return;
      }
      int amount = COUNT_SCALED_ACTIONS.contains(actionType) ? Math.max(1, (int)Math.round(scale)) : 1;
      List<Quest> list = this.questsFor(player);
      boolean completedAny = false;

      for (Quest quest : list) {
         if (quest.completed || !quest.jobId.equals(jobId) || !quest.actionType.equals(actionType)) {
            continue;
         }
         if (!quest.key.equalsIgnoreCase(key) && !quest.key.equalsIgnoreCase("DEFAULT")) {
            continue;
         }
         // An explicit entry for this key beats DEFAULT, so a DEFAULT quest only counts keys the job
         // doesn't list on their own.
         if (quest.key.equalsIgnoreCase("DEFAULT") && !key.equalsIgnoreCase("DEFAULT")) {
            JobDefinition job = this.jobManager.get(jobId);
            Map<String, ActionReward> table = job == null ? null : job.getActionsByType().get(actionType);
            if (table != null && table.containsKey(key.toUpperCase(Locale.ROOT))) {
               continue;
            }
         }
         quest.progress = Math.min(quest.target, quest.progress + amount);
         this.dirty = true;
         if (quest.progress >= quest.target) {
            quest.completed = true;
            completedAny = true;
            this.playerJobManager.grantBonus(player, jobId, quest.money, quest.xp);
            player.sendMessage(this.messages.get("quests.completed", this.placeholders(quest)));
            if (this.playerJobManager.isSoundEnabled(player)) {
               player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
            }
         }
      }

      if (completedAny && list.stream().allMatch(q -> q.completed)) {
         int streak = this.advanceStreak(player.getUniqueId());
         double bonus = this.allCompleteBonus() * this.streakMultiplier(streak);
         this.playerJobManager.grantBonus(player, null, bonus, 0.0);
         player.sendMessage(this.messages.get("quests.all-completed", Map.of("money", MoneyFormat.format(bonus))));
         if (streak > 1) {
            player.sendMessage(this.messages.get("quests.streak", Map.of(
               "days", String.valueOf(streak),
               "bonus", String.format("%.0f", (this.streakMultiplier(streak) - 1.0) * 100.0)
            )));
         }
      }
   }

   public Map<String, String> placeholders(Quest quest) {
      return Map.of(
         "job", this.messages.jobName(quest.jobId),
         "action", this.messages.raw("quests.actions." + quest.actionType, Map.of()),
         "target-name", quest.key.equalsIgnoreCase("DEFAULT") ? this.messages.raw("quests.any", Map.of()) : prettify(quest.key),
         "progress", String.valueOf(quest.progress),
         "amount", String.valueOf(quest.target),
         "money", MoneyFormat.format(quest.money),
         "xp", String.format("%.0f", quest.xp),
         "bar", progressBar(quest.progress, quest.target)
      );
   }

   public int completedCount(Player player) {
      return (int)this.questsFor(player).stream().filter(q -> q.completed).count();
   }

   /** Admin: throw away today's set so the next lookup rolls a new one. */
   public void reset(UUID uuid) {
      this.quests.remove(uuid);
      this.dirty = true;
   }

   public static String prettify(String key) {
      String lower = key.toLowerCase(Locale.ROOT).replace('_', ' ');
      return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
   }

   public static String progressBar(double current, double max) {
      int filled = max <= 0.0 ? 10 : (int)Math.min(10L, Math.round(current / max * 10.0));
      return "&a" + "|".repeat(filled) + "&7" + "|".repeat(10 - filled);
   }

   private void load() {
      if (!this.file.exists()) {
         return;
      }
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
      ConfigurationSection streakSection = yaml.getConfigurationSection("streaks");
      if (streakSection != null) {
         for (String uuidString : streakSection.getKeys(false)) {
            try {
               this.streaks.put(
                  UUID.fromString(uuidString),
                  new Streak(streakSection.getInt(uuidString + ".days"), streakSection.getString(uuidString + ".last", ""))
               );
            } catch (IllegalArgumentException var7) {
            }
         }
      }

      for (String uuidString : yaml.getKeys(false)) {
         if (uuidString.equals("streaks")) {
            continue;
         }

         ConfigurationSection section = yaml.getConfigurationSection(uuidString);
         if (section == null) {
            continue;
         }
         try {
            List<Quest> list = new ArrayList<>();
            for (Map<?, ?> raw : section.getMapList("quests")) {
               list.add(
                  new Quest(
                     String.valueOf(raw.get("job")),
                     String.valueOf(raw.get("action")),
                     String.valueOf(raw.get("key")),
                     ((Number)raw.get("target")).intValue(),
                     ((Number)raw.get("progress")).intValue(),
                     ((Number)raw.get("money")).doubleValue(),
                     ((Number)raw.get("xp")).doubleValue(),
                     Boolean.TRUE.equals(raw.get("completed"))
                  )
               );
            }
            this.quests.put(UUID.fromString(uuidString), new DailyQuests(section.getString("date", ""), list));
         } catch (RuntimeException var8) {
            this.plugin.getLogger().warning("Skipping malformed quest data for " + uuidString);
         }
      }
   }

   public void save() {
      if (!this.dirty) {
         return;
      }
      String today = this.today();
      YamlConfiguration yaml = new YamlConfiguration();
      for (Entry<UUID, DailyQuests> entry : this.quests.entrySet()) {
         if (!entry.getValue().date.equals(today)) {
            continue;
         }
         List<Map<String, Object>> list = new ArrayList<>();
         for (Quest quest : entry.getValue().quests) {
            Map<String, Object> raw = new HashMap<>();
            raw.put("job", quest.jobId);
            raw.put("action", quest.actionType);
            raw.put("key", quest.key);
            raw.put("target", quest.target);
            raw.put("progress", quest.progress);
            raw.put("money", quest.money);
            raw.put("xp", quest.xp);
            raw.put("completed", quest.completed);
            list.add(raw);
         }
         String base = entry.getKey().toString();
         yaml.set(base + ".date", entry.getValue().date);
         yaml.set(base + ".quests", list);
      }
      for (Entry<UUID, Streak> entry : this.streaks.entrySet()) {
         yaml.set("streaks." + entry.getKey() + ".days", entry.getValue().days);
         yaml.set("streaks." + entry.getKey() + ".last", entry.getValue().lastDate);
      }

      try {
         yaml.save(this.file);
         this.dirty = false;
      } catch (IOException var8) {
         this.plugin.getLogger().warning("Could not save quests.yml: " + var8.getMessage());
      }
   }

   private record Streak(int days, String lastDate) {
   }

   private record DailyQuests(String date, List<Quest> quests) {
   }

   public static final class Quest {
      public final String jobId;
      public final String actionType;
      public final String key;
      public final int target;
      public int progress;
      public final double money;
      public final double xp;
      public boolean completed;

      Quest(String jobId, String actionType, String key, int target, int progress, double money, double xp, boolean completed) {
         this.jobId = jobId;
         this.actionType = actionType;
         this.key = key;
         this.target = target;
         this.progress = progress;
         this.money = money;
         this.xp = xp;
         this.completed = completed;
      }
   }
}
