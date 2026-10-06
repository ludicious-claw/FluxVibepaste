package ru.kirka.fluxclient.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.msdf.MsdfFont;
import ru.kirka.fluxclient.render.msdf.MsdfRenderer;

public class FadeOutBatching extends Batching {
   protected MsdfFont font;
   private float fadeoutStart;
   private float fadeoutEnd;
   private float maxWidth;
   private float x;

   public FadeOutBatching(VertexFormat vertexFormat, MsdfFont font, float fadeoutStart, float fadeoutEnd, float maxWidth, float x) {
      super(vertexFormat);
      this.font = font;
      this.fadeoutStart = fadeoutStart;
      this.fadeoutEnd = fadeoutEnd;
      this.maxWidth = maxWidth;
      this.x = x;
   }

   @Override
   public void draw() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, this.font.getTextureId());
      ShaderProgram shader = RenderSystem.setShader(MsdfRenderer.MSDF_FONT_SHADER_KEY);
      float thickness = 0.05F;
      float smoothness = 0.5F;
      if (shader != null) {
         setUniform(shader, "Range", this.font.getAtlas().range());
         setUniform(shader, "Thickness", thickness);
         setUniform(shader, "Smoothness", smoothness);
         setUniform(shader, "EnableFadeout", 1);
         setUniform(shader, "FadeoutStart", this.fadeoutStart);
         setUniform(shader, "FadeoutEnd", this.fadeoutEnd);
         setUniform(shader, "MaxWidth", this.maxWidth);
         setUniform(shader, "TextPosX", this.x);
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
