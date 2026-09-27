package com.yamakotaro.ecojobs;

public record ActionReward(double money, double xp, double moneyPerLevel, double xpPerLevel) {
   public double moneyFor(double scale) {
      return this.money + this.moneyPerLevel * scale;
   }

   public double xpFor(double scale) {
      return this.xp + this.xpPerLevel * scale;
   }
}
