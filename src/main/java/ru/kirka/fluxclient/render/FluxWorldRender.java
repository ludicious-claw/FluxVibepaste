package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public final class FluxWorldRender {
   public static final Identifier BLOOM_TEXTURE = Identifier.of("fluxclient", "textures/bloom.png");
   public static final Identifier TARGET_TEXTURE = Identifier.of("fluxclient", "textures/target.png");

   private FluxWorldRender() {
   }

   public static int ensureOpaque(int color) {
      return color >>> 24 == 0 ? color | 0xFF000000 : color;
   }

   public static int withAlpha(int color, int alpha) {
      return color & 16777215 | (Math.max(0, Math.min(255, alpha)) & 0xFF) << 24;
   }

   public static void drawBillboardGlow(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot, Vec3d worldPos, float size, int color) {
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      matrices.push();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >>> 24 & 0xFF;
      float luminance = (0.299F * r + 0.587F * g + 0.114F * b) / 255.0F;
      Tessellator tessellator = Tessellator.getInstance();
      if (luminance >= 0.2F) {
         RenderSystem.blendFunc(770, 1);
         matrices.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
         matrices.multiply(camRot);
         matrices.scale(size, size, size);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float fr = r / 255.0F;
         float fg = g / 255.0F;
         float fb = b / 255.0F;
         float fa = a / 255.0F;
         buffer.vertex(matrices.peek().getPositionMatrix(), -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(fr, fg, fb, fa);
         buffer.vertex(matrices.peek().getPositionMatrix(), 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(fr, fg, fb, fa);
         buffer.vertex(matrices.peek().getPositionMatrix(), 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(fr, fg, fb, fa);
         buffer.vertex(matrices.peek().getPositionMatrix(), -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(fr, fg, fb, fa);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.blendFunc(770, 771);
      } else {
         RenderSystem.blendFunc(770, 771);
         matrices.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
         matrices.multiply(camRot);
         matrices.scale(size * 1.2F, size * 1.2F, size * 1.2F);
         BufferBuilder halo = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float ha = a / 255.0F * 0.3F;
         halo.vertex(matrices.peek().getPositionMatrix(), -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, ha);
         halo.vertex(matrices.peek().getPositionMatrix(), 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, ha);
         halo.vertex(matrices.peek().getPositionMatrix(), 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, ha);
         halo.vertex(matrices.peek().getPositionMatrix(), -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, ha);
         BufferRenderer.drawWithGlobalProgram(halo.end());
         matrices.scale(0.8333333F, 0.8333333F, 0.8333333F);
         BufferBuilder core = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float fr = r / 255.0F;
         float fg = g / 255.0F;
         float fb = b / 255.0F;
         float fa = a / 255.0F;
         core.vertex(matrices.peek().getPositionMatrix(), -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(fr, fg, fb, fa);
         core.vertex(matrices.peek().getPositionMatrix(), 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(fr, fg, fb, fa);
         core.vertex(matrices.peek().getPositionMatrix(), 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(fr, fg, fb, fa);
         core.vertex(matrices.peek().getPositionMatrix(), -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(fr, fg, fb, fa);
         BufferRenderer.drawWithGlobalProgram(core.end());
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      matrices.pop();
   }

   public static void drawTexturedBillboard(
      MatrixStack matrices,
      Vec3d cameraPos,
      Quaternionf camRot,
      Vec3d worldPos,
      float size,
      int color,
      Identifier texture,
      float rotationDegrees,
      boolean throughWalls
   ) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, texture);
      matrices.push();
      RenderSystem.disableCull();
      if (throughWalls) {
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
      } else {
         RenderSystem.depthMask(false);
      }

      RenderSystem.enableBlend();
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >>> 24 & 0xFF;
      RenderSystem.blendFunc(770, 771);
      matrices.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
      matrices.multiply(camRot);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationDegrees));
      matrices.scale(size, size, size);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float fr = r / 255.0F;
      float fg = g / 255.0F;
      float fb = b / 255.0F;
      float fa = a / 255.0F;
      buffer.vertex(matrices.peek().getPositionMatrix(), -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(fr, fg, fb, fa);
      buffer.vertex(matrices.peek().getPositionMatrix(), 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(fr, fg, fb, fa);
      buffer.vertex(matrices.peek().getPositionMatrix(), 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(fr, fg, fb, fa);
      buffer.vertex(matrices.peek().getPositionMatrix(), -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(fr, fg, fb, fa);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      if (throughWalls) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
      } else {
         RenderSystem.depthMask(true);
      }

      RenderSystem.enableCull();
      matrices.pop();
   }
}
