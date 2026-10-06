package ru.kirka.fluxclient.util;

import net.minecraft.client.MinecraftClient;

public final class ServerUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private ServerUtility() {
   }

   public static boolean isST() {
      return is("spooky");
   }

   public static boolean isFT() {
      return is("funtime") || is("playft");
   }

   public static boolean isRW() {
      return is("reallyworld") || is("playrw");
   }

   public static boolean isFS() {
      return is("funsky");
   }

   public static boolean isHW() {
      return is("holy") || is("holly") || is("playhw");
   }

   public static boolean isServerForHPFix() {
      return isFT() || isRW() || isFS();
   }

   public static String getIP() {
      return mc.player != null && mc.player.networkHandler != null && mc.player.networkHandler.getServerInfo() != null
         ? mc.player.networkHandler.getServerInfo().address
         : "single";
   }

   public static boolean is(String ip) {
      return getIP().toLowerCase().contains(ip.toLowerCase());
   }
}
