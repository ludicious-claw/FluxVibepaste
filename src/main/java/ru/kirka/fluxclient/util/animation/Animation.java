package ru.kirka.fluxclient.util.animation;

import ru.kirka.fluxclient.util.math.MathUtil;

public class Animation {
   private final float duration;
   private final Easing easing;
   private float progress = 0.0F;
   private float target = 0.0F;
   private long lastTime = System.currentTimeMillis();

   public Animation(float durationMs, Easing easing) {
      this.duration = durationMs;
      this.easing = easing;
   }

   public void setTarget(float target) {
      this.target = target;
   }

   public void reset(float value) {
      this.progress = value;
      this.target = value;
      this.lastTime = System.currentTimeMillis();
   }

   public void update() {
      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastTime) / this.duration;
      this.lastTime = now;
      if (this.progress < this.target) {
         this.progress = MathUtil.clamp(this.progress + delta, 0.0F, this.target);
      } else if (this.progress > this.target) {
         this.progress = MathUtil.clamp(this.progress - delta, this.target, 1.0F);
      }
   }

   public float getValue() {
      return this.easing.ease(this.progress);
   }

   public boolean isFinished() {
      return this.progress == this.target;
   }
}
