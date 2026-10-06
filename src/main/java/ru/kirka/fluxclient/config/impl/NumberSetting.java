package ru.kirka.fluxclient.config.impl;

import ru.kirka.fluxclient.config.Setting;

public class NumberSetting extends Setting<Float> {
   private final float min;
   private final float max;
   private final float step;

   public NumberSetting(String name, String description, float defaultValue, float min, float max, float step) {
      super(name, description, defaultValue);
      this.min = min;
      this.max = max;
      this.step = step;
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public float getStep() {
      return this.step;
   }
}
