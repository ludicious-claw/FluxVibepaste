package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.ui.shader.BlurShader;
import ru.kirka.fluxclient.ui.shader.GradientShader;
import ru.kirka.fluxclient.ui.shader.NoiseShader;
import ru.kirka.fluxclient.ui.shader.RectangleShader;
import ru.kirka.fluxclient.ui.shader.TextureShader;
import ru.kirka.fluxclient.util.MathUtil;

public class Draw2DProcessor implements Interface {
   private final RectangleShader rectangleShader = new RectangleShader();
   private final TextureShader textureShader = new TextureShader();
   private final GradientShader gradientShader = new GradientShader();
   private final BlurShader blurShader = new BlurShader();
   private final NoiseShader noiseShader = new NoiseShader();
   private float scale = 1.0F;

   public void setScale(float scale) {
      this.scale = scale;
   }

   public void a(float scale) {
      this.setScale(scale);
   }

   public float getScale() {
      return this.scale;
   }

   public float a() {
      return this.getScale();
   }

   public RectangleShader getRectangleShader() {
      return this.rectangleShader;
   }

   public RectangleShader b() {
      return this.rectangleShader;
   }

   public TextureShader getTextureShader() {
      return this.textureShader;
   }

   public TextureShader c() {
      return this.textureShader;
   }

   public GradientShader getGradientShader() {
      return this.gradientShader;
   }

   public GradientShader d() {
      return this.gradientShader;
   }

   public BlurShader getBlurShader() {
      return this.blurShader;
   }

   public BlurShader e() {
      return this.blurShader;
   }

   public NoiseShader getNoiseShader() {
      return this.noiseShader;
   }

   public NoiseShader f() {
      return this.noiseShader;
   }

