package ru.kirka.fluxclient.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.render.draw.DynamicLightUtility;
import ru.kirka.fluxclient.render.draw.Utils;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
   @Inject(
      method = "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
      at = @At("RETURN")
   )
   private void onRenderWorld(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      CallbackInfo ci
   ) {
      Utils.onRender(positionMatrix, projectionMatrix);
      MatrixStack matrices = new MatrixStack();
      matrices.multiplyPositionMatrix(positionMatrix);
      FluxContext.get().getEventBus().post(new Render3DEvent(matrices, positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)));
   }

   @Inject(
      method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I",
      at = @At("RETURN"),
      cancellable = true
   )
   private static void applyDynamicFullbright(BlockRenderView world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> info) {
      info.setReturnValue(DynamicLightUtility.apply(pos, info.getReturnValueI()));
   }
}
