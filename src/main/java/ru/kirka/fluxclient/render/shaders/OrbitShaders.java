package ru.kirka.fluxclient.render.shaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class OrbitShaders {
   public static final String NEBULA = "block_nebula";
   public static final String STARFIELD = "block_starfield";
   public static final String COBWEB = "block_cobweb";
   public static final String PLASMA = "block_plasma";
   public static final String AURORA = "sky_aurora";
   public static final String GALAXY = "sky_galaxy";
   public static final String HAND_COSMIC = "hand_cosmic";
   public static final String HAND_RAINBOW = "hand_rainbow";
   public static final String HAND_PLASMA = "hand_plasma";
   public static final String HAND_HOLOGRAM = "hand_hologram";
   private static final Map<String, String> FRAG_FILES = Map.ofEntries(
      Map.entry("block_nebula", "block_nebula.fsh"),
      Map.entry("block_starfield", "block_starfield.fsh"),
      Map.entry("block_cobweb", "block_cobweb.fsh"),
      Map.entry("block_plasma", "block_plasma.fsh"),
      Map.entry("sky_aurora", "sky_aurora.fsh"),
      Map.entry("sky_galaxy", "sky_galaxy.fsh"),
      Map.entry("hand_cosmic", "hand_cosmic.fsh"),
      Map.entry("hand_rainbow", "hand_rainbow.fsh"),
      Map.entry("hand_plasma", "hand_plasma.fsh"),
      Map.entry("hand_hologram", "hand_hologram.fsh")
   );
   private static final Map<String, OrbitShader> CACHE = new HashMap<>();
   private static final Map<String, Boolean> FAILED = new HashMap<>();
   private static String vertexSource;
   private static String skyVertexSource;

   private OrbitShaders() {
   }

   public static Optional<OrbitShader> get(String name) {
      if (FAILED.getOrDefault(name, false)) {
         return Optional.empty();
      } else {
         OrbitShader cached = CACHE.get(name);
         if (cached != null) {
            return cached.isValid() ? Optional.of(cached) : Optional.empty();
         } else {
            String fragFile = FRAG_FILES.get(name);
            if (fragFile == null) {
               return Optional.empty();
            } else {
               try {
                  String vsh;
                  if (name.startsWith("sky_")) {
                     if (skyVertexSource == null) {
                        skyVertexSource = read("/assets/fluxclient/shaders/vertex/sky.vsh");
                     }

                     vsh = skyVertexSource;
                  } else {
                     if (vertexSource == null) {
                        vertexSource = read("/assets/fluxclient/shaders/vertex/passthrough.vsh");
                     }

                     vsh = vertexSource;
                  }

                  String fragSource = read("/assets/fluxclient/shaders/fragment/" + fragFile);
                  OrbitShader shader = new OrbitShader(vsh, fragSource);
                  if (!shader.isValid()) {
                     FAILED.put(name, true);
                     return Optional.empty();
                  } else {
                     CACHE.put(name, shader);
                     return Optional.of(shader);
                  }
               } catch (Exception var6) {
                  FluxLogger.error("Не удалось загрузить орбитальный шейдер " + name, var6);
                  FAILED.put(name, true);
                  return Optional.empty();
               }
            }
         }
      }
   }

   private static String read(String path) throws Exception {
      String var2;
      try (InputStream in = OrbitShaders.class.getResourceAsStream(path)) {
         if (in == null) {
            throw new IllegalStateException("Шейдер не найден: " + path);
         }

         var2 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }

      return var2;
   }
}
