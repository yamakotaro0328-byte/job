package com.yamakotaro.ecojobs;

import com.yamakotaro.ecojobs.commands.JobsCommand;
import com.yamakotaro.ecojobs.listeners.BlockJobListener;
import com.yamakotaro.ecojobs.listeners.CraftingJobListener;
import com.yamakotaro.ecojobs.listeners.EntityJobListener;
import com.yamakotaro.ecojobs.listeners.EvenMoreFishBridge;
import com.yamakotaro.ecojobs.listeners.ExplorerListener;
import com.yamakotaro.ecojobs.listeners.TradeJobListener;
import com.yamakotaro.ecojobs.menu.AdminMenuListener;
import com.yamakotaro.ecojobs.menu.HubMenuListener;
import com.yamakotaro.ecojobs.menu.JobsMenuListener;
import com.yamakotaro.ecojobs.storage.JobStorage;
import com.yamakotaro.ecojobs.storage.MySqlJobStorage;
import com.yamakotaro.ecojobs.storage.YamlJobStorage;
import com.yamakotaro.ecojobs.tasks.PerkHeartbeatTask;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class EcoJobsPlugin extends JavaPlugin {
   private static final long SAVE_INTERVAL_TICKS = 6000L;
   private static final long PLACED_BLOCK_CLEAR_INTERVAL_TICKS = 36000L;
   private static final long PERK_HEARTBEAT_INTERVAL_TICKS = 100L;
   private static final long ACTION_BAR_FLUSH_INTERVAL_TICKS = 5L;
   private PlayerJobManager playerJobManager;
   private YamlConfiguration config;
   private QuestManager questManager;
   private JobBossBar bossBar;

   public void onEnable() {
      this.saveDefaultConfig();
      this.reloadPluginConfig();
      Messages messages = new Messages(this);
      JobManager jobManager = new JobManager(this);
      EconomyHolder economyHolder = new EconomyHolder(this);
      economyHolder.setup();
      JobOverrides jobOverrides = new JobOverrides(this);
      BoosterManager boosterManager = new BoosterManager();
      PerkManager perkManager = new PerkManager(this, jobManager);
      JobStorage storage = (JobStorage)("mysql".equalsIgnoreCase(this.config().getString("storage.type", "yaml"))
         ? new MySqlJobStorage(this)
         : new YamlJobStorage(this));
      this.playerJobManager = new PlayerJobManager(this, jobManager, economyHolder, messages, storage, jobOverrides, boosterManager, perkManager);
      this.questManager = new QuestManager(this, jobManager, messages);
      this.questManager.setPlayerJobManager(this.playerJobManager);
      this.playerJobManager.setQuestManager(this.questManager);
      this.bossBar = new JobBossBar(this, messages);
      this.playerJobManager.setBossBar(this.bossBar);
      PlacedBlockTracker placedBlockTracker = new PlacedBlockTracker();
      EvenMoreFishBridge evenMoreFish = new EvenMoreFishBridge(this, this.playerJobManager);
      evenMoreFish.register();
      this.getServer().getPluginManager().registerEvents(new BlockJobListener(this.playerJobManager, placedBlockTracker, jobManager, perkManager), this);
      this.getServer().getPluginManager().registerEvents(new EntityJobListener(this, this.playerJobManager, jobManager, evenMoreFish), this);
      this.getServer().getPluginManager().registerEvents(new CraftingJobListener(this.playerJobManager), this);
      this.getServer().getPluginManager().registerEvents(new TradeJobListener(this.playerJobManager), this);
      this.getServer().getPluginManager().registerEvents(new ExplorerListener(this.playerJobManager), this);
      this.getServer()
         .getPluginManager()
         .registerEvents(new JobsMenuListener(jobManager, this.playerJobManager, jobOverrides, boosterManager, perkManager, messages), this);
      this.getServer()
         .getPluginManager()
         .registerEvents(new AdminMenuListener(jobManager, this.playerJobManager, jobOverrides, boosterManager, messages), this);
      this.getServer().getPluginManager().registerEvents(new HubMenuListener(jobManager, this.playerJobManager, jobOverrides, boosterManager, messages), this);
      JobsCommand jobsCommand = new JobsCommand(this, jobManager, this.playerJobManager, jobOverrides, boosterManager, messages, this.questManager);
      this.getCommand("jobs").setExecutor(jobsCommand);
      this.getCommand("jobs").setTabCompleter(jobsCommand);
      if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new EcoJobsPlaceholders(this, this.playerJobManager, this.questManager).register();
      }

      this.getServer().getScheduler().runTaskTimer(this, this.playerJobManager::save, 6000L, 6000L);
      this.getServer().getScheduler().runTaskTimer(this, placedBlockTracker::clear, 36000L, 36000L);
      this.getServer().getScheduler().runTaskTimer(this, new PerkHeartbeatTask(jobManager, this.playerJobManager, perkManager), 100L, 100L);
      this.getServer().getScheduler().runTaskTimer(this, this.playerJobManager::flushEarnedActionBars, 5L, 5L);
      this.getServer().getScheduler().runTaskTimer(this, this.bossBar::tick, 20L, 20L);
      this.getServer().getScheduler().runTaskTimer(this, this.questManager::save, 6000L, 6000L);
   }

   public void onDisable() {
      if (this.bossBar != null) {
         this.bossBar.hideAll();
      }

      if (this.playerJobManager != null) {
         this.playerJobManager.close();
      }
   }

   public void reloadPluginConfig() {
      this.config = YamlIo.load(new File(this.getDataFolder(), "config.yml"));
      this.fillMissingFromBundledConfig();
   }

   /**
    * Configs written by older versions lack the sections added later (quests, bossbar, ...) and their
    * messages. Fill just those gaps in memory from the bundled config.yml so upgrades work without
    * the admin regenerating their file; anything the admin has set is left untouched.
    */
   private void fillMissingFromBundledConfig() {
      InputStream stream = this.getResource("config.yml");
      if (stream != null) {
         YamlConfiguration bundled;
         try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            bundled = YamlConfiguration.loadConfiguration(reader);
         } catch (IOException var8) {
            return;
         }

         for (String path : List.of("quests", "bossbar", "reward-commands", "anti-farm.spawn-reason-multipliers", "anti-farm.max-money-per-hour")) {
            if (!this.config.isSet(path) && bundled.isSet(path)) {
               this.config.set(path, bundled.get(path));
            }
         }

         ConfigurationSection messages = bundled.getConfigurationSection("messages");
         if (messages != null) {
            for (String key : messages.getKeys(true)) {
               String path = "messages." + key;
               if (!bundled.isConfigurationSection(path) && !this.config.isSet(path)) {
                  this.config.set(path, bundled.get(path));
               }
            }
         }
      }
   }

   public YamlConfiguration config() {
      return this.config;
   }
}
