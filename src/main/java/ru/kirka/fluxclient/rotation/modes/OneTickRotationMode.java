package ru.kirka.fluxclient.rotation.modes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.rotation.MoveCorrection;
import ru.kirka.fluxclient.rotation.Rotation;
import ru.kirka.fluxclient.rotation.RotationHandler;
import ru.kirka.fluxclient.rotation.RotationPriority;

public final class OneTickRotationMode implements AuraRotationMode {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private Rotation rotation = Rotation.ZERO;
   private int targetId = -1;
   private boolean ready;
   private boolean yawNudge;

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      if (target != null && target.isAlive()) {
         if (this.ready && this.targetId == target.getId() && this.canUsePreparedRotation(aura, target)) {
            AuraRotationSupport.sendRotationPackets(this.rotation);
         } else {
            this.prepare(aura, target);
         }

         this.nudgeClientYaw();
         handler.rotate(this.rotation, moveCorrection, 180.0F, 180.0F, 180.0F, RotationPriority.TO_TARGET);
      } else {
         this.reset();
      }
   }

   private void prepare(Aura aura, LivingEntity target) {
      this.rotation = AuraRotationSupport.rotationToTarget(aura, target);
      this.targetId = target.getId();
      this.ready = true;
      AuraRotationSupport.sendRotationPackets(this.rotation);
   }

   private boolean canUsePreparedRotation(Aura aura, LivingEntity target) {
      return this.ready && (AuraRotationSupport.canTrace(aura, target, this.rotation, !aura.isNoHitInvEnabled()) || !aura.isWallsEnabled());
   }

   private void nudgeClientYaw() {
      if (mc.player != null) {
         this.yawNudge = !this.yawNudge;
         mc.player.setYaw(mc.player.getYaw() + (this.yawNudge ? 0.1F : -0.1F));
      }
   }

   @Override
   public void reset() {
      this.ready = false;
      this.targetId = -1;
      this.rotation = Rotation.ZERO;
   }
}