   public void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      this.drawRoundedRect(matrices, x, y, width, height, this.toRadiusVector(radius), color);
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      this.drawRoundedRect(matrices, x, y, width, height, radius, color);
   }

   public void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, Vector4f radius, int color) {
      float[] padding = MathUtil.calculatePadding(0.8F);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float drawX = x - padding[0] / 2.0F;
      float drawY = y - padding[1] / 2.0F;
      float drawWidth = width + padding[0];
      float drawHeight = height + padding[1];
      this.enableRender();
      this.rectangleShader.a();
      this.rectangleShader.a(width, height);
      this.rectangleShader.a(radius);
      this.rectangleShader.a(0.8F);
      this.rectangleShader.b(0.0F);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.buildQuad(buffer, matrix, drawX, drawY, drawWidth, drawHeight, color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      this.disableRender();
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, Vector4f radius, int color) {
      this.drawRoundedRect(matrices, x, y, width, height, radius, color);
   }

   public void drawOutline(MatrixStack matrices, float x, float y, float width, float height, float radius, float outlineWidth, int color) {
      this.drawOutline(matrices, x, y, width, height, this.toRadiusVector(radius), outlineWidth, color);
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, float radius, float outlineWidth, int color) {
      this.drawOutline(matrices, x, y, width, height, radius, outlineWidth, color);
   }

   public void drawOutline(MatrixStack matrices, float x, float y, float width, float height, Vector4f radius, float outlineWidth, int color) {
      float[] padding = MathUtil.calculatePadding(0.8F);
      float halfOutlineWidth = outlineWidth * 0.5F;
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float drawX = x - halfOutlineWidth - padding[0] / 2.0F;
      float drawY = y - halfOutlineWidth - padding[1] / 2.0F;
      float drawWidth = width + outlineWidth + padding[0];
      float drawHeight = height + outlineWidth + padding[1];
      this.enableRender();
      this.rectangleShader.a();
      this.rectangleShader.a(width + outlineWidth, height + outlineWidth);
      this.rectangleShader.a(new Vector4f(radius.x + halfOutlineWidth, radius.y + halfOutlineWidth, radius.z + halfOutlineWidth, radius.w + halfOutlineWidth));
      this.rectangleShader.a(0.8F);
      this.rectangleShader.b(outlineWidth);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.buildQuad(buffer, matrix, drawX, drawY, drawWidth, drawHeight, color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      this.disableRender();
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, Vector4f radius, float outlineWidth, int color) {
      this.drawOutline(matrices, x, y, width, height, radius, outlineWidth, color);
   }

   public void drawTexture(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, float radius, int color) {
      int textureId = mc.getTextureManager().getTexture(texture).getGlId();
      this.a(matrices, x, y, width, height, radius, color, 0.0F, 0.0F, 1.0F, 1.0F, textureId);
   }

   public void a(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, float radius, int color) {
      this.drawTexture(matrices, texture, x, y, width, height, radius, color);
   }

   public void a(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      float u,
      float v,
      float textureWidth,
      float textureHeight,
      int textureId
   ) {
      this.a(matrices, x, y, width, height, this.toRadiusVector(radius), color, u, v, textureWidth, textureHeight, textureId);
   }

   public void a(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      Vector4f radius,
      int color,
      float u,
      float v,
      float textureWidth,
      float textureHeight,
      int textureId
   ) {
      float[] padding = MathUtil.calculatePadding(0.8F);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float drawX = x - padding[0] / 2.0F;
      float drawY = y - padding[1] / 2.0F;
      float drawWidth = width + padding[0];
      float drawHeight = height + padding[1];
      this.enableRender();
      RenderSystem.setShaderTexture(0, textureId);
      this.textureShader.a();
      this.textureShader.a(width, height);
      this.textureShader.a(radius);
      this.textureShader.a(0.8F);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      this.buildTexturedQuad(buffer, matrix, drawX, drawY, drawWidth, drawHeight, u, v, textureWidth, textureHeight, color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      this.disableRender();
   }

   public void drawPlayerHead(
      MatrixStack matrices, Identifier skin, LivingEntity target, float x, float y, float width, float height, float radius, float alpha
   ) {
      if (skin != null) {
         int color = ColorUtil.convertToARGB(255, 255, 255, (int)(alpha * 255.0F));
         int textureId = mc.getTextureManager().getTexture(skin).getGlId();
         this.a(matrices, x, y, width, height, radius, color, 0.125F, 0.125F, 0.125F, 0.125F, textureId);
         this.a(matrices, x, y, width, height, radius, color, 0.625F, 0.125F, 0.125F, 0.125F, textureId);
      }
   }

   public void a(MatrixStack matrices, Identifier skin, LivingEntity target, float x, float y, float width, float height, float radius, float alpha) {
      this.drawPlayerHead(matrices, skin, target, x, y, width, height, radius, alpha);
   }

   public void drawGradientRect(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int topLeftColor,
      int topRightColor,
      int bottomLeftColor,
      int bottomRightColor
   ) {
      this.a(matrices, x, y, width, height, this.toRadiusVector(radius), topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
   }

   public void a(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int topLeftColor,
      int topRightColor,
      int bottomLeftColor,
      int bottomRightColor
   ) {
      this.drawGradientRect(matrices, x, y, width, height, radius, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
   }

   public void a(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      Vector4f radius,
      int topLeftColor,
      int topRightColor,
      int bottomLeftColor,
      int bottomRightColor
   ) {
      float[] padding = MathUtil.calculatePadding(1.0F);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float[] normalizedTopLeft = ColorUtil.a(topLeftColor);
      float[] normalizedBottomLeft = ColorUtil.a(bottomLeftColor);
      float[] normalizedBottomRight = ColorUtil.a(bottomRightColor);
      float[] normalizedTopRight = ColorUtil.a(topRightColor);
      float drawX = x - padding[0] / 2.0F;
      float drawY = y - padding[1] / 2.0F;
      float drawWidth = width + padding[0];
      float drawHeight = height + padding[1];
      this.enableRender();
      this.gradientShader.a();
      this.gradientShader.a(width, height);
      this.gradientShader.a(radius);
      this.gradientShader.a(1.0F);
      this.gradientShader.a(normalizedTopLeft[0], normalizedTopLeft[1], normalizedTopLeft[2], normalizedTopLeft[3]);
      this.gradientShader.b(normalizedBottomLeft[0], normalizedBottomLeft[1], normalizedBottomLeft[2], normalizedBottomLeft[3]);
      this.gradientShader.d(normalizedBottomRight[0], normalizedBottomRight[1], normalizedBottomRight[2], normalizedBottomRight[3]);
      this.gradientShader.c(normalizedTopRight[0], normalizedTopRight[1], normalizedTopRight[2], normalizedTopRight[3]);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, drawX, drawY, 0.0F).color(topLeftColor);
      buffer.vertex(matrix, drawX, drawY + drawHeight, 0.0F).color(bottomLeftColor);
      buffer.vertex(matrix, drawX + drawWidth, drawY + drawHeight, 0.0F).color(bottomRightColor);
      buffer.vertex(matrix, drawX + drawWidth, drawY, 0.0F).color(topRightColor);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      this.disableRender();
   }

   public void b(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      this.a(matrices, x, y, width, height, radius, color, 0.8F);
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float mix) {
      this.a(matrices, x, y, width, height, this.toRadiusVector(radius), color, color, color, color, mix);
   }

   public float[] calculateScreenUV(Matrix4f matrix, float drawX, float drawY, float drawWidth, float drawHeight) {
      Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(matrix);
      Vector4f tl = new Vector4f(drawX, drawY, 0.0F, 1.0F).mul(mvp);
      Vector4f br = new Vector4f(drawX + drawWidth, drawY + drawHeight, 0.0F, 1.0F).mul(mvp);
      float w1 = Math.abs(tl.w) > 1.0E-4F ? tl.w : 1.0F;
      float w2 = Math.abs(br.w) > 1.0E-4F ? br.w : 1.0F;
      float uLeft = MathHelper.clamp(tl.x / w1 * 0.5F + 0.5F, 0.0F, 1.0F);
      float vTop = MathHelper.clamp(tl.y / w1 * 0.5F + 0.5F, 0.0F, 1.0F);
      float uRight = MathHelper.clamp(br.x / w2 * 0.5F + 0.5F, 0.0F, 1.0F);
      float vBottom = MathHelper.clamp(br.y / w2 * 0.5F + 0.5F, 0.0F, 1.0F);
      return new float[]{uLeft, vTop, uRight, vBottom};
   }

   public void a(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      Vector4f radius,
      int topLeftColor,
      int topRightColor,
      int bottomLeftColor,
      int bottomRightColor,
      float mix
   ) {
      if (!this.blurShader.e().isEmpty()) {
         Framebuffer framebuffer = (Framebuffer)this.blurShader.e().getFirst();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float drawX = x - 0.6F;
         float drawY = y - 0.6F;
         float drawWidth = width + 1.2F;
         float drawHeight = height + 1.2F;
         float[] uv = this.calculateScreenUV(matrix, drawX, drawY, drawWidth, drawHeight);
         float uLeft = uv[0];
         float vTop = uv[1];
         float uRight = uv[2];
         float vBottom = uv[3];
         float[] normalizedTopLeft = ColorUtil.a(topLeftColor);
         float[] normalizedBottomLeft = ColorUtil.a(bottomLeftColor);
         float[] normalizedBottomRight = ColorUtil.a(bottomRightColor);
         float[] normalizedTopRight = ColorUtil.a(topRightColor);
         this.enableRender();
         RenderSystem.setShaderTexture(0, framebuffer.getColorAttachment());
         this.blurShader.a();
         this.blurShader.a(width, height);
         this.blurShader.a(radius);
         this.blurShader.a(0.8F);
         this.blurShader.b(mix);
         this.blurShader.c((normalizedTopLeft[3] + normalizedBottomLeft[3] + normalizedBottomRight[3] + normalizedTopRight[3]) * 0.25F);
         this.blurShader.a(normalizedTopLeft[0], normalizedTopLeft[1], normalizedTopLeft[2], normalizedTopLeft[3]);
         this.blurShader.b(normalizedBottomLeft[0], normalizedBottomLeft[1], normalizedBottomLeft[2], normalizedBottomLeft[3]);
         this.blurShader.d(normalizedBottomRight[0], normalizedBottomRight[1], normalizedBottomRight[2], normalizedBottomRight[3]);
         this.blurShader.c(normalizedTopRight[0], normalizedTopRight[1], normalizedTopRight[2], normalizedTopRight[3]);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         buffer.vertex(matrix, drawX, drawY, 0.0F).texture(uLeft, vTop).color(topLeftColor);
         buffer.vertex(matrix, drawX, drawY + drawHeight, 0.0F).texture(uLeft, vBottom).color(bottomLeftColor);
         buffer.vertex(matrix, drawX + drawWidth, drawY + drawHeight, 0.0F).texture(uRight, vBottom).color(bottomRightColor);
         buffer.vertex(matrix, drawX + drawWidth, drawY, 0.0F).texture(uRight, vTop).color(topRightColor);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         this.disableRender();
      }
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float alpha, int glowColor, float glowRadius) {
      this.a(matrices, x, y, width, height, this.toRadiusVector(radius), color, alpha, glowColor, glowRadius);
   }

   public void a(MatrixStack matrices, float x, float y, float width, float height, Vector4f radius, int color, float alpha, int glowColor, float glowRadius) {
      if (!this.blurShader.e().isEmpty()) {
         float clampedGlowRadius = Math.max(glowRadius, 0.0F);
         float padding = 1.2F;
         Framebuffer framebuffer = (Framebuffer)this.blurShader.e().getFirst();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float drawX = x - padding * 0.5F;
         float drawY = y - padding * 0.5F;
         float drawWidth = width + padding;
         float drawHeight = height + padding;
         float[] uv = this.calculateScreenUV(matrix, drawX, drawY, drawWidth, drawHeight);
         float uLeft = uv[0];
         float vTop = uv[1];
         float uRight = uv[2];
         float vBottom = uv[3];
         float[] normalizedGlowColor = ColorUtil.a(glowColor);
         this.drawBlurredQuad(
            matrix, drawX, drawY, drawWidth, drawHeight, width, height, radius, alpha, 0.8F, 0.0F, null, 0.0F, null, uLeft, vTop, uRight, vBottom, framebuffer
         );
         float innerWidth = width - -2.2F;
         float innerHeight = height - -2.2F;
         Vector4f innerRadius = new Vector4f(
            Math.max(0.0F, radius.x - -1.1F), Math.max(0.0F, radius.y - -1.1F), Math.max(0.0F, radius.z - -1.1F), Math.max(0.0F, radius.w - -1.1F)
         );
         float glowX = x - 1.1F - clampedGlowRadius;
         float glowY = y - 1.1F - clampedGlowRadius;
         float glowW = innerWidth + clampedGlowRadius * 2.0F;
         float glowH = innerHeight + clampedGlowRadius * 2.0F;
         float[] glowUv = this.calculateScreenUV(matrix, glowX, glowY, glowW, glowH);
         this.drawBlurredQuad(
            matrix,
            glowX,
            glowY,
            glowW,
            glowH,
            innerWidth,
            innerHeight,
            innerRadius,
            alpha,
            0.8F,
            clampedGlowRadius,
            normalizedGlowColor,
            0.0F,
            null,
            glowUv[0],
            glowUv[1],
            glowUv[2],
            glowUv[3],
            framebuffer
         );
         this.drawRoundedRect(matrices, x, y, width, height, radius, color);
      }
   }

   public void drawThemedBlurredPanel(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int themeColor,
      float themeMix,
      int glowColor,
      float glowRadius,
      int glassOverlayColor
   ) {
      if (!this.blurShader.e().isEmpty()) {
         Framebuffer framebuffer = (Framebuffer)this.blurShader.e().getFirst();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         Vector4f radVec = this.toRadiusVector(radius);
         float padding = 1.2F;
         float drawX = x - padding * 0.5F;
         float drawY = y - padding * 0.5F;
         float drawWidth = width + padding;
         float drawHeight = height + padding;
         float[] uv = this.calculateScreenUV(matrix, drawX, drawY, drawWidth, drawHeight);
         float uLeft = uv[0];
         float vTop = uv[1];
         float uRight = uv[2];
         float vBottom = uv[3];
         if (glowRadius > 0.001F && glowColor != 0) {
            float clampedGlowRadius = Math.max(glowRadius, 0.0F);
            float innerWidth = width - -2.2F;
            float innerHeight = height - -2.2F;
            Vector4f innerRadius = new Vector4f(
               Math.max(0.0F, radVec.x - -1.1F), Math.max(0.0F, radVec.y - -1.1F), Math.max(0.0F, radVec.z - -1.1F), Math.max(0.0F, radVec.w - -1.1F)
            );
            float[] normalizedGlowColor = ColorUtil.a(glowColor);
            float glowX = x - 1.1F - clampedGlowRadius;
            float glowY = y - 1.1F - clampedGlowRadius;
            float glowW = innerWidth + clampedGlowRadius * 2.0F;
            float glowH = innerHeight + clampedGlowRadius * 2.0F;
            float[] glowUv = this.calculateScreenUV(matrix, glowX, glowY, glowW, glowH);
            this.drawBlurredQuad(
               matrix,
               glowX,
               glowY,
               glowW,
               glowH,
               innerWidth,
               innerHeight,
               innerRadius,
               1.0F,
               0.8F,
               clampedGlowRadius,
               normalizedGlowColor,
               0.0F,
               null,
               glowUv[0],
               glowUv[1],
               glowUv[2],
               glowUv[3],
               framebuffer
            );
         }

         float[] normalizedThemeColor = ColorUtil.a(themeColor);
         this.drawBlurredQuad(
            matrix,
            drawX,
            drawY,
            drawWidth,
            drawHeight,
            width,
            height,
            radVec,
            1.0F,
            0.8F,
            0.0F,
            null,
            themeMix,
            normalizedThemeColor,
            uLeft,
            vTop,
            uRight,
            vBottom,
            framebuffer
         );
         if (glassOverlayColor != 0) {
            this.drawRoundedRect(matrices, x, y, width, height, radVec, glassOverlayColor);
         }
      }
   }

   public void b(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float alpha) {
      if (!this.blurShader.e().isEmpty()) {
         float padding = 1.2F;
         Framebuffer framebuffer = (Framebuffer)this.blurShader.e().getFirst();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float drawX = x - padding * 0.5F;
         float drawY = y - padding * 0.5F;
         float drawWidth = width + padding;
         float drawHeight = height + padding;
         float[] uv = this.calculateScreenUV(matrix, drawX, drawY, drawWidth, drawHeight);
         this.drawBlurredQuad(
            matrix,
            drawX,
            drawY,
            drawWidth,
            drawHeight,
            width,
            height,
            this.toRadiusVector(radius),
            alpha,
            0.8F,
            0.0F,
            null,
            0.0F,
            null,
            uv[0],
            uv[1],
            uv[2],
            uv[3],
            framebuffer
         );
         this.drawRoundedRect(matrices, x, y, width, height, radius, color);
      }
   }

   private void drawBlurredQuad(
      Matrix4f matrix,
      float drawX,
      float drawY,
      float drawWidth,
      float drawHeight,
      float width,
      float height,
      Vector4f radius,
      float alpha,
      float smoothness,
      float glowRadius,
      float[] glowColor,
      float mix,
      float[] tintColor,
      float uLeft,
      float vTop,
      float uRight,
      float vBottom,
      Framebuffer framebuffer
   ) {
      this.enableRender();
      RenderSystem.setShaderTexture(0, framebuffer.getColorAttachment());
      this.blurShader.a();
      this.blurShader.a(width, height);
      this.blurShader.a(radius);
      this.blurShader.c(alpha);
      this.blurShader.d(glowRadius);
      if (glowColor != null) {
         this.blurShader.e(glowColor[0], glowColor[1], glowColor[2], glowColor[3]);
      }

      this.blurShader.a(smoothness);
      this.blurShader.b(mix);
      if (tintColor != null) {
         this.blurShader.a(tintColor[0], tintColor[1], tintColor[2], tintColor[3]);
         this.blurShader.b(tintColor[0], tintColor[1], tintColor[2], tintColor[3]);
         this.blurShader.d(tintColor[0], tintColor[1], tintColor[2], tintColor[3]);
         this.blurShader.c(tintColor[0], tintColor[1], tintColor[2], tintColor[3]);
      } else {
         this.blurShader.a(1.0F, 1.0F, 1.0F, 1.0F);
         this.blurShader.b(1.0F, 1.0F, 1.0F, 1.0F);
         this.blurShader.d(1.0F, 1.0F, 1.0F, 1.0F);
         this.blurShader.c(1.0F, 1.0F, 1.0F, 1.0F);
      }

      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix, drawX, drawY, 0.0F).texture(uLeft, vTop).color(-1);
      buffer.vertex(matrix, drawX, drawY + drawHeight, 0.0F).texture(uLeft, vBottom).color(-1);
      buffer.vertex(matrix, drawX + drawWidth, drawY + drawHeight, 0.0F).texture(uRight, vBottom).color(-1);
      buffer.vertex(matrix, drawX + drawWidth, drawY, 0.0F).texture(uRight, vTop).color(-1);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      this.disableRender();
   }

   public void a(DrawContext context, float x, float y, float width, float height, int color) {
      context.getMatrices().push();
      context.getMatrices().translate(x, y, 0.0F);
      context.getMatrices().scale(width, height, 1.0F);
      context.fill(0, 0, 1, 1, color);
      context.getMatrices().pop();
   }

   private void enableRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
   }

   private void disableRender() {
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private Vector4f toRadiusVector(float radius) {
      return new Vector4f(radius, radius, radius, radius);
   }

   private void buildQuad(BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix, x, y, 0.0F).color(color);
      buffer.vertex(matrix, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y, 0.0F).color(color);
   }

   private void buildTexturedQuad(
      BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, float u, float v, float textureWidth, float textureHeight, int color
   ) {
      buffer.vertex(matrix, x, y, 0.0F).texture(u, v).color(color);
      buffer.vertex(matrix, x, y + height, 0.0F).texture(u, v + textureHeight).color(color);
      buffer.vertex(matrix, x + width, y + height, 0.0F).texture(u + textureWidth, v + textureHeight).color(color);
      buffer.vertex(matrix, x + width, y, 0.0F).texture(u + textureWidth, v).color(color);
   }
}
