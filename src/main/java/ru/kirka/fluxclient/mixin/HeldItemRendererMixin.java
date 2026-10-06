package ru.kirka.fluxclient.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.HandRenderEvent;
import ru.kirka.fluxclient.feature.impl.render.HandESP;
import ru.kirka.fluxclient.feature.impl.render.HandShader;
import ru.kirka.fluxclient.feature.impl.render.SwingAnimations;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At("HEAD")
   )
   private void swingAnimation(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      boolean isMainHand = hand == Hand.MAIN_HAND;
      Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      HandRenderEvent event = new HandRenderEvent(arm, swingProgress, item, equipProgress, matrices);
      FluxContext.get().getEventBus().post(event);
      SwingAnimations animations = FluxContext.get().getModuleManager().getModule(SwingAnimations.class);
      if (animations != null && animations.isEnabled()) {
         animations.apply(matrices, swingProgress, hand);
      }
   }

   @Inject(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = @At("HEAD")
   )
   private void handEspHead(float tickDelta, MatrixStack matrices, Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
      HandESP.apply();
      HandShader.apply();
   }

   @Inject(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = @At("RETURN")
   )
   private void handEspTail(float tickDelta, MatrixStack matrices, Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
      HandShader.restore();
      HandESP.restore();
   }
}
