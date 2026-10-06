package ru.kirka.fluxclient.config.impl;

import java.awt.Color;
import ru.kirka.fluxclient.config.Setting;

public class ColorSetting extends Setting<Color> {
   public ColorSetting(String name, String description, Color defaultValue) {
      super(name, description, defaultValue);
   }

   public int getRGB() {
      return this.value.getRGB();
   }

   public float[] getFloatRGBA() {
      return new float[]{this.value.getRed() / 255.0F, this.value.getGreen() / 255.0F, this.value.getBlue() / 255.0F, this.value.getAlpha() / 255.0F};
   }
}
