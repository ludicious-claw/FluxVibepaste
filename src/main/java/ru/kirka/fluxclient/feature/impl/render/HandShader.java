package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class HandShader extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Стиль шейдера руки", "Космос", "Космос", "Радуга", "Плазма", "Голограмма", "Неон", "Небула");
   public final NumberSetting speed = new NumberSetting("Скорость", "Скорость анимации шейдера", 1.5F, 0.2F, 5.0F, 0.1F);
   public final NumberSetting glow = new NumberSetting("Свечение", "Интенсивность свечения", 1.6F, 0.2F, 4.0F, 0.1F);
   public final NumberSetting alpha = new NumberSetting("Прозрачность", "Прозрачность текстуры", 0.85F, 0.1F, 1.0F, 0.05F);
   public final ColorSetting color = new ColorSetting("Цвет 1", "Основной неоновый цвет", new Color(168, 85, 247, 255));
   public final ColorSetting secondColor = new ColorSetting("Цвет 2", "Вторичный цвет градиента", new Color(59, 130, 246, 255));
   public final BooleanSetting wireframe = new BooleanSetting("Сетка", "Отрисовка каркаса полигонов", false);
   public final NumberSetting wireframeWidth = new NumberSetting("Толщина сетки", "Толщина линий полигональной сетки", 1.5F, 0.5F, 4.0F, 0.5F);
   public final BooleanSetting rainbow = new BooleanSetting("Радужный цикл", "Плавная смена всех цветов спектра", false);
   private static boolean wasApplied = false;

   public HandShader() {
      super("HandShader", "Шейдерные эффекты и неон для руки и первого лица", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.speed);
      this.registerSetting(this.glow);
      this.registerSetting(this.alpha);
      this.registerSetting(this.color);
      this.registerSetting(this.secondColor);
      this.registerSetting(this.wireframe);
      this.registerSetting(this.wireframeWidth);
      this.registerSetting(this.rainbow);
   }

   public static void apply() {
      HandShader hs = FluxContext.get().getModuleManager().getModule(HandShader.class);
      if (hs != null && hs.isEnabled()) {
         wasApplied = true;
         RenderSystem.enableBlend();
         Color base = hs.color.get();
         if (hs.rainbow.get()) {
            float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F * hs.speed.get();
            int rgb = Color.HSBtoRGB(hue % 1.0F, 0.85F, 1.0F);
            base = new Color(rgb);
         }

         float r = base.getRed() / 255.0F * hs.glow.get();
         float g = base.getGreen() / 255.0F * hs.glow.get();
         float b = base.getBlue() / 255.0F * hs.glow.get();
         float a = hs.alpha.get();
         String m = hs.mode.get();
         switch (m) {
            case "Неон":
               RenderSystem.blendFunc(770, 1);
               RenderSystem.setShaderColor(r, g, b, a);
               break;
            case "Плазма":
               RenderSystem.blendFunc(770, 1);
               float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.005 * hs.speed.get().floatValue()) * 0.3 + 0.7);
               RenderSystem.setShaderColor(r * pulse, g * (1.0F - pulse * 0.5F), b * pulse, a);
               break;
            case "Радуга":
               float time = (float)(System.currentTimeMillis() % 3600L) / 3600.0F * hs.speed.get();
               int rgb = Color.HSBtoRGB(time % 1.0F, 0.9F, 1.0F);
               Color c = new Color(rgb);
               RenderSystem.setShaderColor(c.getRed() / 255.0F * hs.glow.get(), c.getGreen() / 255.0F * hs.glow.get(), c.getBlue() / 255.0F * hs.glow.get(), a);
               break;
            case "Голограмма":
               RenderSystem.blendFunc(770, 771);
               Color holo = new Color(34, 211, 238);
               RenderSystem.setShaderColor(holo.getRed() / 255.0F, holo.getGreen() / 255.0F, holo.getBlue() / 255.0F, a * 0.75F);
               break;
            default:
               RenderSystem.blendFunc(770, 771);
               RenderSystem.setShaderColor(r, g, b, a);
         }

         if (hs.wireframe.get()) {
            GL11.glPolygonMode(1032, 6913);
            GL11.glLineWidth(hs.wireframeWidth.get());
         }
      }
   }

   public static void restore() {
      if (wasApplied) {
         wasApplied = false;
         GL11.glPolygonMode(1032, 6914);
         GL11.glLineWidth(1.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }
}
