package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class CustomSky extends Module {
   public final ColorSetting skyColor = new ColorSetting("Цвет неба", "Цвет", new Color(15, 10, 28, 255));
   public final ColorSetting cloudsColor = new ColorSetting("Цвет облаков", "Цвет", new Color(147, 51, 234, 180));

   public CustomSky() {
      super("CustomSky", "Кастомный цвет неба и облаков", Category.RENDER, -1);
      this.registerSetting(this.skyColor);
      this.registerSetting(this.cloudsColor);
   }
}
