package com.yamakotaro.ecojobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabCompleteUtil {
   private TabCompleteUtil() {
   }

   public static List<String> onlinePlayerNames(String prefix, UUID exclude) {
      String lower = prefix.toLowerCase();
      List<String> result = new ArrayList<>();

      for (Player player : Bukkit.getOnlinePlayers()) {
         if ((exclude == null || !player.getUniqueId().equals(exclude)) && player.getName().toLowerCase().startsWith(lower)) {
            result.add(player.getName());
         }
      }

      return result;
   }

   public static List<String> filterPrefix(List<String> options, String prefix) {
      String lower = prefix.toLowerCase();
      List<String> result = new ArrayList<>();

      for (String option : options) {
         if (option.toLowerCase().startsWith(lower)) {
            result.add(option);
         }
      }

      return result;
   }
}
