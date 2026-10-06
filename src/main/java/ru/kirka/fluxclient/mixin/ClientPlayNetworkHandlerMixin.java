package ru.kirka.fluxclient.mixin;

import java.util.Locale;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.DeathEvent;
import ru.kirka.fluxclient.event.impl.PlaceDeniedEvent;
import ru.kirka.fluxclient.event.impl.ServerJoinEvent;
import ru.kirka.fluxclient.event.impl.TransferEvent;
import ru.kirka.fluxclient.event.impl.WorldChangeEvent;
import ru.kirka.fluxclient.feature.impl.combat.Velocity;
import ru.kirka.fluxclient.feature.impl.render.PopChams;
import ru.kirka.fluxclient.feature.impl.render.XRay;
import ru.kirka.fluxclient.util.WorldUtility;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
   @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
   private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
         Velocity vel = FluxContext.get().getModuleManager().getModule(Velocity.class);
         if (vel != null && vel.isEnabled()) {
            float h = vel.horizontal.get() / 100.0F;
            float v = vel.vertical.get() / 100.0F;
            if (h == 0.0F && v == 0.0F) {
               ci.cancel();
            } else {
               mc.player.setVelocity(packet.getVelocityX() / 8000.0 * h, packet.getVelocityY() / 8000.0 * v, packet.getVelocityZ() / 8000.0 * h);
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "onExplosion", at = @At("HEAD"), cancellable = true)
   private void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
      Velocity vel = FluxContext.get().getModuleManager().getModule(Velocity.class);
      if (vel != null && vel.isEnabled() && vel.explosions.get()) {
         float h = vel.horizontal.get() / 100.0F;
         float v = vel.vertical.get() / 100.0F;
         if (h == 0.0F && v == 0.0F) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "onHealthUpdate", at = @At("HEAD"))
   private void onHealthUpdateDeath(HealthUpdateS2CPacket packet, CallbackInfo ci) {
      if (packet.getHealth() <= 0.0F) {
         try {
            FluxContext.get().getEventBus().post(new DeathEvent());
         } catch (Exception var4) {
         }
      }
   }

   @Inject(method = "onBlockEntityUpdate", at = @At("TAIL"))
   private void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         BlockPos pos = packet.getPos();
         BlockEntity be = mc.world.getBlockEntity(pos);
         if (be != null && !WorldUtility.blockEntities.contains(be)) {
            WorldUtility.blockEntities.add(be);
         }
      }
   }

   @Inject(method = "onChunkData", at = @At("TAIL"))
   private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         WorldChunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());
         if (chunk != null) {
            chunk.getBlockEntities().values().forEach(be -> {
               if (!WorldUtility.blockEntities.contains(be)) {
                  WorldUtility.blockEntities.add(be);
               }
            });
            XRay xray = FluxContext.get().getModuleManager().getModule(XRay.class);
            if (xray != null && xray.isEnabled()) {
               new Thread(() -> xray.scanChunk(chunk), "XRay-ChunkScan").start();
            }
         }
      }
   }

   @Inject(method = "onGameJoin", at = @At("TAIL"))
   private void onGameJoinEvent(GameJoinS2CPacket packet, CallbackInfo ci) {
      WorldUtility.blockEntities.clear();

      try {
         FluxContext.get().getEventBus().post(new WorldChangeEvent());
         FluxContext.get().getEventBus().post(new ServerJoinEvent());
      } catch (Exception var4) {
      }
   }

   @Inject(method = "onGameMessage", at = @At("HEAD"))
   private void onGameMessageDeny(GameMessageS2CPacket packet, CallbackInfo ci) {
      try {
         String text = packet.content().getString().toLowerCase(Locale.ROOT);
         if (text.contains("can't place")) {
            FluxContext.get().getEventBus().post(new PlaceDeniedEvent());
         }

         if (text.contains("перемещен") || text.contains("перемещён") || text.contains("moved to")) {
            FluxContext.get().getEventBus().post(new TransferEvent());
         }
      } catch (Exception var4) {
      }
   }

   @Inject(method = "onEntityStatus", at = @At("HEAD"))
   private void onTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
      if (packet.getStatus() == 35) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            Entity entity = packet.getEntity(mc.world);
            if (entity != null) {
               PopChams popChams = FluxContext.get().getModuleManager().getModule(PopChams.class);
               if (popChams != null && popChams.isEnabled()) {
                  popChams.onTotemPop(entity);
               }
            }
         }
      }
   }
}
