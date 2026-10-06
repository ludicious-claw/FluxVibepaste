package ru.kirka.fluxclient.util;

import net.minecraft.client.network.PlayerListEntry;
import ru.kirka.fluxclient.common.Interface;

public class ServerUtil implements Interface {
   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static String getServerAddress() {
      if (mc.getCurrentServerEntry() != null) {
         return mc.getCurrentServerEntry().address;
      } else {
         return mc.isConnectedToLocalServer() ? "Singleplayer" : "None";
      }
   }

   public static String b() {
      return getServerAddress();
   }

   public static double getSpeed() {
      return mc.player == null ? 0.0 : Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0;
   }

   public static double c() {
      return getSpeed();
   }

   public static int getPing() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         return entry != null ? entry.getLatency() : 0;
      } else {
         return 0;
      }
   }

   public static int d() {
      return getPing();
   }
}
