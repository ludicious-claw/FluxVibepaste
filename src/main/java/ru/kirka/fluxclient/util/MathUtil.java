package ru.kirka.fluxclient.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.common.Interface;

public class MathUtil implements Interface {
   private MathUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   public static float a(float start, float end, float delta) {
      return lerp(start, end, delta);
   }

   public static Vec3d interpolateEntity(Entity entity, float partialTicks) {
      return new Vec3d(
         entity.prevX + (entity.getX() - entity.prevX) * partialTicks,
         entity.prevY + (entity.getY() - entity.prevY) * partialTicks,
         entity.prevZ + (entity.getZ() - entity.prevZ) * partialTicks
      );
   }

   public static Vec3d a(Entity entity, float partialTicks) {
      return interpolateEntity(entity, partialTicks);
   }

   public static float clamp(float num, float min, float max) {
      return Math.min(Math.max(num, min), max);
   }

   public static float b(float num, float min, float max) {
      return clamp(num, min, max);
   }

   public static double scale(double coordinate, int factor) {
      return coordinate * mc.getWindow().getScaleFactor() / mc.getWindow().calculateScaleFactor(factor, mc.forcesUnicodeFont());
   }

   public static float smoothstep(float value) {
      float clamped = clamp(value, 0.0F, 1.0F);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }

   public static float a(float value) {
      return smoothstep(value);
   }

   public static float getRandom(float min, float max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static float a(float min, float max) {
      return getRandom(min, max);
   }

   public static float[] calculatePadding(float smoothness) {
      float horizontal = -smoothness / 2.0F + smoothness * 2.0F;
      float vertical = smoothness / 2.0F + smoothness;
      return new float[]{horizontal, vertical};
   }

   public static float[] b(float smoothness) {
      return calculatePadding(smoothness);
   }

   public static String getRomanNumeral(int amplifier) {
      String[] strings = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
      return amplifier >= 0 && amplifier < strings.length ? strings[amplifier] : String.valueOf(amplifier + 1);
   }

   public static String a(int amplifier) {
      return getRomanNumeral(amplifier);
   }

   public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static boolean a(double mouseX, double mouseY, float x, float y, float width, float height) {
      return isHovered(mouseX, mouseY, x, y, width, height);
   }

   public static float expSmooth(float current, float target, float speed) {
      float delta = mc.getRenderTickCounter().getLastFrameDuration();
      return current + (target - current) * (1.0F - (float)Math.exp(-speed * delta));
   }

   public static float c(float current, float target, float speed) {
      return expSmooth(current, target, speed);
   }
}
