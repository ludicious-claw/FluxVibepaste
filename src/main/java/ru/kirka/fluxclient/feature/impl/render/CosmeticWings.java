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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
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

public class CosmeticWings extends Module {
   public final ModeSetting style = new ModeSetting("Стиль", "Форма и модель крыльев", "Дракон", "Дракон", "Ангел", "Флюкс Неон", "Кристалл");
   public final NumberSetting scale = new NumberSetting("Размер", "Масштаб крыльев", 1.0F, 0.4F, 2.2F, 0.05F);
   public final NumberSetting flapSpeed = new NumberSetting("Скорость взмахов", "Частота анимации взмахов", 1.2F, 0.3F, 3.0F, 0.1F);
   public final ModeSetting colorMode = new ModeSetting("Окраска", "Цветовой режим", "Градиент", "Градиент", "Тема клиента", "Радуга", "Неон");
   public final ColorSetting color1 = new ColorSetting("Цвет 1", "Основной цвет (костяк/основание)", new Color(225, 29, 72, 255));
   public final ColorSetting color2 = new ColorSetting("Цвет 2", "Вторичный цвет (перья/кончики)", new Color(147, 51, 234, 255));
   public final BooleanSetting glow = new BooleanSetting("Неоновое свечение", "Яркое аддитивное свечение", true);
   public final BooleanSetting trailParticles = new BooleanSetting("Частицы с крыльев", "Шлейф искр при движении", true);
   public final BooleanSetting renderInFirstPerson = new BooleanSetting("От 1-го лица", "Видеть крылья в виде от 1 лица", false);
   public final BooleanSetting renderOnOthers = new BooleanSetting("На других игроках", "Отображать крылья на окружающих игроках", false);
   private final List<CosmeticWings.WingParticle> wingParticles = new ArrayList<>();
   private long lastParticleTime = 0L;

