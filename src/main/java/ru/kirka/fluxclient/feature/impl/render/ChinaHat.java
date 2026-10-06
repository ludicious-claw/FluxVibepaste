package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;

public class ChinaHat extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Форма головного убора", "Конусная шляпа", "Конусная шляпа", "Нимб (Halo)", "Орбитальные кольца");
   public final NumberSetting radius = new NumberSetting("Радиус", "Радиус полей шляпы", 0.65F, 0.3F, 1.5F, 0.05F);
   public final NumberSetting height = new NumberSetting("Высота конуса", "Угол скоса шляпы", 0.28F, 0.05F, 0.8F, 0.05F);
   public final NumberSetting offsetY = new NumberSetting("Высота над головой", "Смещение по высоте", 0.2F, -0.3F, 0.8F, 0.05F);
   public final NumberSetting rotateSpeed = new NumberSetting("Скорость вращения", "Вращение шляпы", 1.2F, 0.0F, 4.0F, 0.1F);
   public final ModeSetting colorMode = new ModeSetting("Цвет", "Режим цвета", "Градиент", "Градиент", "Тема клиента", "Радуга");
   public final ColorSetting colorCenter = new ColorSetting("Цвет вершины", "Цвет макушки конуса", new Color(255, 255, 255, 240));
   public final ColorSetting colorEdge = new ColorSetting("Цвет полей", "Цвет кромки шляпы", new Color(225, 29, 72, 190));
   public final BooleanSetting outline = new BooleanSetting("Неоновая кромка", "Яркое светящееся кольцо на краю", true);
   public final BooleanSetting renderInFirstPerson = new BooleanSetting("От 1-го лица", "Видеть шляпу в виде от 1 лица", false);

   public ChinaHat() {
      super("ChinaHat", "Стильная азиатская конусная шляпа или нимб над головой", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.radius);
      this.registerSetting(this.height);
      this.registerSetting(this.offsetY);
      this.registerSetting(this.rotateSpeed);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.colorCenter);
      this.registerSetting(this.colorEdge);
      this.registerSetting(this.outline);
      this.registerSetting(this.renderInFirstPerson);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos) {
      if (mc.player != null) {
         if (!mc.options.getPerspective().isFirstPerson() || this.renderInFirstPerson.get()) {
            float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
            double px = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
            double py = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY());
            double pz = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());
            float headHeight = mc.player.getStandingEyeHeight() + (mc.player.isSneaking() ? -0.1F : 0.12F) + this.offsetY.get();
            float time = (float)(System.currentTimeMillis() % 10000000L / 1000.0);
            float rot = time * this.rotateSpeed.get() * 90.0F % 360.0F;
            matrices.push();
            matrices.translate(px - cameraPos.x, py + headHeight - cameraPos.y, pz - cameraPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot));
            float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());
            float yaw = MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * 0.45F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(770, 1);
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            Matrix4f m = matrices.peek().getPositionMatrix();
            Color cTip = this.getCenterColor();
            Color cEdge = this.getEdgeColor();
            int segments = 40;
            float r = this.radius.get();
            float h = this.height.get();
            String mType = this.mode.get();
            if (mType.equals("Нимб (Halo)")) {
               BufferBuilder ring = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
               float inner = r * 0.75F;

               for (int i = 0; i <= segments; i++) {
                  double angle = i * 2.0 * Math.PI / segments;
                  float cos = (float)Math.cos(angle);
                  float sin = (float)Math.sin(angle);
                  ring.vertex(m, cos * inner, 0.0F, sin * inner).color(cTip.getRed(), cTip.getGreen(), cTip.getBlue(), cTip.getAlpha());
                  ring.vertex(m, cos * r, 0.0F, sin * r).color(cEdge.getRed(), cEdge.getGreen(), cEdge.getBlue(), cEdge.getAlpha());
               }

               BufferRenderer.drawWithGlobalProgram(ring.end());
            } else {
               BufferBuilder fan = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
               fan.vertex(m, 0.0F, h, 0.0F).color(cTip.getRed(), cTip.getGreen(), cTip.getBlue(), cTip.getAlpha());

               for (int i = 0; i <= segments; i++) {
                  double angle = i * 2.0 * Math.PI / segments;
                  float x = (float)(Math.cos(angle) * r);
                  float z = (float)(Math.sin(angle) * r);
                  fan.vertex(m, x, 0.0F, z).color(cEdge.getRed(), cEdge.getGreen(), cEdge.getBlue(), cEdge.getAlpha());
               }

               BufferRenderer.drawWithGlobalProgram(fan.end());
               if (this.outline.get()) {
                  GL11.glLineWidth(2.0F);
                  BufferBuilder line = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int i = 0; i <= segments; i++) {
                     double angle = i * 2.0 * Math.PI / segments;
                     float x = (float)(Math.cos(angle) * r);
                     float z = (float)(Math.sin(angle) * r);
                     line.vertex(m, x, 0.0F, z).color(255, 255, 255, 255);
                  }

                  BufferRenderer.drawWithGlobalProgram(line.end());
                  GL11.glLineWidth(1.0F);
               }
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            matrices.pop();
         }
      }
   }

   private Color getCenterColor() {
      String cm = this.colorMode.get();
      if (cm.equals("Тема клиента")) {
         return ImGuiTheme.primaryColor();
      } else if (cm.equals("Радуга")) {
         float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
         return new Color(Color.HSBtoRGB(hue, 0.7F, 1.0F));
      } else {
         return this.colorCenter.get();
      }
   }

   private Color getEdgeColor() {
      String cm = this.colorMode.get();
      if (cm.equals("Тема клиента")) {
         return ImGuiTheme.primaryColor();
      } else if (cm.equals("Радуга")) {
         float hue = ((float)(System.currentTimeMillis() % 4000L) / 4000.0F + 0.3F) % 1.0F;
         return new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
      } else {
         return this.colorEdge.get();
      }
   }
}
