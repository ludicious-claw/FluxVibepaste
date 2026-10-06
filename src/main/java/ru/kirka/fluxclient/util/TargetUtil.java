package ru.kirka.fluxclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.feature.impl.combat.Aura;

public final class TargetUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static LivingEntity getActiveTarget() {
      if (mc.player != null && mc.world != null) {
         if (Aura.target != null && Aura.target.isAlive()) {
            return Aura.target;
         } else if (mc.targetedEntity instanceof LivingEntity living && living.isAlive() && living != mc.player) {
            return living;
         } else {
            Vec3d eye = mc.player.getEyePos();
            Vec3d look = mc.player.getRotationVec(1.0F).normalize();
            LivingEntity closest = null;
            double bestDist = 14.0;

            for (Entity e : mc.world.getEntities()) {
               if (e instanceof LivingEntity living && living != mc.player && living.isAlive()) {
                  Vec3d toEntity = living.getEyePos().subtract(eye);
                  double dist = toEntity.length();
                  if (dist <= bestDist && look.dotProduct(toEntity.normalize()) > 0.982) {
                     bestDist = dist;
                     closest = living;
                  }
               }
            }

            return closest;
         }
      } else {
         return null;
      }
   }
}
