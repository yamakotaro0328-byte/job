package com.yamakotaro.ecojobs.menu;

import com.yamakotaro.ecojobs.ActionReward;
import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.JobOverrides;
import com.yamakotaro.ecojobs.Messages;
import com.yamakotaro.ecojobs.PerkDefinition;
import com.yamakotaro.ecojobs.PerkManager;
import com.yamakotaro.ecojobs.PlayerJobManager;
import com.yamakotaro.ecojobs.PlayerJobProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class JobInfoMenuHolder implements InventoryHolder {
   public static final int HEADER_SLOT = 4;
   public static final int BACK_SLOT = 45;
   public static final int PREV_PAGE_SLOT = 46;
   public static final int NEXT_PAGE_SLOT = 48;
   public static final int CLOSE_SLOT = 49;
   private final Messages messages;
   private final String jobId;
   private final Inventory inventory;
   private int page;
   private double explorerDistancePerMilestone;

   public JobInfoMenuHolder(Messages messages, String jobId) {
      this.messages = messages;
      this.jobId = jobId;
      this.inventory = Bukkit.createInventory(this, 54, messages.get("menu.job-info-title", Map.of("job", messages.jobName(jobId))));
   }

   public String getJobId() {
      return this.jobId;
   }

   public int getPage() {
      return this.page;
   }

   public void render(
      JobManager jobManager, PlayerJobManager playerJobManager, JobOverrides jobOverrides, PerkManager perkManager, Player viewer, int requestedPage
   ) {
      this.inventory.clear();
      MenuUtil.fillBorder(this.inventory);
      JobDefinition job = jobManager.get(this.jobId);
      if (job != null) {
         this.inventory.setItem(4, this.headerItem(job, jobManager, playerJobManager, jobOverrides, viewer));
         this.explorerDistancePerMilestone = jobManager.explorerDistancePerMilestone();
         PlayerJobProgress progress = playerJobManager.allProgress(viewer.getUniqueId()).get(this.jobId);
         int effectiveLevel = progress != null ? perkManager.effectiveLevel(progress) : 0;
         List<JobInfoMenuHolder.Entry> entries = this.buildEntries(job, jobManager, perkManager, effectiveLevel);
         List<Integer> slots = MenuUtil.interiorSlots();
         int pageCount = Math.max(1, (int)Math.ceil((double)entries.size() / slots.size()));
         this.page = Math.max(0, Math.min(requestedPage, pageCount - 1));
         int start = this.page * slots.size();

         for (int i = 0; i < slots.size() && start + i < entries.size(); i++) {
            this.inventory.setItem(slots.get(i), this.entryItem(entries.get(start + i)));
         }

         this.inventory.setItem(45, MenuUtil.backItem(this.messages));
         if (this.page > 0) {
            this.inventory.setItem(46, MenuUtil.prevPageItem(this.messages));
         }

         if (this.page < pageCount - 1) {
            this.inventory.setItem(48, MenuUtil.nextPageItem(this.messages));
         }

         this.inventory.setItem(49, MenuUtil.closeItem(this.messages));
      }
   }

   private List<JobInfoMenuHolder.Entry> buildEntries(JobDefinition job, JobManager jobManager, PerkManager perkManager, int effectiveLevel) {
      List<JobInfoMenuHolder.Entry> entries = new ArrayList<>();
      if ("explorer".equals(this.jobId)) {
         entries.add(
            JobInfoMenuHolder.Entry.ofReward(
               "milestone", Material.COMPASS, new ActionReward(jobManager.explorerMoneyPerMilestone(), jobManager.explorerXpPerMilestone(), 0.0, 0.0)
            )
         );
      } else {
         Map<String, Map<String, ActionReward>> actions = new TreeMap<>(job.getActionsByType());

         for (Map.Entry<String, Map<String, ActionReward>> actionType : actions.entrySet()) {
            for (Map.Entry<String, ActionReward> rewardEntry : new TreeMap<>(actionType.getValue()).entrySet()) {
               entries.add(JobInfoMenuHolder.Entry.ofReward(rewardEntry.getKey(), this.iconFor(rewardEntry.getKey()), rewardEntry.getValue()));
            }
         }
      }

      for (PerkDefinition perk : perkManager.allPerks(job)) {
         boolean unlocked = effectiveLevel >= perk.level();
         int levelsAway = Math.max(0, perk.level() - effectiveLevel);
         entries.add(JobInfoMenuHolder.Entry.ofPerk(perk, this.perkIcon(perk.type()), unlocked, levelsAway));
      }

      return entries;
   }

   private Material iconFor(String key) {
      Material direct = Material.matchMaterial(key);
      if (direct != null) {
         return direct;
      } else {
         Material spawnEgg = Material.matchMaterial(key + "_SPAWN_EGG");
         return spawnEgg != null ? spawnEgg : Material.PAPER;
      }
   }

   private Material perkIcon(String type) {
      String var2 = type.toLowerCase(Locale.ROOT);

      return switch (var2) {
         case "pay-bonus" -> Material.GOLD_NUGGET;
         case "potion" -> Material.POTION;
         case "double-drop" -> Material.HOPPER;
         case "auto-smelt" -> Material.FURNACE;
         case "xp-orb-bonus" -> Material.EXPERIENCE_BOTTLE;
         default -> Material.NETHER_STAR;
      };
   }

   private ItemStack headerItem(JobDefinition job, JobManager jobManager, PlayerJobManager playerJobManager, JobOverrides jobOverrides, Player viewer) {
      Material material = JobsMenuHolder.ICONS.getOrDefault(this.jobId, Material.PAPER);
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta == null) {
         return stack;
      } else {
         meta.displayName(this.messages.get("menu.job-title", Map.of("job", this.messages.jobName(this.jobId))));
         PlayerJobProgress progress = playerJobManager.allProgress(viewer.getUniqueId()).get(this.jobId);
         boolean active = playerJobManager.isJoined(viewer.getUniqueId(), this.jobId);
         boolean maxed = progress != null && progress.getLevel() >= jobManager.maxLevel();
         List<Component> lore = new ArrayList<>();
         if (progress != null) {
            lore.add(
               this.messages
                  .get(
                     "menu.lore-level",
                     Map.of(
                        "level",
                        String.valueOf(progress.getLevel()),
                        "prestige",
                        String.valueOf(progress.getPrestige()),
                        "xp",
                        String.format("%.0f", progress.getXp()),
                        "next_xp",
                        String.format("%.0f", playerJobManager.xpToNextLevel(progress.getLevel()))
                     )
                  )
            );
         } else {
            lore.add(this.messages.get("menu.lore-not-joined", Map.of()));
         }

         boolean enabled = jobOverrides.isEnabled(this.jobId);
         if (!enabled) {
            lore.add(this.messages.get("menu.lore-disabled", Map.of()));
         } else if (progress != null) {
            lore.add(this.messages.get(active ? "menu.lore-click-leave" : "menu.lore-click-rejoin", Map.of()));
         } else {
            lore.add(this.messages.get("menu.lore-click-join", Map.of()));
         }

         if (viewer.hasPermission("ecojobs.top")) {
            lore.add(this.messages.get("menu.lore-click-leaderboard", Map.of()));
         }

         if (active && maxed) {
            lore.add(this.messages.get("menu.lore-click-prestige", Map.of()));
         }

         meta.lore(lore);
         meta.setEnchantmentGlintOverride(active);
         stack.setItemMeta(meta);
         return stack;
      }
   }

   private ItemStack entryItem(JobInfoMenuHolder.Entry entry) {
      return entry.perk() != null ? this.perkItem(entry) : this.rewardItem(entry);
   }

   private ItemStack rewardItem(JobInfoMenuHolder.Entry entry) {
      ItemStack stack = new ItemStack(entry.icon());
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text(prettify(entry.key())));
         List<Component> lore = new ArrayList<>();
         if ("explorer".equals(this.jobId)) {
            lore.add(this.messages.get("menu.job-info-explorer-lore", Map.of("distance", String.valueOf((int)this.explorerDistancePerMilestone))));
         }

         lore.add(
            this.messages.get("menu.job-info-entry-money", Map.of("money", MenuUtil.formatMoneyReward(entry.reward().money(), entry.reward().moneyPerLevel())))
         );
         lore.add(this.messages.get("menu.job-info-entry-xp", Map.of("xp", MenuUtil.formatReward(entry.reward().xp(), entry.reward().xpPerLevel()))));
         meta.lore(lore);
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private ItemStack perkItem(JobInfoMenuHolder.Entry entry) {
      PerkDefinition perk = entry.perk();
      ItemStack stack = new ItemStack(entry.icon());
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         meta.displayName(
            this.messages
               .get(
                  entry.perkUnlocked() ? "menu.job-info-perk-unlocked-title" : "menu.job-info-perk-locked-title", Map.of("level", String.valueOf(perk.level()))
               )
         );
         List<Component> lore = new ArrayList<>();
         lore.add(this.messages.get(this.perkDescriptionKey(perk.type()), this.perkDescriptionArgs(perk)));
         if (!entry.perkUnlocked()) {
            lore.add(this.messages.get("menu.job-info-perk-levels-away", Map.of("levels", String.valueOf(entry.perkLevelsAway()))));
         }

         meta.lore(lore);
         meta.setEnchantmentGlintOverride(entry.perkUnlocked());
         stack.setItemMeta(meta);
      }

      return stack;
   }

   private String perkDescriptionKey(String type) {
      String var2 = type.toLowerCase(Locale.ROOT);

      return switch (var2) {
         case "pay-bonus" -> "menu.perk-desc-pay-bonus";
         case "potion" -> "menu.perk-desc-potion";
         case "double-drop" -> "menu.perk-desc-double-drop";
         case "auto-smelt" -> "menu.perk-desc-auto-smelt";
         case "xp-orb-bonus" -> "menu.perk-desc-xp-orb-bonus";
         default -> "menu.perk-desc-unknown";
      };
   }

   private Map<String, String> perkDescriptionArgs(PerkDefinition perk) {
      String var2 = perk.type().toLowerCase(Locale.ROOT);

      return switch (var2) {
         case "potion" -> Map.of("effect", perk.effect() != null ? perk.effect() : "?");
         case "auto-smelt" -> Map.of();
         default -> Map.of("value", String.format("%.0f", perk.value()));
      };
   }

   private static String prettify(String key) {
      StringBuilder result = new StringBuilder();

      for (String part : key.toLowerCase(Locale.ROOT).split("_")) {
         if (!part.isEmpty()) {
            if (!result.isEmpty()) {
               result.append(' ');
            }

            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
         }
      }

      return result.toString();
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   private record Entry(String key, Material icon, ActionReward reward, PerkDefinition perk, boolean perkUnlocked, int perkLevelsAway) {
      static JobInfoMenuHolder.Entry ofReward(String key, Material icon, ActionReward reward) {
         return new JobInfoMenuHolder.Entry(key, icon, reward, null, false, 0);
      }

      static JobInfoMenuHolder.Entry ofPerk(PerkDefinition perk, Material icon, boolean unlocked, int levelsAway) {
         return new JobInfoMenuHolder.Entry(perk.type() + ":" + perk.level(), icon, null, perk, unlocked, levelsAway);
      }
   }
}
