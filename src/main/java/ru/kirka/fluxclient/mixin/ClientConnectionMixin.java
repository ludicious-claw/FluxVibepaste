package ru.kirka.fluxclient.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.ReceivePacketEvent;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
   @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
   private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      ReceivePacketEvent event = new ReceivePacketEvent(packet);
      if (FluxContext.get() != null && FluxContext.get().getEventBus() != null) {
         FluxContext.get().getEventBus().post(event);
      }

      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
