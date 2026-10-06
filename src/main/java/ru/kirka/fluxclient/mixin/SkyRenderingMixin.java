package ru.kirka.fluxclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.Ambience;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.SkyboxRenderer;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
   @Inject(method = "renderSky(FFF)V", at = @At("HEAD"), cancellable = true)
   private void renderCustomSkybox(float red, float green, float blue, CallbackInfo info) {
      Ambience ambience = this.getAmbience();
      if (ambience != null && ambience.shouldRenderCustomSkybox()) {
         ColorRGBA color = ambience.shouldTintSky() ? ambience.getResolvedSkyColor() : ColorRGBA.WHITE;
         if (ambience.hasCustomSkybox()) {
            SkyboxRenderer.render(ambience.getSkyboxTexture(), color);
         }

         if (ambience.hasShaderSkybox()) {
            float time = (float)(System.currentTimeMillis() % 100000000L) / 1000.0F;
            float opacity = ambience.hasCustomSkybox() ? ambience.getShaderOpacity() : 1.0F;
            SkyboxRenderer.render(ambience.getSkyShaderProgram(), color, time, opacity);
         }

         info.cancel();
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void closeCustomSkybox(CallbackInfo info) {
      SkyboxRenderer.close();
   }

   @Redirect(method = "renderStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0))
   private void redirectStarColor(float red, float green, float blue, float alpha) {
      Ambience ambience = this.getAmbience();
      if (ambience != null && ambience.shouldTintStars()) {
         ColorRGBA color = ambience.getResolvedStarsColor();
         RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
      } else {
         RenderSystem.setShaderColor(red, green, blue, alpha);
      }
   }

   private Ambience getAmbience() {
      return FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(Ambience.class);
   }
}
