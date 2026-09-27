package com.yamakotaro.ecojobs.storage;

import com.yamakotaro.ecojobs.EcoJobsPlugin;
import com.yamakotaro.ecojobs.PlayerJobData;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;

public class MySqlJobStorage implements JobStorage {
   private final EcoJobsPlugin plugin;
   private final MySqlConnectionProvider connectionProvider;
   private final String playersTable;
   private final String progressTable;
   private final String explorerTable;
   private final String milestonesTable;

   public MySqlJobStorage(EcoJobsPlugin plugin) {
      this.plugin = plugin;
      this.connectionProvider = new MySqlConnectionProvider(plugin);
      String prefix = plugin.config().getString("storage.mysql.table-prefix", "ecojobs_");
      this.playersTable = prefix + "players";
      this.progressTable = prefix + "progress";
      this.explorerTable = prefix + "explorer";
      this.milestonesTable = prefix + "milestones";
      this.createTables();
   }

   private void createTables() {
      Connection conn = this.connectionProvider.get();
      if (conn != null) {
         try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS "
                  + this.playersTable
                  + " (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), sound_enabled BOOLEAN NOT NULL DEFAULT TRUE, actionbar_enabled BOOLEAN NOT NULL DEFAULT TRUE)"
            );
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS "
                  + this.progressTable
                  + " (uuid VARCHAR(36), job_id VARCHAR(64), level INT NOT NULL DEFAULT 1, xp DOUBLE NOT NULL DEFAULT 0, prestige INT NOT NULL DEFAULT 0, joined BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY (uuid, job_id))"
            );
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS "
                  + this.explorerTable
                  + " (uuid VARCHAR(36), world VARCHAR(64), distance DOUBLE NOT NULL DEFAULT 0, PRIMARY KEY (uuid, world))"
            );
            statement.executeUpdate(
               "CREATE TABLE IF NOT EXISTS "
                  + this.milestonesTable
                  + " (uuid VARCHAR(36), job_id VARCHAR(64), level INT NOT NULL, PRIMARY KEY (uuid, job_id, level))"
            );
         } catch (SQLException var7) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create EcoJobs tables", (Throwable)var7);
         }
      }
   }

   @Override
   public Map<UUID, PlayerJobData> loadAll() {
      Map<UUID, PlayerJobData> data = new HashMap<>();
      Connection conn = this.connectionProvider.get();
      if (conn == null) {
         return data;
      } else {
         try (
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT uuid, name, sound_enabled, actionbar_enabled FROM " + this.playersTable);
         ) {
            while (rs.next()) {
               PlayerJobData playerData = new PlayerJobData(rs.getString("name"));
               playerData.setSoundEnabled(rs.getBoolean("sound_enabled"));
               playerData.setActionBarEnabled(rs.getBoolean("actionbar_enabled"));
               data.put(UUID.fromString(rs.getString("uuid")), playerData);
            }
         } catch (SQLException var26) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load EcoJobs players", (Throwable)var26);
            return data;
         }

         try (
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT uuid, job_id, level, xp, prestige, joined FROM " + this.progressTable);
         ) {
            while (rs.next()) {
               PlayerJobData playerData = data.get(UUID.fromString(rs.getString("uuid")));
               if (playerData != null) {
                  playerData.getProgress().put(rs.getString("job_id"), new PlayerJobProgress(rs.getInt("level"), rs.getDouble("xp"), rs.getInt("prestige")));
                  if (rs.getBoolean("joined")) {
                     playerData.getJoined().add(rs.getString("job_id"));
                  }
               }
            }
         } catch (SQLException var23) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load EcoJobs progress", (Throwable)var23);
         }

         try (
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT uuid, world, distance FROM " + this.explorerTable);
         ) {
            while (rs.next()) {
               PlayerJobData playerData = data.get(UUID.fromString(rs.getString("uuid")));
               if (playerData != null) {
                  playerData.setExplorerFarthestDistance(rs.getString("world"), rs.getDouble("distance"));
               }
            }
         } catch (SQLException var20) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load EcoJobs explorer distances", (Throwable)var20);
         }

         try (
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT uuid, job_id, level FROM " + this.milestonesTable);
         ) {
            while (rs.next()) {
               PlayerJobData playerData = data.get(UUID.fromString(rs.getString("uuid")));
               if (playerData != null) {
                  playerData.getAnnouncedMilestones().add(rs.getString("job_id") + ":" + rs.getInt("level"));
               }
            }
         } catch (SQLException var17) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load EcoJobs announced milestones", (Throwable)var17);
         }

         return data;
      }
   }

   @Override
   public void saveAll(Map<UUID, PlayerJobData> allData, Set<UUID> dirtyUuids) {
      if (!dirtyUuids.isEmpty()) {
         Connection conn = this.connectionProvider.get();
         if (conn != null) {
            try (
               PreparedStatement playerStatement = conn.prepareStatement(
                  "INSERT INTO "
                     + this.playersTable
                     + " (uuid, name, sound_enabled, actionbar_enabled) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), sound_enabled = VALUES(sound_enabled), actionbar_enabled = VALUES(actionbar_enabled)"
               );
               PreparedStatement progressStatement = conn.prepareStatement(
                  "INSERT INTO "
                     + this.progressTable
                     + " (uuid, job_id, level, xp, prestige, joined) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp), prestige = VALUES(prestige), joined = VALUES(joined)"
               );
               PreparedStatement explorerStatement = conn.prepareStatement(
                  "INSERT INTO " + this.explorerTable + " (uuid, world, distance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE distance = VALUES(distance)"
               );
               PreparedStatement milestoneStatement = conn.prepareStatement(
                  "INSERT IGNORE INTO " + this.milestonesTable + " (uuid, job_id, level) VALUES (?, ?, ?)"
               );
            ) {
               for (UUID uuid : dirtyUuids) {
                  PlayerJobData playerData = allData.get(uuid);
                  if (playerData != null) {
                     playerStatement.setString(1, uuid.toString());
                     playerStatement.setString(2, playerData.getName());
                     playerStatement.setBoolean(3, playerData.isSoundEnabled());
                     playerStatement.setBoolean(4, playerData.isActionBarEnabled());
                     playerStatement.addBatch();

                     for (Entry<String, PlayerJobProgress> entry : playerData.getProgress().entrySet()) {
                        PlayerJobProgress progress = entry.getValue();
                        progressStatement.setString(1, uuid.toString());
                        progressStatement.setString(2, entry.getKey());
                        progressStatement.setInt(3, progress.getLevel());
                        progressStatement.setDouble(4, progress.getXp());
                        progressStatement.setInt(5, progress.getPrestige());
                        progressStatement.setBoolean(6, playerData.getJoined().contains(entry.getKey()));
                        progressStatement.addBatch();
                     }

                     for (Entry<String, Double> entry : playerData.getExplorerDistanceByWorld().entrySet()) {
                        explorerStatement.setString(1, uuid.toString());
                        explorerStatement.setString(2, entry.getKey());
                        explorerStatement.setDouble(3, entry.getValue());
                        explorerStatement.addBatch();
                     }

                     for (String milestoneKey : playerData.getAnnouncedMilestones()) {
                        int separator = milestoneKey.lastIndexOf(58);
                        if (separator >= 0) {
                           milestoneStatement.setString(1, uuid.toString());
                           milestoneStatement.setString(2, milestoneKey.substring(0, separator));
                           milestoneStatement.setInt(3, Integer.parseInt(milestoneKey.substring(separator + 1)));
                           milestoneStatement.addBatch();
                        }
                     }
                  }
               }

               playerStatement.executeBatch();
               progressStatement.executeBatch();
               explorerStatement.executeBatch();
               milestoneStatement.executeBatch();
            } catch (SQLException var22) {
               this.plugin.getLogger().log(Level.SEVERE, "Failed to save EcoJobs data (MySQL)", (Throwable)var22);
            }
         }
      }
   }

   @Override
   public void close() {
      this.connectionProvider.close();
   }
}
