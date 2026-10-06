package ru.kirka.fluxclient.gui.imgui;

import imgui.ImColor;
import java.awt.Color;

public final class ColorUtil {
   private ColorUtil() {
   }

   public static int clamp(int v, int mn, int mx) {
      return Math.max(mn, Math.min(mx, v));
   }

   public static float clamp(float v, float mn, float mx) {
      return Math.max(mn, Math.min(mx, v));
   }

   public static int rgba(int r, int g, int b, int a) {
      return ImColor.rgba(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255), clamp(a, 0, 255));
   }

   public static int withAlpha(int col, float a) {
      int r = col & 0xFF;
      int g = col >> 8 & 0xFF;
      int b = col >> 16 & 0xFF;
      return rgba(r, g, b, (int)(clamp(a, 0.0F, 1.0F) * 255.0F));
   }

   public static int shade(int col, float factor) {
      int r = (int)((col & 0xFF) * factor);
      int g = (int)((col >> 8 & 0xFF) * factor);
      int b = (int)((col >> 16 & 0xFF) * factor);
      int a = col >> 24 & 0xFF;
      return rgba(r, g, b, a);
   }

   public static int lerpColor(int c1, int c2, float t) {
      t = clamp(t, 0.0F, 1.0F);
      int r = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
      int g = (int)((c1 >> 8 & 0xFF) + ((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * t);
      int b = (int)((c1 >> 16 & 0xFF) + ((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * t);
      int a = (int)((c1 >> 24 & 0xFF) + ((c2 >> 24 & 0xFF) - (c1 >> 24 & 0xFF)) * t);
      return rgba(r, g, b, a);
   }

   public static float[] unpack(int col) {
      return new float[]{(col & 0xFF) / 255.0F, (col >> 8 & 0xFF) / 255.0F, (col >> 16 & 0xFF) / 255.0F, (col >> 24 & 0xFF) / 255.0F};
   }

   public static Color toAwt(int col) {
      return new Color(col & 0xFF, col >> 8 & 0xFF, col >> 16 & 0xFF, col >> 24 & 0xFF);
   }

   public static int fromAwt(Color c) {
      return rgba(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
   }

   public static int hsv(int h, float sat, float val, int a) {
      Color c = Color.getHSBColor((h % 360 + 360) % 360 / 360.0F, clamp(sat, 0.0F, 1.0F), clamp(val, 0.0F, 1.0F));
      return rgba(c.getRed(), c.getGreen(), c.getBlue(), a);
   }
}
