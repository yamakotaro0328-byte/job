package com.yamakotaro.ecojobs;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

public class JobOverrides {
   private final EcoJobsPlugin plugin;
   private final File file;
   private final YamlConfiguration yaml;

   public JobOverrides(EcoJobsPlugin plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "job-overrides.yml");
      this.yaml = YamlIo.load(this.file);
   }

   public boolean isEnabled(String jobId) {
      return this.yaml.getBoolean(jobId + ".enabled", true);
   }

   public void setEnabled(String jobId, boolean enabled) {
      this.yaml.set(jobId + ".enabled", enabled);
      this.save();
   }

   public double payMultiplier(String jobId) {
      return this.yaml.getDouble(jobId + ".pay-multiplier", 1.0);
   }

   public void adjustPayMultiplier(String jobId, double delta) {
      double next = Math.max(0.0, this.payMultiplier(jobId) + delta);
      this.yaml.set(jobId + ".pay-multiplier", Math.round(next * 100.0) / 100.0);
      this.save();
   }

   private void save() {
      try {
         YamlIo.save(this.yaml, this.file);
      } catch (IOException var2) {
         this.plugin.getLogger().log(Level.WARNING, "Failed to save job-overrides.yml", (Throwable)var2);
      }
   }
}
