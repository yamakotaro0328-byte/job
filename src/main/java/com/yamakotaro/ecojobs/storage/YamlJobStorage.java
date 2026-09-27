package com.yamakotaro.ecojobs.storage;

import com.yamakotaro.ecojobs.EcoJobsPlugin;
import com.yamakotaro.ecojobs.PlayerJobData;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import com.yamakotaro.ecojobs.YamlIo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlJobStorage implements JobStorage {
   private final EcoJobsPlugin plugin;
   private final File file;

   public YamlJobStorage(EcoJobsPlugin plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "player-jobs.yml");
   }

   @Override
   public Map<UUID, PlayerJobData> loadAll() {
      Map<UUID, PlayerJobData> data = new HashMap<>();
      if (!this.file.exists()) {
         return data;
      } else {
         YamlConfiguration yaml = YamlIo.load(this.file);
         ConfigurationSection playersSection = yaml.getConfigurationSection("players");
         if (playersSection == null) {
            return data;
         } else {
            for (String uuidString : playersSection.getKeys(false)) {
               UUID uuid;
               try {
                  uuid = UUID.fromString(uuidString);
               } catch (IllegalArgumentException var14) {
                  continue;
               }

               ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);
               if (playerSection != null) {
                  PlayerJobData playerData = new PlayerJobData(playerSection.getString("name", "?"));
                  playerData.setSoundEnabled(playerSection.getBoolean("settings.sound", true));
                  playerData.setActionBarEnabled(playerSection.getBoolean("settings.actionbar", true));
                  ConfigurationSection explorerSection = playerSection.getConfigurationSection("explorer-distance");
                  if (explorerSection != null) {
                     for (String worldName : explorerSection.getKeys(false)) {
                        playerData.setExplorerFarthestDistance(worldName, explorerSection.getDouble(worldName, 0.0));
                     }
                  }

                  playerData.getAnnouncedMilestones().addAll(playerSection.getStringList("announced-milestones"));
                  ConfigurationSection jobsSection = playerSection.getConfigurationSection("jobs");
                  if (jobsSection != null) {
                     for (String jobId : jobsSection.getKeys(false)) {
                        ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
                        if (jobSection != null) {
                           playerData.getProgress()
                              .put(
                                 jobId, new PlayerJobProgress(jobSection.getInt("level", 1), jobSection.getDouble("xp", 0.0), jobSection.getInt("prestige", 0))
                              );
                           if (jobSection.getBoolean("joined", true)) {
                              playerData.getJoined().add(jobId);
                           }
                        }
                     }
                  }

                  data.put(uuid, playerData);
               }
            }

            return data;
         }
      }
   }

   @Override
   public void saveAll(Map<UUID, PlayerJobData> allData, Set<UUID> dirtyUuids) {
      YamlConfiguration yaml = new YamlConfiguration();

      for (Entry<UUID, PlayerJobData> entry : allData.entrySet()) {
         String base = "players." + entry.getKey();
         PlayerJobData playerData = entry.getValue();
         yaml.set(base + ".name", playerData.getName());
         yaml.set(base + ".settings.sound", playerData.isSoundEnabled());
         yaml.set(base + ".settings.actionbar", playerData.isActionBarEnabled());

         for (Entry<String, Double> distanceEntry : playerData.getExplorerDistanceByWorld().entrySet()) {
            yaml.set(base + ".explorer-distance." + distanceEntry.getKey(), distanceEntry.getValue());
         }

         if (!playerData.getAnnouncedMilestones().isEmpty()) {
            yaml.set(base + ".announced-milestones", new ArrayList<>(playerData.getAnnouncedMilestones()));
         }

         for (Entry<String, PlayerJobProgress> jobEntry : playerData.getProgress().entrySet()) {
            String jobBase = base + ".jobs." + jobEntry.getKey();
            yaml.set(jobBase + ".level", jobEntry.getValue().getLevel());
            yaml.set(jobBase + ".xp", jobEntry.getValue().getXp());
            yaml.set(jobBase + ".prestige", jobEntry.getValue().getPrestige());
            yaml.set(jobBase + ".joined", playerData.getJoined().contains(jobEntry.getKey()));
         }
      }

      try {
         YamlIo.save(yaml, this.file);
      } catch (IOException var11) {
         this.plugin.getLogger().log(Level.WARNING, "Failed to save player-jobs.yml", (Throwable)var11);
      }
   }

   @Override
   public void close() {
   }
}
