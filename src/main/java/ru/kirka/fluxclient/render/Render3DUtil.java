package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import ru.kirka.fluxclient.render.shaders.OrbitShader;

public final class Render3DUtil {
   private Render3DUtil() {
   }

   public static void setupNeon(boolean throughWalls) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      if (throughWalls) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }

      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
   }

   public static void endNeon() {
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   public static void drawNeonReticleSquare(
      MatrixStack matrices, Vec3d center, Quaternionf camRot, float angleDeg, float size, Color color, boolean throughWalls
   ) {
      setupNeon(throughWalls);
      matrices.push();
      matrices.translate(center.x, center.y, center.z);
      matrices.multiply(camRot);
      matrices.multiply(new Quaternionf().rotateZ((float)Math.toRadians(angleDeg)));
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      float len = size * 0.45F;
      float thick = size * 0.11F;
      drawChevronSet(matrices, buffer, size, len * 1.12F, thick * 1.8F, r, g, b, 70);
      drawChevronSet(matrices, buffer, size, len, thick, r, g, b, 230);
      drawChevronSet(matrices, buffer, size, len * 0.88F, thick * 0.35F, 255, 255, 255, 255);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      matrices.pop();
      endNeon();
   }

   private static void drawChevronSet(MatrixStack matrices, BufferBuilder buffer, float dist, float len, float thick, int r, int g, int b, int a) {
      for (int i = 0; i < 4; i++) {
         matrices.push();
         matrices.multiply(new Quaternionf().rotateZ((float)((Math.PI / 2) * i)));
         Matrix4f m = matrices.peek().getPositionMatrix();
         float apexX = 0.0F;
         float leftX = -len;
         float leftY = dist - len;
         float rightY = dist - len;
         drawThickQuad(buffer, m, apexX, dist, leftX, leftY, thick, r, g, b, a);
         drawThickQuad(buffer, m, apexX, dist, len, rightY, thick, r, g, b, a);
         matrices.pop();
      }
   }

   private static void drawThickQuad(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, float thick, int cr, int cg, int cb, int ca) {
      float dx = y2 - y1;
      float dy = -(x2 - x1);
      float dLen = (float)Math.sqrt(dx * dx + dy * dy);
      if (dLen != 0.0F) {
         dx = dx / dLen * (thick * 0.5F);
         dy = dy / dLen * (thick * 0.5F);
         b.vertex(m, x1 - dx, y1 - dy, 0.0F).color(cr, cg, cb, ca);
         b.vertex(m, x1 + dx, y1 + dy, 0.0F).color(cr, cg, cb, ca);
         b.vertex(m, x2 + dx, y2 + dy, 0.0F).color(cr, cg, cb, ca);
         b.vertex(m, x1 - dx, y1 - dy, 0.0F).color(cr, cg, cb, ca);
         b.vertex(m, x2 + dx, y2 + dy, 0.0F).color(cr, cg, cb, ca);
         b.vertex(m, x2 - dx, y2 - dy, 0.0F).color(cr, cg, cb, ca);
      }
   }

   public static void drawViewAlignedGhost(
      MatrixStack matrices,
      Vec3d targetPos,
      Vec3d cameraPos,
      Quaternionf camRot,
      float radius,
      float baseHeight,
      float time,
      float phaseOffset,
      Color color,
      boolean throughWalls
   ) {
      setupNeon(throughWalls);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      int cr = color.getRed();
      int cg = color.getGreen();
      int cb = color.getBlue();
      int segments = 36;
      float tailArc = 2.1F;
      Vec3d headPos = null;
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         float t = (float)i / segments;
         double angle = time * 3.2 + phaseOffset - (1.0F - t) * tailArc;
         float x = (float)(targetPos.x + Math.cos(angle) * radius);
         float z = (float)(targetPos.z + Math.sin(angle) * radius);
         float y = (float)(targetPos.y + baseHeight + Math.sin(time * 2.6 + angle * 0.8) * 0.42);
         if (i == segments) {
            headPos = new Vec3d(x, y, z);
         }

         float halfWidth = (float)(Math.pow(t, 0.75) * 0.11F);
         int alpha = (int)(color.getAlpha() * Math.pow(t, 1.4));
         buffer.vertex(matrix, x, y - halfWidth, z).color(cr, cg, cb, alpha);
         buffer.vertex(matrix, x, y + halfWidth, z).color(cr, cg, cb, alpha);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      if (headPos != null) {
         drawRadialGlow(matrices, headPos, camRot, 0.22F, color);
      }

      endNeon();
   }

   public static void drawGlowingCylinderRing(
      MatrixStack matrices, Vec3d center, float radius, float height, float bobOffset, Color color, boolean throughWalls
   ) {
      setupNeon(throughWalls);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      int cr = color.getRed();
      int cg = color.getGreen();
      int cb = color.getBlue();
      int segments = 40;
      float topY = (float)center.y + bobOffset + height;
      float bottomY = (float)center.y + bobOffset;
      BufferBuilder cyl = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         float x = (float)(center.x + Math.cos(angle) * radius);
         float z = (float)(center.z + Math.sin(angle) * radius);
         cyl.vertex(matrix, x, topY, z).color(cr, cg, cb, 230);
         cyl.vertex(matrix, x, bottomY, z).color(cr, cg, cb, 0);
      }

      BufferRenderer.drawWithGlobalProgram(cyl.end());
      BufferBuilder ring = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      float rIn = radius - 0.025F;
      float rOut = radius + 0.025F;

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         ring.vertex(matrix, (float)center.x + cos * rIn, topY, (float)center.z + sin * rIn).color(255, 255, 255, 255);
         ring.vertex(matrix, (float)center.x + cos * rOut, topY, (float)center.z + sin * rOut).color(cr, cg, cb, 200);
      }

      BufferRenderer.drawWithGlobalProgram(ring.end());
      endNeon();
   }

   public static void drawRadialGlow(MatrixStack matrices, Vec3d pos, Quaternionf camRot, float radius, Color color) {
      setupNeon(true);
      matrices.push();
      matrices.translate(pos.x, pos.y, pos.z);
      matrices.multiply(camRot);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = color.getAlpha();
      BufferBuilder fan = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      fan.vertex(matrix, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
      int segments = 16;

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         fan.vertex(matrix, (float)(Math.cos(angle) * radius), (float)(Math.sin(angle) * radius), 0.0F).color(r, g, b, 0);
      }

      BufferRenderer.drawWithGlobalProgram(fan.end());
      BufferBuilder core = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float coreSize = radius * 0.28F;
      core.vertex(matrix, 0.0F, coreSize, 0.0F).color(255, 255, 255, a);
      core.vertex(matrix, -coreSize, 0.0F, 0.0F).color(r, g, b, a);
      core.vertex(matrix, coreSize, 0.0F, 0.0F).color(r, g, b, a);
      core.vertex(matrix, 0.0F, -coreSize, 0.0F).color(255, 255, 255, a);
      core.vertex(matrix, coreSize, 0.0F, 0.0F).color(r, g, b, a);
      core.vertex(matrix, -coreSize, 0.0F, 0.0F).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(core.end());
      matrices.pop();
      endNeon();
   }

   public static void drawRing(MatrixStack matrices, Vec3d center, float innerRadius, float outerRadius, Color color, int segments) {
      setupNeon(false);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = color.getAlpha();

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         buffer.vertex(matrix, (float)center.x + cos * innerRadius, (float)center.y, (float)center.z + sin * innerRadius).color(r, g, b, a);
         buffer.vertex(matrix, (float)center.x + cos * outerRadius, (float)center.y, (float)center.z + sin * outerRadius).color(r, g, b, 0);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      endNeon();
   }

   public static void drawBox(MatrixStack matrices, Box box, Color outlineColor, Color fillColor, float lineWidth, boolean throughWalls) {
      if (fillColor != null && fillColor.getAlpha() > 0) {
         drawBoxFill(matrices, box, fillColor, throughWalls);
      }

      if (outlineColor != null && outlineColor.getAlpha() > 0) {
         drawBoxOutline(matrices, box, outlineColor, lineWidth, throughWalls);
      }
   }

   public static void drawBoxFill(MatrixStack matrices, Box box, Color color, boolean throughWalls) {
      setupNeon(throughWalls);
      Matrix4f m = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = color.getAlpha();
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
      addQuad(buffer, m, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, r, g, b, a);
      addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
      addQuad(buffer, m, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
      addQuad(buffer, m, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
      addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      endNeon();
   }

   public static void drawBoxOutline(MatrixStack matrices, Box box, Color color, float lineWidth, boolean throughWalls) {
      setupNeon(throughWalls);
      GL11.glLineWidth(lineWidth);
      Matrix4f m = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = color.getAlpha();
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      addLine(buffer, m, x1, y1, z1, x2, y1, z1, r, g, b, a);
      addLine(buffer, m, x2, y1, z1, x2, y1, z2, r, g, b, a);
      addLine(buffer, m, x2, y1, z2, x1, y1, z2, r, g, b, a);
      addLine(buffer, m, x1, y1, z2, x1, y1, z1, r, g, b, a);
      addLine(buffer, m, x1, y2, z1, x2, y2, z1, r, g, b, a);
      addLine(buffer, m, x2, y2, z1, x2, y2, z2, r, g, b, a);
      addLine(buffer, m, x2, y2, z2, x1, y2, z2, r, g, b, a);
      addLine(buffer, m, x1, y2, z2, x1, y2, z1, r, g, b, a);
      addLine(buffer, m, x1, y1, z1, x1, y2, z1, r, g, b, a);
      addLine(buffer, m, x2, y1, z1, x2, y2, z1, r, g, b, a);
      addLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a);
      addLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      GL11.glLineWidth(1.0F);
      endNeon();
   }

   public static void drawCornerBox(MatrixStack matrices, Box box, Color color, float lineWidth, float cornerFactor, boolean throughWalls) {
      setupNeon(throughWalls);
      GL11.glLineWidth(lineWidth);
      Matrix4f m = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = color.getAlpha();
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      float lenX = (x2 - x1) * cornerFactor;
      float lenY = (y2 - y1) * cornerFactor;
      float lenZ = (z2 - z1) * cornerFactor;
      addLine(buffer, m, x1, y1, z1, x1 + lenX, y1, z1, r, g, b, a);
      addLine(buffer, m, x1, y1, z1, x1, y1 + lenY, z1, r, g, b, a);
      addLine(buffer, m, x1, y1, z1, x1, y1, z1 + lenZ, r, g, b, a);
      addLine(buffer, m, x2, y1, z1, x2 - lenX, y1, z1, r, g, b, a);
      addLine(buffer, m, x2, y1, z1, x2, y1 + lenY, z1, r, g, b, a);
      addLine(buffer, m, x2, y1, z1, x2, y1, z1 + lenZ, r, g, b, a);
      addLine(buffer, m, x1, y1, z2, x1 + lenX, y1, z2, r, g, b, a);
      addLine(buffer, m, x1, y1, z2, x1, y1 + lenY, z2, r, g, b, a);
      addLine(buffer, m, x1, y1, z2, x1, y1, z2 - lenZ, r, g, b, a);
      addLine(buffer, m, x2, y1, z2, x2 - lenX, y1, z2, r, g, b, a);
      addLine(buffer, m, x2, y1, z2, x2, y1 + lenY, z2, r, g, b, a);
      addLine(buffer, m, x2, y1, z2, x2, y1, z2 - lenZ, r, g, b, a);
      addLine(buffer, m, x1, y2, z1, x1 + lenX, y2, z1, r, g, b, a);
      addLine(buffer, m, x1, y2, z1, x1, y2 - lenY, z1, r, g, b, a);
      addLine(buffer, m, x1, y2, z1, x1, y2, z1 + lenZ, r, g, b, a);
      addLine(buffer, m, x2, y2, z1, x2 - lenX, y2, z1, r, g, b, a);
      addLine(buffer, m, x2, y2, z1, x2, y2 - lenY, z1, r, g, b, a);
      addLine(buffer, m, x2, y2, z1, x2, y2, z1 + lenZ, r, g, b, a);
      addLine(buffer, m, x1, y2, z2, x1 + lenX, y2, z2, r, g, b, a);
      addLine(buffer, m, x1, y2, z2, x1, y2 - lenY, z2, r, g, b, a);
      addLine(buffer, m, x1, y2, z2, x1, y2, z2 - lenZ, r, g, b, a);
      addLine(buffer, m, x2, y2, z2, x2 - lenX, y2, z2, r, g, b, a);
      addLine(buffer, m, x2, y2, z2, x2, y2 - lenY, z2, r, g, b, a);
      addLine(buffer, m, x2, y2, z2, x2, y2, z2 - lenZ, r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      GL11.glLineWidth(1.0F);
      endNeon();
   }

   private static void addLine(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int bCol, int a) {
      b.vertex(m, x1, y1, z1).color(r, g, bCol, a);
      b.vertex(m, x2, y2, z2).color(r, g, bCol, a);
   }

   private static void addQuad(
      BufferBuilder b,
      Matrix4f m,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      int r,
      int g,
      int bCol,
      int a
   ) {
      b.vertex(m, x1, y1, z1).color(r, g, bCol, a);
      b.vertex(m, x2, y2, z2).color(r, g, bCol, a);
      b.vertex(m, x3, y3, z3).color(r, g, bCol, a);
      b.vertex(m, x1, y1, z1).color(r, g, bCol, a);
      b.vertex(m, x3, y3, z3).color(r, g, bCol, a);
      b.vertex(m, x4, y4, z4).color(r, g, bCol, a);
   }

   public static void drawBoxShaded(MatrixStack matrices, Box box, OrbitShader shader, Color baseColor, float alpha, boolean throughWalls) {
      if (shader != null && shader.isValid()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.disableCull();
         if (throughWalls) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.depthMask(false);
         shader.bind();
         shader.setModelViewMatrix(matrices.peek().getPositionMatrix());
         shader.setFloat("time", (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
         shader.setFloat4("baseColor", baseColor.getRed() / 255.0F, baseColor.getGreen() / 255.0F, baseColor.getBlue() / 255.0F, 1.0F);
         shader.setFloat("alpha", alpha);
         MinecraftClient mcClient = MinecraftClient.getInstance();
         if (mcClient.player != null) {
            shader.setFloat("cameraYaw", mcClient.player.getYaw());
            shader.setFloat("cameraPitch", mcClient.player.getPitch());
         }

         float x1 = (float)box.minX;
         float y1 = (float)box.minY;
         float z1 = (float)box.minZ;
         float x2 = (float)box.maxX;
         float y2 = (float)box.maxY;
         float z2 = (float)box.maxZ;
         float[] vertices = new float[]{
            x1,
            y1,
            z1,
            0.0F,
            0.0F,
            x2,
            y1,
            z1,
            1.0F,
            0.0F,
            x2,
            y1,
            z2,
            1.0F,
            1.0F,
            x1,
            y1,
            z1,
            0.0F,
            0.0F,
            x2,
            y1,
            z2,
            1.0F,
            1.0F,
            x1,
            y1,
            z2,
            0.0F,
            1.0F,
            x1,
            y2,
            z2,
            0.0F,
            0.0F,
            x2,
            y2,
            z2,
            1.0F,
            0.0F,
            x2,
            y2,
            z1,
            1.0F,
            1.0F,
            x1,
            y2,
            z2,
            0.0F,
            0.0F,
            x2,
            y2,
            z1,
            1.0F,
            1.0F,
            x1,
            y2,
            z1,
            0.0F,
            1.0F,
            x1,
            y1,
            z1,
            0.0F,
            0.0F,
            x1,
            y2,
            z1,
            0.0F,
            1.0F,
            x2,
            y2,
            z1,
            1.0F,
            1.0F,
            x1,
            y1,
            z1,
            0.0F,
            0.0F,
            x2,
            y2,
            z1,
            1.0F,
            1.0F,
            x2,
            y1,
            z1,
            1.0F,
            0.0F,
            x2,
            y1,
            z2,
            0.0F,
            0.0F,
            x2,
            y2,
            z2,
            0.0F,
            1.0F,
            x1,
            y2,
            z2,
            1.0F,
            1.0F,
            x2,
            y1,
            z2,
            0.0F,
            0.0F,
            x1,
            y2,
            z2,
            1.0F,
            1.0F,
            x1,
            y1,
            z2,
            1.0F,
            0.0F,
            x1,
            y1,
            z2,
            0.0F,
            0.0F,
            x1,
            y2,
            z2,
            0.0F,
            1.0F,
            x1,
            y2,
            z1,
            1.0F,
            1.0F,
            x1,
            y1,
            z2,
            0.0F,
            0.0F,
            x1,
            y2,
            z1,
            1.0F,
            1.0F,
            x1,
            y1,
            z1,
            1.0F,
            0.0F,
            x2,
            y1,
            z1,
            0.0F,
            0.0F,
            x2,
            y2,
            z1,
            0.0F,
            1.0F,
            x2,
            y2,
            z2,
            1.0F,
            1.0F,
            x2,
            y1,
            z1,
            0.0F,
            0.0F,
            x2,
            y2,
            z2,
            1.0F,
            1.0F,
            x2,
            y1,
            z2,
            1.0F,
            0.0F
         };
         OrbitShader.drawTriangles(vertices, 12);
         shader.unbind();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   public static void drawCone(
      MatrixStack matrices, Vec3d tip, float radius, float height, Color centerColor, Color edgeColor, int segments, boolean throughWalls
   ) {
      setupNeon(throughWalls);
      Matrix4f m = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      int cr = centerColor.getRed();
      int cg = centerColor.getGreen();
      int cb = centerColor.getBlue();
      int ca = centerColor.getAlpha();
      int er = edgeColor.getRed();
      int eg = edgeColor.getGreen();
      int eb = edgeColor.getBlue();
      int ea = edgeColor.getAlpha();
      BufferBuilder fan = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      fan.vertex(m, (float)tip.x, (float)tip.y, (float)tip.z).color(cr, cg, cb, ca);
      float baseY = (float)tip.y - height;

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         float x = (float)(tip.x + Math.cos(angle) * radius);
         float z = (float)(tip.z + Math.sin(angle) * radius);
         fan.vertex(m, x, baseY, z).color(er, eg, eb, ea);
      }

      BufferRenderer.drawWithGlobalProgram(fan.end());
      endNeon();
   }
}
