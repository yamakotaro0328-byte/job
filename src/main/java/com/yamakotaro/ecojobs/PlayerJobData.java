package com.yamakotaro.ecojobs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PlayerJobData {
   private String name;
   private final Map<String, PlayerJobProgress> progress = new LinkedHashMap<>();
   private final Set<String> joined = new LinkedHashSet<>();
   private final Map<String, Double> explorerDistanceByWorld = new HashMap<>();
   private final Set<String> announcedMilestones = new LinkedHashSet<>();
   private boolean soundEnabled = true;
   private boolean actionBarEnabled = true;

   public PlayerJobData(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Map<String, PlayerJobProgress> getProgress() {
      return this.progress;
   }

   public Set<String> getJoined() {
      return this.joined;
   }

   public double getExplorerFarthestDistance(String worldName) {
      return this.explorerDistanceByWorld.getOrDefault(worldName, 0.0);
   }

   public void setExplorerFarthestDistance(String worldName, double distance) {
      this.explorerDistanceByWorld.put(worldName, distance);
   }

   public Map<String, Double> getExplorerDistanceByWorld() {
      return this.explorerDistanceByWorld;
   }

   public Set<String> getAnnouncedMilestones() {
      return this.announcedMilestones;
   }

   public boolean isSoundEnabled() {
      return this.soundEnabled;
   }

   public void setSoundEnabled(boolean soundEnabled) {
      this.soundEnabled = soundEnabled;
   }

   public boolean isActionBarEnabled() {
      return this.actionBarEnabled;
   }

   public void setActionBarEnabled(boolean actionBarEnabled) {
      this.actionBarEnabled = actionBarEnabled;
   }
}
