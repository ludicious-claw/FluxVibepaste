package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.kirka.fluxclient.ui.shader.GradientUtil;

public class Font {
   private final ShaderProgramKey shaderKey = new ShaderProgramKey(
      Identifier.of("fluxclient", "core/text/text"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );
   private final String fontName;
   private final AbstractTexture fontTexture;
   private final FontData.AtlasData atlasData;
   private final FontData.MetricsData metricsData;
   private final Map<Integer, MsdfGlyph> glyphs;
   private final Map<Integer, Map<Integer, Float>> kernings;

   public Font(
      String name,
      AbstractTexture texture,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      Map<Integer, MsdfGlyph> glyphs,
      Map<Integer, Map<Integer, Float>> kernings
   ) {
      this.fontName = name;
      this.fontTexture = texture;
      this.atlasData = atlas;
      this.metricsData = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
   }

   public static FontBuilder a() {
      return new FontBuilder();
   }

   public String b() {
      return this.fontName;
   }

   public FontData.AtlasData c() {
      return this.atlasData;
   }

   public FontData.MetricsData d() {
      return this.metricsData;
   }

   public FontData.MetricsData getFontData() {
      return this.metricsData;
   }

   private void a(float outlineThickness, float thickness, float smoothness, int outlineColor) {
      this.a(outlineThickness, thickness, smoothness, outlineColor, -1.0F, -1.0F);
   }

   private void a(float outlineThickness, float thickness, float smoothness, int outlineColor, float fadeStart, float fadeEnd) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, this.fontTexture.getGlId());
      ShaderProgram shader = RenderSystem.setShader(this.shaderKey);
      if (shader != null) {
         if (shader.getUniform("uRange") != null) {
            shader.getUniform("uRange").set(this.atlasData.range());
         }

         if (shader.getUniform("uThickness") != null) {
            shader.getUniform("uThickness").set(thickness);
         }

         if (shader.getUniform("uSmoothness") != null) {
            shader.getUniform("uSmoothness").set(smoothness);
         }

         boolean outlineEnabled = outlineThickness > 0.0F;
         if (shader.getUniform("uOutline") != null) {
            shader.getUniform("uOutline").set(outlineEnabled ? 1 : 0);
         }

         if (outlineEnabled && shader.getUniform("uOutlineThickness") != null) {
            shader.getUniform("uOutlineThickness").set(outlineThickness);
            float[] outlineComponents = ColorUtil.a(outlineColor);
            shader.getUniform("uOutlineColor").set(outlineComponents[0], outlineComponents[1], outlineComponents[2], outlineComponents[3]);
         }

         boolean fadeEnabled = fadeEnd > fadeStart;
         if (shader.getUniform("uFadeEnabled") != null) {
            shader.getUniform("uFadeEnabled").set(fadeEnabled ? 1 : 0);
         }

         if (fadeEnabled && shader.getUniform("uFadeStart") != null) {
            shader.getUniform("uFadeStart").set(fadeStart);
            shader.getUniform("uFadeEnd").set(fadeEnd);
         }
      }
   }

   private void a(BufferBuilder builder) {
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public void a(
      MatrixStack matrixStack,
      Text text,
      float x,
      float y,
      float size,
      float alpha,
      float thickness,
      float smoothness,
      float spacing,
      int outlineColor,
      float outlineThickness
   ) {
      try {
         Matrix4f matrix = matrixStack.peek().getPositionMatrix();
         this.a(outlineThickness, thickness, smoothness, outlineColor);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float adjustedThickness = (thickness + outlineThickness * 0.5F) * 0.5F * size;
         float baselineY = y + this.metricsData.baselineHeight() * size;
         boolean hasGlyphs = this.a(matrix, builder, this.a(text), size, alpha, adjustedThickness, spacing, x, baselineY, 0.0F);
         if (hasGlyphs) {
            this.a(builder);
         }
      } catch (Exception var17) {
         var17.printStackTrace();
      }
   }

   public void a(
      MatrixStack matrixStack,
      String text,
      float x,
      float y,
      float size,
      float thickness,
      int color,
      int colorSecond,
      float offset,
      float smoothness,
      float spacing,
      int outlineColor,
      float outlineThickness
   ) {
      if (text != null && !text.isEmpty()) {
         Matrix4f matrix = matrixStack.peek().getPositionMatrix();
         this.a(outlineThickness, thickness, smoothness, outlineColor);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float adjustedThickness = (thickness + outlineThickness * 0.5F) * 0.5F * size;
         float baselineY = y + this.metricsData.baselineHeight() * size;
         boolean hasGlyphs = this.a(matrix, builder, text, size, adjustedThickness, spacing, x, baselineY, 0.0F, color, colorSecond, offset);
         if (hasGlyphs) {
            this.a(builder);
         }
      }
   }

   public List<MsdfGlyph.a> a(Text text) {
      List<MsdfGlyph.a> result = new ArrayList<>();
      boolean[] started = new boolean[]{false};
      text.visit((style, string) -> {
         if (string != null && !string.isEmpty()) {
            if (!started[0]) {
               string = string.replaceFirst("^\\s+", "");
               if (string.isEmpty()) {
                  return Optional.empty();
               }

               started[0] = true;
            }

            int color = style.getColor() != null ? style.getColor().getRgb() | 0xFF000000 : -1;
            result.addAll(this.a(string, color));
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.EMPTY);
      return result;
   }

   private List<MsdfGlyph.a> a(String raw, int color) {
      String raw2 = raw.replace('⚡', 'ŝ').replace('★', 'Ş');
      List<MsdfGlyph.a> result = new ArrayList<>();

      for (int i = 0; i < raw2.length(); i++) {
         char c = raw2.charAt(i);
         if (i + 1 < raw2.length()) {
            char n = raw2.charAt(i + 1);
            if (c != 3618 && c != 9889 && (c != 167 || "0123456789abcdefklor".indexOf(n) < 0)) {
               if (this.glyphs.containsKey(Integer.valueOf(c))) {
                  result.add(new MsdfGlyph.a(c, color));
               }
            } else {
               i++;
            }
         } else if (this.glyphs.containsKey(Integer.valueOf(c))) {
            result.add(new MsdfGlyph.a(c, color));
         }
      }

      return result;
   }

   public void a(MatrixStack matrixStack, Text text, float x, float y, float size) {
      this.a(matrixStack, text, x, y, size, 0.0F, 1.0F);
   }

   public void a(MatrixStack matrixStack, Text text, float x, float y, float size, double alpha) {
      this.a(matrixStack, text, x, y, size, 0.0F, (float)alpha);
   }

   public void a(MatrixStack matrixStack, Text text, float x, float y, float size, float thickness, float alpha) {
      this.a(matrixStack, text, x, y, size, alpha, thickness, 0.5F, 0.0F, -1, thickness);
   }

   public void a(MatrixStack matrixStack, String text, float x, float y, float size, int color) {
      this.a(matrixStack, text, x, y, size, color, 0.0F);
   }

   public void a(MatrixStack matrixStack, String text, float x, float y, float size, int color, float thickness) {
      this.a(matrixStack, text, x, y, size, thickness, color, -1, -1.0F, 0.5F, 0.0F, -1, thickness);
   }

   public void a(MatrixStack matrixStack, String text, float x, float y, float size, int color, float speed, float offset) {
      this.a(matrixStack, GradientUtil.a(text, color, speed, offset), x, y, size);
   }

   public void b(MatrixStack matrixStack, String text, float x, float y, float size, int color) {
      this.b(matrixStack, text, x, y, size, color, 0.0F);
   }

   public void b(MatrixStack matrixStack, String text, float x, float y, float size, int color, float thickness) {
      float textWidth = this.b(text, size, thickness);
      this.a(matrixStack, text, x - textWidth / 2.0F, y, size, color, thickness);
   }

   public void b(MatrixStack matrixStack, String text, float x, float y, float size, int color, float speed, float offset) {
      this.a(matrixStack, GradientUtil.a(text, color, speed, offset), x - this.a(text, size) / 2.0F, y, size);
   }

   public void c(MatrixStack matrixStack, String text, float x, float y, float size, int color, float visibleWidth) {
      this.c(matrixStack, text, x, y, size, color, 0.0F, visibleWidth);
   }

   public void c(MatrixStack matrixStack, String text, float x, float y, float size, int color, float thickness, float visibleWidth) {
      if (text != null && !text.isEmpty() && !(visibleWidth <= 0.0F)) {
         float fadeStart = x + Math.max(0.0F, visibleWidth - 5.0F);
         float fadeEnd = x + visibleWidth;
         Matrix4f matrix = matrixStack.peek().getPositionMatrix();
         this.a(0.0F, thickness, 0.5F, -1, fadeStart, fadeEnd);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float adjustedThickness = (thickness + thickness * 0.5F) * 0.5F * size;
         float baselineY = y + this.metricsData.baselineHeight() * size;
         boolean hasGlyphs = this.a(matrix, builder, text, size, adjustedThickness, 0.0F, x, baselineY, 0.0F, color, -1, -1.0F);
         if (hasGlyphs) {
            this.a(builder);
         }
      }
   }

   public boolean a(
      Matrix4f matrix,
      VertexConsumer consumer,
      String text,
      float size,
      float thickness,
      float spacing,
      float x,
      float y,
      float z,
      int color,
      int colorSecond,
      float offset
   ) {
      String text2 = text.replace('⚡', 'ŝ').replace('★', 'Ş');
      int previousChar = -1;
      float totalWidth = this.a(text2, size);
      float time = (float)System.currentTimeMillis() % 3000.0F / 3000.0F;
      boolean hasGlyphs = false;

      for (int i = 0; i < text2.length(); i++) {
         int codePoint = text2.charAt(i);
         MsdfGlyph glyph = this.glyphs.get(codePoint);
         if (glyph != null) {
            hasGlyphs = true;
            float x2 = x + this.a(previousChar, codePoint, size);
            int currentColor = color;
            if (offset > 1.0F) {
               currentColor = ColorUtil.makeGradient(color, colorSecond, x2 - x, totalWidth, time, offset);
            }

            x = x2 + glyph.a(matrix, consumer, size, x2, y, z, currentColor) + thickness + spacing;
            previousChar = codePoint;
         }
      }

      return hasGlyphs;
   }

   public boolean a(
      Matrix4f matrix,
      VertexConsumer consumer,
      List<MsdfGlyph.a> coloredGlyphs,
      float size,
      float alpha,
      float thickness,
      float spacing,
      float x,
      float y,
      float z
   ) {
      int previousChar = -1;
      boolean started = false;
      boolean hasGlyphs = false;

      for (int i = 0; i < coloredGlyphs.size(); i++) {
         MsdfGlyph.a glyphData = coloredGlyphs.get(i);
         int codePoint = glyphData.a();
         if (started || codePoint != 32) {
            started = true;
            int color = glyphData.b();
            MsdfGlyph glyph = this.glyphs.get(codePoint);
            if (glyph != null) {
               hasGlyphs = true;
               float x2 = x + this.a(previousChar, codePoint, size);
               float advance = glyph.a(matrix, consumer, size, x2, y, z, ColorUtil.applyAlphaToColor(color, alpha));
               if (i < coloredGlyphs.size() - 1) {
                  advance += thickness + spacing;
               }

               x = x2 + advance;
               previousChar = codePoint;
            }
         }
      }

      return hasGlyphs;
   }

   private float a(int previousChar, int currentChar, float size) {
      Map<Integer, Float> kerning = this.kernings.get(previousChar);
      return kerning == null ? 0.0F : kerning.getOrDefault(currentChar, 0.0F) * size;
   }

   public float a(float size) {
      return size;
   }

   public float a(Text text, float size) {
      return this.a(text, size, 0.0F);
   }

   public float a(Text text, float size, float thickness) {
      if (text == null) {
         return 0.0F;
      } else {
         List<MsdfGlyph.a> coloredGlyphs = this.a(text);
         return this.a(coloredGlyphs, size, thickness);
      }
   }

   public float a(String text, float size) {
      return this.b(text, size, 0.0F);
   }

   public float b(String text, float size) {
      MsdfGlyph glyph;
      return text != null && !text.isEmpty() && (glyph = this.glyphs.get(Integer.valueOf(text.charAt(0)))) != null ? glyph.b(size) : 0.0F;
   }

   public float a(String text, float size, float centerY) {
      if (text != null && !text.isEmpty()) {
         MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(text.charAt(0)));
         if (glyph == null) {
            return centerY - this.a(size) / 2.0F;
         } else {
            float inkCenter = (this.metricsData.baselineHeight() - glyph.a() + glyph.b() / 2.0F) * size;
            return centerY - inkCenter;
         }
      } else {
         return centerY - this.a(size) / 2.0F;
      }
   }

   public float b(String text, float size, float thickness) {
      if (text != null && !text.isEmpty()) {
         String text2 = text.replace('⚡', 'ŝ').replace('★', 'Ş');
         int previousChar = -1;
         float width = 0.0F;
         int renderedGlyphs = 0;

         for (int i = 0; i < text2.length(); i++) {
            int codePoint = text2.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(codePoint);
            if (glyph != null) {
               width = width + this.a(previousChar, codePoint, size) + glyph.a(size);
               renderedGlyphs++;
               previousChar = codePoint;
            }
         }

         return renderedGlyphs > 0 ? width + renderedGlyphs * thickness : width;
      } else {
         return 0.0F;
      }
   }

   private float a(List<MsdfGlyph.a> coloredGlyphs, float size, float thickness) {
      int previousChar = -1;
      float width = 0.0F;
      int renderedGlyphs = 0;

      for (MsdfGlyph.a coloredGlyph : coloredGlyphs) {
         int codePoint = coloredGlyph.a();
         MsdfGlyph glyph = this.glyphs.get(codePoint);
         if (glyph != null) {
            width = width + this.a(previousChar, codePoint, size) + glyph.a(size);
            renderedGlyphs++;
            previousChar = codePoint;
         }
      }

      return renderedGlyphs > 1 ? width + (renderedGlyphs - 1) * thickness : width;
   }

   public float a(MatrixStack matrixStack, String text, float x, float y, float size, int color, float maxWidth, boolean isHovered, float offset, float delta) {
      if (text != null && !text.isEmpty() && !(maxWidth <= 0.0F)) {
         float textWidth = this.a(text, size);
         float wrap = textWidth + 12.0F;
         if (isHovered || offset > 0.0F) {
            offset += delta * 1.5F;
            if (offset >= wrap) {
               offset = isHovered ? offset - wrap : 0.0F;
            }
         }

         if (!(textWidth <= maxWidth) && offset != 0.0F) {
            ScissorUtil.a(matrixStack, x - 1.0F, y - size * 0.5F, maxWidth + 2.0F, size * 1.5F + 0.5F);
            this.a(0.0F, 0.0F, 0.5F, -1, x + maxWidth - 5.0F, x + maxWidth);
            BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float baselineY = y + this.metricsData.baselineHeight() * size;
            this.a(matrixStack.peek().getPositionMatrix(), builder, text, size, 0.0F, 0.0F, x - offset, baselineY, 0.0F, color, -1, -1.0F);
            this.a(matrixStack.peek().getPositionMatrix(), builder, text, size, 0.0F, 0.0F, x - offset + wrap, baselineY, 0.0F, color, -1, -1.0F);
            this.a(builder);
            ScissorUtil.a(matrixStack);
            return offset;
         } else {
            this.c(matrixStack, text, x, y, size, color, 0.0F, maxWidth);
            return offset;
         }
      } else {
         return 0.0F;
      }
   }
}
