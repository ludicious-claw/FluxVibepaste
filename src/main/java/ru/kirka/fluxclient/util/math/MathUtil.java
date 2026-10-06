package ru.kirka.fluxclient.util.math;

public final class MathUtil {
   private MathUtil() {
   }

   public static float clamp(float val, float min, float max) {
      return Math.max(min, Math.min(max, val));
   }

   public static float lerp(float start, float end, float delta) {
      return start + delta * (end - start);
   }

   public static float damp(float current, float target, float speed, float dt) {
      if (dt <= 0.0F) {
         return current;
      } else {
         float t = 1.0F - (float)Math.exp(-speed * dt);
         return current + (target - current) * t;
      }
   }
}
