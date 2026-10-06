package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.Render3DUtil;

public class PopChams extends Module {
   public final NumberSetting duration = new NumberSetting("Длительность", "Время растворения (сек)", 1.8F, 0.5F, 4.0F, 0.1F);
   public final NumberSetting ascendSpeed = new NumberSetting("Скорость взлета", "Скорость подъема вверх", 0.85F, 0.1F, 2.5F, 0.05F);
   public final NumberSetting expandFactor = new NumberSetting("Расширение", "Увеличение силуэта", 0.35F, 0.0F, 1.0F, 0.05F);
   public final ColorSetting startColor = new ColorSetting("Цвет призрака", "Начальный цвет силуэта", new Color(251, 191, 36, 230));
   public final ColorSetting endColor = new ColorSetting("Цвет угасания", "Цвет при растворении", new Color(239, 68, 68, 0));
   public final BooleanSetting rings = new BooleanSetting("Кольца энергии", "Отрисовка расходящихся колец", true);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Видно сквозь препятствия", true);
   private final List<PopChams.PopPhantom> phantoms = new ArrayList<>();

   public PopChams() {
      super("PopChams", "Светящийся восходящий призрак игрока при срабатывании тотема", Category.RENDER, -1);
      this.registerSetting(this.duration);
      this.registerSetting(this.ascendSpeed);
      this.registerSetting(this.expandFactor);
      this.registerSetting(this.startColor);
      this.registerSetting(this.endColor);
      this.registerSetting(this.rings);
      this.registerSetting(this.throughWalls);
   }

   public void onTotemPop(Entity entity) {
      if (this.isEnabled() && entity != null) {
         Vec3d pos = entity.getPos();
         float yaw = entity.getYaw();
         float pitch = entity.getPitch();
         synchronized (this.phantoms) {
            this.phantoms.add(new PopChams.PopPhantom(pos, yaw, pitch, System.currentTimeMillis()));
         }
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      long now = System.currentTimeMillis();
      long maxLifetime = (long)(this.duration.get() * 1000.0F);
      synchronized (this.phantoms) {
         this.phantoms.removeIf(px -> now - px.spawnTime > maxLifetime);
      }

      if (!this.phantoms.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.disableCull();
         if (this.throughWalls.get()) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Tessellator tessellator = Tessellator.getInstance();

         for (PopChams.PopPhantom p : this.phantoms) {
            float progress = (float)(now - p.spawnTime) / (float)maxLifetime;
            if (!(progress > 1.0F)) {
               float yOffset = progress * this.ascendSpeed.get();
               float scale = 1.0F + progress * this.expandFactor.get();
               Color c1 = this.startColor.get();
               Color c2 = this.endColor.get();
               int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
               int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
               int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
               int a = (int)(c1.getAlpha() * (1.0F - progress));
               matrices.push();
               matrices.translate(p.pos.x - cameraPos.x, p.pos.y + yOffset - cameraPos.y, p.pos.z - cameraPos.z);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.yaw + 180.0F));
               matrices.scale(scale, scale, scale);
               Matrix4f m = matrices.peek().getPositionMatrix();
               BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
               this.drawBoxMesh(buffer, m, -0.2F, 0.75F, -0.1F, 0.2F, 1.45F, 0.1F, r, g, b, a);
               this.drawBoxMesh(buffer, m, -0.18F, 1.45F, -0.18F, 0.18F, 1.85F, 0.18F, 255, 255, 255, a);
               this.drawBoxMesh(buffer, m, 0.22F, 0.75F, -0.08F, 0.38F, 1.45F, 0.08F, r, g, b, a);
               this.drawBoxMesh(buffer, m, -0.38F, 0.75F, -0.08F, -0.22F, 1.45F, 0.08F, r, g, b, a);
               this.drawBoxMesh(buffer, m, -0.19F, 0.0F, -0.09F, -0.02F, 0.75F, 0.09F, r, g, b, a);
               this.drawBoxMesh(buffer, m, 0.02F, 0.0F, -0.09F, 0.19F, 0.75F, 0.09F, r, g, b, a);
               BufferRenderer.drawWithGlobalProgram(buffer.end());
               if (this.rings.get()) {
                  float ringR = 0.5F + progress * 1.5F;
                  Color ringCol = new Color(r, g, b, (int)(a * 0.6F));
                  Render3DUtil.drawRing(matrices, Vec3d.ZERO.add(0.0, 0.8, 0.0), Math.max(0.0F, ringR - 0.08F), ringR, ringCol, 32);
               }

               matrices.pop();
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void drawBoxMesh(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int bCol, int a) {
      this.quad(b, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, bCol, a);
      this.quad(b, m, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, r, g, bCol, a);
      this.quad(b, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, bCol, a);
      this.quad(b, m, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, bCol, a);
      this.quad(b, m, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, bCol, a);
      this.quad(b, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, bCol, a);
   }

   private void quad(
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

   private record PopPhantom(Vec3d pos, float yaw, float pitch, long spawnTime) {
   }
}
