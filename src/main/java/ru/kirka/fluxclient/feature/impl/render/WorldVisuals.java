package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.event.impl.TickEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.animation.Animation;
import ru.kirka.fluxclient.render.animation.Easing;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.Draw3DUtility;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.RenderUtility;
import ru.kirka.fluxclient.render.draw.Utils;

public class WorldVisuals extends Module {
   public final ColorSetting color = new ColorSetting("Цвет", "Цвет неоновых кубов", new Color(130, 90, 255, 230));
   public final NumberSetting density = new NumberSetting("Количество", "Плотность кубов в мире", 60.0F, 10.0F, 120.0F, 5.0F);
   public final NumberSetting size = new NumberSetting("Размер", "Базовый размер кубов", 0.25F, 0.05F, 0.6F, 0.05F);
   public final NumberSetting speed = new NumberSetting("Скорость", "Скорость вращения и движения", 1.0F, 0.2F, 3.0F, 0.1F);
   private final List<WorldVisuals.CubeParticle> particles = new ArrayList<>();
   private static final Identifier BLOOM_TEXTURE = Identifier.of("rockstar", "textures/bloom.png");
   private static final Box CUBE_BOX = new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
   private final EventListener<TickEvent> onTick = event -> {
      if (mc.player != null) {
         long now = System.currentTimeMillis();
         this.particles.removeIf(p -> p.alpha.getValue() == 0.0F && now - p.spawnTime > p.liveDurationMs);

         for (WorldVisuals.CubeParticle p : this.particles) {
            p.tick();
         }

         int targetCount = Math.round(this.density.get());
         float spd = this.speed.get();
         float sz = this.size.get();

         while (this.particles.size() < targetCount) {
            Vec3d pPos = mc.player.getPos();
            Vec3d pos = pPos.add(
               ThreadLocalRandom.current().nextDouble(-20.0, 20.0),
               ThreadLocalRandom.current().nextDouble(0.0, 6.0),
               ThreadLocalRandom.current().nextDouble(-20.0, 20.0)
            );
            Vec3d motion = new Vec3d(
                  ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                  ThreadLocalRandom.current().nextDouble(0.0, 2.0),
                  ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
               )
               .multiply(spd);
            Vec3d rotMotion = new Vec3d(
                  ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                  ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                  ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
               )
               .multiply(spd);
            long life = ThreadLocalRandom.current().nextLong(2000L, 5000L);
            float curSz = sz * (float)ThreadLocalRandom.current().nextDouble(0.7, 1.3);
            this.particles.add(new WorldVisuals.CubeParticle(pos, Vec3d.ZERO, motion, rotMotion, life, curSz));
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!this.particles.isEmpty() && mc.gameRenderer != null) {
         MatrixStack ms = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         float tickDelta = event.getTickDelta();
         long now = System.currentTimeMillis();
         Color c = this.color.get();
         ColorRGBA baseColor = new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
         ms.push();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder bloomBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (WorldVisuals.CubeParticle p : this.particles) {
            Vec3d pos = Utils.getInterpolatedPos(p.prev, p.pos, tickDelta);
            float bigSize = 4.0F * p.size;
            ms.push();
            RenderUtility.prepareMatrices(ms, pos);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms,
               bloomBuilder,
               (double)(-bigSize / 2.0F),
               (double)(-bigSize / 2.0F),
               0.0,
               (double)bigSize,
               (double)bigSize,
               baseColor.withAlpha(255.0F * p.alpha.getValue() * 0.4F)
            );
            ms.pop();
         }

         BuiltBuffer builtBloom = bloomBuilder.endNullable();
         if (builtBloom != null) {
            BufferRenderer.drawWithGlobalProgram(builtBloom);
         }

         RenderSystem.depthMask(true);
         RenderSystem.setShaderTexture(0, 0);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.disableDepthTest();
         ms.pop();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder linesBuffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);

         for (WorldVisuals.CubeParticle p : this.particles) {
            boolean alive = now - p.spawnTime < p.liveDurationMs;
            p.alpha.update(alive);
            Vec3d pos = Utils.getInterpolatedPos(p.prev, p.pos, tickDelta);
            Vec3d rot = Utils.getInterpolatedPos(p.prevRot, p.rotate, tickDelta);
            ms.push();
            ms.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            ms.multiply(new Quaternionf().rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
            ms.scale(p.size, p.size, p.size);
            Draw3DUtility.renderBoxInternalDiagonals(ms, linesBuffer, CUBE_BOX, baseColor.withAlpha(255.0F * p.alpha.getValue() * 0.35F));
            Draw3DUtility.renderOutlinedBox(ms, linesBuffer, CUBE_BOX, baseColor.withAlpha(220.0F * p.alpha.getValue()));
            ms.pop();
         }

         BuiltBuffer builtLines = linesBuffer.endNullable();
         if (builtLines != null) {
            BufferRenderer.drawWithGlobalProgram(builtLines);
         }

         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   };

   public WorldVisuals() {
      super("WorldVisuals", "Парящие неоновые 3D кубы в окружающем мире с неоновым свечением", Category.RENDER, -1);
      this.registerSetting(this.color);
      this.registerSetting(this.density);
      this.registerSetting(this.size);
      this.registerSetting(this.speed);
   }

   @Override
   protected void onEnable() {
      this.particles.clear();
   }

   private static class CubeParticle {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveDurationMs;
      final long spawnTime;
      final float size;
      final Animation alpha = new Animation(600L, Easing.FIGMA_EASE_IN_OUT);

      CubeParticle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveDurationMs, float size) {
         this.pos = pos;
         this.rotate = rotate;
         this.motion = motion.multiply(0.04);
         this.rotateMotion = rotateMotion.multiply(0.04);
         this.liveDurationMs = liveDurationMs;
         this.spawnTime = System.currentTimeMillis();
         this.size = size;
         this.prevRot = rotate;
         this.prev = pos;
         this.alpha.setDuration(800L);
      }

      void tick() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }
   }
}
