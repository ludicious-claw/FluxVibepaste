package ru.kirka.fluxclient.mixin;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.misc.Freecam;
import ru.kirka.fluxclient.feature.impl.render.CameraDynamics;
import ru.kirka.fluxclient.feature.impl.render.NoRender;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   private Quaternionf rotation;

   @Shadow
   protected abstract void setPos(double var1, double var3, double var5);

   @Inject(method = "update", at = @At("RETURN"))
   private void freecamCam(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      if (Freecam.active) {
         this.setPos(Freecam.camX, Freecam.camY, Freecam.camZ);
      }

      CameraDynamics cd = FluxContext.get().getModuleManager().getModule(CameraDynamics.class);
      if (cd != null && cd.isEnabled()) {
         float roll = cd.getCalculatedRoll(tickDelta);
         if (roll != 0.0F) {
            this.rotation.rotateZ((float)Math.toRadians(roll));
         }
      }
   }

   @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
   private void getSubmergedFluidState(CallbackInfoReturnable<CameraSubmersionType> ci) {
      NoRender noRender = FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.water.get()) {
         ci.setReturnValue(CameraSubmersionType.NONE);
      }
   }

   @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
   private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> info) {
      NoRender noRender = FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.cameraClip.get()) {
         info.setReturnValue(desiredCameraDistance);
      }
   }
}
