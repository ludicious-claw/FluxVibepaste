package ru.kirka.fluxclient.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.msdf.MsdfFont;
import ru.kirka.fluxclient.render.msdf.MsdfRenderer;

public class FontBatching extends Batching {
   protected MsdfFont font;

   public FontBatching(VertexFormat vertexFormat, MsdfFont font) {
      super(vertexFormat);
      this.font = font;
   }

   @Override
   public void draw() {
      float thickness = 0.05F;
      float smoothness = 0.5F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, this.font.getTextureId());
      ShaderProgram shader = RenderSystem.setShader(MsdfRenderer.MSDF_FONT_SHADER_KEY);
      if (shader != null) {
         setUniform(shader, "Range", this.font.getAtlas().range());
         setUniform(shader, "Thickness", thickness);
         setUniform(shader, "Smoothness", smoothness);
         setUniform(shader, "EnableFadeout", 0);
      }

      this.build();
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      if (active == this) {
         active = null;
      }
   }

   private static void setUniform(ShaderProgram shader, String name, float value) {
      GlUniform u = shader.getUniform(name);
      if (u != null) {
         u.set(value);
      }
   }

   private static void setUniform(ShaderProgram shader, String name, int value) {
      GlUniform u = shader.getUniform(name);
      if (u != null) {
         u.set(value);
      }
   }
}
