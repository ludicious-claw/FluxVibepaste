package ru.kirka.fluxclient.render.shaders;

import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class SdfShader {
   private static ShaderProgram program;
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         try {
            program = new ShaderProgram(
               Identifier.of("fluxclient", "shaders/vertex/passthrough.vsh"), Identifier.of("fluxclient", "shaders/fragment/rect_sdf.fsh")
            );
            initialized = true;
            FluxLogger.info("SDF-шейдер успешно скомпилирован!");
         } catch (Exception var1) {
            FluxLogger.error("Ошибка инициализации SDF-шейдера", var1);
         }
      }
   }

   public static ShaderProgram getProgram() {
      if (!initialized) {
         init();
      }

      return program;
   }

   public static void bind(float width, float height, float radius, int color) {
      bind(width, height, radius, radius, radius, radius, color, color, 0.0F, 0, 0.0F, 0);
   }

   public static void bind(
      float width,
      float height,
      float rTL,
      float rTR,
      float rBR,
      float rBL,
      int color1,
      int color2,
      float borderWidth,
      int borderColor,
      float shadowSoftness,
      int shadowColor
   ) {
      ShaderProgram prog = getProgram();
      if (prog != null) {
         prog.bind();
         prog.setUniform2f("u_size", width, height);
         prog.setUniform4f("u_radius", rTL, rTR, rBR, rBL);
         prog.setUniform4f("u_color1", (color1 >> 16 & 0xFF) / 255.0F, (color1 >> 8 & 0xFF) / 255.0F, (color1 & 0xFF) / 255.0F, (color1 >> 24 & 0xFF) / 255.0F);
         prog.setUniform4f("u_color2", (color2 >> 16 & 0xFF) / 255.0F, (color2 >> 8 & 0xFF) / 255.0F, (color2 & 0xFF) / 255.0F, (color2 >> 24 & 0xFF) / 255.0F);
         prog.setUniform1f("u_border_width", borderWidth);
         prog.setUniform4f(
            "u_border_color",
            (borderColor >> 16 & 0xFF) / 255.0F,
            (borderColor >> 8 & 0xFF) / 255.0F,
            (borderColor & 0xFF) / 255.0F,
            (borderColor >> 24 & 0xFF) / 255.0F
         );
         prog.setUniform1f("u_shadow_softness", shadowSoftness);
         prog.setUniform4f(
            "u_shadow_color",
            (shadowColor >> 16 & 0xFF) / 255.0F,
            (shadowColor >> 8 & 0xFF) / 255.0F,
            (shadowColor & 0xFF) / 255.0F,
            (shadowColor >> 24 & 0xFF) / 255.0F
         );
      }
   }

   public static void unbind() {
      if (program != null) {
         program.unbind();
      }
   }
}
