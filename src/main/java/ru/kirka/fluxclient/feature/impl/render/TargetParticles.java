package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;

public class TargetParticles extends Module {
   public final ModeSetting shape = new ModeSetting(
      "Форма", "Внешний вид вылетающих частиц", "Сердечки", "Сердечки", "Доллары", "Снежинки", "Кристаллы", "Звёзды"
   );
   public final NumberSetting count = new NumberSetting("Количество", "Частиц за один удар", 14.0F, 4.0F, 30.0F, 1.0F);
   public final NumberSetting speed = new NumberSetting("Скорость разлета", "Сила импульса при ударе", 1.8F, 0.5F, 4.0F, 0.1F);
   public final NumberSetting size = new ModeSetting("Размер", "Масштаб частиц", "0.18").is("0.18")
      ? new NumberSetting("Размер", "Масштаб частиц", 0.18F, 0.08F, 0.45F, 0.02F)
      : new NumberSetting("Размер", "Масштаб частиц", 0.18F, 0.08F, 0.45F, 0.02F);
   public final NumberSetting lifetime = new NumberSetting("Время жизни", "Длительность анимации (сек)", 0.75F, 0.3F, 2.0F, 0.05F);
   public final ModeSetting colorMode = new ModeSetting("Цвет", "Цветовая схема", "Тема клиента", "Тема клиента", "Пользовательский", "Радуга", "Золото");
   public final ColorSetting customColor = new ColorSetting("Свой цвет", "Цвет частиц", new Color(236, 72, 153, 245));
   public final BooleanSetting physics = new BooleanSetting("Гравитация", "Падение и затухание по физике", true);
   private static final List<TargetParticles.HitParticle> particles = new ArrayList<>();
   private final Map<LivingEntity, Integer> prevHurtTimes = new HashMap<>();
   private static final Random random = new Random();

