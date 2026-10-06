package ru.kirka.fluxclient.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.NoRender;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
   @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
   private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoRender noRender = FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.fire.get()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
   private static void renderInWallOverlayHook(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      NoRender noRender = FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.cameraClip.get()) {
         ci.cancel();
      }
   }
}
