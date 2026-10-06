package ru.kirka.fluxclient.mixin;

import java.awt.Color;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.CustomFog;
import ru.kirka.fluxclient.feature.impl.render.NoRender;
import ru.kirka.fluxclient.feature.impl.render.ShaderSky;
import ru.kirka.fluxclient.render.color.ColorRGBA;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
   @Inject(
      method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      NoRender noRender = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && (noRender.blindness.get() || noRender.darkness.get())) {
         info.setReturnValue(null);
      }
   }

   @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true)
   private static void onGetFogColor(
      Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      CustomFog customFog = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(CustomFog.class);
      if (customFog != null && customFog.isEnabled() && customFog.shouldModifyFog(camera) && customFog.getFogColorEnabled().get()) {
         ColorRGBA c = customFog.getEffectiveFogColor();
         cir.setReturnValue(new Vector4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F));
      } else {
         ShaderSky shaderSky = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(ShaderSky.class);
         if (shaderSky != null && shaderSky.isEnabled()) {
            Color c = shaderSky.horizonColor.get();
            cir.setReturnValue(new Vector4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F));
         }
      }
   }

   @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
   private static void onApplyFog(
      Camera camera, FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir
   ) {
      CustomFog customFog = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(CustomFog.class);
      if (customFog != null && customFog.isEnabled() && customFog.shouldModifyFog(camera)) {
         float start = MathHelper.clamp(customFog.getDistance().getFirstValue(), -8.0F, viewDistance);
         float end = MathHelper.clamp(customFog.getDistance().getSecondValue(), 0.0F, viewDistance);
         if (fogType == FogType.FOG_SKY) {
            start = 0.0F;
            end = Math.min(end, viewDistance);
         }

         Fog original = (Fog)cir.getReturnValue();
         float r = original != null ? original.red() : color.x();
         float g = original != null ? original.green() : color.y();
         float b = original != null ? original.blue() : color.z();
         float a = original != null ? original.alpha() : color.w();
         if (customFog.getFogColorEnabled().get()) {
            ColorRGBA c = customFog.getEffectiveFogColor();
            r = c.getRed() / 255.0F;
            g = c.getGreen() / 255.0F;
            b = c.getBlue() / 255.0F;
            a = c.getAlpha() / 255.0F;
         }

         cir.setReturnValue(new Fog(start, end, FogShape.SPHERE, r, g, b, a));
      }
   }
}
