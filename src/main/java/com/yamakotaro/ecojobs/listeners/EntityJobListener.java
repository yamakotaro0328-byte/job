package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.JobDefinition;
import com.yamakotaro.ecojobs.JobManager;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerFishEvent.State;

public class EntityJobListener implements Listener {
   private final PlayerJobManager jobs;
   private final JobManager jobManager;
   private final EvenMoreFishBridge evenMoreFish;
   private final Map<UUID, Map<UUID, Long>> recentPlayerKills = new HashMap<>();

   public EntityJobListener(PlayerJobManager jobs, JobManager jobManager, EvenMoreFishBridge evenMoreFish) {
      this.jobs = jobs;
      this.jobManager = jobManager;
      this.evenMoreFish = evenMoreFish;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEntityDeath(EntityDeathEvent event) {
      Player killer = event.getEntity().getKiller();
      if (killer != null) {
         String type = event.getEntityType().name();
         this.jobs.reward(killer, "slayer", "kill-boss", type, 1.0);
         this.jobs.reward(killer, "hunter", "kill-mob", type, 1.0);
         if (this.isListedHostile(type) && this.wasRangedKill(event.getEntity().getLastDamageCause())) {
            this.jobs.reward(killer, "archer", "kill-mob-ranged", type, 1.0);
         }
      }
   }

   private boolean isListedHostile(String entityType) {
      JobDefinition hunter = this.jobManager.get("hunter");
      return hunter != null && hunter.getReward("kill-mob", entityType) != null;
   }

   private boolean wasRangedKill(EntityDamageEvent lastDamage) {
      return lastDamage instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Projectile;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player victim = event.getPlayer();
      Player killer = victim.getKiller();
      if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
         if (this.shouldPayForPlayerKill(killer.getUniqueId(), victim.getUniqueId())) {
            this.jobs.reward(killer, "warrior", "kill-player", "default", 1.0);
         }
      }
   }

   private boolean shouldPayForPlayerKill(UUID killer, UUID victim) {
      long cooldownMillis = this.jobManager.playerKillCooldownMillis();
      if (cooldownMillis <= 0L) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         Map<UUID, Long> victims = this.recentPlayerKills.computeIfAbsent(killer, k -> new HashMap<>());
         victims.entrySet().removeIf(entry -> now - entry.getValue() >= cooldownMillis);
         if (victims.containsKey(victim)) {
            return false;
         } else {
            victims.put(victim, now);
            return true;
         }
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEntityBreed(EntityBreedEvent event) {
      if (event.getBreeder() instanceof Player player) {
         this.jobs.reward(player, "breeder", "breed-entity", event.getEntityType().name(), 1.0);
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEntityTame(EntityTameEvent event) {
      if (event.getOwner() instanceof Player player) {
         this.jobs.reward(player, "tamer", "tame-entity", event.getEntityType().name(), 1.0);
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onShear(PlayerShearEntityEvent event) {
      this.jobs.reward(event.getPlayer(), "shearer", "shear-entity", event.getEntity().getType().name(), 1.0);
   }

   @EventHandler(
      ignoreCancelled = true,
      priority = EventPriority.HIGHEST
   )
   public void onFish(PlayerFishEvent event) {
      if (event.getState() == State.CAUGHT_FISH) {
         if (event.getCaught() instanceof Item item) {
            if (!this.evenMoreFish.handledRecently(event.getPlayer()) && !this.evenMoreFish.isEvenMoreFishItem(item.getItemStack())) {
               String material = item.getItemStack().getType().name();
               switch (material) {
                  case "COD":
                  case "SALMON":
                  case "PUFFERFISH":
                  case "TROPICAL_FISH":
                     this.jobs.reward(event.getPlayer(), "fisherman", "catch-fish", material, 1.0);
                     break;
                  default:
                     this.jobs.reward(event.getPlayer(), "treasurehunter", "catch-treasure", material, 1.0);
               }
            }
         }
      }
   }
}
