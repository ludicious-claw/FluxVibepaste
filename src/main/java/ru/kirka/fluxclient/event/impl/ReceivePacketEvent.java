package ru.kirka.fluxclient.event.impl;

import net.minecraft.network.packet.Packet;
import ru.kirka.fluxclient.event.EventCancellable;

public class ReceivePacketEvent extends EventCancellable {
   private final Packet<?> packet;

   public ReceivePacketEvent(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
