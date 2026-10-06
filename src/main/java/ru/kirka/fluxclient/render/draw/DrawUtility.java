package ru.kirka.fluxclient.render.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.batching.impl.IconBatching;
import ru.kirka.fluxclient.render.batching.impl.RectBatching;
import ru.kirka.fluxclient.render.batching.impl.RoundedRectBatching;
import ru.kirka.fluxclient.render.batching.impl.SquircleBatching;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.geometry.gradient.Gradient;
import ru.kirka.fluxclient.render.obj.CustomSprite;
import ru.kirka.fluxclient.render.penis.PenisSprite;
import ru.kirka.fluxclient.render.shader.BlurProgram;
import ru.kirka.fluxclient.render.shader.FogBlurPost;
import ru.kirka.fluxclient.render.shader.GlProgram;
import ru.kirka.fluxclient.render.shader.SkyShaderProgram;

public final class DrawUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final HookLimiter limiter = new HookLimiter(true);
   public static GlProgram rectangleProgram;
   private static GlProgram squircleProgram;
   private static GlProgram roundedTextureProgram;
   private static GlProgram squircleTextureProgram;
   private static GlProgram borderProgram;
   private static GlProgram loadingProgram;
   private static GlProgram glassProgram;
   private static GlProgram gradientRectangleProgram;
   public static SkyShaderProgram skyNebulaProgram;
   public static SkyShaderProgram skyCausticProgram;
   public static BlurProgram blurProgram;
   public static FogBlurPost fogBlurPost;
   private static final CustomRenderTarget buffer = new CustomRenderTarget(false);

   public static void initializeShaders() {
      rectangleProgram = new GlProgram(Identifier.of("rockstar", "rectangle/data"), VertexFormats.POSITION_COLOR);
      squircleProgram = new GlProgram(Identifier.of("rockstar", "squircle/data"), VertexFormats.POSITION_COLOR);
      squircleTextureProgram = new GlProgram(Identifier.of("rockstar", "squircle_texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      roundedTextureProgram = new GlProgram(Identifier.of("rockstar", "texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      borderProgram = new GlProgram(Identifier.of("rockstar", "border/data"), VertexFormats.POSITION_COLOR);
      loadingProgram = new GlProgram(Identifier.of("rockstar", "loading/data"), VertexFormats.POSITION_COLOR);
      glassProgram = new GlProgram(Identifier.of("rockstar", "liquidglass/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      gradientRectangleProgram = new GlProgram(Identifier.of("rockstar", "gradient_rectangle/data"), VertexFormats.POSITION_COLOR);
      skyNebulaProgram = new SkyShaderProgram(Identifier.of("rockstar", "sky_nebula/data"));
      skyCausticProgram = new SkyShaderProgram(Identifier.of("rockstar", "sky_caustic/data"));
      blurProgram = new BlurProgram();
      blurProgram.initShaders();
      fogBlurPost = new FogBlurPost();
      fogBlurPost.initShaders();
   }

   public static void updateBuffer() {
      buffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.setup();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      if (mc.getFramebuffer() != null) {
         mc.getFramebuffer().beginRead();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
         drawQuad(0.0F, 0.0F, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
         mc.getFramebuffer().endRead();
         RenderSystem.disableBlend();
         mc.getFramebuffer().beginWrite(true);
      }

      buffer.stop();
   }

   private static void drawQuad(float x, float y, float width, float height, boolean flip) {
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      int color = -1;
      float vTop = flip ? 0.0F : 1.0F;
      float vBottom = flip ? 1.0F : 0.0F;
      builder.vertex(x, y, 0.0F).texture(0.0F, vBottom).color(-1);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, vTop).color(-1);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, vTop).color(-1);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, vBottom).color(-1);
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }
   }

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, from.x, from.y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, to.x, to.y, 0.0F).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= resolution; i++) {
            float t = (float)i / resolution;
            float x = (float)MathUtility.cubicBezier(t, p0.x, p1.x, p2.x, p3.x);
            float y = (float)MathUtility.cubicBezier(t, p0.y, p1.y, p2.y, p3.y);
            builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         }

         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      if (Batching.getActive() instanceof RectBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      } else {
         matrices.push();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         drawSetup();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         matrices.pop();
      }
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof SquircleBatching sb) {
         sb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius() * squirt / 2.0F,
            borderRadius.bottomLeftRadius() * squirt / 2.0F,
            borderRadius.topRightRadius() * squirt / 2.0F,
            borderRadius.bottomRightRadius() * squirt / 2.0F,
            color.getRGB()
         );
         matrices.pop();
      } else {
         if (squircleProgram != null) {
            squircleProgram.use();
            if (squircleProgram.findUniform("Size") != null) {
               squircleProgram.findUniform("Size").set(width, height);
            }

            if (squircleProgram.findUniform("Radius") != null) {
               squircleProgram.findUniform("Radius")
                  .set(
                     borderRadius.topLeftRadius() * squirt / 2.0F,
                     borderRadius.bottomLeftRadius() * squirt / 2.0F,
                     borderRadius.topRightRadius() * squirt / 2.0F,
                     borderRadius.bottomRightRadius() * squirt / 2.0F
                  );
            }

            if (squircleProgram.findUniform("Smoothness") != null) {
               squircleProgram.findUniform("Smoothness").set(smoothness);
            }

            if (squircleProgram.findUniform("CornerSmoothness") != null) {
               squircleProgram.findUniform("CornerSmoothness").set(squirt);
            }
         }

         drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float ax = x - horizontalPadding / 2.0F;
         float ay = y - verticalPadding / 2.0F;
         float aw = width + horizontalPadding;
         float ah = height + verticalPadding;
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(m, ax, ay, 0.0F).color(color.getRGB());
         builder.vertex(m, ax, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay, 0.0F).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         matrices.pop();
      }
   }

   public static void drawLoadingRect(
      MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (loadingProgram != null) {
         loadingProgram.use();
         if (loadingProgram.findUniform("Size") != null) {
            loadingProgram.findUniform("Size").set(width, height);
         }

         if (loadingProgram.findUniform("Radius") != null) {
            loadingProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (loadingProgram.findUniform("Smoothness") != null) {
            loadingProgram.findUniform("Smoothness").set(smoothness);
         }

         if (loadingProgram.findUniform("Progress") != null) {
            loadingProgram.findUniform("Progress").set(progress);
         }

         if (loadingProgram.findUniform("StripeWidth") != null) {
            loadingProgram.findUniform("StripeWidth").set(0.0F);
         }

         if (loadingProgram.findUniform("Fade") != null) {
            loadingProgram.findUniform("Fade").set(0.5F);
         }
      }

      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      drawEnd();
      matrices.pop();
   }

   public static void drawLiquidGlass(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float globalAlpha,
      float fresnelPower,
      ColorRGBA fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      boolean clean
   ) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      drawSetup();
      RenderSystem.disableCull();
      int texId = clean ? (mc.getFramebuffer() != null ? mc.getFramebuffer().getColorAttachment() : 0) : BlurProgram.getTexture();
      RenderSystem.setShaderTexture(0, texId);
      if (glassProgram != null) {
         glassProgram.use();
         if (glassProgram.findUniform("GlobalAlpha") != null) {
            glassProgram.findUniform("GlobalAlpha").set(globalAlpha);
         }

         if (glassProgram.findUniform("Size") != null) {
            glassProgram.findUniform("Size").set(width, height);
         }

         if (glassProgram.findUniform("Radius") != null) {
            glassProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (glassProgram.findUniform("Smoothness") != null) {
            glassProgram.findUniform("Smoothness").set(0.5F);
         }

         if (glassProgram.findUniform("FresnelPower") != null) {
            glassProgram.findUniform("FresnelPower").set(fresnelPower);
         }

         if (glassProgram.findUniform("FresnelColor") != null) {
            float[] rgb = ColorUtility.getRGBf(fresnelColor.getRGB());
            glassProgram.findUniform("FresnelColor").set(rgb[0], rgb[1], rgb[2]);
         }

         if (glassProgram.findUniform("FresnelAlpha") != null) {
            glassProgram.findUniform("FresnelAlpha").set(ColorUtility.alphaf(fresnelColor.getRGB()));
         }

         if (glassProgram.findUniform("BaseAlpha") != null) {
            glassProgram.findUniform("BaseAlpha").set(baseAlpha);
         }

         if (glassProgram.findUniform("FresnelInvert") != null) {
            glassProgram.findUniform("FresnelInvert").set(fresnelInvert ? 1 : 0);
         }

         if (glassProgram.findUniform("FresnelMix") != null) {
            glassProgram.findUniform("FresnelMix").set(fresnelMix);
         }

         if (glassProgram.findUniform("DistortStrength") != null) {
            glassProgram.findUniform("DistortStrength").set(distortStrength);
         }

         if (glassProgram.findUniform("CornerSmoothness") != null) {
            glassProgram.findUniform("CornerSmoothness").set(squirt);
         }
      }

      int screenWidth = mc.getWindow().getFramebufferWidth();
      int screenHeight = mc.getWindow().getFramebufferHeight();
      float u = x / screenWidth;
      float v = (screenHeight - y - height) / screenHeight;
      float texWidth = width / screenWidth;
      float texHeight = height / screenHeight;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, x, y, 0.0F).texture(u, v + texHeight).color(color.getRGB());
      builder.vertex(matrix, x, y + height, 0.0F).texture(u, v).color(color.getRGB());
      builder.vertex(matrix, x + width, y + height, 0.0F).texture(u + texWidth, v).color(color.getRGB());
      builder.vertex(matrix, x + width, y, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      drawEnd();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof RoundedRectBatching rb) {
         rb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius(),
            borderRadius.bottomLeftRadius(),
            borderRadius.topRightRadius(),
            borderRadius.bottomRightRadius(),
            color.getRGB()
         );
         matrices.pop();
      } else {
         if (rectangleProgram != null) {
            rectangleProgram.use();
            if (rectangleProgram.findUniform("Size") != null) {
               rectangleProgram.findUniform("Size").set(width, height);
            }

            if (rectangleProgram.findUniform("Radius") != null) {
               rectangleProgram.findUniform("Radius")
                  .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
            }

            if (rectangleProgram.findUniform("Smoothness") != null) {
               rectangleProgram.findUniform("Smoothness").set(smoothness);
            }
         }

         drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float ax = x - horizontalPadding / 2.0F;
         float ay = y - verticalPadding / 2.0F;
         float aw = width + horizontalPadding;
         float ah = height + verticalPadding;
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(m, ax, ay, 0.0F).color(color.getRGB());
         builder.vertex(m, ax, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay, 0.0F).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         matrices.pop();
      }
   }

   public static void drawRoundedRect(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color1,
      ColorRGBA color2,
      ColorRGBA color3,
      ColorRGBA color4
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (gradientRectangleProgram != null) {
         gradientRectangleProgram.use();
         if (gradientRectangleProgram.findUniform("Size") != null) {
            gradientRectangleProgram.findUniform("Size").set(width, height);
         }

         if (gradientRectangleProgram.findUniform("Radius") != null) {
            gradientRectangleProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (gradientRectangleProgram.findUniform("Smoothness") != null) {
            gradientRectangleProgram.findUniform("Smoothness").set(smoothness);
         }

         if (gradientRectangleProgram.findUniform("TopLeftColor") != null) {
            gradientRectangleProgram.findUniform("TopLeftColor")
               .set(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F, color1.getAlpha() / 255.0F);
         }

         if (gradientRectangleProgram.findUniform("BottomLeftColor") != null) {
            gradientRectangleProgram.findUniform("BottomLeftColor")
               .set(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F, color2.getAlpha() / 255.0F);
         }

         if (gradientRectangleProgram.findUniform("BottomRightColor") != null) {
            gradientRectangleProgram.findUniform("BottomRightColor")
               .set(color3.getRed() / 255.0F, color3.getGreen() / 255.0F, color3.getBlue() / 255.0F, color3.getAlpha() / 255.0F);
         }

         if (gradientRectangleProgram.findUniform("TopRightColor") != null) {
            gradientRectangleProgram.findUniform("TopRightColor")
               .set(color4.getRed() / 255.0F, color4.getGreen() / 255.0F, color4.getBlue() / 255.0F, color4.getAlpha() / 255.0F);
         }
      }

      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color1.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color2.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color3.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color4.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      drawRoundedRect(
         matrices,
         x,
         y,
         width,
         height,
         borderRadius,
         gradient.getTopLeftColor(),
         gradient.getBottomLeftColor(),
         gradient.getBottomRightColor(),
         gradient.getTopRightColor()
      );
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.5F;
      float externalSmoothness = 1.0F;
      if (borderProgram != null) {
         borderProgram.use();
         if (borderProgram.findUniform("Size") != null) {
            borderProgram.findUniform("Size").set(width, height);
         }

         if (borderProgram.findUniform("Radius") != null) {
            borderProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (borderProgram.findUniform("Smoothness") != null) {
            borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
         }

         if (borderProgram.findUniform("Thickness") != null) {
            borderProgram.findUniform("Thickness").set(borderThickness);
         }
      }

      drawSetup();
      float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
      float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      drawEnd();
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         RenderSystem.setShaderTexture(0, identifier);
         builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
      } else {
         matrices.push();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, identifier);
         drawSetup();
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor
   ) {
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, identifier);
         int color = clor.getRGB();
         float x2 = x + width;
         float y2 = y + height;
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         matrices.push();
         int color = clor.getRGB();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         float x2 = x + width;
         float y2 = y + height;
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, identifier);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
         RenderSystem.disableBlend();
      }
   }

   public static void drawAnimationSprite(MatrixStack matrices, PenisSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      if (sprite != null) {
         drawTexture(matrices, sprite.texture(), x, y, width, height, sprite.u1(), sprite.u2(), sprite.v1(), sprite.v2(), color);
      }
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      drawTexture(
         matrices,
         Identifier.of("rockstar", sprite.getTexture().getTexture()),
         x,
         y,
         width,
         height,
         sprite.x / sprite.getTexture().getWidth(),
         (sprite.x + sprite.getTexture().getStep()) / sprite.getTexture().getWidth(),
         0.0F,
         1.0F,
         color
      );
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, ColorRGBA.WHITE);
   }

   public static void drawRoundedTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (roundedTextureProgram != null) {
         roundedTextureProgram.use();
         RenderSystem.setShaderTexture(0, identifier);
         if (roundedTextureProgram.findUniform("Size") != null) {
            roundedTextureProgram.findUniform("Size").set(width, height);
         }

         if (roundedTextureProgram.findUniform("Radius") != null) {
            roundedTextureProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (roundedTextureProgram.findUniform("Smoothness") != null) {
            roundedTextureProgram.findUniform("Smoothness").set(smoothness);
         }
      }

      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         float horizontalPadding = -softness / 2.0F + softness * 2.0F;
         float verticalPadding = softness / 2.0F + softness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      } else {
         if (rectangleProgram != null) {
            rectangleProgram.use();
            if (rectangleProgram.findUniform("Size") != null) {
               rectangleProgram.findUniform("Size").set(width, height);
            }

            if (rectangleProgram.findUniform("Radius") != null) {
               rectangleProgram.findUniform("Radius")
                  .set(
                     borderRadius.topLeftRadius() * 3.0F,
                     borderRadius.bottomLeftRadius() * 3.0F,
                     borderRadius.topRightRadius() * 3.0F,
                     borderRadius.bottomRightRadius() * 3.0F
                  );
            }

            if (rectangleProgram.findUniform("Smoothness") != null) {
               rectangleProgram.findUniform("Smoothness").set(softness);
            }
         }

         drawSetup();
         float horizontalPadding = -softness / 2.0F + softness * 2.0F;
         float verticalPadding = softness / 2.0F + softness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         matrices.pop();
      }
   }

   public static void drawBlur(
      MatrixStack matrices, float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.03F;
      blurRadius /= 22.5F;
      if (blurRadius > 0.0F) {
         if (blurProgram != null) {
            blurProgram.setBlurOffset(2.0F);
         }

         if (squircleTextureProgram != null) {
            squircleTextureProgram.use();
            RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
            if (squircleTextureProgram.findUniform("Size") != null) {
               squircleTextureProgram.findUniform("Size").set(width, height);
            }

            if (squircleTextureProgram.findUniform("Radius") != null) {
               squircleTextureProgram.findUniform("Radius")
                  .set(
                     borderRadius.topLeftRadius() * squirt / 2.0F,
                     borderRadius.bottomLeftRadius() * squirt / 2.0F,
                     borderRadius.topRightRadius() * squirt / 2.0F,
                     borderRadius.bottomRightRadius() * squirt / 2.0F
                  );
            }

            if (squircleTextureProgram.findUniform("Smoothness") != null) {
               squircleTextureProgram.findUniform("Smoothness").set(0.1F);
            }

            if (squircleTextureProgram.findUniform("CornerSmoothness") != null) {
               squircleTextureProgram.findUniform("CornerSmoothness").set(squirt);
            }
         }

         drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         int screenWidth = mc.getWindow().getFramebufferWidth();
         int screenHeight = mc.getWindow().getFramebufferHeight();
         float u = adjustedX / screenWidth;
         float v = (screenHeight - adjustedY - adjustedHeight) / screenHeight;
         float texWidth = adjustedWidth / screenWidth;
         float texHeight = adjustedHeight / screenHeight;
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u, v + texHeight).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u, v).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u + texWidth, v).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      blurRadius /= 22.5F;
      if (blurRadius > 0.0F) {
         if (blurProgram != null) {
            blurProgram.setBlurOffset(2.0F);
         }

         if (roundedTextureProgram != null) {
            roundedTextureProgram.use();
            RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
            if (roundedTextureProgram.findUniform("Size") != null) {
               roundedTextureProgram.findUniform("Size").set(width, height);
            }

            if (roundedTextureProgram.findUniform("Radius") != null) {
               roundedTextureProgram.findUniform("Radius")
                  .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
            }

            if (roundedTextureProgram.findUniform("Smoothness") != null) {
               roundedTextureProgram.findUniform("Smoothness").set(0.01F);
            }
         }

         drawSetup();
         int screenWidth = mc.getWindow().getFramebufferWidth();
         int screenHeight = mc.getWindow().getFramebufferHeight();
         float u = x / screenWidth;
         float v = (screenHeight - y - height) / screenHeight;
         float texWidth = width / screenWidth;
         float texHeight = height / screenHeight;
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u, v + texHeight).color(color.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(u, v).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(u + texWidth, v).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }
   }

   public static void drawPlayerHeadWithHat(MatrixStack matrices, PlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      Identifier skinTexture = player instanceof AbstractClientPlayerEntity clientPlayer
         ? clientPlayer.getSkinTextures().texture()
         : mc.getSkinProvider().getSkinTextures(player.getGameProfile()).texture();
      drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
      drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
   }

   public static void drawEntityHeadWithHat(MatrixStack matrices, LivingEntity entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      if (entity instanceof PlayerEntity player) {
         drawPlayerHeadWithHat(matrices, player, x, y, size, borderRadius, color);
      } else {
         try {
            if (mc.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer livingRenderer) {
               EntityRenderState state = livingRenderer.createRenderState();
               livingRenderer.updateRenderState(entity, state, mc.getRenderTickCounter().getTickDelta(false));
               Identifier skinTexture = livingRenderer.getTexture((LivingEntityRenderState)state);
               drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
               drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
            }
         } catch (Throwable var11) {
         }
      }
   }

   public static void drawPlayerHeadWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.125F, 0.125F, 0.25F, 0.25F);
   }

   private static void drawPlayerHatLayerWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.625F, 0.125F, 0.75F, 0.25F);
      RenderSystem.disableBlend();
   }

   public static void drawRoundedTextureWithUV(
      MatrixStack matrices,
      Identifier identifier,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float u1,
      float v1,
      float u2,
      float v2
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (roundedTextureProgram != null) {
         roundedTextureProgram.use();
         RenderSystem.setShaderTexture(0, identifier);
         if (roundedTextureProgram.findUniform("Size") != null) {
            roundedTextureProgram.findUniform("Size").set(width, height);
         }

         if (roundedTextureProgram.findUniform("Radius") != null) {
            roundedTextureProgram.findUniform("Radius")
               .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         }

         if (roundedTextureProgram.findUniform("Smoothness") != null) {
            roundedTextureProgram.findUniform("Smoothness").set(smoothness);
         }
      }

      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u1, v1).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u1, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u2, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u2, v1).color(color.getRGB());
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void drawEnd() {
      RenderSystem.disableBlend();
   }

   public static GlProgram getSquircleProgram() {
      return squircleProgram;
   }

   private DrawUtility() {
      throw new UnsupportedOperationException();
   }
}
