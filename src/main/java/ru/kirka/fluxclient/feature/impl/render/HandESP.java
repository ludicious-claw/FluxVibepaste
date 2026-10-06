package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class HandESP extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Тип подсветки руки", "Обводка", "Обводка", "Заливка", "Обводка + Заливка");
   public final NumberSetting thickness = new NumberSetting("Толщина", "Толщина обводки", 1.5F, 0.5F, 4.0F, 0.5F);
   public final NumberSetting alpha = new NumberSetting("Заливка", "Прозрачность заливки", 0.35F, 0.05F, 1.0F, 0.05F);
   public final ColorSetting color = new ColorSetting("Цвет", "Цвет руки и предмета", new Color(147, 51, 234, 255));

   public HandESP() {
      super("HandESP", "Обводка / заливка руки и предмета в руке", Category.RENDER, 0);
      this.registerSetting(this.mode);
      this.registerSetting(this.thickness);
      this.registerSetting(this.alpha);
      this.registerSetting(this.color);
   }

   public static void apply() {
      HandESP m = FluxContext.get().getModuleManager().getModule(HandESP.class);
      if (m != null && m.isEnabled()) {
         Color selected = m.color.get();
         float r = selected.getRed() / 255.0F;
         float g = selected.getGreen() / 255.0F;
         float b = selected.getBlue() / 255.0F;
         String md = m.mode.get();
         RenderSystem.setShaderColor(r, g, b, md.contains("Заливка") ? m.alpha.get() : 1.0F);
         if (md.contains("Обводка")) {
            GL11.glPolygonMode(1032, 6913);
            GL11.glLineWidth(m.thickness.get());
         }

         if (md.contains("Заливка")) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(r, g, b, m.alpha.get());
         }
      }
   }

   public static void restore() {
      GL11.glPolygonMode(1032, 6914);
      GL11.glLineWidth(1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }
}
