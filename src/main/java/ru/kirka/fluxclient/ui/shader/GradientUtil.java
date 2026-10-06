package ru.kirka.fluxclient.ui.shader;

import java.awt.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.kirka.fluxclient.render.ColorUtil;

public class GradientUtil {
   private GradientUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static MutableText a(String text, int startColor, int endColor, int speed, float ratio) {
      MutableText component = Text.literal("");
      if (text != null && !text.isEmpty()) {
         float time = (float)(System.currentTimeMillis() % 10000L) / 1000.0F * (100.0F / speed);
         int length = text.length();

         for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            int color = ColorUtil.makeGradient(startColor, endColor, i, length, time, ratio);
            Style style = Style.EMPTY.withColor(color & 16777215);
            component.append(Text.literal(String.valueOf(c)).setStyle(style));
         }

         return component;
      } else {
         return component;
      }
   }

   public static MutableText a(String text, int color, float speed, float offset) {
      MutableText component = Text.literal("");
      if (text != null && !text.isEmpty()) {
         float time = (float)(System.currentTimeMillis() % (long)(speed * 1000.0F)) / (speed * 1000.0F);
         float[] hsb = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null);

         for (int i = 0; i < text.length(); i++) {
            float factor = (float)(Math.sin((time + i * offset / text.length()) * Math.PI * 2.0) * 0.5 + 0.5);
            int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * (0.5F + 0.5F * factor)) & 16777215;
            component.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(rgb)));
         }

         return component;
      } else {
         return component;
      }
   }

   public static int a(int speed, int angle, int startColor, int endColor, long time) {
      float animatedAngle = (float)((angle + time / 10L) % 360L);
      float ratio = animatedAngle / 360.0F;
      return ColorUtil.lerpColorValue(startColor, endColor, (float)((Math.sin(ratio * Math.PI * 2.0 * speed) + 1.0) / 2.0));
   }
}
