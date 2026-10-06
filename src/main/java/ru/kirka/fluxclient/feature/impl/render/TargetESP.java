package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import imgui.ImColor;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.render.FluxWorldRender;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.CrystalRenderer;
import ru.kirka.fluxclient.render.shaders.OrbitShader;
import ru.kirka.fluxclient.render.shaders.OrbitShaders;
import ru.kirka.fluxclient.util.TargetUtil;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;

public class TargetESP extends Module {
   private final ModeSetting mode = new ModeSetting("Режим", "Режим отображения", "Орбита", "Призраки", "Круг", "Квадратик", "Орбита", "Кристаллы");
   private final NumberSetting ghostSpeed = new NumberSetting("Скорость анимации", "Скорость призраков", 1.5F, 0.5F, 5.0F, 0.1F);
   private final NumberSetting ghostParticleSize = new NumberSetting("Размер частиц", "Размер частиц призраков", 0.25F, 0.05F, 0.5F, 0.01F);
   private final NumberSetting ghostCount = new NumberSetting("Количество призраков", "Сколько орбит-призраков", 4.0F, 2.0F, 6.0F, 1.0F);
   private final NumberSetting circleSpeed = new NumberSetting("Скорость анимации", "Скорость кольца", 1.5F, 0.5F, 5.0F, 0.1F);
   private final NumberSetting squareSpeed = new NumberSetting("Скорость анимации", "Скорость вращения текстуры", 2.5F, 0.5F, 5.0F, 0.1F);
   private final NumberSetting squareSize = new NumberSetting("Размер квадратика", "Размер текстуры-прицела", 1.4F, 0.5F, 2.0F, 0.1F);
   private final ModeSetting orbitShape = new ModeSetting("Форма", "Форма фигур орбиты", "Стрелки", "Стрелки", "Ромбы", "Кубы");
   private final NumberSetting orbitSpeed = new NumberSetting("Скорость анимации", "Скорость орбиты", 1.5F, 0.5F, 5.0F, 0.1F);
   private final NumberSetting figuresPerRing = new NumberSetting("Фигур по кругу", "Фигур на одном слое", 3.0F, 2.0F, 8.0F, 1.0F);
   private final NumberSetting orbitLayerCount = new NumberSetting("Слоёв по высоте", "Слоёв орбиты", 3.0F, 2.0F, 5.0F, 1.0F);
   private final NumberSetting orbitVerticalSpread = new NumberSetting("Отступ между слоями", "Высота орбиты", 1.0F, 0.3F, 2.0F, 0.05F);
   private final NumberSetting orbitRadius = new NumberSetting("Дистанция", "Радиус орбиты", 0.9F, 0.5F, 2.0F, 0.05F);
   private final NumberSetting orbitFigureSize = new NumberSetting("Размер фигур", "Размер фигур", 0.2F, 0.08F, 0.4F, 0.01F);
   private final BooleanSetting orbitRotationEnabled = new BooleanSetting("Вращение", "Вращать орбиту", true);
   private final BooleanSetting orbitShaderEnabled = new BooleanSetting("Шейдер", "GLSL-заливка фигур", true);
   private final ModeSetting orbitShaderType = new ModeSetting("Тип шейдера", "Фрагментный шейдер", "Небула", "Небула", "Звёзды", "Паутина", "Плазма");
   private final BooleanSetting orbitGlowEnabled = new BooleanSetting("Свечение", "Подсветка фигур", true);
   private final NumberSetting orbitGlowSize = new NumberSetting("Размер свечения", "Размер подсветки", 0.85F, 0.1F, 1.0F, 0.05F);
   private final NumberSetting orbitGlowOpacity = new NumberSetting("Прозрачность свечения", "Прозрачность подсветки", 0.25F, 0.1F, 1.0F, 0.05F);
   private final BooleanSetting useClientColor = new BooleanSetting("Цвет клиента", "Цвет темы клиента", true);
   private final ColorSetting customColor = new ColorSetting("Кастомный цвет", "Свой цвет ESP", Color.WHITE);
   private final BooleanSetting hitEffectEnabled = new BooleanSetting("Включить", "Эффект при ударе", true);
   private final BooleanSetting hitColorEnabled = new BooleanSetting("Изменять цвет", "Краснеть при уроне", true);
   private final ColorSetting hitColor = new ColorSetting("Цвет урона", "Цвет при ударе", new Color(255, 50, 50));
   private final BooleanSetting hitSpeedEnabled = new BooleanSetting("Ускорять анимацию", "Ускорение при ударе", true);
   private final NumberSetting hitSpeedMultiplier = new NumberSetting("Множитель ускорения", "Во сколько раз ускорить", 3.0F, 1.2F, 5.0F, 0.1F);
   private final NumberSetting hitEffectDuration = new NumberSetting("Длительность эффекта", "Длительность в секундах", 0.8F, 0.3F, 2.0F, 0.1F);
   private final BooleanSetting targetPlayers = new BooleanSetting("Игроки", "Подсвечивать игроков", true);
   private final BooleanSetting targetMobs = new BooleanSetting("Мобы", "Подсвечивать мобов", true);
   private final Map<LivingEntity, Animation> visibilityAnimations = new HashMap<>();
   private final Map<LivingEntity, Integer> previousHurtTimes = new HashMap<>();
   private final Map<LivingEntity, Long> hitTimestamps = new HashMap<>();
   private final Map<LivingEntity, Double> rotationDegrees = new HashMap<>();
   private final Map<LivingEntity, Double> animationPhase = new HashMap<>();
   private long lastFrameTimeMs = System.currentTimeMillis();

