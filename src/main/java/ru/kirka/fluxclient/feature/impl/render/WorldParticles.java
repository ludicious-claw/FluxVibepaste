package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;

public class WorldParticles extends Module {
   public final ModeSetting type = new ModeSetting(
      "Тип", "Вид окружающих частиц", "Светлячки", "Светлячки", "Звёздная пыль", "Лепестки сакуры", "Искры огня", "Снежный вихрь"
   );
   public final NumberSetting density = new NumberSetting("Плотность", "Количество частиц вокруг игрока", 50.0F, 15.0F, 120.0F, 5.0F);
   public final NumberSetting speed = new NumberSetting("Скорость", "Скорость дрейфа и ветра", 1.0F, 0.2F, 3.0F, 0.1F);
   public final NumberSetting size = new NumberSetting("Размер", "Масштаб частиц", 0.16F, 0.05F, 0.45F, 0.02F);
   public final ModeSetting colorMode = new ModeSetting("Цвет", "Цветовая палитра", "По умолчанию", "По умолчанию", "Тема клиента", "Свой цвет", "Радуга");
   public final ColorSetting customColor = new ColorSetting("Свой цвет", "Пользовательский оттенок", new Color(168, 85, 247, 230));
   private final List<WorldParticles.AmbientParticle> particles = new ArrayList<>();
   private final Random random = new Random();

