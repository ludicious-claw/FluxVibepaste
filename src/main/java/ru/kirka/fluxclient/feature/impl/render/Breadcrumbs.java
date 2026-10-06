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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;

public class Breadcrumbs extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Тип шлейфа", "Лента", "Лента", "Линия");
   public final NumberSetting lifetime = new NumberSetting("Длительность", "Время жизни шлейфа (сек)", 3.0F, 0.5F, 10.0F, 0.5F);
   public final NumberSetting width = new NumberSetting("Ширина ленты", "Ширина шлейфа", 0.35F, 0.05F, 1.2F, 0.05F);
   public final ModeSetting colorMode = new ModeSetting("Цвет", "Цветовая схема", "Градиент", "Градиент", "Тема клиента", "Радуга");
   public final ColorSetting startColor = new ColorSetting("Цвет у ног", "Цвет начала ленты", new Color(244, 63, 94, 240));
   public final ColorSetting endColor = new ColorSetting("Цвет хвоста", "Цвет угасания ленты", new Color(147, 51, 234, 0));
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Видно сквозь препятствия", true);
   private final List<Breadcrumbs.TrailPoint> points = new ArrayList<>();
   private Vec3d lastPlayerPos = null;

   public Breadcrumbs() {
      super("Breadcrumbs", "3D неоновый шлейф (лента) за ногами игрока при ходьбе и беге", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.lifetime);
      this.registerSetting(this.width);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.startColor);
      this.registerSetting(this.endColor);
      this.registerSetting(this.throughWalls);
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         Vec3d curPos = mc.player.getPos().add(0.0, 0.05, 0.0);
         long now = System.currentTimeMillis();
         synchronized (this.points) {
            if (this.lastPlayerPos == null || curPos.squaredDistanceTo(this.lastPlayerPos) > 0.04) {
               this.points.add(new Breadcrumbs.TrailPoint(curPos, now));
               this.lastPlayerPos = curPos;
            }

            long maxAge = (long)(this.lifetime.get() * 1000.0F);
            this.points.removeIf(p -> now - p.time > maxAge);
         }
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos) {
      if (mc.player != null) {
         long now = System.currentTimeMillis();
         long maxAge = (long)(this.lifetime.get() * 1000.0F);
         if (maxAge > 0L) {
            List<Breadcrumbs.TrailPoint> validPoints = new ArrayList<>();
            synchronized (this.points) {
               for (Breadcrumbs.TrailPoint p : this.points) {
                  float progress = (float)(now - p.time) / (float)maxAge;
                  if (progress >= 0.0F && progress <= 1.0F) {
                     validPoints.add(p);
                  }
               }
            }

            if (validPoints.size() >= 2) {
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

               try {
                  Tessellator tessellator = Tessellator.getInstance();
                  Matrix4f m = matrices.peek().getPositionMatrix();
                  float ribbonWidth = this.width.get();
                  String mType = this.mode.get();
                  if (mType.equals("Линия")) {
                     BufferBuilder b = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

                     for (Breadcrumbs.TrailPoint px : validPoints) {
                        float progress = Math.max(0.0F, Math.min(1.0F, (float)(now - px.time) / (float)maxAge));
                        Color col = this.getPointColor(progress);
                        b.vertex(m, (float)(px.pos.x - cameraPos.x), (float)(px.pos.y - cameraPos.y), (float)(px.pos.z - cameraPos.z))
                           .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
                     }

                     BufferRenderer.drawWithGlobalProgram(b.end());
                  } else {
                     BufferBuilder b = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                     for (Breadcrumbs.TrailPoint px : validPoints) {
                        float progress = Math.max(0.0F, Math.min(1.0F, (float)(now - px.time) / (float)maxAge));
                        Color col = this.getPointColor(progress);
                        int r = col.getRed();
                        int g = col.getGreen();
                        int bl = col.getBlue();
                        int a = col.getAlpha();
                        float rx = (float)(px.pos.x - cameraPos.x);
                        float ry = (float)(px.pos.y - cameraPos.y);
                        float rz = (float)(px.pos.z - cameraPos.z);
                        b.vertex(m, rx, ry, rz).color(r, g, bl, a);
                        b.vertex(m, rx, ry + ribbonWidth, rz).color(r, g, bl, 0);
                     }

                     BufferRenderer.drawWithGlobalProgram(b.end());
                  }
               } catch (Throwable var28) {
               } finally {
                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.enableDepthTest();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.disableBlend();
               }
            }
         }
      }
   }

   private Color getPointColor(float progress) {
      String cm = this.colorMode.get();
      int alpha = (int)((1.0F - progress) * 230.0F);
      if (cm.equals("Радуга")) {
         float hue = ((float)(System.currentTimeMillis() % 4000L) / 4000.0F + progress * 0.5F) % 1.0F;
         Color rgb = new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
         return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
      } else if (cm.equals("Тема клиента")) {
         Color theme = ImGuiTheme.primaryColor();
         return new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), alpha);
      } else {
         Color c1 = this.startColor.get();
         Color c2 = this.endColor.get();
         int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
         int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
         int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
         return new Color(r, g, b, alpha);
      }
   }

   private record TrailPoint(Vec3d pos, long time) {
   }
}
