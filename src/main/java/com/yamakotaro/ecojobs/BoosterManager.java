package com.yamakotaro.ecojobs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class BoosterManager {
   public static final String GLOBAL_SCOPE = "all";
   private final Map<String, BoosterManager.ActiveBooster> boosters = new HashMap<>();

   public void start(String scope, double moneyMultiplier, double xpMultiplier, long durationMillis, String activatedBy) {
      this.boosters
         .put(scope, new BoosterManager.ActiveBooster(scope, moneyMultiplier, xpMultiplier, System.currentTimeMillis() + durationMillis, activatedBy));
   }

   public boolean stop(String scope) {
      return this.boosters.remove(scope) != null;
   }

   public int stopAll() {
      int count = this.active().size();
      this.boosters.clear();
      return count;
   }

   public BoosterManager.ActiveBooster getActiveBooster(String scope) {
      return this.active(scope);
   }

   public double moneyMultiplierFor(String jobId) {
      return this.combined(jobId, BoosterManager.ActiveBooster::moneyMultiplier);
   }

   public double xpMultiplierFor(String jobId) {
      return this.combined(jobId, BoosterManager.ActiveBooster::xpMultiplier);
   }

   private double combined(String jobId, ToDoubleFunction<BoosterManager.ActiveBooster> extractor) {
      double result = 1.0;
      BoosterManager.ActiveBooster global = this.active("all");
      if (global != null) {
         result *= extractor.applyAsDouble(global);
      }

      if (!jobId.equals("all")) {
         BoosterManager.ActiveBooster jobBooster = this.active(jobId);
         if (jobBooster != null) {
            result *= extractor.applyAsDouble(jobBooster);
         }
      }

      return result;
   }

   private BoosterManager.ActiveBooster active(String scope) {
      BoosterManager.ActiveBooster booster = this.boosters.get(scope);
      if (booster == null) {
         return null;
      } else if (System.currentTimeMillis() >= booster.expiresAtMillis()) {
         this.boosters.remove(scope);
         return null;
      } else {
         return booster;
      }
   }

   public Collection<BoosterManager.ActiveBooster> active() {
      this.boosters.entrySet().removeIf(entry -> System.currentTimeMillis() >= entry.getValue().expiresAtMillis());
      return this.boosters.values();
   }

   public record ActiveBooster(String scope, double moneyMultiplier, double xpMultiplier, long expiresAtMillis, String activatedBy) {
   }
}
