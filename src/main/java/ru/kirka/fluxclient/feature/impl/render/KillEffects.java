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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.EntityDeathEvent;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.animation.Animation;
import ru.kirka.fluxclient.render.animation.Easing;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.RenderUtility;

public class KillEffects extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Тип эффекта при убийстве", "Молния", "Молния", "Взрыв", "Тотем", "Сердца");
   public final ColorSetting color = new ColorSetting("Цвет", "Цвет молнии и эффекта", new Color(0, 190, 255, 230));
   public final BooleanSetting sound = new BooleanSetting("Звук", "Воспроизводить звук при смерти врага", true);
   private final List<KillEffects.Lightning> lightnings = new ArrayList<>();
   private static final Identifier BLOOM_TEXTURE = Identifier.of("rockstar", "textures/bloom.png");
   private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
      if (mc.world != null && mc.player != null) {
         if (event.getEntity() != mc.player) {
            Vec3d deathPos = event.getEntity().getPos();
            String currentMode = this.mode.get();
            if ("Молния".equals(currentMode)) {
               Color c = this.color.get();
               this.lightnings.add(new KillEffects.Lightning(deathPos, new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())));
               if (this.sound.get()) {
                  mc.world
                     .playSound(mc.player, deathPos.x, deathPos.y, deathPos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0F, 1.0F);
               }
            } else if ("Взрыв".equals(currentMode)) {
               for (int i = 0; i < 20; i++) {
                  mc.world
                     .addParticle(
                        ParticleTypes.EXPLOSION,
                        deathPos.x,
                        deathPos.y + 1.0,
                        deathPos.z,
                        ThreadLocalRandom.current().nextDouble(-0.2, 0.2),
                        ThreadLocalRandom.current().nextDouble(0.0, 0.3),
                        ThreadLocalRandom.current().nextDouble(-0.2, 0.2)
                     );
               }

               if (this.sound.get()) {
                  mc.world
                     .playSound(
                        mc.player,
                        deathPos.x,
                        deathPos.y,
                        deathPos.z,
                        (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                     );
               }
            } else if ("Тотем".equals(currentMode)) {
               for (int i = 0; i < 30; i++) {
                  mc.world
                     .addParticle(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        deathPos.x,
                        deathPos.y + 1.0,
                        deathPos.z,
                        ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                        ThreadLocalRandom.current().nextDouble(0.1, 0.5),
                        ThreadLocalRandom.current().nextDouble(-0.3, 0.3)
                     );
               }

               if (this.sound.get()) {
                  mc.world.playSound(mc.player, deathPos.x, deathPos.y, deathPos.z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
               }
            } else if ("Сердца".equals(currentMode)) {
               for (int i = 0; i < 15; i++) {
                  mc.world
                     .addParticle(
                        ParticleTypes.HEART,
                        deathPos.x,
                        deathPos.y + 1.0,
                        deathPos.z,
                        ThreadLocalRandom.current().nextDouble(-0.2, 0.2),
                        ThreadLocalRandom.current().nextDouble(0.1, 0.3),
                        ThreadLocalRandom.current().nextDouble(-0.2, 0.2)
                     );
               }
            }
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!this.lightnings.isEmpty() && mc.gameRenderer != null) {
         MatrixStack ms = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         ms.push();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (KillEffects.Lightning lightning : this.lightnings) {
            lightning.render(builder, ms, camera);
            if (lightning.animation.getValue() == 1.0F) {
               lightning.showing = false;
            }
         }

         BuiltBuffer builtBuffer = builder.endNullable();
         if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
         }

         RenderSystem.depthMask(true);
         RenderSystem.setShaderTexture(0, 0);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.disableDepthTest();
         ms.pop();
         this.lightnings.removeIf(l -> !l.showing && l.animation.getValue() == 0.0F);
      }
   };

   public KillEffects() {
      super("KillEffects", "Визуальные эффекты (молния, взрыв, тотем) на месте убийства сущности", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.color);
      this.registerSetting(this.sound);
   }

   static class Lightning {
      final Vec3d pos;
      final ColorRGBA color;
      boolean showing = true;
      final Animation animation = new Animation(500L, 0.0F, Easing.BOUNCE_IN);
      final List<Vec3d> poses = new ArrayList<>();

      public Lightning(Vec3d pos, ColorRGBA color) {
         this.pos = pos;
         this.color = color;
         Vec3d lastPos = pos;

         for (int i = 0; i < 200; i++) {
            double rx = ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
            double rz = ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
            lastPos = lastPos.add(rx, 0.25, rz);
            this.poses.add(lastPos);
         }
      }

      void render(BufferBuilder builder, MatrixStack ms, Camera camera) {
         this.animation.update(this.showing);
         float animValue = this.animation.getValue();

         for (Vec3d p : this.poses) {
            float size = (float)(2.0 + 5.0 * (p.y - this.pos.y) / 50.0);
            ms.push();
            RenderUtility.prepareMatrices(ms, p);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms, builder, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, this.color.withAlpha(255.0F * animValue * 0.4F)
            );
            ms.pop();
         }
      }
   }
}