   public TargetESP() {
      super("TargetESP", "Отображает ESP вокруг цели", Category.RENDER, -1);
      this.ghostSpeed.visibleWhen(() -> this.mode.is("Призраки"));
      this.ghostParticleSize.visibleWhen(() -> this.mode.is("Призраки"));
      this.ghostCount.visibleWhen(() -> this.mode.is("Призраки"));
      this.circleSpeed.visibleWhen(() -> this.mode.is("Круг"));
      this.squareSpeed.visibleWhen(() -> this.mode.is("Квадратик"));
      this.squareSize.visibleWhen(() -> this.mode.is("Квадратик"));
      this.orbitShape.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitSpeed.visibleWhen(() -> this.mode.is("Орбита"));
      this.figuresPerRing.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitLayerCount.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitVerticalSpread.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitRadius.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitFigureSize.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitRotationEnabled.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitShaderEnabled.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitShaderType.visibleWhen(() -> this.mode.is("Орбита") && this.orbitShaderEnabled.get());
      this.orbitGlowEnabled.visibleWhen(() -> this.mode.is("Орбита"));
      this.orbitGlowSize.visibleWhen(() -> this.mode.is("Орбита") && this.orbitGlowEnabled.get());
      this.orbitGlowOpacity.visibleWhen(() -> this.mode.is("Орбита") && this.orbitGlowEnabled.get());
      this.customColor.visibleWhen(() -> !this.useClientColor.get());
      this.hitColorEnabled.visibleWhen(() -> this.hitEffectEnabled.get());
      this.hitColor.visibleWhen(() -> this.hitEffectEnabled.get() && this.hitColorEnabled.get());
      this.hitSpeedEnabled.visibleWhen(() -> this.hitEffectEnabled.get());
      this.hitSpeedMultiplier.visibleWhen(() -> this.hitEffectEnabled.get() && this.hitSpeedEnabled.get());
      this.hitEffectDuration.visibleWhen(() -> this.hitEffectEnabled.get() && this.hitSpeedEnabled.get());
      this.registerSetting(this.mode);
      this.registerSetting(this.ghostSpeed);
      this.registerSetting(this.ghostParticleSize);
      this.registerSetting(this.ghostCount);
      this.registerSetting(this.circleSpeed);
      this.registerSetting(this.squareSpeed);
      this.registerSetting(this.squareSize);
      this.registerSetting(this.orbitShape);
      this.registerSetting(this.orbitSpeed);
      this.registerSetting(this.figuresPerRing);
      this.registerSetting(this.orbitLayerCount);
      this.registerSetting(this.orbitVerticalSpread);
      this.registerSetting(this.orbitRadius);
      this.registerSetting(this.orbitFigureSize);
      this.registerSetting(this.orbitRotationEnabled);
      this.registerSetting(this.orbitShaderEnabled);
      this.registerSetting(this.orbitShaderType);
      this.registerSetting(this.orbitGlowEnabled);
      this.registerSetting(this.orbitGlowSize);
      this.registerSetting(this.orbitGlowOpacity);
      this.registerSetting(this.useClientColor);
      this.registerSetting(this.customColor);
      this.registerSetting(this.hitEffectEnabled);
      this.registerSetting(this.hitColorEnabled);
      this.registerSetting(this.hitColor);
      this.registerSetting(this.hitSpeedEnabled);
      this.registerSetting(this.hitSpeedMultiplier);
      this.registerSetting(this.hitEffectDuration);
      this.registerSetting(this.targetPlayers);
      this.registerSetting(this.targetMobs);
   }

