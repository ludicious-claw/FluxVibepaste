package ru.kirka.fluxclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public final class MoveUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private MoveUtil() {
   }

   public static boolean isMoving() {
      return mc.player != null && mc.options != null
         ? mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()
         : false;
   }

   public static double getDirection() {
      return mc.player != null && mc.options != null ? getDirection(mc.player.getYaw()) : 0.0;
   }

   public static double getDirection(float yaw) {
      if (mc.player != null && mc.options != null) {
         float forward = 0.0F;
         float strafe = 0.0F;
         if (mc.options.forwardKey.isPressed()) {
            forward++;
         }

         if (mc.options.backKey.isPressed()) {
            forward--;
         }

         if (mc.options.leftKey.isPressed()) {
            strafe++;
         }

         if (mc.options.rightKey.isPressed()) {
            strafe--;
         }

         if (forward == 0.0F && strafe == 0.0F) {
            return Math.toRadians(yaw);
         } else {
            if (forward < 0.0F) {
               yaw += 180.0F;
            }

            float forwardFactor = 1.0F;
            if (forward < 0.0F) {
               forwardFactor = -0.5F;
            } else if (forward > 0.0F) {
               forwardFactor = 0.5F;
            }

            if (strafe > 0.0F) {
               yaw -= 90.0F * forwardFactor;
            } else if (strafe < 0.0F) {
               yaw += 90.0F * forwardFactor;
            }

            return Math.toRadians(yaw);
         }
      } else {
         return Math.toRadians(yaw);
      }
   }

   public static void strafe(double speed) {
      if (mc.player != null) {
         if (!isMoving()) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
         } else {
            double dir = getDirection();
            double x = -Math.sin(dir) * speed;
            double z = Math.cos(dir) * speed;
            mc.player.setVelocity(x, mc.player.getVelocity().y, z);
         }
      }
   }

   public static void strafe() {
      if (mc.player != null) {
         strafe(getHorizontalSpeed());
      }
   }

   public static double getHorizontalSpeed() {
      if (mc.player == null) {
         return 0.0;
      } else {
         Vec3d vel = mc.player.getVelocity();
         return Math.hypot(vel.x, vel.z);
      }
   }

   public static double getBaseMoveSpeed() {
      double baseSpeed = 0.2873;
      if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.SPEED)) {
         StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.SPEED);
         if (effect != null) {
            baseSpeed *= 1.0 + 0.2 * (effect.getAmplifier() + 1);
         }
      }

      return baseSpeed;
   }

   public static double getJumpMotion() {
      double motion = 0.42;
      if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
         StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
         if (effect != null) {
            motion += (effect.getAmplifier() + 1) * 0.1;
         }
      }

      return motion;
   }

   public static void stop() {
      if (mc.player != null) {
         mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
      }
   }
}
