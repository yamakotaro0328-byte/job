package com.yamakotaro.ecojobs.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutTest {
   @Test
   void progressBarClampsAndFills() {
      assertEquals("&a&8" + "▌".repeat(10), Layout.bar(0, 100, 10));
      assertEquals("&a" + "▌".repeat(5) + "&8" + "▌".repeat(5), Layout.bar(50, 100, 10));
      assertEquals("&a" + "▌".repeat(10) + "&8", Layout.bar(250, 100, 10));
      assertEquals(100, Layout.percent(5, 0));
   }

   @Test
   void centeredSlotsStayInsideTheRow() {
      assertEquals(List.of(31), Layout.centered(3, 1));
      assertEquals(List.of(29, 31, 33), Layout.centered(3, 3));
      assertEquals(List.of(28, 29, 30, 31, 32, 33, 34), Layout.centered(3, 9));
   }

   @Test
   void durationAndPrettify() {
      assertEquals("1h 5m", Layout.duration(65L * 60000L));
      assertEquals("0m", Layout.duration(-5L));
      assertEquals("Deepslate Diamond Ore", Layout.prettify("DEEPSLATE_DIAMOND_ORE"));
   }
}
