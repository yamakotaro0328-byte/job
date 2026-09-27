package com.yamakotaro.ecojobs.listeners;

import com.yamakotaro.ecojobs.EcoJobsPlugin;
import com.yamakotaro.ecojobs.PlayerJobManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EvenMoreFishBridge implements Listener {
   private static final String[] EVENT_CLASS_CANDIDATES = new String[]{
      "com.oheers.fish.api.EMFFishEvent", "com.oheers.fish.api.event.EMFFishEvent", "com.oheers.fish.api.event.FishCatchEvent"
   };
   private static final String[] PLAYER_ACCESSORS = new String[]{"getPlayer", "getUser"};
   private static final String[] FISH_ACCESSORS = new String[]{"getFish", "getCaughtFish"};
   private static final String[] RARITY_NAME_ACCESSORS = new String[]{"getValue", "getName", "getIdentifier"};
   private static final String EMF_NAMESPACE = "evenmorefish";
   private static final long RECENT_CATCH_MILLIS = 1000L;
   private final EcoJobsPlugin plugin;
   private final PlayerJobManager jobs;
   private final Map<UUID, Long> recentCatches = new HashMap<>();
   private boolean warned;

   public EvenMoreFishBridge(EcoJobsPlugin plugin, PlayerJobManager jobs) {
      this.plugin = plugin;
      this.jobs = jobs;
   }

   public void register() {
      if (this.plugin.getServer().getPluginManager().getPlugin("EvenMoreFish") != null) {
         Class<? extends Event> eventClass = this.findEventClass();
         if (eventClass == null) {
            this.plugin
               .getLogger()
               .warning(
                  "EvenMoreFish is installed, but none of its known event classes were found - custom fish will fall back to the normal fisherman/treasurehunter rewards."
               );
         } else {
            this.plugin
               .getServer()
               .getPluginManager()
               .registerEvent(eventClass, this, EventPriority.NORMAL, (listener, event) -> this.handleCatch(event), this.plugin, true);
            this.plugin.getLogger().info("EvenMoreFish detected: custom fish now pay the fisherman job by rarity (" + eventClass.getName() + ").");
         }
      }
   }

   private Class<? extends Event> findEventClass() {
      for (String candidate : EVENT_CLASS_CANDIDATES) {
         try {
            Class<?> found = Class.forName(candidate);
            if (Event.class.isAssignableFrom(found)) {
               return (Class<? extends Event>)found;
            }
         } catch (ClassNotFoundException var6) {
         }
      }

      return null;
   }

   private void handleCatch(Event event) {
      try {
         Object rawPlayer = this.invokeFirst(event, PLAYER_ACCESSORS);
         Object fish = this.invokeFirst(event, FISH_ACCESSORS);
         if (!(rawPlayer instanceof Player player && fish != null)) {
            this.warnOnce("EvenMoreFish's event did not expose a player and a fish where expected", null);
            return;
         }

         String rarity = this.rarityNameOf(fish);
         this.recentCatches.put(player.getUniqueId(), System.currentTimeMillis());
         this.jobs.reward(player, "fisherman", "catch-emf-fish", rarity == null ? "default" : rarity, 1.0);
      } catch (RuntimeException | ReflectiveOperationException var6) {
         this.warnOnce("EvenMoreFish integration failed while handling a catch", var6);
      }
   }

   private String rarityNameOf(Object fish) throws ReflectiveOperationException {
      Object rarity = this.invokeFirst(fish, "getRarity");
      if (rarity == null) {
         return null;
      } else {
         String value = this.invokeFirst(rarity, RARITY_NAME_ACCESSORS) instanceof String text ? text : rarity.toString();
         return value == null ? null : value.toLowerCase(Locale.ROOT);
      }
   }

   private Object invokeFirst(Object target, String... methodNames) throws ReflectiveOperationException {
      for (String methodName : methodNames) {
         try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
         } catch (NoSuchMethodException var8) {
         }
      }

      return null;
   }

   public boolean handledRecently(Player player) {
      Long at = this.recentCatches.get(player.getUniqueId());
      if (at == null) {
         return false;
      } else if (System.currentTimeMillis() - at >= 1000L) {
         this.recentCatches.remove(player.getUniqueId());
         return false;
      } else {
         return true;
      }
   }

   public boolean isEvenMoreFishItem(ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         ItemMeta meta = stack.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
               if ("evenmorefish".equalsIgnoreCase(key.getNamespace())) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private void warnOnce(String message, Throwable error) {
      if (!this.warned) {
         this.warned = true;
         if (error != null) {
            this.plugin.getLogger().log(Level.WARNING, message + " - giving up on the integration for this session.", error);
         } else {
            this.plugin.getLogger().warning(message + " - giving up on the integration for this session.");
         }
      }
   }
}
