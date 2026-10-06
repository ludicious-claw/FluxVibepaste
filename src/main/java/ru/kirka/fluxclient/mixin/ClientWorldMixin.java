package ru.kirka.fluxclient.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.CustomFog;
import ru.kirka.fluxclient.feature.impl.render.CustomSky;
import ru.kirka.fluxclient.feature.impl.render.ShaderSky;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
   @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
   private void onGetSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
      CustomFog customFog = FluxContext.get().getModuleManager().getModule(CustomFog.class);
      if (customFog != null && customFog.isEnabled() && customFog.blendSky.get()) {
         cir.setReturnValue(customFog.fogColor.get().getRGB());
      } else {
         ShaderSky shaderSky = FluxContext.get().getModuleManager().getModule(ShaderSky.class);
         if (shaderSky != null && shaderSky.isEnabled()) {
            cir.setReturnValue(shaderSky.zenithColor.get().getRGB());
         } else {
            CustomSky sky = FluxContext.get().getModuleManager().getModule(CustomSky.class);
            if (sky != null && sky.isEnabled()) {
               cir.setReturnValue(sky.skyColor.getRGB());
            }
         }
      }
   }

   @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
   private void onGetCloudsColor(float tickDelta, CallbackInfoReturnable<Integer> cir) {
      ShaderSky shaderSky = FluxContext.get().getModuleManager().getModule(ShaderSky.class);
      if (shaderSky != null && shaderSky.isEnabled() && shaderSky.hideClouds.get()) {
         cir.setReturnValue(0);
      } else {
         CustomFog customFog = FluxContext.get().getModuleManager().getModule(CustomFog.class);
         if (customFog != null && customFog.isEnabled() && customFog.blendSky.get()) {
            cir.setReturnValue(customFog.fogColor.get().getRGB());
         } else {
            CustomSky sky = FluxContext.get().getModuleManager().getModule(CustomSky.class);
            if (sky != null && sky.isEnabled()) {
               cir.setReturnValue(sky.cloudsColor.getRGB());
            }
         }
      }
   }
}
