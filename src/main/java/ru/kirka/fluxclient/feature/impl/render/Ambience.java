package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Vector3f;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.ReceivePacketEvent;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.DynamicLightUtility;
import ru.kirka.fluxclient.render.shader.SkyShaderProgram;

public class Ambience extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int CLIENT_NIGHT_VISION_DURATION = 400;
   private static final int CLIENT_NIGHT_VISION_REFRESH_TICKS = 220;
   public final ModeSetting preset = new ModeSetting(
      "Пресет", "Атмосферный стиль", "Киберпанк", "Киберпанк", "Кровавая луна", "Золотой закат", "Глубокая ночь", "Пользовательский"
   );
   public final ColorSetting worldTint = new ColorSetting("Цвет мира", "Оттенок освещения мира", new Color(168, 85, 247, 180));
   public final NumberSetting intensity = new NumberSetting("Интенсивность", "Сила наложения фильтра", 0.35F, 0.05F, 1.0F, 0.05F);
   public final BooleanSetting endSky = new BooleanSetting("Небо Края", "Отображать темное небо Энда", false);
   public final BooleanSetting tintSky = new BooleanSetting("Красить небо", true);
   public final BooleanSetting tintClouds = new BooleanSetting("Красить облака", true);
   public final BooleanSetting tintStars = new BooleanSetting("Красить звезды", true);
   public final ColorSetting skyColor = new ColorSetting("Цвет неба", "Кастомный цвет неба", new Color(138, 43, 226));
   public final ColorSetting cloudColor = new ColorSetting("Цвет облаков", "Кастомный цвет облаков", new Color(180, 100, 255));
   public final ColorSetting starsColor = new ColorSetting("Цвет звезд", "Кастомный цвет звезд", new Color(255, 255, 255));
   public final ModeSetting skybox = new ModeSetting(
      "Скайбокс", "Кубическая текстура неба", "Default", "Default", "Bright Clouds", "Lake", "Cloud Space", "Clear Evening", "Underwater"
   );
   public final BooleanSetting shaderSky = new BooleanSetting("Шейдер неба", "Анимированный шейдер неба", false);
   public final ModeSetting shaderType = new ModeSetting("Тип шейдера", "Алгоритм шейдера", "Nebula", "Nebula", "Caustic");
   public final NumberSetting shaderOpacity = new NumberSetting("Прозрачность шейдера", "Непрозрачность (%)", 70.0F, 0.0F, 100.0F, 5.0F);
   public final BooleanSetting customTime = new BooleanSetting("Свое время", "Зафиксировать время мира", false);
   public final NumberSetting time = new NumberSetting("Время суток", "Время мира (тики)", 18000.0F, 0.0F, 24000.0F, 500.0F);
   public final BooleanSetting bright = new BooleanSetting("Подсветка", "Увеличение яркости мира", true);
   public final ModeSetting brightnessMode = new ModeSetting("Режим яркости", "Способ освещения", "Gamma", "Gamma", "Effect", "Dynamic");
   public final NumberSetting dynamicRadius = new NumberSetting("Радиус динамики", "Радиус динамического света", 7.5F, 4.0F, 12.0F, 0.5F);
   public final NumberSetting dynamicLight = new NumberSetting("Сила динамики", "Уровень динамического света", 15.0F, 8.0F, 15.0F, 1.0F);
   public final BooleanSetting dynamicOnlyInCave = new BooleanSetting("Только в пещерах", "Динамический свет только под землей", false);
   public final BooleanSetting nightMode = new BooleanSetting("Ночной фильтр", "Атмосферный ночной пост-эффект", false);
   public final ColorSetting nightModeColor = new ColorSetting("Цвет ночи", "Оттенок ночи", new Color(80, 120, 220));
   public final NumberSetting nightModeStrength = new NumberSetting("Сила ночи", "Сила ночного фильтра (%)", 70.0F, 0.0F, 100.0F, 1.0F);
   private long oldTime;
   private Vec3d dynamicLightPos;
   private int dynamicLightRadius = -1;
   private int dynamicLightLevel = -1;
   private boolean dynamicLightActive;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.get()) {
         event.cancel();
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> this.updateDynamicBrightness();

   public Ambience() {
      super("Ambience", "Кинематографичная атмосфера, кастомные скайбоксы, шейдеры неба и освещение", Category.RENDER, -1);
      this.registerSetting(this.preset);
      this.registerSetting(this.worldTint);
      this.registerSetting(this.intensity);
      this.registerSetting(this.endSky);
      this.registerSetting(this.tintSky);
      this.registerSetting(this.tintClouds);
      this.registerSetting(this.tintStars);
      this.registerSetting(this.skyColor);
      this.registerSetting(this.cloudColor);
      this.registerSetting(this.starsColor);
      this.registerSetting(this.skybox);
      this.registerSetting(this.shaderSky);
      this.registerSetting(this.shaderType);
      this.registerSetting(this.shaderOpacity);
      this.registerSetting(this.customTime);
      this.registerSetting(this.time);
      this.registerSetting(this.bright);
      this.registerSetting(this.brightnessMode);
      this.registerSetting(this.dynamicRadius);
      this.registerSetting(this.dynamicLight);
      this.registerSetting(this.dynamicOnlyInCave);
      this.registerSetting(this.nightMode);
      this.registerSetting(this.nightModeColor);
      this.registerSetting(this.nightModeStrength);
   }

   public Color getActiveTintColor() {
      String p = this.preset.get();

      return switch (p) {
         case "Кровавая луна" -> new Color(225, 29, 72, 200);
         case "Золотой закат" -> new Color(245, 158, 11, 200);
         case "Глубокая ночь" -> new Color(30, 58, 138, 220);
         case "Киберпанк" -> new Color(147, 51, 234, 190);
         default -> (Color)this.worldTint.get();
      };
   }

   public boolean isBrightnessEnabled() {
      return this.bright.get() && !this.nightMode.get();
   }

   public boolean shouldUseGammaBrightness() {
      return this.isEnabled() && this.isBrightnessEnabled() && this.brightnessMode.is("Gamma");
   }

   public boolean isNightModeActive() {
      return this.isEnabled() && this.nightMode.get();
   }

   public Vector3f getNightModeTint() {
      Color c = this.nightModeColor.get();
      return new Vector3f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
   }

   public float getNightModeStrengthValue() {
      return this.nightModeStrength.get() / 100.0F;
   }

   public boolean hasCustomSkybox() {
      return !this.skybox.is("Default");
   }

   public boolean hasShaderSkybox() {
      return this.shaderSky.get();
   }

   public boolean shouldRenderCustomSkybox() {
      return this.isEnabled() && (this.hasCustomSkybox() || this.hasShaderSkybox());
   }

   public boolean shouldTintSky() {
      return this.isEnabled() && this.tintSky.get();
   }

   public boolean shouldTintClouds() {
      return this.isEnabled() && this.tintClouds.get();
   }

   public boolean shouldTintStars() {
      return this.isEnabled() && this.tintStars.get();
   }

   public ColorRGBA getResolvedSkyColor() {
      return new ColorRGBA(this.skyColor.get().getRed(), this.skyColor.get().getGreen(), this.skyColor.get().getBlue(), this.skyColor.get().getAlpha());
   }

   public ColorRGBA getResolvedCloudColor() {
      return new ColorRGBA(this.cloudColor.get().getRed(), this.cloudColor.get().getGreen(), this.cloudColor.get().getBlue(), this.cloudColor.get().getAlpha());
   }

   public ColorRGBA getResolvedStarsColor() {
      return new ColorRGBA(this.starsColor.get().getRed(), this.starsColor.get().getGreen(), this.starsColor.get().getBlue(), this.starsColor.get().getAlpha());
   }

   public Identifier getSkyboxTexture() {
      int index = Math.max(1, this.skybox.getModes().indexOf(this.skybox.get()));
      return Identifier.of("fluxclient", "sky/" + index + ".png");
   }

   public SkyShaderProgram getSkyShaderProgram() {
      return this.shaderType.is("Caustic") ? DrawUtility.skyCausticProgram : DrawUtility.skyNebulaProgram;
   }

   public float getShaderOpacity() {
      return this.shaderOpacity.get() / 100.0F;
   }

   @Override
   public void onTick() {
      if (mc.world == null) {
         this.removeClientNightVision();
         this.clearDynamicBrightness();
      } else {
         this.updateBrightnessEffect();
         if (!this.shouldUseDynamicBrightness()) {
            this.clearDynamicBrightness();
         }

         super.onTick();
      }
   }

   @Override
   public void onEnable() {
      if (mc.world != null) {
         this.oldTime = mc.world.getTimeOfDay();
      }

      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.removeClientNightVision();
      this.clearDynamicBrightness();
      super.onDisable();
   }

   private void updateBrightnessEffect() {
      if (mc.player != null) {
         if (!this.shouldUseEffectBrightness()) {
            this.removeClientNightVision();
         } else {
            StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (current == null || this.isClientNightVision(current) && current.getDuration() <= 220) {
               mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            }
         }
      }
   }

   private boolean shouldUseEffectBrightness() {
      return this.isEnabled() && this.isBrightnessEnabled() && this.brightnessMode.is("Effect");
   }

   private boolean shouldUseDynamicBrightness() {
      return this.isEnabled() && this.isBrightnessEnabled() && this.brightnessMode.is("Dynamic") && (!this.dynamicOnlyInCave.get() || this.isPlayerInCave());
   }

   private boolean isPlayerInCave() {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = BlockPos.ofFloored(mc.player.getPos());
         return !mc.world.isSkyVisible(pos) && mc.world.getLightLevel(LightType.SKY, pos) < 4;
      } else {
         return false;
      }
   }

   private void removeClientNightVision() {
      if (mc.player != null) {
         StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
         if (this.isClientNightVision(current)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
         }
      }
   }

   private boolean isClientNightVision(StatusEffectInstance effect) {
      return effect != null && effect.getEffectType() == StatusEffects.NIGHT_VISION && effect.getAmplifier() == 0 && effect.getDuration() <= 400;
   }

   private void updateDynamicBrightness() {
      if (mc.player != null && mc.world != null && this.shouldUseDynamicBrightness()) {
         Vec3d pos = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight() * 0.5, 0.0);
         float radius = this.dynamicRadius.get();
         float light = this.dynamicLight.get();
         int radiusCeil = MathHelper.ceil(radius);
         int lightFloor = MathHelper.floor(light);
         DynamicLightUtility.set(pos.x, pos.y, pos.z, radius, light);
         if (this.shouldRefreshDynamicBrightness(pos, radiusCeil, lightFloor)) {
            this.refreshDynamicBrightnessArea(this.dynamicLightPos, this.dynamicLightRadius);
            this.refreshDynamicBrightnessArea(pos, radiusCeil);
            this.dynamicLightPos = pos;
            this.dynamicLightRadius = radiusCeil;
            this.dynamicLightLevel = lightFloor;
            this.dynamicLightActive = true;
         }
      } else {
         this.clearDynamicBrightness();
      }
   }

   private boolean shouldRefreshDynamicBrightness(Vec3d pos, int radius, int light) {
      return !this.dynamicLightActive
         || this.dynamicLightPos == null
         || this.dynamicLightRadius != radius
         || this.dynamicLightLevel != light
         || pos.squaredDistanceTo(this.dynamicLightPos) >= 0.0225;
   }

   private void clearDynamicBrightness() {
      if (!this.dynamicLightActive) {
         DynamicLightUtility.clear();
      } else {
         DynamicLightUtility.clear();
         this.refreshDynamicBrightnessArea(this.dynamicLightPos, this.dynamicLightRadius);
         this.dynamicLightActive = false;
         this.dynamicLightPos = null;
         this.dynamicLightRadius = -1;
         this.dynamicLightLevel = -1;
      }
   }

   private void refreshDynamicBrightnessArea(Vec3d pos, int radius) {
      if (pos != null && radius > 0 && mc.worldRenderer != null) {
         int padding = Math.max(1, MathHelper.ceil(radius / Math.max(this.dynamicLight.get(), 1.0F)));
         int minX = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.x) - radius - padding);
         int minY = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.y) - radius - padding);
         int minZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.z) - radius - padding);
         int maxX = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.x) + radius + padding);
         int maxY = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.y) + radius + padding);
         int maxZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(pos.z) + radius + padding);
         mc.worldRenderer.scheduleBlockRenders(minX, minY, minZ, maxX, maxY, maxZ);
      }
   }
}
