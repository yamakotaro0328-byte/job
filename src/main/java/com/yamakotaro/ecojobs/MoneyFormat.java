package com.yamakotaro.ecojobs;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormat {
   private MoneyFormat() {
   }

   public static String format(double amount) {
      return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
   }
}
