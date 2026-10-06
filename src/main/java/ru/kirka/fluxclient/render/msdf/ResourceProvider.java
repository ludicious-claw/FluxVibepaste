package ru.kirka.fluxclient.render.msdf;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
   private static final Gson GSON = new Gson();

   public static Identifier getShaderIdentifier(String name) {
      return Identifier.of("rockstar", "core/" + name);
   }

   public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
      return (T)GSON.fromJson(toString(identifier), clazz);
   }

   public static String toString(Identifier identifier) {
      return toString(identifier, "\n");
   }

   public static String toString(Identifier identifier, String delimiter) {
      ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

      try {
         String var5;
         try (
            InputStream inputStream = resourceManager.open(identifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
         ) {
            var5 = reader.lines().collect(Collectors.joining(delimiter));
         }

         return var5;
      } catch (IOException var11) {
         throw new RuntimeException("Failed to read resource: " + identifier, var11);
      }
   }

   private ResourceProvider() {
      throw new UnsupportedOperationException();
   }
}
