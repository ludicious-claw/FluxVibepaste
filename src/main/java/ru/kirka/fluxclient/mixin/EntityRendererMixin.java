package ru.kirka.fluxclient.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.NameTagsPlus;
import ru.kirka.fluxclient.util.EntityRenderStateAddition;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "updateRenderState", at = @At("HEAD"))
   private void updateRenderingEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
      if (state instanceof EntityRenderStateAddition addition) {
         addition.flux$setEntity(entity);
      }
   }

   @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
   private void onRenderLabel(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      NameTagsPlus nametags = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(NameTagsPlus.class);
      if (nametags != null && nametags.isEnabled()) {
         ci.cancel();
      }
   }
}