   public TargetParticles() {
      super("TargetParticles", "Кастомные частицы (сердечки, доллары, снежинки) при ударе по цели", Category.RENDER, -1);
      this.registerSetting(this.shape);
      this.registerSetting(this.count);
      this.registerSetting(this.speed);
      this.registerSetting(this.size);
      this.registerSetting(this.lifetime);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.customColor);
      this.registerSetting(this.physics);
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && living != mc.player && living.isAlive()) {
               int currentHurt = living.hurtTime;
               int prevHurt = this.prevHurtTimes.getOrDefault(living, 0);
               this.prevHurtTimes.put(living, currentHurt);
               if (currentHurt >= 9 && prevHurt < 9 && mc.player.distanceTo(living) <= 7.0F) {
                  this.spawnBurst(living);
               }
            }
         }
      } else {
         this.prevHurtTimes.clear();
      }
   }

   public static void spawnAt(Vec3d pos, int numSparks) {
      for (int i = 0; i < numSparks; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double elevation = (random.nextDouble() - 0.2) * 1.2;
         double spd = 0.08 + random.nextDouble() * 0.12;
         Vec3d vel = new Vec3d(Math.cos(angle) * spd, elevation * spd + 0.06, Math.sin(angle) * spd);
         float rot = random.nextFloat() * 360.0F;
         float rotSpeed = (random.nextFloat() - 0.5F) * 18.0F;
         particles.add(new TargetParticles.HitParticle(pos, vel, rot, rotSpeed, System.currentTimeMillis()));
      }
   }

   private void spawnBurst(LivingEntity target) {
      Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
      int num = Math.round(this.count.get());
      float force = this.speed.get();

      for (int i = 0; i < num; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double elevation = (random.nextDouble() - 0.15) * 1.5;
         double spd = (0.05 + random.nextDouble() * 0.12) * force;
         Vec3d vel = new Vec3d(Math.cos(angle) * spd, elevation * spd + 0.04 * force, Math.sin(angle) * spd);
         float rot = random.nextFloat() * 360.0F;
         float rotSpeed = (random.nextFloat() - 0.5F) * 15.0F;
         particles.add(new TargetParticles.HitParticle(center, vel, rot, rotSpeed, System.currentTimeMillis()));
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (!particles.isEmpty() && mc.player != null) {
         long now = System.currentTimeMillis();
         long maxLife = (long)(this.lifetime.get() * 1000.0F);
         particles.removeIf(px -> now - px.spawnTime > maxLife);
         if (!particles.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 1);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            float baseScale = this.size.get();
            boolean hasGrav = this.physics.get();
            String currentShape = this.shape.get();

            for (TargetParticles.HitParticle p : particles) {
               float progress = (float)(now - p.spawnTime) / (float)maxLife;
               if (!(progress > 1.0F)) {
                  p.pos = p.pos.add(p.vel);
                  if (hasGrav) {
                     p.vel = new Vec3d(p.vel.x * 0.95, p.vel.y - 0.0035, p.vel.z * 0.95);
                  }

                  p.rotation = p.rotation + p.rotSpeed;
                  float curScale = baseScale * (1.0F - progress * 0.35F);
                  int alpha = (int)((1.0F - progress) * 240.0F);
                  Color c = this.resolveColor(progress);
                  int r = c.getRed();
                  int g = c.getGreen();
                  int b = c.getBlue();
                  matrices.push();
                  matrices.translate(p.pos.x - cameraPos.x, p.pos.y - cameraPos.y, p.pos.z - cameraPos.z);
                  matrices.multiply(camRot);
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));
                  Matrix4f m = matrices.peek().getPositionMatrix();
                  switch (currentShape) {
                     case "Сердечки":
                        this.drawHeart(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Доллары":
                        this.drawDollar(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Снежинки":
                        this.drawSnowflake(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     case "Звёзды":
                        this.drawStar(tessellator, m, curScale, r, g, b, alpha);
                        break;
                     default:
                        this.drawCrystal(tessellator, m, curScale, r, g, b, alpha);
                  }

                  matrices.pop();
               }
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private Color resolveColor(float progress) {
      String var2 = this.colorMode.get();

      return switch (var2) {
         case "Тема клиента" -> ImGuiTheme.primaryColor();
         case "Радуга" -> {
            float hue = ((float)(System.currentTimeMillis() % 3600L) / 3600.0F + progress) % 1.0F;
            yield new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
         }
         case "Золото" -> new Color(245, 158, 11, 245);
         default -> (Color)this.customColor.get();
      };
   }

   private void drawHeart(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float half = s * 0.5F;
      buf.vertex(m, 0.0F, -half * 1.1F, 0.0F).color(r, g, b, a);
      buf.vertex(m, -half, 0.1F * s, 0.0F).color(r, g, b, a);
      buf.vertex(m, half, 0.1F * s, 0.0F).color(r, g, b, a);
      buf.vertex(m, -half * 0.5F, half * 0.85F, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -half, 0.1F * s, 0.0F).color(r, g, b, a);
      buf.vertex(m, 0.0F, 0.15F * s, 0.0F).color(r, g, b, a);
      buf.vertex(m, half * 0.5F, half * 0.85F, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, 0.0F, 0.15F * s, 0.0F).color(r, g, b, a);
      buf.vertex(m, half, 0.1F * s, 0.0F).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawDollar(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float h = s * 0.55F;
      float w = s * 0.35F;
      buf.vertex(m, -w * 0.15F, h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, w * 0.15F, h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, w * 0.15F, -h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -w * 0.15F, h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, w * 0.15F, -h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -w * 0.15F, -h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -w, h * 0.8F, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, h * 0.8F, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, h * 0.2F, 0.0F).color(r, g, b, a);
      buf.vertex(m, -w, -h * 0.2F, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, -h * 0.2F, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, -h * 0.8F, 0.0F).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawSnowflake(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      float rad = s * 0.65F;

      for (int i = 0; i < 3; i++) {
         double angle = (Math.PI / 3) * i;
         float cos = (float)Math.cos(angle) * rad;
         float sin = (float)Math.sin(angle) * rad;
         buf.vertex(m, -cos, -sin, 0.0F).color(r, g, b, a);
         buf.vertex(m, cos, sin, 0.0F).color(r, g, b, a);
         float bCos = (float)Math.cos(angle + 0.6) * (rad * 0.45F);
         float bSin = (float)Math.sin(angle + 0.6) * (rad * 0.45F);
         buf.vertex(m, cos * 0.6F, sin * 0.6F, 0.0F).color(255, 255, 255, a);
         buf.vertex(m, cos * 0.6F + bCos, sin * 0.6F + bSin, 0.0F).color(r, g, b, a);
         buf.vertex(m, -cos * 0.6F, -sin * 0.6F, 0.0F).color(255, 255, 255, a);
         buf.vertex(m, -cos * 0.6F - bCos, -sin * 0.6F - bSin, 0.0F).color(r, g, b, a);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawCrystal(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float h = s * 0.65F;
      float w = s * 0.45F;
      buf.vertex(m, 0.0F, h, 0.0F).color(255, 255, 255, a);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, a);
      buf.vertex(m, 0.0F, -h, 0.0F).color(r, g, b, a);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, a);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, a);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawStar(Tessellator tess, Matrix4f m, float s, int r, int g, int b, int a) {
      BufferBuilder buf = tess.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buf.vertex(m, 0.0F, 0.0F, 0.0F).color(255, 255, 255, a);
      float outerR = s * 0.65F;
      float innerR = s * 0.26F;

      for (int i = 0; i <= 10; i++) {
         double angle = (Math.PI / 5) * i - (Math.PI / 2);
         float currentR = i % 2 == 0 ? outerR : innerR;
         float x = (float)(Math.cos(angle) * currentR);
         float y = (float)(Math.sin(angle) * currentR);
         buf.vertex(m, x, y, 0.0F).color(r, g, b, a);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private static class HitParticle {
      Vec3d pos;
      Vec3d vel;
      float rotation;
      float rotSpeed;
      long spawnTime;

      HitParticle(Vec3d pos, Vec3d vel, float rotation, float rotSpeed, long spawnTime) {
         this.pos = pos;
         this.vel = vel;
         this.rotation = rotation;
         this.rotSpeed = rotSpeed;
         this.spawnTime = spawnTime;
      }
   }
}
