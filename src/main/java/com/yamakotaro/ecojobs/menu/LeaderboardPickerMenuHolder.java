package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.Messages;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LeaderboardPickerMenuHolder implements InventoryHolder {
   public static final int BACK_SLOT = 45;
   public static final int CLOSE_SLOT = 49;
   private final Messages messages;
   private final Inventory inventory;
   private final Map<Integer, String> slotToJobId = new HashMap<>();

   public LeaderboardPickerMenuHolder(Messages messages) {
      this.messages = messages;
      this.inventory = Bukkit.createInventory(this, 54, messages.get("menu.leaderboard-picker-title", Map.of()));
   }

   public String jobIdAt(int slot) {
      return this.slotToJobId.get(slot);
   }

   public void render(JobManager jobManager) {
      this.inventory.clear();
      this.slotToJobId.clear();
      int slot = 0;

      for (String jobId : jobManager.all().keySet()) {
         if (slot >= 45) {
            break;
         }

         Material material = JobsMenuHolder.ICONS.getOrDefault(jobId, Material.PAPER);
         ItemStack stack = new ItemStack(material);
         ItemMeta meta = stack.getItemMeta();
         if (meta != null) {
            meta.displayName(this.messages.get("menu.job-title", Map.of("job", this.messages.jobName(jobId))));
            stack.setItemMeta(meta);
         }

         this.inventory.setItem(slot, stack);
         this.slotToJobId.put(slot, jobId);
         slot++;
      }

      this.inventory.setItem(45, MenuUtil.backItem(this.messages));
      this.inventory.setItem(49, MenuUtil.closeItem(this.messages));
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