   @Override
   protected void onDisable() {
      this.visibilityAnimations.clear();
      this.previousHurtTimes.clear();
      this.hitTimestamps.clear();
      this.rotationDegrees.clear();
      this.animationPhase.clear();
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.player != null && mc.world != null) {
         long now = this.lastFrameTimeMs;
         this.lastFrameTimeMs = System.currentTimeMillis();
         float frameDelta = Math.min((float)(this.lastFrameTimeMs - now) / 1000.0F, 0.1F);
         float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
         LivingEntity current = this.findTarget();
         if (current != null && this.isRenderableTarget(current)) {
            this.visibilityAnimations.computeIfAbsent(current, ex -> new Animation(500.0F, Easing.LINEAR)).setTarget(1.0F);
         }

         Set<LivingEntity> toRemove = new HashSet<>();

         for (Entry<LivingEntity, Animation> entry : this.visibilityAnimations.entrySet()) {
            LivingEntity entity = entry.getKey();
            if (!this.isRenderableTarget(entity)) {
               toRemove.add(entity);
            } else {
               Animation anim = entry.getValue();
               if (entity != current) {
                  anim.setTarget(0.0F);
               }

               anim.update();
               float alpha = anim.getValue();
               if (anim.isFinished() && !(alpha > 0.001F)) {
                  toRemove.add(entity);
               } else {
                  this.updateAnimationPhase(entity, frameDelta);
                  String var15 = this.mode.get();
                  switch (var15) {
                     case "Призраки":
                        this.renderGhosts(matrices, cameraPos, camRot, tickDelta, entity, alpha);
                        break;
                     case "Круг":
                        this.renderCircle(matrices, cameraPos, tickDelta, entity, alpha);
                        break;
                     case "Квадратик":
                        this.renderSquare(matrices, cameraPos, camRot, tickDelta, entity, alpha);
                        break;
                     case "Кристаллы":
                        this.renderCrystals(matrices, cameraPos, tickDelta, entity, alpha);
                        break;
                     case "Орбита":
                        this.renderOrbit(matrices, cameraPos, tickDelta, entity, alpha);
                        break;
                     default:
                        this.renderOrbit(matrices, cameraPos, tickDelta, entity, alpha);
                  }
               }
            }
         }

         for (LivingEntity e : toRemove) {
            this.visibilityAnimations.remove(e);
            this.rotationDegrees.remove(e);
            this.animationPhase.remove(e);
            this.previousHurtTimes.remove(e);
            this.hitTimestamps.remove(e);
         }
      }
   }

   private LivingEntity findTarget() {
      LivingEntity target = TargetUtil.getActiveTarget();
      return this.isRenderableTarget(target) ? target : null;
   }

   private boolean matchesTargetType(LivingEntity entity) {
      return entity != null && entity != mc.player ? entity instanceof PlayerEntity ? this.targetPlayers.get() : this.targetMobs.get() : false;
   }

   private boolean isRenderableTarget(LivingEntity entity) {
      if (entity == null || mc.player == null || mc.world == null) {
         return false;
      } else if (!this.matchesTargetType(entity)) {
         return false;
      } else if (!entity.isAlive() || entity.getWorld() != mc.world) {
         return false;
      } else if (entity.getWidth() <= 0.0F || entity.getHeight() <= 0.0F) {
         return false;
      } else if (entity.squaredDistanceTo(mc.player) > 25.0) {
         return false;
      } else if (!mc.player.canSee(entity)) {
         return false;
      } else {
         if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            boolean holding = !entity.getMainHandStack().isEmpty() || !entity.getOffHandStack().isEmpty();
            boolean armored = false;

            for (ItemStack stack : entity.getArmorItems()) {
               if (!stack.isEmpty()) {
                  armored = true;
                  break;
               }
            }

            if (!holding && !armored) {
               return false;
            }
         }

         return true;
      }
   }

   private Color baseColor() {
      if (this.useClientColor.get()) {
         int col = ImGuiTheme.ACCENT_COLOR;
         return new Color(col & 0xFF, col >> 8 & 0xFF, col >> 16 & 0xFF);
      } else {
         return this.customColor.get();
      }
   }

   private Color getRenderColor(LivingEntity entity) {
      if (this.hitEffectEnabled.get() && this.hitColorEnabled.get() && entity != null && entity.hurtTime > 0) {
         float f = entity.hurtTime / 10.0F;
         Color base = this.baseColor();
         Color hit = this.hitColor.get();
         int r = (int)(base.getRed() + (hit.getRed() - base.getRed()) * f);
         int g = (int)(base.getGreen() + (hit.getGreen() - base.getGreen()) * f);
         int b = (int)(base.getBlue() + (hit.getBlue() - base.getBlue()) * f);
         return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
      } else {
         return this.baseColor();
      }
   }

   private float baseAnimationSpeed() {
      String var1 = this.mode.get();

      return switch (var1) {
         case "Призраки" -> this.ghostSpeed.get();
         case "Круг" -> this.circleSpeed.get();
         case "Квадратик" -> this.squareSpeed.get();
         case "Орбита" -> this.orbitSpeed.get();
         default -> 1.5F;
      };
   }

   private float animationSpeed(LivingEntity entity) {
      float base = this.baseAnimationSpeed();
      if (this.hitEffectEnabled.get() && this.hitSpeedEnabled.get() && entity != null) {
         int hurt = entity.hurtTime;
         int prev = this.previousHurtTimes.getOrDefault(entity, 0);
         this.previousHurtTimes.put(entity, hurt);
         if (hurt >= 9 && prev < 9) {
            this.hitTimestamps.put(entity, System.currentTimeMillis());
         }

         Long hitAt = this.hitTimestamps.get(entity);
         if (hitAt == null) {
            return base;
         } else {
            float elapsed = (float)(System.currentTimeMillis() - hitAt) / 1000.0F;
            float duration = this.hitEffectDuration.get();
            if (elapsed > duration) {
               return base;
            } else {
               float t = elapsed / duration;
               float strength;
               if (t >= 0.15F) {
                  float k = (t - 0.15F) / 0.85F;
                  strength = 1.0F - k * k * (3.0F - 2.0F * k);
               } else {
                  float k = t / 0.15F;
                  strength = k * k * (3.0F - 2.0F * k);
               }

               return base * (1.0F + (this.hitSpeedMultiplier.get() - 1.0F) * strength);
            }
         }
      } else {
         return base;
      }
   }

   private void updateAnimationPhase(LivingEntity entity, float frameDelta) {
      float speed = this.animationSpeed(entity);
      this.rotationDegrees.put(entity, (this.rotationDegrees.getOrDefault(entity, 0.0) + frameDelta * speed * 50.0) % 360.0);
      this.animationPhase.put(entity, this.animationPhase.getOrDefault(entity, 0.0) + frameDelta * speed * 2.5);
   }

   private static Vec3d interpolate(Entity entity, float tickDelta) {
      Vec3d prev = new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
      return prev.add(entity.getPos().subtract(prev).multiply(tickDelta));
   }

   private void renderGhosts(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot, float tickDelta, LivingEntity entity, float alpha) {
      if (this.isRenderableTarget(entity) && !(alpha <= 0.0F) && mc.player.canSee(entity)) {
         Vec3d pos = interpolate(entity, tickDelta);
         double step = Math.toRadians(50.0) / 15.0;
         int count = Math.round(this.ghostCount.get());
         double phase = this.animationPhase.getOrDefault(entity, 0.0);
         double radius = Math.max(entity.getWidth(), 0.5F) + 0.3;
         double centerY = pos.y + Math.max(entity.getHeight(), 0.5F) / 2.0F;
         Vec3d[] axes = new Vec3d[]{
            new Vec3d(1.0, 1.0, 1.0),
            new Vec3d(-1.0, 1.0, -1.0),
            new Vec3d(1.0, -1.0, 1.0),
            new Vec3d(-1.0, -1.0, 1.0),
            new Vec3d(1.0, 1.0, -1.0),
            new Vec3d(-1.0, -1.0, -1.0)
         };

         for (int i = 0; i < count; i++) {
            double offset = phase + i * Math.PI / 2.0;
            Vec3d axis = axes[i % axes.length];
            double len = Math.sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z);
            Vec3d normal = new Vec3d(axis.x / len, axis.y / len, axis.z / len);
            Vec3d ref = new Vec3d(0.0, 1.0, 0.0);
            if (Math.abs(normal.dotProduct(ref)) > 0.99) {
               ref = new Vec3d(1.0, 0.0, 0.0);
            }

            Vec3d u = normal.crossProduct(ref).normalize();
            Vec3d v = normal.crossProduct(u).normalize();

            for (int j = 0; j < 15; j++) {
               double a = j * step + offset;
               Vec3d point = u.multiply(Math.cos(a)).add(v.multiply(Math.sin(a))).multiply(radius);
               Vec3d world = new Vec3d(pos.x + point.x, centerY + point.y, pos.z + point.z);

               try {
                  float size = this.ghostParticleSize.get() * (1.0F + j / 15.0F);
                  int color = FluxWorldRender.withAlpha(FluxWorldRender.ensureOpaque(this.getRenderColor(entity).getRGB()), Math.max(1, (int)(alpha * 150.0F)));
                  FluxWorldRender.drawBillboardGlow(matrices, cameraPos, camRot, world, size, color);
               } catch (Exception var35) {
               }
            }
         }
      }
   }

   private void renderCircle(MatrixStack matrices, Vec3d cameraPos, float tickDelta, LivingEntity entity, float alpha) {
      if (!(alpha <= 0.0F) && entity != null && this.isRenderableTarget(entity) && mc.player.canSee(entity)) {
         float radius = entity.getWidth() * 0.7F;
         Vec3d pos = interpolate(entity, tickDelta);
         double t = this.animationPhase.getOrDefault(entity, 0.0) % (Math.PI * 2);
         boolean secondHalf = t > Math.PI;
         double cycle = t / Math.PI;
         double k = secondHalf ? cycle - 1.0 : 1.0 - cycle;
         double eased = k >= 0.5 ? 1.0 - Math.pow(-2.0 * k + 2.0, 2.0) / 2.0 : 2.0 * k * k;
         double heightOffset = entity.getHeight() / 2.0F * (eased <= 0.5 ? eased : 1.0 - eased) * (secondHalf ? -1 : 1);
         Tessellator tessellator = Tessellator.getInstance();
         Vec3d camPos = mc.gameRenderer.getCamera().getPos();
         Color color = this.getRenderColor(entity);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         matrices.push();

         try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(3.0F);
            BufferBuilder wall = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            for (int i = 0; i <= 360; i++) {
               double a = Math.toRadians(i);
               float x = (float)(pos.x + Math.cos(a) * radius - camPos.x);
               float z = (float)(pos.z + Math.sin(a) * radius - camPos.z);
               float yBase = (float)(pos.y + entity.getHeight() * eased - camPos.y);
               wall.vertex(matrix, x, yBase, z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(100.0F * alpha));
               float yTop = (float)(pos.y + entity.getHeight() * eased + heightOffset - camPos.y);
               wall.vertex(matrix, x, yTop, z).color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            }

            BufferRenderer.drawWithGlobalProgram(wall.end());
            BufferBuilder ring = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

            for (int i = 0; i <= 360; i++) {
               double a = Math.toRadians(i);
               float x = (float)(pos.x + Math.cos(a) * radius - camPos.x);
               float y = (float)(pos.y + entity.getHeight() * eased - camPos.y);
               float z = (float)(pos.z + Math.sin(a) * radius - camPos.z);
               ring.vertex(matrix, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255.0F * alpha));
            }

            BufferRenderer.drawWithGlobalProgram(ring.end());
         } catch (Exception var34) {
         } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrices.pop();
         }
      }
   }

   private void renderSquare(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot, float tickDelta, LivingEntity entity, float alpha) {
      if (!(alpha <= 0.0F) && entity != null && this.isRenderableTarget(entity) && mc.player.canSee(entity)) {
         Vec3d pos = interpolate(entity, tickDelta);
         double rotation = this.rotationDegrees.getOrDefault(entity, 0.0);
         double phase = this.animationPhase.getOrDefault(entity, 0.0);
         float size = (float)(this.squareSize.get().floatValue() * (1.0 + 0.05 * Math.sin(phase))) * alpha;
         Vec3d center = new Vec3d(pos.x, pos.y + entity.getHeight() / 2.0F, pos.z);
         int color = FluxWorldRender.withAlpha(FluxWorldRender.ensureOpaque(this.getRenderColor(entity).getRGB()), Math.max(1, (int)(alpha * 255.0F)));
         FluxWorldRender.drawTexturedBillboard(matrices, cameraPos, camRot, center, size, color, FluxWorldRender.TARGET_TEXTURE, (float)rotation, true);
      }
   }

   private void renderOrbit(MatrixStack matrices, Vec3d cameraPos, float tickDelta, LivingEntity entity, float alpha) {
      if (!(alpha <= 0.0F) && this.isRenderableTarget(entity) && mc.player.canSee(entity)) {
         Vec3d pos = interpolate(entity, tickDelta);
         Vec3d camPos = mc.gameRenderer.getCamera().getPos();
         Color color = this.getRenderColor(entity);
         int perRing = Math.round(this.figuresPerRing.get());
         int layers = Math.round(this.orbitLayerCount.get());
         float height = entity.getHeight();
         float span = height * this.orbitVerticalSpread.get();
         float lower = (height - span) / 2.0F;
         float baseRadius = this.orbitRadius.get() + entity.getWidth() / 2.0F;
         float figureSize = this.orbitFigureSize.get() * alpha;
         Vec3d center = pos.add(0.0, height / 2.0F, 0.0);
         double rotation = this.orbitRotationEnabled.get() ? this.rotationDegrees.getOrDefault(entity, 0.0) : 0.0;
         double phase = this.animationPhase.getOrDefault(entity, 0.0);
         float animatedRadius = (float)(baseRadius + Math.sin(phase) * 0.08);
         String shape = this.orbitShape.get();
         Optional<OrbitShader> shader = this.resolveOrbitShader();
         if (this.orbitGlowEnabled.get()) {
            this.renderOrbitGlow(
               matrices,
               cameraPos,
               mc.gameRenderer.getCamera().getRotation(),
               pos,
               center,
               color,
               alpha,
               perRing,
               layers,
               span,
               lower,
               animatedRadius,
               figureSize,
               rotation,
               shape
            );
         }

         this.renderOrbitFigures(
            matrices, camPos, pos, center, color, alpha, perRing, layers, span, lower, animatedRadius, figureSize, rotation, phase, shape, shader
         );
      }
   }

   private Optional<OrbitShader> resolveOrbitShader() {
      if (!this.orbitShaderEnabled.get()) {
         return Optional.empty();
      } else {
         Optional<OrbitShader> shader = OrbitShaders.get(this.shaderName(this.orbitShaderType));
         return shader.filter(OrbitShader::isValid);
      }
   }

   private void renderOrbitGlow(
      MatrixStack matrices,
      Vec3d cameraPos,
      Quaternionf camRot,
      Vec3d entityPos,
      Vec3d center,
      Color color,
      float alpha,
      int perRing,
      int layers,
      float span,
      float lower,
      float animatedRadius,
      float figureSize,
      double rotation,
      String shape
   ) {
      float glowSize = this.orbitGlowSize.get();
      int glowColor = FluxWorldRender.withAlpha(FluxWorldRender.ensureOpaque(color.getRGB()), (int)(alpha * 255.0F * this.orbitGlowOpacity.get()));

      for (int layer = 0; layer < layers; layer++) {
         float y = layerY(entityPos.y, lower, span, layer, layers);
         float radius = layerRadius(animatedRadius, layer, layers);
         double offset = angularOffset(layer, perRing);

         for (int i = 0; i < perRing; i++) {
            double angle = Math.toRadians(rotation + offset + 360.0 * i / perRing);
            Vec3d figurePos = new Vec3d(center.x + radius * Math.cos(angle), y, center.z + radius * Math.sin(angle));
            if (shape.equals("Стрелки")) {
               Vec3d toCenter = center.subtract(figurePos);
               if (toCenter.lengthSquared() > 1.0E-6) {
                  figurePos = figurePos.add(toCenter.normalize().multiply(figureSize * 0.75F));
               }
            }

            FluxWorldRender.drawBillboardGlow(matrices, cameraPos, camRot, figurePos, glowSize, glowColor);
         }
      }
   }

   private void renderOrbitFigures(
      MatrixStack matrices,
      Vec3d camPos,
      Vec3d entityPos,
      Vec3d center,
      Color color,
      float alpha,
      int perRing,
      int layers,
      float span,
      float lower,
      float animatedRadius,
      float figureSize,
      double rotation,
      double phase,
      String shape,
      Optional<OrbitShader> shader
   ) {
      matrices.push();

      try {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Matrix4f matrix = matrices.peek().getPositionMatrix();

         int shapeType = switch (shape) {
            case "Стрелки" -> 0;
            case "Ромбы" -> 1;
            case "Кубы" -> 2;
            default -> -1;
         };
         int passes = shader.isPresent() ? 2 : 1;

         for (int pass = 0; pass < passes; pass++) {
            boolean shaderPass = pass == 1;

            try {
               if (shaderPass) {
                  OrbitShader program = shader.orElseThrow();
                  program.bind();
                  program.setFloat("time", (float)(phase * 0.4));
                  program.setFloat2("screenSize", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
                  program.setFloat4("baseColor", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);
                  program.setFloat("alpha", alpha * 1.5F);
                  RenderSystem.blendFunc(770, 1);
               }

               for (int layer = 0; layer < layers; layer++) {
                  float y = layerY(entityPos.y, lower, span, layer, layers);
                  float radius = layerRadius(animatedRadius, layer, layers);
                  double offset = angularOffset(layer, perRing);

                  for (int i = 0; i < perRing; i++) {
                     double angle = Math.toRadians(rotation + offset + 360.0 * i / perRing);
                     float fx = (float)(center.x + radius * Math.cos(angle));
                     float fz = (float)(center.z + radius * Math.sin(angle));
                     Vec3d forward = center.subtract(fx, y, fz).normalize();
                     Vec3d side = new Vec3d(-forward.z, 0.0, forward.x).normalize();
                     Vec3d up = side.crossProduct(forward);
                     int opacity = (int)(alpha * 220.0F);
                     switch (shapeType) {
                        case 0:
                           this.drawArrow(
                              matrix,
                              camPos,
                              color,
                              shaderPass,
                              fx,
                              y,
                              fz,
                              forward.x,
                              forward.y,
                              forward.z,
                              up.x,
                              up.y,
                              up.z,
                              side.x,
                              side.y,
                              side.z,
                              figureSize,
                              opacity
                           );
                           break;
                        case 1:
                           this.drawDiamond(
                              matrix,
                              camPos,
                              color,
                              shaderPass,
                              fx,
                              y,
                              fz,
                              forward.x,
                              forward.y,
                              forward.z,
                              up.x,
                              up.y,
                              up.z,
                              side.x,
                              side.y,
                              side.z,
                              figureSize,
                              opacity
                           );
                           break;
                        case 2:
                           this.drawCube(matrix, camPos, color, shaderPass, fx, y, fz, up.x, up.y, up.z, side.x, side.y, side.z, figureSize, opacity);
                     }
                  }
               }
            } finally {
               if (shaderPass) {
                  shader.ifPresent(program -> {
                     try {
                        program.unbind();
                     } catch (Exception var2) {
                     }
                  });
                  RenderSystem.defaultBlendFunc();
               }
            }
         }
      } finally {
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         matrices.pop();
      }
   }

   private static float layerY(double entityY, float lower, float span, int layer, int layers) {
      int intervals = Math.max(1, layers - 1);
      return (float)(entityY + lower + span * layer / intervals);
   }

   private static float layerRadius(float baseRadius, int layer, int layers) {
      float middle = (layers - 1) / 2.0F;
      return baseRadius * (1.0F - Math.abs(layer - middle) / Math.max(middle, 0.001F) * 0.3F);
   }

   private static double angularOffset(int layer, int perRing) {
      return 180.0 / perRing * (layer % 2);
   }

   private void drawArrow(
      Matrix4f matrix,
      Vec3d camPos,
      Color color,
      boolean shaderPass,
      float fx,
      float fy,
      float fz,
      double fwdX,
      double fwdY,
      double fwdZ,
      double upX,
      double upY,
      double upZ,
      double sideX,
      double sideY,
      double sideZ,
      float size,
      int opacity
   ) {
      float half = size * 0.5F;
      this.emitQuad(
         matrix,
         camPos,
         color,
         shaderPass,
         (float)(fx + fwdX * size * 1.5 - camPos.x),
         (float)(fy + fwdY * size * 1.5 - camPos.y),
         (float)(fz + fwdZ * size * 1.5 - camPos.z),
         (float)(fx + upX * half - camPos.x),
         (float)(fy + upY * half - camPos.y),
         (float)(fz + upZ * half - camPos.z),
         (float)(fx - upX * half - camPos.x),
         (float)(fy - upY * half - camPos.y),
         (float)(fz - upZ * half - camPos.z),
         (float)(fx + sideX * half - camPos.x),
         (float)(fy + sideY * half - camPos.y),
         (float)(fz + sideZ * half - camPos.z),
         opacity
      );
   }

   private void drawDiamond(
      Matrix4f matrix,
      Vec3d camPos,
      Color color,
      boolean shaderPass,
      float fx,
      float fy,
      float fz,
      double fwdX,
      double fwdY,
      double fwdZ,
      double upX,
      double upY,
      double upZ,
      double sideX,
      double sideY,
      double sideZ,
      float size,
      int opacity
   ) {
      double crossX = upY * sideZ - upZ * sideY;
      double crossY = upZ * sideX - upX * sideZ;
      double crossZ = upX * sideY - upY * sideX;
      float half = size * 0.6F;
      float tipX = (float)(fx + fwdX * size * 1.2 - camPos.x);
      float tipY = (float)(fy + fwdY * size * 1.2 - camPos.y);
      float tipZ = (float)(fz + fwdZ * size * 1.2 - camPos.z);
      float tailX = (float)(fx - fwdX * size * 1.2 - camPos.x);
      float tailY = (float)(fy - fwdY * size * 1.2 - camPos.y);
      float tailZ = (float)(fz - fwdZ * size * 1.2 - camPos.z);
      float upPX = (float)(fx + upX * half - camPos.x);
      float upPY = (float)(fy + upY * half - camPos.y);
      float upPZ = (float)(fz + upZ * half - camPos.z);
      float upNX = (float)(fx - upX * half - camPos.x);
      float upNY = (float)(fy - upY * half - camPos.y);
      float upNZ = (float)(fz - upZ * half - camPos.z);
      float sidePX = (float)(fx + crossX * half - camPos.x);
      float sidePY = (float)(fy + crossY * half - camPos.y);
      float sidePZ = (float)(fz + crossZ * half - camPos.z);
      float sideNX = (float)(fx - crossX * half - camPos.x);
      float sideNY = (float)(fy - crossY * half - camPos.y);
      float sideNZ = (float)(fz - crossZ * half - camPos.z);
      this.emitTriangle(matrix, color, shaderPass, tipX, tipY, tipZ, upPX, upPY, upPZ, sidePX, sidePY, sidePZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tipX, tipY, tipZ, sidePX, sidePY, sidePZ, upNX, upNY, upNZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tipX, tipY, tipZ, upNX, upNY, upNZ, sideNX, sideNY, sideNZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tipX, tipY, tipZ, sideNX, sideNY, sideNZ, upPX, upPY, upPZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tailX, tailY, tailZ, sidePX, sidePY, sidePZ, upPX, upPY, upPZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tailX, tailY, tailZ, upNX, upNY, upNZ, sidePX, sidePY, sidePZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tailX, tailY, tailZ, sideNX, sideNY, sideNZ, upNX, upNY, upNZ, opacity);
      this.emitTriangle(matrix, color, shaderPass, tailX, tailY, tailZ, upPX, upPY, upPZ, sideNX, sideNY, sideNZ, opacity);
   }

   private void drawCube(
      Matrix4f matrix,
      Vec3d camPos,
      Color color,
      boolean shaderPass,
      float fx,
      float fy,
      float fz,
      double upX,
      double upY,
      double upZ,
      double sideX,
      double sideY,
      double sideZ,
      float size,
      int opacity
   ) {
      double fwdX = upY * sideZ - upZ * sideY;
      double fwdY = upZ * sideX - upX * sideZ;
      double fwdZ = upX * sideY - upY * sideX;
      float half = size * 0.5F;
      float[][] v = new float[8][3];
      int idx = 0;

      for (int i = -1; i <= 1; i += 2) {
         for (int j = -1; j <= 1; j += 2) {
            for (int k = -1; k <= 1; k += 2) {
               v[idx++] = new float[]{
                  (float)(fx + fwdX * half * i + upX * half * j + sideX * half * k - camPos.x),
                  (float)(fy + fwdY * half * i + upY * half * j + sideY * half * k - camPos.y),
                  (float)(fz + fwdZ * half * i + upZ * half * j + sideZ * half * k - camPos.z)
               };
            }
         }
      }

      int[][] faces = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

      for (int[] face : faces) {
         this.emitQuad(
            matrix,
            color,
            shaderPass,
            v[face[0]][0],
            v[face[0]][1],
            v[face[0]][2],
            v[face[1]][0],
            v[face[1]][1],
            v[face[1]][2],
            v[face[2]][0],
            v[face[2]][1],
            v[face[2]][2],
            v[face[3]][0],
            v[face[3]][1],
            v[face[3]][2],
            opacity
         );
      }
   }

   private void emitTriangle(
      Matrix4f matrix, Color color, boolean shaderPass, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int opacity
   ) {
      if (!shaderPass) {
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
         buffer.vertex(matrix, x2, y2, z2).color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 3 / 4);
         buffer.vertex(matrix, x3, y3, z3).color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 3 / 4);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      } else {
         Vector4f a = matrix.transform(new Vector4f(x1, y1, z1, 1.0F));
         Vector4f b = matrix.transform(new Vector4f(x2, y2, z2, 1.0F));
         Vector4f c = matrix.transform(new Vector4f(x3, y3, z3, 1.0F));
         OrbitShader.drawTriangles(new float[]{a.x, a.y, a.z, 0.5F, 0.0F, b.x, b.y, b.z, 0.0F, 1.0F, c.x, c.y, c.z, 1.0F, 1.0F}, 1);
      }
   }

   private void emitQuad(
      Matrix4f matrix,
      Color color,
      boolean shaderPass,
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
      int opacity
   ) {
      this.emitTriangle(matrix, color, shaderPass, x1, y1, z1, x2, y2, z2, x3, y3, z3, opacity);
      this.emitTriangle(matrix, color, shaderPass, x1, y1, z1, x3, y3, z3, x4, y4, z4, opacity);
   }

   private void emitQuad(
      Matrix4f matrix,
      Vec3d camPos,
      Color color,
      boolean shaderPass,
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
      int opacity
   ) {
      this.emitQuad(matrix, color, shaderPass, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, opacity);
   }

   private String shaderName(ModeSetting setting) {
      String var2 = setting.get();

      return switch (var2) {
         case "Звёзды" -> "block_starfield";
         case "Паутина" -> "block_cobweb";
         case "Плазма" -> "block_plasma";
         default -> "block_nebula";
      };
   }

   private void renderCrystals(MatrixStack matrices, Vec3d cameraPos, float tickDelta, LivingEntity entity, float alpha) {
      if (!(alpha <= 0.0F) && entity != null && this.isRenderableTarget(entity) && mc.player.canSee(entity)) {
         Vec3d pos = interpolate(entity, tickDelta);
         Color baseCol = this.getRenderColor(entity);
         ColorRGBA color = new ColorRGBA(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), (int)(255.0F * alpha));
         float width = entity.getWidth() * 1.5F;
         double moving = this.animationPhase.getOrDefault(entity, 0.0) * 40.0;
         matrices.push();
         matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         BufferBuilder builder = CrystalRenderer.createBuffer();

         for (int i = 0; i < 360; i += 30) {
            float val = 1.2F - 0.5F * alpha;
            float sin = (float)(Math.sin(Math.toRadians(i + moving * 0.3F)) * width * val);
            float cos = (float)(Math.cos(Math.toRadians(i + moving * 0.3F)) * width * val);
            float size = 0.12F;
            matrices.push();
            matrices.translate(sin, 0.1F + entity.getHeight() * Math.abs((float)Math.sin(Math.toRadians(i))), cos);
            Vec3d targetCenter = new Vec3d(0.0, entity.getHeight() / 2.0, 0.0);
            Vec3d crystalPos = new Vec3d(sin, 0.1F + entity.getHeight() * Math.abs((float)Math.sin(Math.toRadians(i))), cos);
            Vector3f directionToTarget = new Vector3f(
                  (float)(targetCenter.x - crystalPos.x), (float)(targetCenter.y - crystalPos.y), (float)(targetCenter.z - crystalPos.z)
               )
               .normalize();
            Vector3f initialDirection = new Vector3f(0.0F, 1.0F, 0.0F);
            Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
            matrices.multiply(rotation);
            CrystalRenderer.render(matrices, builder, 0.0F, 0.0F, 0.0F, size, color);
            matrices.pop();
         }

         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         matrices.pop();
      }
   }

   public static int iconColor() {
      return ImColor.rgba(168, 85, 247, 255);
   }
}
