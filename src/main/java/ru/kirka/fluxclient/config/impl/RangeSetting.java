package ru.kirka.fluxclient.config.impl;

import net.minecraft.util.math.MathHelper;
import ru.kirka.fluxclient.config.Setting;

public class RangeSetting extends Setting<float[]> {
   private float min;
   private float max;
   private float step;

   public RangeSetting(String name, String description, float firstValue, float secondValue, float min, float max, float step) {
      super(name, description, new float[]{firstValue, secondValue});
      this.min = min;
      this.max = max;
      this.step = step;
   }

   public RangeSetting(String name, float firstValue, float secondValue, float min, float max, float step) {
      this(name, "", firstValue, secondValue, min, max, step);
   }

   public float getFirstValue() {
      return this.value[0];
   }

   public float getSecondValue() {
      return this.value[1];
   }

   public void setFirstValue(float val) {
      float clamped = (float)MathHelper.clamp(Math.round(val * (1.0 / this.step)) / (1.0 / this.step), this.min, this.max);
      this.value[0] = clamped;
   }

   public void setSecondValue(float val) {
      float clamped = (float)MathHelper.clamp(Math.round(val * (1.0 / this.step)) / (1.0 / this.step), this.min, this.max);
      this.value[1] = clamped;
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
