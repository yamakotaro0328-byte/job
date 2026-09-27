package com.yamakotaro.ecojobs;

public class PlayerJobProgress {
   private int level;
   private double xp;
   private int prestige;
   private double earned;
   private long actions;

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

   /** Lifetime money earned from this job (all sources: actions, quests, milestones). */
   public double getEarned() {
      return this.earned;
   }

   public void setEarned(double earned) {
      this.earned = earned;
   }

   /** Lifetime number of paid actions performed for this job. */
   public long getActions() {
      return this.actions;
   }

   public void setActions(long actions) {
      this.actions = actions;
   }
}
