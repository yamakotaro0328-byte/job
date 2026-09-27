package com.yamakotaro.ecojobs.tasks;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.PerkManager;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PerkHeartbeatTask implements Runnable {
   private static final int EFFECT_DURATION_TICKS = 160;
   private final JobManager jobManager;
   private final PlayerJobManager playerJobManager;
   private final PerkManager perkManager;

   public PerkHeartbeatTask(JobManager jobManager, PlayerJobManager playerJobManager, PerkManager perkManager) {
      this.jobManager = jobManager;
      this.playerJobManager = playerJobManager;
      this.perkManager = perkManager;
   }

   @Override
   public void run() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         for (Entry<String, PlayerJobProgress> entry : this.playerJobManager.joinedJobs(player.getUniqueId()).entrySet()) {
            JobDefinition job = this.jobManager.get(entry.getKey());
            if (job != null) {
               int effectiveLevel = this.perkManager.effectiveLevel(entry.getValue());
               this.perkManager.applyPotionPerks(player, job, effectiveLevel, 160);
            }
         }
      }
   }
}
