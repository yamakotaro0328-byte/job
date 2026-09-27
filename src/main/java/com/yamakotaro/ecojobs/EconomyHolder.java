package com.yamakotaro.ecojobs;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHolder {
   private final Plugin plugin;
   private Economy economy;

   public EconomyHolder(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean setup() {
      if (this.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      } else {
         RegisteredServiceProvider<Economy> registration = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);
         if (registration == null) {
            return false;
         } else {
            this.economy = (Economy)registration.getProvider();
            return true;
         }
      }
   }

   public Economy get() {
      return this.economy;
   }
}
