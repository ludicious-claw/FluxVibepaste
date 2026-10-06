package ru.kirka.fluxclient.render.shaders;

import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class MsdfShader {
   private static ShaderProgram program;
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         try {
            program = new ShaderProgram(
               Identifier.of("fluxclient", "shaders/vertex/passthrough.vsh"), Identifier.of("fluxclient", "shaders/fragment/msdf_font.fsh")
            );
            initialized = true;
            FluxLogger.info("MSDF-шейдер успешно скомпилирован!");
         } catch (Exception var1) {
            FluxLogger.error("Ошибка инициализации MSDF-шейдера", var1);
         }
      }
   }

   public static ShaderProgram getProgram() {
      if (!initialized) {
         init();
      }

      return program;
   }

   public static void bind(int textureSlot, int color, float pxRange) {
      ShaderProgram prog = getProgram();
      if (prog != null) {
         prog.bind();
         prog.setUniform1i("u_msdf_atlas", textureSlot);
         prog.setUniform1f("u_px_range", pxRange <= 0.0F ? 4.0F : pxRange);
         prog.setUniform4f("u_text_color", (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F);
      }
   }

   public static void unbind() {
      if (program != null) {
         program.unbind();
      }
   }
}
