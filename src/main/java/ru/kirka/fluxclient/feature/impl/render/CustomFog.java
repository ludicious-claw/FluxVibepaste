package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.config.impl.RangeSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.color.Colors;

public class CustomFog extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final RangeSetting distance = new RangeSetting("Дистанция", "Диапазон начала и конца тумана", 2.0F, 50.0F, 0.0F, 100.0F, 0.5F);
   public final BooleanSetting fogColorEnabled = new BooleanSetting("Свой цвет", "Окрашивать туман в кастомный цвет", true);
   public final BooleanSetting themeSync = new BooleanSetting("Синхр. темы", "Использовать акцентный цвет темы клиента", false);
   public final ColorSetting fogColor = new ColorSetting("Цвет тумана", "Цвет тумана и горизонта", new Color(38, 16, 68, 255));
   public final BooleanSetting depthBlur = new BooleanSetting("Размытие глубины", "Пост-эффект размытия объектов вдалеке", false);
   public final NumberSetting blurStrength = new NumberSetting("Сила размытия", "Интенсивность глубинного блюра", 1.0F, 0.1F, 10.0F, 0.05F);
   public final NumberSetting blurOffset = new NumberSetting("Смещение размытия", "Смещение выборки блюра", 1.0F, 0.05F, 12.0F, 0.05F);
   public final BooleanSetting noSkyBlur = new BooleanSetting("Не блюрить небо", "Исключить небо из шейдера размытия", false);
   public final BooleanSetting blendSky = new BooleanSetting("Сливать с небом", "Сливать цвет неба и облаков с цветом тумана", false);

   public CustomFog() {
      super("CustomFog", "Кинематографичный 3D туман с настройкой глубины и цвета", Category.RENDER, -1);
      this.registerSetting(this.distance);
      this.registerSetting(this.fogColorEnabled);
      this.registerSetting(this.themeSync);
      this.registerSetting(this.fogColor);
      this.registerSetting(this.depthBlur);
      this.registerSetting(this.blurStrength);
      this.registerSetting(this.blurOffset);
      this.registerSetting(this.noSkyBlur);
      this.registerSetting(this.blendSky);
   }

   public boolean shouldModifyFog(Camera camera) {
      if (this.isEnabled() && mc.world != null && mc.player != null && camera != null) {
         CameraSubmersionType submersion = camera.getSubmersionType();
         return submersion != CameraSubmersionType.LAVA && submersion != CameraSubmersionType.WATER && submersion != CameraSubmersionType.POWDER_SNOW
            ? !(
               camera.getFocusedEntity() instanceof LivingEntity livingEntity
                  && (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS) || livingEntity.hasStatusEffect(StatusEffects.DARKNESS))
            )
            : false;
      } else {
         return false;
      }
   }

   public boolean shouldApplyDepthBlur(Camera camera) {
      return this.depthBlur.get() && this.shouldModifyFog(camera);
   }

   public ColorRGBA getEffectiveFogColor() {
      if (this.themeSync.get()) {
         return Colors.getAccent();
      } else {
         Color c = this.fogColor.get();
         return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
      }
   }

   public RangeSetting getDistance() {
      return this.distance;
   }

   public BooleanSetting getFogColorEnabled() {
      return this.fogColorEnabled;
   }

   public BooleanSetting getDepthBlur() {
      return this.depthBlur;
   }

   public NumberSetting getBlurStrength() {
      return this.blurStrength;
   }

   public NumberSetting getBlurOffset() {
      return this.blurOffset;
   }

   public BooleanSetting getNoSkyBlur() {
      return this.noSkyBlur;
   }
}
