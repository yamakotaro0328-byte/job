package com.yamakotaro.ecojobs;

import com.yamakotaro.ecojobs.storage.JobStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlayerJobManager {
   private static final long TOP_CACHE_TTL_MILLIS = 5000L;
   private final EcoJobsPlugin plugin;
   private final JobManager jobManager;
   private final EconomyHolder economyHolder;
   private final Messages messages;
   private final JobStorage storage;
   private final JobOverrides jobOverrides;
   private final BoosterManager boosterManager;
   private final PerkManager perkManager;
   private final Map<UUID, PlayerJobData> data = new HashMap<>();
   private final Set<UUID> dirtyUuids = new HashSet<>();
   private final Map<String, PlayerJobManager.CachedTop> topCache = new HashMap<>();
   private final Map<UUID, Map<String, Double>> pendingEarnings = new HashMap<>();
   private final Map<UUID, HourlyEarnings> hourlyEarnings = new HashMap<>();
   private boolean noEconomyWarned;
   private QuestManager questManager;
   private JobBossBar bossBar;

   public PlayerJobManager(
      EcoJobsPlugin plugin,
      JobManager jobManager,
      EconomyHolder economyHolder,
      Messages messages,
      JobStorage storage,
      JobOverrides jobOverrides,
      BoosterManager boosterManager,
      PerkManager perkManager
   ) {
      this.plugin = plugin;
      this.jobManager = jobManager;
      this.economyHolder = economyHolder;
      this.messages = messages;
      this.storage = storage;
      this.jobOverrides = jobOverrides;
      this.boosterManager = boosterManager;
      this.perkManager = perkManager;
      this.data.putAll(storage.loadAll());
   }

   public void setQuestManager(QuestManager questManager) {
      this.questManager = questManager;
   }

   public void setBossBar(JobBossBar bossBar) {
      this.bossBar = bossBar;
   }

   public boolean isJoined(UUID uuid, String jobId) {
      PlayerJobData playerData = this.data.get(uuid);
      return playerData != null && playerData.getJoined().contains(jobId);
   }

   public Map<String, PlayerJobProgress> joinedJobs(UUID uuid) {
      PlayerJobData playerData = this.data.get(uuid);
      if (playerData == null) {
         return Map.of();
      } else {
         Map<String, PlayerJobProgress> result = new LinkedHashMap<>();

         for (String jobId : playerData.getJoined()) {
            PlayerJobProgress progress = playerData.getProgress().get(jobId);
            if (progress != null) {
               result.put(jobId, progress);
            }
         }

         return result;
      }
   }

   public Map<String, PlayerJobProgress> allProgress(UUID uuid) {
      PlayerJobData playerData = this.data.get(uuid);
      return playerData != null ? playerData.getProgress() : Map.of();
   }

   public UUID findByName(String name) {
      for (Entry<UUID, PlayerJobData> entry : this.data.entrySet()) {
         if (entry.getValue().getName().equalsIgnoreCase(name)) {
            return entry.getKey();
         }
      }

      return null;
   }

   public String nameOf(UUID uuid) {
      PlayerJobData playerData = this.data.get(uuid);
      return playerData != null ? playerData.getName() : null;
   }

   public boolean isSoundEnabled(Player player) {
      return this.soundEnabledFor(player.getUniqueId());
   }

   public void toggleSoundEnabled(Player player) {
      PlayerJobData playerData = this.dataFor(player);
      playerData.setSoundEnabled(!playerData.isSoundEnabled());
      this.markDirty(player.getUniqueId());
   }

   public boolean isActionBarEnabled(Player player) {
      PlayerJobData playerData = this.data.get(player.getUniqueId());
      return playerData == null || playerData.isActionBarEnabled();
   }

   public void toggleActionBarEnabled(Player player) {
      PlayerJobData playerData = this.dataFor(player);
      playerData.setActionBarEnabled(!playerData.isActionBarEnabled());
      this.markDirty(player.getUniqueId());
   }

   private boolean soundEnabledFor(UUID uuid) {
      PlayerJobData playerData = this.data.get(uuid);
      return playerData == null || playerData.isSoundEnabled();
   }

   public PlayerJobManager.JoinResult join(Player player, String jobId) {
      JobDefinition job = this.jobManager.get(jobId);
      if (job == null) {
         return PlayerJobManager.JoinResult.UNKNOWN_JOB;
      } else if (!this.jobOverrides.isEnabled(job.getId())) {
         return PlayerJobManager.JoinResult.JOB_DISABLED;
      } else {
         PlayerJobData playerData = this.dataFor(player);
         if (playerData.getJoined().contains(job.getId())) {
            return PlayerJobManager.JoinResult.ALREADY_JOINED;
         } else {
            boolean bypassLimit = player.hasPermission("ecojobs.bypass.maxjobs");
            if (!bypassLimit && playerData.getJoined().size() >= this.jobManager.maxConcurrentJobs()) {
               return PlayerJobManager.JoinResult.MAX_JOBS_REACHED;
            } else {
               playerData.getProgress().computeIfAbsent(job.getId(), k -> new PlayerJobProgress(1, 0.0));
               playerData.getJoined().add(job.getId());
               this.markDirty(player.getUniqueId());
               return PlayerJobManager.JoinResult.SUCCESS;
            }
         }
      }
   }

   public PlayerJobManager.LeaveResult leave(UUID uuid, String jobId) {
      JobDefinition job = this.jobManager.get(jobId);
      if (job == null) {
         return PlayerJobManager.LeaveResult.UNKNOWN_JOB;
      } else {
         PlayerJobData playerData = this.data.get(uuid);
         if (playerData != null && playerData.getJoined().remove(job.getId())) {
            this.markDirty(uuid);
            return PlayerJobManager.LeaveResult.SUCCESS;
         } else {
            return PlayerJobManager.LeaveResult.NOT_JOINED;
         }
      }
   }

   public PlayerJobManager.PrestigeResult prestige(Player player, String jobId) {
      JobDefinition job = this.jobManager.get(jobId);
      if (job == null) {
         return PlayerJobManager.PrestigeResult.UNKNOWN_JOB;
      } else {
         PlayerJobProgress progress = this.joinedJobs(player.getUniqueId()).get(job.getId());
         if (progress == null) {
            return PlayerJobManager.PrestigeResult.NOT_JOINED;
         } else if (progress.getLevel() < this.jobManager.maxLevel()) {
            return PlayerJobManager.PrestigeResult.NOT_MAX_LEVEL;
         } else {
            progress.setLevel(1);
            progress.setXp(0.0);
            progress.setPrestige(progress.getPrestige() + 1);
            this.markDirty(player.getUniqueId());
            this.runRewardCommands("reward-commands.prestige", player, job.getId(), progress.getLevel(), progress.getPrestige());
            Bukkit.getServer()
               .sendMessage(
                  this.messages
                     .get(
                        "jobs.prestige-broadcast",
                        Map.of("player", player.getName(), "job", this.messages.jobName(job.getId()), "prestige", String.valueOf(progress.getPrestige()))
                     )
               );
            return PlayerJobManager.PrestigeResult.SUCCESS;
         }
      }
   }

   public void reward(Player player, String jobId, String actionType, String key, double scale) {
      this.reward(player, jobId, actionType, key, scale, 1.0);
   }

   /** Same as {@link #reward(Player, String, String, String, double)}, with the money scaled by payMultiplier (anti-farm). */
   public void reward(Player player, String jobId, String actionType, String key, double scale, double payMultiplier) {
      JobDefinition job = this.jobManager.get(jobId);
      if (job != null) {
         ActionReward actionReward = job.getReward(actionType, key);
         if (actionReward != null) {
            PlayerJobProgress progress = this.joinedJobs(player.getUniqueId()).get(job.getId());
            if (progress != null) {
               this.applyReward(player, job, progress, actionReward.moneyFor(scale) * payMultiplier, actionReward.xpFor(scale) * payMultiplier);
               if (this.questManager != null) {
                  this.questManager.onAction(player, job.getId(), actionType, key, scale);
               }
            }
         }
      }
   }

   public void checkExplorerMilestones(Player player, String worldName, double currentDistance) {
      JobDefinition explorer = this.jobManager.get("explorer");
      if (explorer != null) {
         PlayerJobProgress progress = this.joinedJobs(player.getUniqueId()).get("explorer");
         if (progress != null) {
            PlayerJobData playerData = this.data.get(player.getUniqueId());
            double perMilestone = this.jobManager.explorerDistancePerMilestone();
            if (!(perMilestone <= 0.0)) {
               double farthest = playerData.getExplorerFarthestDistance(worldName);
               double previousMilestones = Math.floor(farthest / perMilestone);
               double currentMilestones = Math.floor(currentDistance / perMilestone);
               if (currentDistance > farthest) {
                  playerData.setExplorerFarthestDistance(worldName, currentDistance);
                  this.markDirty(player.getUniqueId());
               }

               if (currentMilestones > previousMilestones) {
                  this.applyReward(player, explorer, progress, this.jobManager.explorerMoneyPerMilestone(), this.jobManager.explorerXpPerMilestone());
               }
            }
         }
      }
   }

   private void applyReward(Player player, JobDefinition job, PlayerJobProgress progress, double baseMoney, double baseXp) {
      int effectiveLevel = this.perkManager.effectiveLevel(progress);
      double money = baseMoney * this.moneyMultiplier(job, progress);
      double xp = baseXp * this.boosterManager.xpMultiplierFor(job.getId());
      money = this.applyHourlyCap(player, money);
      if (money > 0.0) {
         Economy economy = this.economyHolder.get();
         if (economy != null) {
            economy.depositPlayer(player, money);
            if (this.isActionBarEnabled(player)) {
               this.queueEarnedActionBar(player.getUniqueId(), job.getId(), money);
            }
         } else if (!this.noEconomyWarned) {
            this.noEconomyWarned = true;
            this.plugin.getLogger().warning("No economy plugin found (Vault) - job levels/xp still work, but no money will be paid out.");
         }
      }

      if (xp > 0.0) {
         progress.setXp(progress.getXp() + xp);
         this.checkLevelUp(player, job, progress);
         this.showBossBar(player, job.getId(), progress);
      }

      int bonusVanillaXp = this.perkManager.xpOrbBonus(job, effectiveLevel);
      if (bonusVanillaXp > 0) {
         player.giveExp(bonusVanillaXp);
      }

      this.markDirty(player.getUniqueId());
   }

   /**
    * Pays a flat bonus (daily quests etc.) outside the per-action formula: no level/booster
    * multipliers and no hourly cap. jobId may be null for money-only bonuses.
    */
   public void grantBonus(Player player, String jobId, double money, double xp) {
      if (money > 0.0) {
         Economy economy = this.economyHolder.get();
         if (economy != null) {
            economy.depositPlayer(player, money);
         }
      }

      JobDefinition job = jobId == null ? null : this.jobManager.get(jobId);
      PlayerJobProgress progress = job == null ? null : this.joinedJobs(player.getUniqueId()).get(job.getId());
      if (progress != null && xp > 0.0) {
         progress.setXp(progress.getXp() + xp);
         this.checkLevelUp(player, job, progress);
         this.showBossBar(player, job.getId(), progress);
         this.markDirty(player.getUniqueId());
      }
   }

   private void showBossBar(Player player, String jobId, PlayerJobProgress progress) {
      if (this.bossBar != null) {
         boolean maxed = progress.getLevel() >= this.jobManager.maxLevel();
         this.bossBar.show(player, jobId, progress.getLevel(), progress.getXp(), this.xpToNextLevel(progress.getLevel()), maxed);
      }
   }

   /** anti-farm.max-money-per-hour: once a player has earned this much from job actions in the current clock hour, further actions pay xp only. */
   private double applyHourlyCap(Player player, double money) {
      double cap = this.plugin.config().getDouble("anti-farm.max-money-per-hour", 0.0);
      if (cap <= 0.0 || money <= 0.0 || player.hasPermission("ecojobs.bypass.hourlycap")) {
         return money;
      } else {
         long hour = System.currentTimeMillis() / 3600000L;
         HourlyEarnings earnings = this.hourlyEarnings.get(player.getUniqueId());
         if (earnings == null || earnings.hour != hour) {
            earnings = new HourlyEarnings(hour);
            this.hourlyEarnings.put(player.getUniqueId(), earnings);
         }

         double allowed = Math.max(0.0, Math.min(money, cap - earnings.earned));
         earnings.earned += allowed;
         if (allowed < money && !earnings.warned) {
            earnings.warned = true;
            player.sendMessage(this.messages.get("jobs.hourly-cap-reached", Map.of("cap", MoneyFormat.format(cap))));
         }

         return allowed;
      }
   }

   public double hourlyEarned(UUID uuid) {
      HourlyEarnings earnings = this.hourlyEarnings.get(uuid);
      return earnings != null && earnings.hour == System.currentTimeMillis() / 3600000L ? earnings.earned : 0.0;
   }

   /** Runs the console commands configured under the given path, e.g. reward-commands.levels.10. */
   private void runRewardCommands(String path, Player player, String jobId, int level, int prestige) {
      for (String command : this.plugin.config().getStringList(path)) {
         String resolved = command.replace("{player}", player.getName())
            .replace("{uuid}", player.getUniqueId().toString())
            .replace("{job}", jobId)
            .replace("{level}", String.valueOf(level))
            .replace("{prestige}", String.valueOf(prestige));
         if (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
         }

         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
      }
   }

   /** Total multiplier applied to a job's base pay: level + prestige + perks, admin override, and boosters. */
   public double moneyMultiplier(JobDefinition job, PlayerJobProgress progress) {
      int level = progress == null ? 0 : progress.getLevel();
      int prestige = progress == null ? 0 : progress.getPrestige();
      int effectiveLevel = progress == null ? 0 : this.perkManager.effectiveLevel(progress);
      double levelMultiplier = 1.0
         + level * this.jobManager.payBonusPerLevel()
         + prestige * this.jobManager.prestigeBonusPerPrestige()
         + this.perkManager.payBonusMultiplier(job, effectiveLevel);
      return levelMultiplier * this.jobOverrides.payMultiplier(job.getId()) * this.boosterManager.moneyMultiplierFor(job.getId());
   }

   /** 1-based leaderboard rank of the player in the job, or 0 if they have no progress in it. */
   public int rankOf(String jobId, UUID uuid) {
      List<PlayerJobManager.TopEntry> sorted = this.sortedTop(jobId);
      for (int i = 0; i < sorted.size(); i++) {
         if (sorted.get(i).uuid().equals(uuid)) {
            return i + 1;
         }
      }

      return 0;
   }

   public int rankedCount(String jobId) {
      return this.sortedTop(jobId).size();
   }

   private void queueEarnedActionBar(UUID uuid, String jobId, double money) {
      this.pendingEarnings.computeIfAbsent(uuid, k -> new HashMap<>()).merge(jobId, money, Double::sum);
   }

   public void flushEarnedActionBars() {
      if (!this.pendingEarnings.isEmpty()) {
         for (Entry<UUID, Map<String, Double>> playerEntry : this.pendingEarnings.entrySet()) {
            Player player = Bukkit.getPlayer(playerEntry.getKey());
            if (player != null) {
               for (Entry<String, Double> jobEntry : playerEntry.getValue().entrySet()) {
                  player.sendActionBar(
                     this.messages
                        .get("jobs.earned", Map.of("money", MoneyFormat.format(jobEntry.getValue()), "job", this.messages.jobName(jobEntry.getKey())))
                  );
               }
            }
         }

         this.pendingEarnings.clear();
      }
   }

   private void checkLevelUp(Player player, JobDefinition job, PlayerJobProgress progress) {
      int maxLevel = this.jobManager.maxLevel();

      while (progress.getLevel() < maxLevel) {
         double required = this.xpToNextLevel(progress.getLevel());
         if (progress.getXp() < required) {
            break;
         }

         progress.setXp(progress.getXp() - required);
         progress.setLevel(progress.getLevel() + 1);
         player.sendMessage(this.messages.get("jobs.level-up", Map.of("job", this.messages.jobName(job.getId()), "level", String.valueOf(progress.getLevel()))));
         if (this.soundEnabledFor(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
         }

         this.runRewardCommands("reward-commands.every-level", player, job.getId(), progress.getLevel(), progress.getPrestige());
         this.runRewardCommands("reward-commands.levels." + progress.getLevel(), player, job.getId(), progress.getLevel(), progress.getPrestige());
         this.runRewardCommands("reward-commands.jobs." + job.getId() + "." + progress.getLevel(), player, job.getId(), progress.getLevel(), progress.getPrestige());
         if (this.jobManager.milestoneLevels().contains(progress.getLevel())) {
            this.awardMilestone(player, job, progress.getLevel());
         }

         if (progress.getLevel() >= maxLevel) {
            player.sendMessage(this.messages.get("jobs.max-level-reached", Map.of("job", this.messages.jobName(job.getId()))));
         }
      }

      if (progress.getLevel() >= maxLevel) {
         progress.setXp(0.0);
      }
   }

   private void awardMilestone(Player player, JobDefinition job, int level) {
      double money = this.jobManager.milestoneBonusMoney();
      if (money > 0.0) {
         Economy economy = this.economyHolder.get();
         if (economy != null) {
            economy.depositPlayer(player, money);
         }
      }

      if (this.markMilestoneAnnounced(player.getUniqueId(), job.getId(), level)) {
         Bukkit.getServer()
            .sendMessage(
               this.messages
                  .get(
                     "jobs.milestone-broadcast",
                     Map.of(
                        "player",
                        player.getName(),
                        "job",
                        this.messages.jobName(job.getId()),
                        "level",
                        String.valueOf(level),
                        "money",
                        MoneyFormat.format(money)
                     )
                  )
            );
      } else {
         player.sendMessage(
            this.messages
               .get(
                  "jobs.milestone-repeat",
                  Map.of("job", this.messages.jobName(job.getId()), "level", String.valueOf(level), "money", MoneyFormat.format(money))
               )
         );
      }

      if (this.soundEnabledFor(player.getUniqueId())) {
         player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
      }
   }

   private boolean markMilestoneAnnounced(UUID uuid, String jobId, int level) {
      PlayerJobData playerData = this.data.get(uuid);
      if (playerData == null) {
         return true;
      } else {
         boolean firstTime = playerData.getAnnouncedMilestones().add(jobId + ":" + level);
         if (firstTime) {
            this.markDirty(uuid);
         }

         return firstTime;
      }
   }

   public double xpToNextLevel(int level) {
      return this.jobManager.baseXpToLevel() * Math.pow(level, this.jobManager.growthExponent());
   }

   public List<PlayerJobManager.TopEntry> top(String jobId, int limit) {
      List<PlayerJobManager.TopEntry> sorted = this.sortedTop(jobId);
      return sorted.size() > limit ? sorted.subList(0, limit) : sorted;
   }

   private List<PlayerJobManager.TopEntry> sortedTop(String jobId) {
      PlayerJobManager.CachedTop cached = this.topCache.get(jobId);
      long now = System.currentTimeMillis();
      if (cached != null && now - cached.computedAtMillis() < 5000L) {
         return cached.sorted();
      } else {
         List<PlayerJobManager.TopEntry> entries = new ArrayList<>();

         for (Entry<UUID, PlayerJobData> playerEntry : this.data.entrySet()) {
            PlayerJobProgress progress = playerEntry.getValue().getProgress().get(jobId);
            if (progress != null) {
               entries.add(
                  new PlayerJobManager.TopEntry(
                     playerEntry.getKey(), playerEntry.getValue().getName(), progress.getLevel(), progress.getXp(), progress.getPrestige()
                  )
               );
            }
         }

         entries.sort(
            Comparator.comparingInt(PlayerJobManager.TopEntry::prestige)
               .reversed()
               .thenComparing(Comparator.comparingInt(PlayerJobManager.TopEntry::level).reversed())
               .thenComparing(Comparator.comparingDouble(PlayerJobManager.TopEntry::xp).reversed())
         );
         List<PlayerJobManager.TopEntry> sorted = List.copyOf(entries);
         this.topCache.put(jobId, new PlayerJobManager.CachedTop(now, sorted));
         return sorted;
      }
   }

   private PlayerJobData dataFor(Player player) {
      PlayerJobData playerData = this.data.computeIfAbsent(player.getUniqueId(), k -> new PlayerJobData(player.getName()));
      playerData.setName(player.getName());
      return playerData;
   }

   private void markDirty(UUID uuid) {
      this.dirtyUuids.add(uuid);
   }

   public void save() {
      if (!this.dirtyUuids.isEmpty()) {
         this.storage.saveAll(this.data, this.dirtyUuids);
         this.dirtyUuids.clear();
      }
   }

   public void close() {
      this.save();
      if (this.questManager != null) {
         this.questManager.save();
      }

      this.storage.close();
   }

   private static final class HourlyEarnings {
      final long hour;
      double earned;
      boolean warned;

      HourlyEarnings(long hour) {
         this.hour = hour;
      }
   }

   private record CachedTop(long computedAtMillis, List<PlayerJobManager.TopEntry> sorted) {
   }

   public static enum JoinResult {
      SUCCESS,
      UNKNOWN_JOB,
      JOB_DISABLED,
      ALREADY_JOINED,
      MAX_JOBS_REACHED;
   }

   public static enum LeaveResult {
      SUCCESS,
      UNKNOWN_JOB,
      NOT_JOINED;
   }

   public static enum PrestigeResult {
      SUCCESS,
      UNKNOWN_JOB,
      NOT_JOINED,
      NOT_MAX_LEVEL;
   }

   public record TopEntry(UUID uuid, String name, int level, double xp, int prestige) {
   }
}
