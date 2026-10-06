package ru.kirka.fluxclient.render;

import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.util.MathUtil;

public class AnimationUtil implements Interface {
   private float currentValue;
   private float previousValue;
   private float animationSpeed;
   private float animationValue;
   private float fromValue = 0.0F;
   private float toValue = 1.0F;
   private long lastUpdateTime = System.currentTimeMillis();

   public void setValue(float value) {
      this.currentValue = value;
      this.previousValue = value;
      this.animationValue = value;
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public void resetTimer() {
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public void c(float value) {
      this.setValue(value);
   }

   public void setPreviousValue(float prevValue) {
      this.previousValue = prevValue;
   }

   public void d(float prevValue) {
      this.setPreviousValue(prevValue);
   }

   public float getValue() {
      return this.currentValue;
   }

   public float a() {
      return this.getValue();
   }

   public float getPreviousValue() {
      return this.previousValue;
   }

   public float b() {
      return this.getPreviousValue();
   }

   public float getAnimationValue() {
      return this.animationValue;
   }

   public float c() {
      return this.getAnimationValue();
   }

   public void setAnimationValue(float animationValue) {
      this.animationValue = animationValue;
   }

   public void e(float animationValue) {
      this.setAnimationValue(animationValue);
   }

   public void runAnimation(boolean expanding) {
      this.previousValue = this.currentValue;
      float direction = expanding ? 1.0F : -1.0F;
      this.currentValue = MathUtil.clamp(this.currentValue + direction * this.animationSpeed * 20.0F * this.getDeltaSeconds(), this.fromValue, this.toValue);
   }

   public void a(boolean expanding) {
      this.runAnimation(expanding);
   }

   public void update(float fromValue, float toValue, float animationSpeed, EasingList.EasingFunction easing, float partialTicks) {
      this.animationSpeed = animationSpeed;
      this.fromValue = fromValue;
      this.toValue = toValue;
      float progress = MathUtil.lerp(this.previousValue, this.currentValue, partialTicks);
      if (easing != null) {
         float clamped = MathUtil.clamp(progress, 0.0F, 1.0F);
         this.animationValue = easing.ease(clamped);
      } else {
         this.animationValue = progress;
      }
   }

   public void a(float fromValue, float toValue, float animationSpeed, EasingList.EasingFunction easing, float partialTicks) {
      this.update(fromValue, toValue, animationSpeed, easing, partialTicks);
   }

   public void addTarget(float amount) {
      this.toValue += amount;
   }

   public void a(float amount) {
      this.addTarget(amount);
   }

   public void setTarget(float value) {
      this.toValue = value;
      this.currentValue = value;
   }

   public void b(float value) {
      this.setTarget(value);
   }

   public float animate(float min, float max, float speed) {
      this.toValue = MathUtil.clamp(this.toValue, min, max);
      this.currentValue = MathUtil.expSmooth(this.currentValue, this.toValue, speed);
      this.animationValue = this.currentValue;
      return this.currentValue;
   }

   public float a(float min, float max, float speed) {
      return this.animate(min, max, speed);
   }

   public float animate(float target, float speed) {
      this.toValue = target;
      this.currentValue = MathUtil.expSmooth(this.currentValue, this.toValue, speed);
      this.animationValue = this.currentValue;
      return this.currentValue;
   }

   public float a(float target, float speed) {
      return this.animate(target, speed);
   }

   private float getDeltaSeconds() {
      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastUpdateTime) / 1000.0F;
      this.lastUpdateTime = now;
      return Math.min(Math.max(0.0F, delta), 0.05F);
   }

   public float d() {
      return this.getDeltaSeconds();
   }
}
