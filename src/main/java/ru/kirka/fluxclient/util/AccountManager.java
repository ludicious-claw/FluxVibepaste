package ru.kirka.fluxclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import ru.kirka.fluxclient.mixin.MinecraftClientAccessor;

public final class AccountManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type LIST_TYPE = (new TypeToken<ArrayList<AccountManager.Entry>>() {}).getType();
   private static final Pattern NICK = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
   private static final ArrayList<AccountManager.Entry> ACCOUNT_LIST = new ArrayList<>();
   private static boolean loaded;

   private AccountManager() {
   }

   public static boolean isValidNick(String name) {
      return name != null && NICK.matcher(name).matches();
   }

   public static synchronized List<AccountManager.Entry> getAccounts() {
      ensureLoaded();
      ArrayList<AccountManager.Entry> copy = new ArrayList<>(ACCOUNT_LIST);
      copy.sort(Comparator.<AccountManager.Entry>comparingLong(e -> e.lastUsed).reversed());
      return copy;
   }

   public static synchronized String addAccount(String name) {
      ensureLoaded();
      if (!isValidNick(name)) {
         return "Только латиница, цифры и _ (3-16)";
      } else {
         for (int i = 0; i < ACCOUNT_LIST.size(); i++) {
            if (ACCOUNT_LIST.get(i).name.equalsIgnoreCase(name)) {
               return "Такой аккаунт уже есть";
            }
         }

         ACCOUNT_LIST.add(new AccountManager.Entry(name, System.currentTimeMillis()));
         save();
         return null;
      }
   }

   public static synchronized void removeAccount(String name) {
      ensureLoaded();
      ACCOUNT_LIST.removeIf(e -> e.name.equalsIgnoreCase(name));
      save();
   }

   public static synchronized boolean switchTo(String name) {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && name != null && !name.isEmpty()) {
            Session cur = mc.getSession();
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            Session next = new Session(name, id, cur.getAccessToken(), cur.getXuid(), cur.getClientId(), cur.getAccountType());
            MinecraftClientAccessor accessor = (MinecraftClientAccessor)mc;
            accessor.flux$setSession(next);
            touch(name);
            save();
            return true;
         } else {
            return false;
         }
      } catch (Exception var6) {
         return false;
      }
   }

   private static void touch(String name) {
      for (int i = 0; i < ACCOUNT_LIST.size(); i++) {
         AccountManager.Entry e = ACCOUNT_LIST.get(i);
         if (e.name.equalsIgnoreCase(name)) {
            e.name = name;
            e.lastUsed = System.currentTimeMillis();
         }
      }
   }

   public static String currentName() {
      try {
         return MinecraftClient.getInstance().getSession().getUsername();
      } catch (Exception var1) {
         return "Player";
      }
   }

   private static File file() {
      try {
         return new File(MinecraftClient.getInstance().runDirectory, "fluxclient/accounts.json");
      } catch (Exception var1) {
         return new File("run/fluxclient/accounts.json");
      }
   }

   private static synchronized void ensureLoaded() {
      if (!loaded) {
         loaded = true;

         try {
            File f = file();
            if (!f.isFile()) {
               return;
            }

            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            List<AccountManager.Entry> list = (List<AccountManager.Entry>)GSON.fromJson(json, LIST_TYPE);
            if (list == null) {
               return;
            }

            for (AccountManager.Entry e : list) {
               if (e != null && isValidNick(e.name)) {
                  boolean dup = false;

                  for (int i = 0; i < ACCOUNT_LIST.size(); i++) {
                     if (ACCOUNT_LIST.get(i).name.equalsIgnoreCase(e.name)) {
                        dup = true;
                        break;
                     }
                  }

                  if (!dup) {
                     ACCOUNT_LIST.add(e);
                  }
               }
            }
         } catch (Exception var7) {
         }
      }
   }

   private static synchronized void save() {
      try {
         File f = file();
         File parent = f.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }

         String self = currentName().toLowerCase(Locale.ROOT);

         for (int i = 0; i < ACCOUNT_LIST.size(); i++) {
            AccountManager.Entry e = ACCOUNT_LIST.get(i);
            if (e.name.equalsIgnoreCase(self)) {
               e.lastUsed = Math.max(e.lastUsed, System.currentTimeMillis());
            }
         }

         Files.writeString(f.toPath(), GSON.toJson(ACCOUNT_LIST, LIST_TYPE), StandardCharsets.UTF_8);
      } catch (Exception var5) {
      }
   }

   public static final class Entry {
      public String name = "";
      public long lastUsed = 0L;

      public Entry() {
      }

      public Entry(String entryName, long entryLastUsed) {
         this.name = entryName;
         this.lastUsed = entryLastUsed;
      }
   }
}
