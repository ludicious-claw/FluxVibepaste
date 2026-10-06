package ru.kirka.fluxclient.rotation.modes;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.rotation.MoveCorrection;
import ru.kirka.fluxclient.rotation.Rotation;
import ru.kirka.fluxclient.rotation.RotationHandler;
import ru.kirka.fluxclient.rotation.RotationPriority;

public final class ReallyWorldRotationMode implements AuraRotationMode {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int MIN_VIA_PROTOCOL = 755;
   private static final int MAX_VIA_PROTOCOL = 765;
   private Rotation rotation = Rotation.ZERO;
   private int targetId = -1;
   private boolean hasCustomRotation;
   private boolean ready;
   private boolean yawNudge;

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      if (target != null && target.isAlive()) {
         Rotation targetRotation = AuraRotationSupport.rotationToTarget(aura, target);
         this.ready = this.needsCustomRotation(aura, target, targetRotation);
         if (!this.ready) {
            this.hasCustomRotation = false;
            handler.rotate(targetRotation, moveCorrection, 160.0F, 160.0F, 45.0F, RotationPriority.TO_TARGET);
         } else {
            if (this.hasCustomRotation && this.targetId == target.getId() && this.canUseTarget(aura, target)) {
               AuraRotationSupport.sendRotationPackets(this.rotation);
            } else {
               this.prepare(target, targetRotation);
            }

            this.nudgeClientYaw();
            handler.rotate(this.rotation, moveCorrection, 180.0F, 180.0F, 180.0F, RotationPriority.TO_TARGET);
         }
      } else {
         this.reset();
      }
   }

   private boolean needsCustomRotation(Aura aura, LivingEntity target, Rotation targetRotation) {
      return mc.player != null && mc.world != null && this.isViaProtocolInRange() && !AuraRotationSupport.canTrace(aura, target, targetRotation, true);
   }

   private void prepare(LivingEntity target, Rotation targetRotation) {
      this.rotation = targetRotation;
      this.targetId = target.getId();
      this.hasCustomRotation = true;
      AuraRotationSupport.sendRotationPackets(this.rotation);
   }

   private boolean canUseTarget(Aura aura, LivingEntity target) {
      return this.ready && (AuraRotationSupport.canTrace(aura, target, this.rotation, !aura.isNoHitInvEnabled()) || !aura.isWallsEnabled());
   }

   private void nudgeClientYaw() {
      if (mc.player != null) {
         this.yawNudge = !this.yawNudge;
         mc.player.setYaw(mc.player.getYaw() + (this.yawNudge ? 0.1F : -0.1F));
      }
   }

   private boolean isViaProtocolInRange() {
      int protocol = this.currentProtocolVersion();
      return protocol >= 755 && protocol <= 765;
   }

   private int currentProtocolVersion() {
      Integer viaVersion = this.viaFabricPlusProtocolVersion();
      return viaVersion != null ? viaVersion : SharedConstants.getGameVersion().getSaveVersion().getId();
   }

   private Integer viaFabricPlusProtocolVersion() {
      try {
         Class<?> viaFabricPlus = Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
         Object impl = viaFabricPlus.getMethod("getImpl").invoke(null);
         Object targetVersion = impl.getClass().getMethod("getTargetVersion").invoke(impl);
         return targetVersion.getClass().getMethod("getVersion").invoke(targetVersion) instanceof Integer value ? value : null;
      } catch (Throwable var6) {
         return null;
      }
   }

   @Override
   public void reset() {
      this.ready = false;
      this.hasCustomRotation = false;
      this.targetId = -1;
      this.rotation = Rotation.ZERO;
   }
}
