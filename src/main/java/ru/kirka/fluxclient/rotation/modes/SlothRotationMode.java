package ru.kirka.fluxclient.rotation.modes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.rotation.MoveCorrection;
import ru.kirka.fluxclient.rotation.Rotation;
import ru.kirka.fluxclient.rotation.RotationHandler;
import ru.kirka.fluxclient.rotation.RotationPriority;
import ru.kirka.fluxclient.rotation.RotationState;

public final class SlothRotationMode implements AuraRotationMode {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private float yawOffset;
   private float pitchOffset;
   private int tickCounter;
   private int stateTicks;

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      if (mc.player != null && target != null) {
         Rotation base = handler.getState() == RotationState.IDLE ? handler.getPrevRotation() : handler.getCurrentRotation();
         Vec3d delta = AuraRotationSupport.targetDelta(target, AuraRotationSupport.random(0.5F, 1.0F));
         Rotation deltaRotation = AuraRotationSupport.rotationDelta(base, delta);
         this.updateOffsets(aura.isRotationTargetValid(target), deltaRotation.getYaw(), deltaRotation.getPitch());
         float yaw = base.getYaw() + deltaRotation.getYaw() + this.yawOffset;
         float pitch = MathHelper.clamp(base.getPitch() + deltaRotation.getPitch() + this.pitchOffset, -85.0F, 85.0F);
         float yawSpeed = aura.isReadyToAttackNow() ? AuraRotationSupport.random(18.0F, 36.0F) : AuraRotationSupport.random(7.0F, 16.0F);
         float pitchSpeed = aura.isReadyToAttackNow() ? AuraRotationSupport.random(10.0F, 24.0F) : AuraRotationSupport.random(4.0F, 12.0F);
         handler.rotate(new Rotation(yaw, pitch), moveCorrection, yawSpeed, pitchSpeed, AuraRotationSupport.random(15.0F, 45.0F), RotationPriority.TO_TARGET);
      }
   }

   private void updateOffsets(boolean validTarget, float yawDelta, float pitchDelta) {
      this.tickCounter++;
      this.stateTicks++;
      if (this.tickCounter % 5 == 0) {
         float yawTarget = validTarget ? AuraRotationSupport.random(-2.5F, 2.5F) : AuraRotationSupport.random(-8.0F, 8.0F);
         float pitchTarget = validTarget ? AuraRotationSupport.random(-1.5F, 1.5F) : AuraRotationSupport.random(-4.0F, 4.0F);
         if (Math.abs(yawDelta) < 8.0F && Math.abs(pitchDelta) < 6.0F) {
            yawTarget *= 0.35F;
            pitchTarget *= 0.35F;
         }

         float blend = this.stateTicks > 20 ? 0.35F : 0.18F;
         this.yawOffset = AuraRotationSupport.lerp(blend, this.yawOffset, yawTarget);
         this.pitchOffset = AuraRotationSupport.lerp(blend, this.pitchOffset, pitchTarget);
      }
   }

   @Override
   public void reset() {
      this.yawOffset = 0.0F;
      this.pitchOffset = 0.0F;
      this.tickCounter = 0;
      this.stateTicks = 0;
   }
}
