package com.yamakotaro.ecojobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlIo {
   private YamlIo() {
   }

   public static YamlConfiguration load(File file) {
      if (!file.exists()) {
         return new YamlConfiguration();
      } else {
         try {
            YamlConfiguration var2;
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
               var2 = YamlConfiguration.loadConfiguration(reader);
            }

            return var2;
         } catch (IOException var6) {
            return new YamlConfiguration();
         }
      }
   }

   public static void save(YamlConfiguration config, File file) throws IOException {
      File parent = file.getParentFile();
      if (parent != null) {
         parent.mkdirs();
      }

      Files.writeString(file.toPath(), config.saveToString(), StandardCharsets.UTF_8);
   }
}
