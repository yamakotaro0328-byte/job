package com.yamakotaro.ecojobs;

import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {
   private final EcoJobsPlugin plugin;

   public Messages(EcoJobsPlugin plugin) {
      this.plugin = plugin;
   }

   private String language() {
      return this.plugin.config().getString("language", "en");
   }

   public String raw(String path, Map<String, String> replacements) {
      FileConfiguration config = this.plugin.config();
      String language = this.language();
      String value = config.getString("messages." + language + "." + path);
      if (value == null) {
         value = config.getString("messages.en." + path, path);
      }

      for (Entry<String, String> entry : replacements.entrySet()) {
         value = value.replace("{" + entry.getKey() + "}", entry.getValue());
      }

      return value;
   }

   public Component get(String path, Map<String, String> replacements) {
      return LegacyComponentSerializer.legacyAmpersand().deserialize(this.raw(path, replacements));
   }

   public String jobName(String jobId) {
      FileConfiguration config = this.plugin.config();
      String value = config.getString("job-names." + this.language() + "." + jobId);
      if (value == null) {
         value = config.getString("job-names.en." + jobId, jobId);
      }

      return value;
   }
}
