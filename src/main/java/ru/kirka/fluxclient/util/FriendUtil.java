package ru.kirka.fluxclient.util;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendUtil {
   private static final Set<String> FRIENDS = ConcurrentHashMap.newKeySet();

   private FriendUtil() {
   }

   public static boolean isFriend(String name) {
      return name == null ? false : FRIENDS.contains(name.toLowerCase(Locale.ROOT));
   }

   public static void addFriend(String name) {
      if (name != null && !name.isBlank()) {
         FRIENDS.add(name.toLowerCase(Locale.ROOT));
      }
   }

   public static void removeFriend(String name) {
      if (name != null) {
         FRIENDS.remove(name.toLowerCase(Locale.ROOT));
      }
   }

   public static void clear() {
      FRIENDS.clear();
   }
}
