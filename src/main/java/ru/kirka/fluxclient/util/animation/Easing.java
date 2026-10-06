package ru.kirka.fluxclient.util.animation;

public enum Easing {
   LINEAR,
   EASE_OUT_QUAD,
   EASE_IN_OUT_CUBIC,
   EASE_OUT_BACK,
   EASE_OUT_EXPO;

   public float ease(float x) {
      return switch (this) {
         case LINEAR -> x;
         case EASE_OUT_QUAD -> 1.0F - (1.0F - x) * (1.0F - x);
         case EASE_IN_OUT_CUBIC -> x < 0.5F ? 4.0F * x * x * x : 1.0F - (float)Math.pow(-2.0F * x + 2.0F, 3.0) / 2.0F;
         case EASE_OUT_BACK -> {
            float c = 1.70158F;
            float u = x - 1.0F;
            yield 1.0F + (c + 1.0F) * u * u * u + c * u * u;
         }
         case EASE_OUT_EXPO -> x >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * x);
      };
   }
}
