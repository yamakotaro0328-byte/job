package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.EcoJobsPlugin;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PerkManager;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.QuestManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Shared services + text/sound helpers handed to every menu. */
public record MenuContext(
   EcoJobsPlugin plugin,
   JobManager jobManager,
   PlayerJobManager playerJobManager,
   JobOverrides jobOverrides,
   BoosterManager boosterManager,
   PerkManager perkManager,
   QuestManager questManager,
   Messages messages
) {
   /** One GUI line with italics forced off (Minecraft italicises lore/custom names by default). */
   public Component text(String key, Map<String, String> placeholders) {
      return line(this.messages.raw("gui." + key, placeholders));
   }

   public Component text(String key) {
      return this.text(key, Map.of());
   }

   public String raw(String key, Map<String, String> placeholders) {
      return this.messages.raw("gui." + key, placeholders);
   }

   public String raw(String key) {
      return this.raw(key, Map.of());
   }

   /** A message that may span several lore lines (split on newlines). */
   public List<Component> lines(String key, Map<String, String> placeholders) {
      List<Component> result = new ArrayList<>();
      for (String part : this.messages.raw("gui." + key, placeholders).split("\n")) {
         result.add(line(part));
      }

      return result;
   }

   public static Component line(String legacy) {
      return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
   }

   public String jobName(String jobId) {
      return this.messages.jobName(jobId);
   }

   public void click(Player player) {
      this.sound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.3F);
   }

   public void success(Player player) {
      this.sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.2F);
   }

   public void deny(Player player) {
      this.sound(player, Sound.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
   }

   public void pageTurn(Player player) {
      this.sound(player, Sound.ITEM_BOOK_PAGE_TURN, 0.7F, 1.0F);
   }

   private void sound(Player player, Sound sound, float volume, float pitch) {
      if (this.playerJobManager.isSoundEnabled(player)) {
         player.playSound(player.getLocation(), sound, volume, pitch);
      }
   }
}
