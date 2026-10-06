package ru.kirka.fluxclient.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.feature.impl.combat.Aura;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
   private static boolean flux$guard = false;

   @Shadow
   public abstract void sendPacket(Packet<?> var1);

   @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
   private void flux$SpoofSilentRotation(Packet<?> packet, CallbackInfo ci) {
      if (!flux$guard) {
         if (packet instanceof PlayerMoveC2SPacket move) {
            float[] silent = Aura.pollSilentRotation();
            if (silent != null) {
               PlayerMoveC2SPacket replacement;
               if (move.changesPosition()) {
                  replacement = new Full(move.getX(0.0), move.getY(0.0), move.getZ(0.0), silent[0], silent[1], move.isOnGround(), move.horizontalCollision());
               } else {
                  replacement = new LookAndOnGround(silent[0], silent[1], move.isOnGround(), move.horizontalCollision());
               }

               ci.cancel();
               flux$guard = true;

               try {
                  this.sendPacket(replacement);
               } finally {
                  flux$guard = false;
               }
            }
         }
      }
   }
}
