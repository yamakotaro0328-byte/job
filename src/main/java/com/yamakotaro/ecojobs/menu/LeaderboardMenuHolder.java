package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class LeaderboardMenuHolder implements InventoryHolder {
   public static final int BACK_SLOT = 45;
   public static final int CLOSE_SLOT = 49;
   private final Messages messages;
   private final String jobId;
   private final LeaderboardMenuHolder.Origin origin;
   private final Inventory inventory;

   public LeaderboardMenuHolder(Messages messages, String jobId, LeaderboardMenuHolder.Origin origin) {
      this.messages = messages;
      this.jobId = jobId;
      this.origin = origin;
      this.inventory = Bukkit.createInventory(this, 54, messages.get("admin.leaderboard-title", Map.of("job", messages.jobName(jobId))));
   }

   public LeaderboardMenuHolder.Origin getOrigin() {
      return this.origin;
   }

   public void render(PlayerJobManager playerJobManager) {
      this.inventory.clear();
      List<PlayerJobManager.TopEntry> top = playerJobManager.top(this.jobId, 45);
      int slot = 0;

      for (PlayerJobManager.TopEntry entry : top) {
         this.inventory.setItem(slot, this.headOf(entry, slot + 1));
         slot++;
      }

      this.inventory.setItem(45, MenuUtil.backItem(this.messages));
      this.inventory.setItem(49, MenuUtil.closeItem(this.messages));
   }

   private ItemStack headOf(PlayerJobManager.TopEntry entry, int rank) {
      ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
      if (stack.getItemMeta() instanceof SkullMeta skullMeta) {
         skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
         skullMeta.displayName(this.messages.get("admin.leaderboard-entry", Map.of("rank", String.valueOf(rank), "player", entry.name())));
         skullMeta.lore(
            List.of(
               this.messages
                  .get(
                     "admin.leaderboard-lore",
                     Map.of("level", String.valueOf(entry.level()), "prestige", String.valueOf(entry.prestige()), "xp", String.format("%.0f", entry.xp()))
                  )
            )
         );
         stack.setItemMeta(skullMeta);
      }

      return stack;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public static enum Origin {
      JOBS_MENU,
      PICKER,
      ADMIN;
   }
}
