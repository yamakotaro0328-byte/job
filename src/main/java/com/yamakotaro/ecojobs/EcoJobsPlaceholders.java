package com.yamakotaro.ecojobs;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class EcoJobsPlaceholders extends PlaceholderExpansion {
   private final EcoJobsPlugin plugin;
   private final PlayerJobManager playerJobManager;

   public EcoJobsPlaceholders(EcoJobsPlugin plugin, PlayerJobManager playerJobManager) {
      this.plugin = plugin;
      this.playerJobManager = playerJobManager;
   }

   public String getIdentifier() {
      return "ecojobs";
   }

   public String getAuthor() {
      return "yamakotaro0328";
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onPlaceholderRequest(Player player, String identifier) {
      String[] parts = identifier.toLowerCase().split("_");
      if (parts.length == 2 && parts[0].equals("total") && parts[1].equals("level")) {
         return player == null ? "0" : String.valueOf(this.totalLevel(player));
      } else if (parts.length == 4 && parts[0].equals("top")) {
         return this.topPlaceholder(parts[1], parts[2], parts[3]);
      } else if (player == null) {
         return null;
      } else if (parts.length == 3 && parts[0].equals("xp") && parts[1].equals("max")) {
         PlayerJobProgress progress = this.playerJobManager.allProgress(player.getUniqueId()).get(parts[2]);
         return progress == null ? "0" : String.format("%.0f", this.playerJobManager.xpToNextLevel(progress.getLevel()));
      } else if (parts.length == 2) {
         String field = parts[0];
         String jobId = parts[1];
         if (field.equals("joined")) {
            return String.valueOf(this.playerJobManager.isJoined(player.getUniqueId(), jobId));
         } else {
            PlayerJobProgress progress = this.playerJobManager.allProgress(player.getUniqueId()).get(jobId);

            return switch (field) {
               case "level" -> String.valueOf(progress != null ? progress.getLevel() : 0);
               case "xp" -> String.format("%.0f", progress != null ? progress.getXp() : 0.0);
               case "prestige" -> String.valueOf(progress != null ? progress.getPrestige() : 0);
               default -> null;
            };
         }
      } else {
         return null;
      }
   }

   private int totalLevel(Player player) {
      int total = 0;

      for (PlayerJobProgress progress : this.playerJobManager.allProgress(player.getUniqueId()).values()) {
         total += progress.getLevel();
      }

      return total;
   }

   private String topPlaceholder(String jobId, String rankString, String field) {
      int rank;
      try {
         rank = Integer.parseInt(rankString);
      } catch (NumberFormatException var9) {
         return null;
      }

      if (rank < 1) {
         return null;
      } else {
         List<PlayerJobManager.TopEntry> top = this.playerJobManager.top(jobId, rank);
         if (top.size() < rank) {
            return "";
         } else {
            PlayerJobManager.TopEntry entry = top.get(rank - 1);

            return switch (field) {
               case "name" -> entry.name();
               case "level" -> String.valueOf(entry.level());
               case "xp" -> String.format("%.0f", entry.xp());
               case "prestige" -> String.valueOf(entry.prestige());
               default -> null;
            };
         }
      }
   }
}
