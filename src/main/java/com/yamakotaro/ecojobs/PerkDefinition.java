package com.yamakotaro.ecojobs;

public record PerkDefinition(int level, String type, double value, String effect) {
   public static final String PAY_BONUS = "pay-bonus";
   public static final String POTION = "potion";
   public static final String DOUBLE_DROP = "double-drop";
   public static final String AUTO_SMELT = "auto-smelt";
   public static final String XP_ORB_BONUS = "xp-orb-bonus";
}
