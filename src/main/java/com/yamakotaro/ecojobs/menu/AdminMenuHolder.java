package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.BoosterManager;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminMenuHolder implements InventoryHolder {
   public static final int BACK_SLOT = 45;
   public static final int BOOSTER_START_SLOT = 48;
   public static final int BOOSTER_STOP_SLOT = 50;
   public static final int CLOSE_SLOT = 53;
   public static final double MULTIPLIER_STEP = 0.1;
   public static final double QUICK_BOOSTER_MULTIPLIER = 2.0;
   public static final long QUICK_BOOSTER_MINUTES = 30L;
   private final Messages messages;
   private final Inventory inventory;
   private final Map<Integer, String> slotToJobId = new HashMap<>();

   public AdminMenuHolder(Messages messages) {
      this.messages = messages;
      this.inventory = Bukkit.createInventory(this, 54, messages.get("admin.title", Map.of()));
   }

   public String jobIdAt(int slot) {
      return this.slotToJobId.get(slot);
   }

   public void render(JobManager jobManager, JobOverrides jobOverrides, BoosterManager boosterManager) {
      this.inventory.clear();
      this.slotToJobId.clear();
      int slot = 0;

      for (String jobId : jobManager.all().keySet()) {
         if (slot >= 45) {
            break;
         }

         this.inventory.setItem(slot, this.buildJobItem(jobId, jobOverrides, boosterManager));
         this.slotToJobId.put(slot, jobId);
         slot++;
      }

      this.inventory.setItem(45, MenuUtil.backItem(this.messages));
      this.inventory.setItem(48, this.boosterStartItem());
      this.inventory.setItem(50, this.boosterStopItem(boosterManager));
      this.inventory.setItem(53, MenuUtil.closeItem(this.messages));
   }

   private ItemStack buildJobItem(String jobId, JobOverrides jobOverrides, BoosterManager boosterManager) {
      Material material = JobsMenuHolder.ICONS.getOrDefault(jobId, Material.PAPER);
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         boolean enabled = jobOverrides.isEnabled(jobId);
         double multiplier = jobOverrides.payMultiplier(jobId);
         meta.displayName(this.messages.get("menu.job-title", Map.of("job", this.messages.jobName(jobId))));
         List<Component> lore = new ArrayList<>();
         lore.add(this.messages.get(enabled ? "admin.lore-enabled-yes" : "admin.lore-enabled-no", Map.of()));
         lore.add(this.messages.get("admin.lore-multiplier", Map.of("multiplier", String.format("%.2f", multiplier))));
         BoosterManager.ActiveBooster booster = boosterManager.getActiveBooster(jobId);
         if (booster != null) {
            lore.add(
               this.messages
                  .get(
                     "admin.lore-job-booster",
                     Map.of(
                        "money",
                        String.format("%.2f", booster.moneyMultiplier()),
                        "xp",
                        String.format("%.2f", booster.xpMultiplier()),
                        "minutes",
                        String.valueOf(remainingMinutes(booster))
                     )
                  )
            );
         }

         lore.add(this.messages.get("admin.lore-controls-toggle", Map.of()));
         lore.add(this.messages.get("admin.lore-controls-multiplier-up", Map.of()));
         lore.add(this.messages.get("admin.lore-controls-multiplier-down", Map.of()));
         lore.add(this.messages.get("admin.lore-controls-leaderboard", Map.of()));
         meta.lore(lore);
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private static long remainingMinutes(BoosterManager.ActiveBooster booster) {
      return Math.max(0L, (booster.expiresAtMillis() - System.currentTimeMillis()) / 60000L);
   }

   private ItemStack boosterStartItem() {
      ItemStack stack = new ItemStack(Material.NETHER_STAR);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get("admin.booster-start-title", Map.of()));
         meta.lore(List.of(this.messages.get("admin.booster-start-lore", Map.of("multiplier", String.format("%.0f", 2.0), "minutes", String.valueOf(30L)))));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private ItemStack boosterStopItem(BoosterManager boosterManager) {
      ItemStack stack = new ItemStack(Material.BARRIER);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(this.messages.get("admin.booster-stop-title", Map.of()));
         meta.lore(List.of(this.messages.get("admin.booster-stop-lore", Map.of("count", String.valueOf(boosterManager.active().size())))));
         stack.setItemMeta(meta);
      }

      return stack;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
