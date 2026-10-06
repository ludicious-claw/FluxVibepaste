package ru.kirka.fluxclient.render.draw;

public final class MathUtility {
   private static final int TABLE_SIZE = 65536;
   private static final double[] TRIG_TABLE = new double[65536];

   public static double sin(double radians) {
      int index = (int)(radians * 10430.378350470453) & 65535;
      return TRIG_TABLE[index];
   }

   public static double cos(double radians) {
      int index = (int)(radians * 10430.378350470453 + 16384.0) & 65535;
      return TRIG_TABLE[index];
   }

   public static float random(double min, double max) {
      return (float)(min + (max - min) * Math.random());
   }

   public static double cubicBezier(double t, double p0, double p1, double p2, double p3) {
      return Math.pow(1.0 - t, 3.0) * p0 + 3.0 * t * Math.pow(1.0 - t, 2.0) * p1 + 3.0 * Math.pow(t, 2.0) * (1.0 - t) * p2 + Math.pow(t, 3.0) * p3;
   }

   public static float interpolate(double oldValue, double newValue, double interpolationValue) {
      return (float)(oldValue + (newValue - oldValue) * interpolationValue);
   }

   private MathUtility() {
      throw new UnsupportedOperationException();
   }

   static {
      for (int i = 0; i < 65536; i++) {
         TRIG_TABLE[i] = Math.sin(i * (Math.PI * 2) / 65536.0);
      }
   }
}
