package com.yamakotaro.ecojobs;

public class PlayerJobProgress {
   private int level;
   private double xp;
   private int prestige;

   public PlayerJobProgress(int level, double xp) {
      this(level, xp, 0);
   }

   public PlayerJobProgress(int level, double xp, int prestige) {
      this.level = level;
      this.xp = xp;
      this.prestige = prestige;
   }

   public int getLevel() {
      return this.level;
   }

   public void setLevel(int level) {
      this.level = level;
   }

   public double getXp() {
      return this.xp;
   }

   public void setXp(double xp) {
      this.xp = xp;
   }

   public int getPrestige() {
      return this.prestige;
   }

   public void setPrestige(int prestige) {
      this.prestige = prestige;
   }
}