   public CosmeticWings() {
      super("CosmeticWings", "Анимированные 3D крылья за спиной игрока с физикой движения", Category.RENDER, -1);
      this.registerSetting(this.style);
      this.registerSetting(this.scale);
      this.registerSetting(this.flapSpeed);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.color1);
      this.registerSetting(this.color2);
      this.registerSetting(this.glow);
      this.registerSetting(this.trailParticles);
      this.registerSetting(this.renderInFirstPerson);
      this.registerSetting(this.renderOnOthers);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.world != null && mc.player != null) {
         boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
         if (!isFirstPerson || this.renderInFirstPerson.get()) {
            this.renderWingsOnPlayer(matrices, mc.player, cameraPos);
         }

         if (this.renderOnOthers.get()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (player != mc.player) {
                  this.renderWingsOnPlayer(matrices, player, cameraPos);
               }
            }
         }

         if (this.trailParticles.get()) {
            this.renderParticles(matrices, cameraPos, camRot);
         }
      }
   }

   private void renderWingsOnPlayer(MatrixStack matrices, PlayerEntity player, Vec3d cameraPos) {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
      double px = MathHelper.lerp(tickDelta, player.prevX, player.getX());
      double py = MathHelper.lerp(tickDelta, player.prevY, player.getY());
      double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
      float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
      double spineY = py + (player.isSneaking() ? 1.05 : 1.25);
      matrices.push();
      matrices.translate(px - cameraPos.x, spineY - cameraPos.y, pz - cameraPos.z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw + 180.0F));
      if (player.isSneaking()) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F));
         matrices.translate(0.0, 0.0, 0.08);
      }

      float sc = this.scale.get();
      matrices.scale(sc, sc, sc);
      float time = (float)(System.currentTimeMillis() % 10000000L / 1000.0);
      float spd = this.flapSpeed.get();
      boolean moving = player.getVelocity().horizontalLengthSquared() > 0.001;
      boolean inAir = !player.isOnGround();
      boolean gliding = player.isGliding();
      float flapAngle;
      if (gliding) {
         flapAngle = (float)(Math.sin(time * 2.0 * spd) * 6.0 + 48.0);
      } else if (inAir) {
         flapAngle = (float)(Math.sin(time * 9.0 * spd) * 35.0 + 25.0);
      } else if (moving) {
         float sprintMul = player.isSprinting() ? 1.4F : 1.0F;
         flapAngle = (float)(Math.sin(time * 7.0 * spd * sprintMul) * (26.0F * sprintMul) + 16.0);
      } else {
         flapAngle = (float)(Math.sin(time * 2.5 * spd) * 10.0 + 8.0);
      }

      Color c1 = this.getActiveColor(this.color1.get(), 0.0F);
      Color c2 = this.getActiveColor(this.color2.get(), 0.5F);
      this.setupRenderState();
      matrices.push();
      matrices.translate(0.08, 0.0, -0.05);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flapAngle));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-8.0F));
      this.drawSingleWing(matrices, c1, c2, false);
      matrices.pop();
      matrices.push();
      matrices.translate(-0.08, 0.0, -0.05);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-flapAngle));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F));
      this.drawSingleWing(matrices, c1, c2, true);
      matrices.pop();
      this.endRenderState();
      matrices.pop();
      if (this.trailParticles.get() && (moving || inAir)) {
         this.spawnWingParticle(player, px, spineY, pz, bodyYaw, flapAngle, c2);
      }
   }

   private Color getActiveColor(Color base, float offset) {
      String cm = this.colorMode.get();
      if (cm.equals("Радуга")) {
         float hue = ((float)(System.currentTimeMillis() % 4000L) / 4000.0F + offset) % 1.0F;
         return new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
      } else {
         return cm.equals("Тема клиента") ? ImGuiTheme.primaryColor() : base;
      }
   }

   private void setupRenderState() {
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      if (this.glow.get()) {
         RenderSystem.blendFunc(770, 1);
      } else {
         RenderSystem.blendFunc(770, 771);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
   }

   private void endRenderState() {
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void drawSingleWing(MatrixStack matrices, Color c1, Color c2, boolean mirror) {
      Matrix4f m = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      float sign = mirror ? -1.0F : 1.0F;
      int r1 = c1.getRed();
      int g1 = c1.getGreen();
      int b1 = c1.getBlue();
      int a1 = c1.getAlpha();
      int r2 = c2.getRed();
      int g2 = c2.getGreen();
      int b2 = c2.getBlue();
      int a2 = (int)(c2.getAlpha() * 0.75F);
      String st = this.style.get();
      BufferBuilder b = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      if (st.equals("Ангел")) {
         float rootX = 0.0F;
         float rootY = 0.0F;
         float rootZ = 0.0F;
         float elbowX = sign * 0.65F;
         float elbowY = 0.45F;
         float elbowZ = -0.1F;
         float tipX = sign * 1.15F;
         float tipY = 0.25F;
         float tipZ = -0.2F;
         float feather1X = sign * 0.85F;
         float feather1Y = -0.35F;
         float feather1Z = -0.15F;
         float feather2X = sign * 0.55F;
         float feather2Y = -0.45F;
         float feather2Z = -0.1F;
         float feather3X = sign * 0.25F;
         float feather3Y = -0.3F;
         float feather3Z = -0.05F;
         this.addTri(b, m, rootX, rootY, rootZ, elbowX, elbowY, elbowZ, tipX, tipY, tipZ, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, tipX, tipY, tipZ, feather1X, feather1Y, feather1Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, feather1X, feather1Y, feather1Z, feather2X, feather2Y, feather2Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, rootX, rootY, rootZ, elbowX, elbowY, elbowZ, feather3X, feather3Y, feather3Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, feather2X, feather2Y, feather2Z, feather3X, feather3Y, feather3Z, r1, g1, b1, a1, r2, g2, b2, a2);
      } else if (st.equals("Флюкс Неон")) {
         float rootX = 0.0F;
         float rootY = 0.0F;
         float rootZ = 0.0F;
         float blade1X = sign * 0.95F;
         float blade1Y = 0.55F;
         float blade1Z = -0.1F;
         float blade2X = sign * 1.25F;
         float blade2Y = 0.15F;
         float blade2Z = -0.18F;
         float blade3X = sign * 0.75F;
         float blade3Y = -0.35F;
         float blade3Z = -0.12F;
         this.addTri(b, m, rootX, rootY, rootZ, blade1X, blade1Y, blade1Z, blade1X * 0.7F, blade1Y * 0.7F - 0.1F, blade1Z, r1, g1, b1, a1, 255, 255, 255, 255);
         this.addTri(b, m, rootX, rootY, rootZ, blade2X, blade2Y, blade2Z, blade2X * 0.6F, blade2Y * 0.6F - 0.15F, blade2Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, rootX, rootY, rootZ, blade3X, blade3Y, blade3Z, blade3X * 0.5F, blade3Y * 0.5F - 0.1F, blade3Z, r1, g1, b1, a1, r2, g2, b2, a2);
      } else {
         float rootX = 0.0F;
         float rootY = 0.0F;
         float rootZ = 0.0F;
         float elbowX = sign * 0.55F;
         float elbowY = 0.5F;
         float elbowZ = -0.08F;
         float tip1X = sign * 1.2F;
         float tip1Y = 0.7F;
         float tip1Z = -0.15F;
         float tip2X = sign * 1.1F;
         float tip2Y = 0.1F;
         float tip2Z = -0.18F;
         float tip3X = sign * 0.85F;
         float tip3Y = -0.4F;
         float tip3Z = -0.12F;
         float innerX = sign * 0.35F;
         float innerY = -0.25F;
         float innerZ = -0.05F;
         this.addTri(b, m, rootX, rootY, rootZ, elbowX, elbowY, elbowZ, tip1X, tip1Y, tip1Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, tip1X, tip1Y, tip1Z, tip2X, tip2Y, tip2Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, tip2X, tip2Y, tip2Z, tip3X, tip3Y, tip3Z, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, rootX, rootY, rootZ, elbowX, elbowY, elbowZ, innerX, innerY, innerZ, r1, g1, b1, a1, r2, g2, b2, a2);
         this.addTri(b, m, elbowX, elbowY, elbowZ, tip3X, tip3Y, tip3Z, innerX, innerY, innerZ, r1, g1, b1, a1, r2, g2, b2, a2);
      }

      BufferRenderer.drawWithGlobalProgram(b.end());
   }

   private void addTri(
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
      int r1,
      int g1,
      int b1,
      int a1,
      int r2,
      int g2,
      int b2,
      int a2
   ) {
      b.vertex(m, x1, y1, z1).color(r1, g1, b1, a1);
      b.vertex(m, x2, y2, z2).color(r2, g2, b2, a2);
      b.vertex(m, x3, y3, z3).color(r2, g2, b2, a2);
      b.vertex(m, x1, y1, z1).color(r1, g1, b1, a1);
      b.vertex(m, x3, y3, z3).color(r2, g2, b2, a2);
      b.vertex(m, x2, y2, z2).color(r2, g2, b2, a2);
   }

   private void spawnWingParticle(PlayerEntity player, double px, double py, double pz, float bodyYaw, float flapAngle, Color col) {
      long now = System.currentTimeMillis();
      if (now - this.lastParticleTime >= 60L) {
         this.lastParticleTime = now;
         double radYaw = Math.toRadians(-bodyYaw + 180.0);
         double radFlap = Math.toRadians(flapAngle);
         double offX = Math.cos(radYaw) * 0.95 - Math.sin(radYaw) * Math.sin(radFlap) * 0.5;
         double offZ = Math.sin(radYaw) * 0.95 + Math.cos(radYaw) * Math.sin(radFlap) * 0.5;
         double offY = 0.35 + Math.sin(radFlap) * 0.2;
         this.wingParticles.add(new CosmeticWings.WingParticle(new Vec3d(px + offX, py + offY, pz + offZ), col, now));
      }
   }

   private void renderParticles(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      long now = System.currentTimeMillis();
      this.wingParticles.removeIf(px -> now - px.spawnTime > 800L);
      if (!this.wingParticles.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Tessellator tessellator = Tessellator.getInstance();

         for (CosmeticWings.WingParticle p : this.wingParticles) {
            float progress = (float)(now - p.spawnTime) / 800.0F;
            int alpha = (int)((1.0F - progress) * 230.0F);
            float sz = (1.0F - progress * 0.6F) * 0.08F;
            matrices.push();
            matrices.translate(p.pos.x - cameraPos.x, p.pos.y - cameraPos.y - progress * 0.15, p.pos.z - cameraPos.z);
            matrices.multiply(camRot);
            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder b = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            Color c = p.color;
            b.vertex(m, 0.0F, 0.0F, 0.0F).color(255, 255, 255, alpha);
            b.vertex(m, -sz, -sz, 0.0F).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            b.vertex(m, sz, -sz, 0.0F).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            b.vertex(m, sz, sz, 0.0F).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            b.vertex(m, -sz, sz, 0.0F).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            b.vertex(m, -sz, -sz, 0.0F).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            BufferRenderer.drawWithGlobalProgram(b.end());
            matrices.pop();
         }

         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private record WingParticle(Vec3d pos, Color color, long spawnTime) {
   }
}
