package com.yamakotaro.ecojobs.commands;

import com.yamakotaro.ecojobs.ActionReward;
import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.EcoJobsPlugin;
import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.MoneyFormat;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.QuestManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import com.yamakotaro.ecojobs.TabCompleteUtil;
import com.yamakotaro.ecojobs.menu.AdminMenu;
import com.yamakotaro.ecojobs.menu.HubMenu;
import com.yamakotaro.ecojobs.menu.MenuContext;
import com.yamakotaro.ecojobs.menu.QuestMenu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor, TabCompleter {
   private final EcoJobsPlugin plugin;
   private final JobManager jobManager;
   private final PlayerJobManager playerJobManager;
   private final JobOverrides jobOverrides;
   private final BoosterManager boosterManager;
   private final Messages messages;
   private final QuestManager questManager;
   private final MenuContext menuContext;
   private static final int CHAT_PAGE_SIZE = 8;

   public JobsCommand(
      EcoJobsPlugin plugin,
      JobManager jobManager,
      PlayerJobManager playerJobManager,
      JobOverrides jobOverrides,
      BoosterManager boosterManager,
      Messages messages,
      QuestManager questManager,
      MenuContext menuContext
   ) {
      this.questManager = questManager;
      this.menuContext = menuContext;
      this.plugin = plugin;
      this.jobManager = jobManager;
      this.playerJobManager = playerJobManager;
      this.jobOverrides = jobOverrides;
      this.boosterManager = boosterManager;
      this.messages = messages;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         this.handleMenu(sender);
         return true;
      } else {
         String var5 = args[0].toLowerCase();
         switch (var5) {
            case "join":
               this.handleJoin(sender, args);
               break;
            case "leave":
               this.handleLeave(sender, args);
               break;
            case "list":
               this.handleList(sender, args);
               break;
            case "stats":
               this.handleStats(sender, args);
               break;
            case "top":
               this.handleTop(sender, args);
               break;
            case "menu":
               this.handleMenu(sender);
               break;
            case "info":
               this.handleInfo(sender, args);
               break;
            case "prestige":
               this.handlePrestige(sender, args);
               break;
            case "admin":
               this.handleAdmin(sender);
               break;
            case "booster":
               this.handleBooster(sender, args);
               break;
            case "reload":
               this.handleReload(sender);
               break;
            case "quests":
            case "quest":
               this.handleQuests(sender, args);
               break;
            default:
               sender.sendMessage(this.messages.get("jobs.usage", Map.of()));
         }

         return true;
      }
   }

   private void handleJoin(CommandSender sender, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.use")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else if (args.length != 2) {
            player.sendMessage(this.messages.get("jobs.usage", Map.of()));
         } else {
            String jobId = args[1].toLowerCase();
            switch (this.playerJobManager.join(player, jobId)) {
               case UNKNOWN_JOB:
                  player.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
                  break;
               case JOB_DISABLED:
                  player.sendMessage(this.messages.get("jobs.job-disabled", Map.of("job", this.messages.jobName(jobId))));
                  break;
               case ALREADY_JOINED:
                  player.sendMessage(this.messages.get("jobs.already-joined", Map.of("job", this.messages.jobName(jobId))));
                  break;
               case MAX_JOBS_REACHED:
                  player.sendMessage(this.messages.get("jobs.max-jobs-reached", Map.of("max", String.valueOf(this.jobManager.maxConcurrentJobs()))));
                  break;
               case SUCCESS:
                  player.sendMessage(this.messages.get("jobs.joined", Map.of("job", this.messages.jobName(jobId))));
            }
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   private void handleLeave(CommandSender sender, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.use")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else if (args.length != 2) {
            player.sendMessage(this.messages.get("jobs.usage", Map.of()));
         } else {
            String jobId = args[1].toLowerCase();
            switch (this.playerJobManager.leave(player.getUniqueId(), jobId)) {
               case UNKNOWN_JOB:
                  player.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
                  break;
               case NOT_JOINED:
                  player.sendMessage(this.messages.get("jobs.not-joined", Map.of("job", this.messages.jobName(jobId))));
                  break;
               case SUCCESS:
                  player.sendMessage(this.messages.get("jobs.left", Map.of("job", this.messages.jobName(jobId))));
            }
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   private void handleList(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ecojobs.use")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else {
         Player player = sender instanceof Player p ? p : null;
         Map<String, PlayerJobProgress> allProgress = player != null ? this.playerJobManager.allProgress(player.getUniqueId()) : Map.of();
         List<Component> entries = new ArrayList<>();

         for (String jobId : this.jobManager.all().keySet()) {
            PlayerJobProgress progress = allProgress.get(jobId);
            if (progress == null) {
               entries.add(this.messages.get("jobs.list-entry-not-joined", Map.of("job", this.messages.jobName(jobId))));
            } else {
               String key = player != null && this.playerJobManager.isJoined(player.getUniqueId(), jobId) ? "jobs.list-entry-joined" : "jobs.list-entry-left";
               entries.add(
                  this.messages
                     .get(
                        key,
                        Map.of(
                           "job",
                           this.messages.jobName(jobId),
                           "level",
                           String.valueOf(progress.getLevel()),
                           "prestige",
                           String.valueOf(progress.getPrestige()),
                           "xp",
                           String.format("%.0f", progress.getXp()),
                           "next_xp",
                           String.format("%.0f", this.playerJobManager.xpToNextLevel(progress.getLevel()))
                        )
                     )
               );
            }
         }

         this.sendPaged(sender, this.messages.get("jobs.list-header", Map.of()), entries, parsePage(args, 1));
      }
   }

   private void handleStats(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ecojobs.use")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else {
         UUID targetUuid;
         String targetName;
         boolean self;
         if (args.length >= 2) {
            Player online = this.plugin.getServer().getPlayerExact(args[1]);
            if (online != null) {
               targetUuid = online.getUniqueId();
               targetName = online.getName();
            } else {
               targetUuid = this.playerJobManager.findByName(args[1]);
               if (targetUuid == null) {
                  sender.sendMessage(this.messages.get("general.player-not-found", Map.of("player", args[1])));
                  return;
               }

               targetName = this.playerJobManager.nameOf(targetUuid);
            }

            self = sender instanceof Player p && p.getUniqueId().equals(targetUuid);
         } else {
            if (!(sender instanceof Player player)) {
               sender.sendMessage(this.messages.get("general.players-only", Map.of()));
               return;
            }

            targetUuid = player.getUniqueId();
            targetName = player.getName();
            self = true;
         }

         Map<String, PlayerJobProgress> allProgress = this.playerJobManager.allProgress(targetUuid);
         if (allProgress.isEmpty()) {
            sender.sendMessage(this.messages.get(self ? "jobs.stats-none" : "jobs.stats-none-other", Map.of("player", targetName)));
         } else {
            List<Component> entries = new ArrayList<>();

            for (Entry<String, PlayerJobProgress> entry : allProgress.entrySet()) {
               PlayerJobProgress progress = entry.getValue();
               boolean active = this.playerJobManager.isJoined(targetUuid, entry.getKey());
               entries.add(
                  this.messages
                     .get(
                        active ? "jobs.stats-entry" : "jobs.stats-entry-inactive",
                        Map.of(
                           "job",
                           this.messages.jobName(entry.getKey()),
                           "level",
                           String.valueOf(progress.getLevel()),
                           "prestige",
                           String.valueOf(progress.getPrestige()),
                           "xp",
                           String.format("%.0f", progress.getXp()),
                           "next_xp",
                           String.format("%.0f", this.playerJobManager.xpToNextLevel(progress.getLevel()))
                        )
                     )
               );
            }

            this.sendPaged(
               sender, this.messages.get(self ? "jobs.stats-header" : "jobs.stats-header-other", Map.of("player", targetName)), entries, parsePage(args, 2)
            );
         }
      }
   }

   private void handleTop(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ecojobs.top")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else if (args.length != 2) {
         sender.sendMessage(this.messages.get("jobs.usage", Map.of()));
      } else {
         String jobId = args[1].toLowerCase();
         if (this.jobManager.get(jobId) == null) {
            sender.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
         } else {
            List<PlayerJobManager.TopEntry> top = this.playerJobManager.top(jobId, 10);
            sender.sendMessage(this.messages.get("jobs.top-header", Map.of("job", this.messages.jobName(jobId))));
            if (top.isEmpty()) {
               sender.sendMessage(this.messages.get("jobs.top-empty", Map.of("job", this.messages.jobName(jobId))));
            } else {
               int rank = 1;

               for (PlayerJobManager.TopEntry entry : top) {
                  sender.sendMessage(
                     this.messages
                        .get(
                           "jobs.top-entry",
                           Map.of(
                              "rank",
                              String.valueOf(rank++),
                              "player",
                              entry.name(),
                              "level",
                              String.valueOf(entry.level()),
                              "prestige",
                              String.valueOf(entry.prestige()),
                              "xp",
                              String.format("%.0f", entry.xp())
                           )
                        )
                  );
               }
            }
         }
      }
   }

   private void handleInfo(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ecojobs.use")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else if (args.length < 2) {
         sender.sendMessage(this.messages.get("jobs.usage", Map.of()));
      } else {
         String jobId = args[1].toLowerCase();
         JobDefinition job = this.jobManager.get(jobId);
         if (job == null) {
            sender.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
         } else if ("explorer".equals(jobId)) {
            sender.sendMessage(this.messages.get("jobs.info-header", Map.of("job", this.messages.jobName(jobId))));
            sender.sendMessage(
               this.messages
                  .get(
                     "jobs.info-explorer",
                     Map.of(
                        "distance",
                        String.valueOf((int)this.jobManager.explorerDistancePerMilestone()),
                        "money",
                        MoneyFormat.format(this.jobManager.explorerMoneyPerMilestone()),
                        "xp",
                        String.format("%.2f", this.jobManager.explorerXpPerMilestone())
                     )
                  )
            );
         } else {
            Map<String, Map<String, ActionReward>> actions = new TreeMap<>(job.getActionsByType());
            List<Component> entries = new ArrayList<>();

            for (Entry<String, Map<String, ActionReward>> actionType : actions.entrySet()) {
               for (Entry<String, ActionReward> rewardEntry : new TreeMap<>(actionType.getValue()).entrySet()) {
                  ActionReward reward = rewardEntry.getValue();
                  entries.add(
                     this.messages
                        .get(
                           "jobs.info-entry",
                           Map.of(
                              "key",
                              rewardEntry.getKey(),
                              "money",
                              formatMoneyReward(reward.money(), reward.moneyPerLevel()),
                              "xp",
                              formatReward(reward.xp(), reward.xpPerLevel())
                           )
                        )
                  );
               }
            }

            if (entries.isEmpty()) {
               sender.sendMessage(this.messages.get("jobs.info-header", Map.of("job", this.messages.jobName(jobId))));
               sender.sendMessage(this.messages.get("jobs.info-empty", Map.of()));
            } else {
               this.sendPaged(sender, this.messages.get("jobs.info-header", Map.of("job", this.messages.jobName(jobId))), entries, parsePage(args, 2));
            }
         }
      }
   }

   private static String formatMoneyReward(double flat, double perLevel) {
      return perLevel > 0.0 ? MoneyFormat.format(perLevel) + "/enchant-level" : MoneyFormat.format(flat);
   }

   private static String formatReward(double flat, double perLevel) {
      return perLevel > 0.0 ? String.format("%.2f/enchant-level", perLevel) : String.format("%.2f", flat);
   }

   private void sendPaged(CommandSender sender, Component header, List<Component> entries, int requestedPage) {
      sender.sendMessage(header);
      int pageCount = Math.max(1, (int)Math.ceil(entries.size() / 8.0));
      int page = Math.max(0, Math.min(requestedPage - 1, pageCount - 1));
      int start = page * 8;
      int end = Math.min(entries.size(), start + 8);

      for (int i = start; i < end; i++) {
         sender.sendMessage(entries.get(i));
      }

      if (pageCount > 1) {
         sender.sendMessage(this.messages.get("jobs.page-footer", Map.of("page", String.valueOf(page + 1), "pages", String.valueOf(pageCount))));
      }
   }

   private static int parsePage(String[] args, int index) {
      if (args.length > index) {
         try {
            int page = Integer.parseInt(args[index]);
            if (page > 0) {
               return page;
            }
         } catch (NumberFormatException var3) {
         }
      }

      return 1;
   }

   private void handlePrestige(CommandSender sender, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.use")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else if (args.length != 2) {
            player.sendMessage(this.messages.get("jobs.usage", Map.of()));
         } else {
            String jobId = args[1].toLowerCase();
            switch (this.playerJobManager.prestige(player, jobId)) {
               case UNKNOWN_JOB:
                  player.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", jobId)));
                  break;
               case NOT_JOINED:
                  player.sendMessage(this.messages.get("jobs.not-joined", Map.of("job", this.messages.jobName(jobId))));
                  break;
               case NOT_MAX_LEVEL:
                  player.sendMessage(
                     this.messages
                        .get("jobs.prestige-not-max-level", Map.of("job", this.messages.jobName(jobId), "max", String.valueOf(this.jobManager.maxLevel())))
                  );
               case SUCCESS:
            }
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   private void handleMenu(CommandSender sender) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.use")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else {
            new HubMenu(this.menuContext, player).open();
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   private void handleAdmin(CommandSender sender) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.admin")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else {
            new AdminMenu(this.menuContext, player, 0).open();
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   private void handleBooster(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ecojobs.admin")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else if (args.length < 2) {
         sender.sendMessage(this.messages.get("jobs.booster-usage", Map.of()));
      } else {
         String var3 = args[1].toLowerCase();
         switch (var3) {
            case "start":
               this.handleBoosterStart(sender, args);
               break;
            case "stop":
               this.handleBoosterStop(sender, args);
               break;
            case "list":
               this.handleBoosterList(sender);
               break;
            default:
               sender.sendMessage(this.messages.get("jobs.booster-usage", Map.of()));
         }
      }
   }

   private void handleBoosterStart(CommandSender sender, String[] args) {
      if (args.length != 6) {
         sender.sendMessage(this.messages.get("jobs.booster-usage", Map.of()));
      } else {
         String scope = this.resolveBoosterScope(args[2]);
         if (scope == null) {
            sender.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", args[2])));
         } else {
            double moneyMultiplier;
            double xpMultiplier;
            int minutes;
            try {
               moneyMultiplier = Double.parseDouble(args[3]);
               xpMultiplier = Double.parseDouble(args[4]);
               minutes = Integer.parseInt(args[5]);
            } catch (NumberFormatException var10) {
               sender.sendMessage(this.messages.get("jobs.booster-invalid-number", Map.of()));
               return;
            }

            this.boosterManager.start(scope, moneyMultiplier, xpMultiplier, minutes * 60000L, sender.getName());
            Bukkit.getServer()
               .sendMessage(
                  this.messages
                     .get(
                        "jobs.booster-started",
                        Map.of(
                           "scope",
                           this.boosterScopeLabel(scope),
                           "money",
                           String.format("%.2f", moneyMultiplier),
                           "xp",
                           String.format("%.2f", xpMultiplier),
                           "minutes",
                           String.valueOf(minutes),
                           "player",
                           sender.getName()
                        )
                     )
               );
         }
      }
   }

   private void handleBoosterStop(CommandSender sender, String[] args) {
      if (args.length != 3) {
         sender.sendMessage(this.messages.get("jobs.booster-usage", Map.of()));
      } else {
         String scope = this.resolveBoosterScope(args[2]);
         if (scope == null) {
            sender.sendMessage(this.messages.get("jobs.unknown-job", Map.of("job", args[2])));
         } else {
            if (this.boosterManager.stop(scope)) {
               Bukkit.getServer()
                  .sendMessage(this.messages.get("jobs.booster-stopped", Map.of("scope", this.boosterScopeLabel(scope), "player", sender.getName())));
            } else {
               sender.sendMessage(this.messages.get("jobs.booster-not-active", Map.of("scope", this.boosterScopeLabel(scope))));
            }
         }
      }
   }

   private void handleBoosterList(CommandSender sender) {
      Collection<BoosterManager.ActiveBooster> active = this.boosterManager.active();
      sender.sendMessage(this.messages.get("jobs.booster-list-header", Map.of()));
      if (active.isEmpty()) {
         sender.sendMessage(this.messages.get("jobs.booster-list-empty", Map.of()));
      } else {
         for (BoosterManager.ActiveBooster booster : active) {
            long minutesLeft = Math.max(0L, (booster.expiresAtMillis() - System.currentTimeMillis()) / 60000L);
            sender.sendMessage(
               this.messages
                  .get(
                     "jobs.booster-list-entry",
                     Map.of(
                        "scope",
                        this.boosterScopeLabel(booster.scope()),
                        "money",
                        String.format("%.2f", booster.moneyMultiplier()),
                        "xp",
                        String.format("%.2f", booster.xpMultiplier()),
                        "minutes",
                        String.valueOf(minutesLeft)
                     )
                  )
            );
         }
      }
   }

   private String resolveBoosterScope(String arg) {
      if (arg.equalsIgnoreCase("all")) {
         return "all";
      } else {
         String jobId = arg.toLowerCase();
         return this.jobManager.get(jobId) != null ? jobId : null;
      }
   }

   private String boosterScopeLabel(String scope) {
      return scope.equals("all") ? this.messages.raw("jobs.booster-scope-all", Map.of()) : this.messages.jobName(scope);
   }

   private void handleReload(CommandSender sender) {
      if (!sender.hasPermission("ecojobs.admin")) {
         sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
      } else {
         this.plugin.reloadPluginConfig();
         this.jobManager.load();
         sender.sendMessage(this.messages.get("general.reloaded", Map.of()));
      }
   }

   private void handleQuests(CommandSender sender, String[] args) {
      if (args.length == 3 && args[1].equalsIgnoreCase("reset")) {
         if (!sender.hasPermission("ecojobs.admin")) {
            sender.sendMessage(this.messages.get("general.no-permission", Map.of()));
            return;
         }

         Player target = Bukkit.getPlayerExact(args[2]);
         if (target == null) {
            sender.sendMessage(this.messages.get("general.player-not-found", Map.of("player", args[2])));
            return;
         }

         this.questManager.reset(target.getUniqueId());
         sender.sendMessage(this.messages.get("quests.reset", Map.of("player", target.getName())));
      } else if (sender instanceof Player player) {
         if (!player.hasPermission("ecojobs.use")) {
            player.sendMessage(this.messages.get("general.no-permission", Map.of()));
         } else if (!this.questManager.isEnabled()) {
            player.sendMessage(this.messages.get("quests.disabled", Map.of()));
         } else {
            new QuestMenu(this.menuContext, player).open();
         }
      } else {
         sender.sendMessage(this.messages.get("general.players-only", Map.of()));
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return TabCompleteUtil.filterPrefix(
            List.of("join", "leave", "list", "stats", "top", "menu", "info", "prestige", "quests", "admin", "booster", "reload"), args[0]
         );
      } else if (args.length == 2) {
         String var7 = args[0].toLowerCase();

         return switch (var7) {
            case "join", "leave", "top", "info", "prestige" -> TabCompleteUtil.filterPrefix(new ArrayList<>(this.jobManager.all().keySet()), args[1]);
            case "stats" -> TabCompleteUtil.onlinePlayerNames(args[1], null);
            case "booster" -> TabCompleteUtil.filterPrefix(List.of("start", "stop", "list"), args[1]);
            case "quests" -> sender.hasPermission("ecojobs.admin") ? TabCompleteUtil.filterPrefix(List.of("reset"), args[1]) : Collections.emptyList();
            default -> Collections.emptyList();
         };
      } else if (args.length == 3 && args[0].equalsIgnoreCase("quests") && args[1].equalsIgnoreCase("reset")) {
         return TabCompleteUtil.onlinePlayerNames(args[2], null);
      } else if (args.length != 3 || !args[0].equalsIgnoreCase("booster") || !args[1].equalsIgnoreCase("start") && !args[1].equalsIgnoreCase("stop")) {
         return Collections.emptyList();
      } else {
         List<String> scopes = new ArrayList<>(this.jobManager.all().keySet());
         scopes.add("all");
         return TabCompleteUtil.filterPrefix(scopes, args[2]);
      }
   }
}