   public WorldParticles() {
      super("WorldParticles", "Живые атмосферные 3D частицы (светлячки, сакура, искры, пыль)", Category.RENDER, -1);
      this.registerSetting(this.type);
      this.registerSetting(this.density);
      this.registerSetting(this.speed);
      this.registerSetting(this.size);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.customColor);
   }

   @Override
   protected void onEnable() {
      this.particles.clear();
      if (mc.player != null) {
         int target = Math.round(this.density.get());

         for (int i = 0; i < target; i++) {
            this.particles.add(this.spawnParticle(true));
         }
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.player != null) {
         int targetCount = Math.round(this.density.get());

         while (this.particles.size() < targetCount) {
            this.particles.add(this.spawnParticle(false));
         }

         if (this.particles.size() > targetCount) {
            this.particles.subList(targetCount, this.particles.size()).clear();
         }

         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Tessellator tessellator = Tessellator.getInstance();
         float baseScale = this.size.get();
         float spd = this.speed.get();
         String currentType = this.type.get();
         Vec3d pPos = mc.player.getPos();

         for (int i = 0; i < this.particles.size(); i++) {
            WorldParticles.AmbientParticle p = this.particles.get(i);
            p.age++;
            this.updateParticlePhysics(p, spd, currentType);
            if (!(p.pos.distanceTo(pPos) > 22.0) && p.age <= p.maxAge) {
               float lifeProgress = (float)p.age / p.maxAge;
               float alphaFactor = (float)Math.sin(lifeProgress * Math.PI);
               int alpha = (int)(alphaFactor * 225.0F);
               if (alpha > 2) {
                  Color c = this.resolveColor(currentType);
                  int r = c.getRed();
                  int g = c.getGreen();
                  int b = c.getBlue();
                  float curScale = baseScale * p.scaleMultiplier;
                  matrices.push();
                  matrices.translate(p.pos.x - cameraPos.x, p.pos.y - cameraPos.y, p.pos.z - cameraPos.z);
                  matrices.multiply(camRot);
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));
                  Matrix4f m = matrices.peek().getPositionMatrix();
                  switch (currentType) {
                     case "Светлячки":
                        this.drawFirefly(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Звёздная пыль":
                        this.drawSparkle(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Лепестки сакуры":
                        this.drawPetal(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Снежный вихрь":
                        this.drawSnowflake(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     default:
                        this.drawEmber(tessellator, m, curScale, r, g, b, alpha);
                  }

                  matrices.pop();
               }
            } else {
               this.particles.set(i, this.spawnParticle(false));
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void updateParticlePhysics(WorldParticles.AmbientParticle p, float spd, String curType) {
      p.rotation = p.rotation + p.rotSpeed * spd;
      switch (curType) {
         case "Светлячки":
            double swayX = Math.sin(p.age * 0.04 + p.seed) * 0.015 * spd;
            double swayY = Math.sin(p.age * 0.03 + p.seed * 2.0) * 0.012 * spd;
            double swayZ = Math.cos(p.age * 0.04 + p.seed) * 0.015 * spd;
            p.pos = p.pos.add(p.velocity.x * spd + swayX, p.velocity.y * spd + swayY, p.velocity.z * spd + swayZ);
            break;
         case "Лепестки сакуры": {
            double driftX = Math.sin(p.age * 0.05 + p.seed) * 0.025 * spd + 0.015 * spd;
            double driftZ = Math.cos(p.age * 0.04 + p.seed) * 0.02 * spd + 0.01 * spd;
            p.pos = p.pos.add(driftX, -0.022 * spd, driftZ);
            break;
         }
         case "Искры огня": {
            double driftX = Math.sin(p.age * 0.08 + p.seed) * 0.01 * spd;
            p.pos = p.pos.add(driftX, 0.035 * spd, p.velocity.z * spd);
            break;
         }
         case "Снежный вихрь":
            double angle = p.age * 0.06 + p.seed;
            p.pos = p.pos.add(Math.cos(angle) * 0.02 * spd, -0.028 * spd, Math.sin(angle) * 0.02 * spd);
            break;
         default:
            p.pos = p.pos.add(p.velocity.multiply(spd * 0.7));
      }
   }

   private Color resolveColor(String curType) {
      String var2 = this.colorMode.get();

      return switch (var2) {
         case "Тема клиента" -> ImGuiTheme.primaryColor();
         case "Свой цвет" -> (Color)this.customColor.get();
         case "Радуга" -> {
            float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F % 1.0F;
            yield new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
         }
         default -> switch (curType) {
            case "Светлячки" -> new Color(250, 204, 21);
            case "Лепестки сакуры" -> new Color(244, 114, 182);
            case "Искры огня" -> new Color(249, 115, 22);
            case "Снежный вихрь" -> new Color(224, 242, 254);
            default -> new Color(192, 132, 252);
         };
      };
   }

   private void drawFirefly(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buf.vertex(m, 0.0F, 0.0F, 0.0F).color(255, 255, 255, a);
      int segs = 12;

      for (int i = 0; i <= segs; i++) {
         double angle = (Math.PI * 2) / segs * i;
         buf.vertex(m, (float)Math.cos(angle) * s, (float)Math.sin(angle) * s, 0.0F).color(r, g, b, 0);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawSparkle(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float h = s * 1.2F;
      float w = s * 0.35F;
      buf.vertex(m, 0.0F, h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, 0);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, 0);
      buf.vertex(m, 0.0F, -h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, 0);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, 0);
      buf.vertex(m, h, 0.0F, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, 0.0F, w, 0.0F).color(r, g, b, 0);
      buf.vertex(m, 0.0F, -w, 0.0F).color(r, g, b, 0);
      buf.vertex(m, -h, 0.0F, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, 0.0F, -w, 0.0F).color(r, g, b, 0);
      buf.vertex(m, 0.0F, w, 0.0F).color(r, g, b, 0);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawPetal(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buf.vertex(m, 0.0F, 0.0F, 0.0F).color(255, 230, 240, a);
      float pw = s * 0.7F;
      float ph = s * 1.1F;
      buf.vertex(m, -pw * 0.5F, ph * 0.6F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, 0.0F, ph, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, pw * 0.5F, ph * 0.6F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, pw * 0.4F, -ph * 0.3F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, 0.0F, -ph * 0.6F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, -pw * 0.4F, -ph * 0.3F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      buf.vertex(m, -pw * 0.5F, ph * 0.6F, 0.0F).color(r, g, b, (int)(a * 0.8F));
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawSnowflake(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < 3; i++) {
         double angle = (Math.PI / 3) * i;
         float cos = (float)Math.cos(angle) * s;
         float sin = (float)Math.sin(angle) * s;
         buf.vertex(m, -cos, -sin, 0.0F).color(r, g, b, a);
         buf.vertex(m, cos, sin, 0.0F).color(r, g, b, a);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawEmber(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float w = s * 0.5F;
      buf.vertex(m, 0.0F, s, 0.0F).color(255, 240, 150, a);
      buf.vertex(m, -w, -s * 0.5F, 0.0F).color(r, g, b, (int)(a * 0.7F));
      buf.vertex(m, w, -s * 0.5F, 0.0F).color(r, g, b, (int)(a * 0.7F));
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private WorldParticles.AmbientParticle spawnParticle(boolean randomAge) {
      Vec3d center = mc.player.getPos();
      Vec3d pos = center.add((this.random.nextDouble() - 0.5) * 26.0, this.random.nextDouble() * 9.0 - 2.0, (this.random.nextDouble() - 0.5) * 26.0);
      Vec3d vel = new Vec3d((this.random.nextDouble() - 0.5) * 0.015, (this.random.nextDouble() - 0.5) * 0.01, (this.random.nextDouble() - 0.5) * 0.015);
      int maxAge = 120 + this.random.nextInt(160);
      int age = randomAge ? this.random.nextInt(maxAge) : 0;
      float rot = this.random.nextFloat() * 360.0F;
      float rotSpd = (this.random.nextFloat() - 0.5F) * 2.5F;
      float scaleMul = 0.7F + this.random.nextFloat() * 0.6F;
      double seed = this.random.nextDouble() * 100.0;
      return new WorldParticles.AmbientParticle(pos, vel, age, maxAge, rot, rotSpd, scaleMul, seed);
   }

   private static class AmbientParticle {
      Vec3d pos;
      Vec3d velocity;
      int age;
      int maxAge;
      float rotation;
      float rotSpeed;
      float scaleMultiplier;
      double seed;

      AmbientParticle(Vec3d pos, Vec3d velocity, int age, int maxAge, float rotation, float rotSpeed, float scaleMultiplier, double seed) {
         this.pos = pos;
         this.velocity = velocity;
         this.age = age;
         this.maxAge = maxAge;
         this.rotation = rotation;
         this.rotSpeed = rotSpeed;
         this.scaleMultiplier = scaleMultiplier;
         this.seed = seed;
      }
   }
}
