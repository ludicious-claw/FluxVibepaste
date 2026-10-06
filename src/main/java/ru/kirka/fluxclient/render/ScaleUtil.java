package ru.kirka.fluxclient.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import ru.kirka.fluxclient.common.Interface;

public class ScaleUtil implements Interface {
   private ScaleUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void applyScale(DrawContext context, int targetScale) {
      Window window = mc.getWindow();
      double previous = window.getScaleFactor();
      double target = window.calculateScaleFactor(targetScale, mc.forcesUnicodeFont());
      window.setScaleFactor(target);
      context.getMatrices().push();
      float ratio = (float)(target / previous);
      context.getMatrices().scale(ratio, ratio, 1.0F);
   }

   public static void a(DrawContext context, int scale) {
      applyScale(context, scale);
   }

   public static void restoreScale(DrawContext context) {
      context.getMatrices().pop();
      int vanillaScale = (Integer)mc.options.getGuiScale().getValue();
      mc.getWindow().setScaleFactor(mc.getWindow().calculateScaleFactor(vanillaScale, mc.forcesUnicodeFont()));
   }

   public static void a(DrawContext context) {
      restoreScale(context);
   }

   public static void b(DrawContext context) {
      applyScale(context, (Integer)mc.options.getGuiScale().getValue());
   }

   public static void c(DrawContext context) {
      context.getMatrices().pop();
      mc.getWindow().setScaleFactor(mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont()));
   }
}
