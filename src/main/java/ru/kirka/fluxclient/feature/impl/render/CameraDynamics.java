package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class CameraDynamics extends Module {
   public final BooleanSetting cameraRoll = new BooleanSetting("Наклон камеры", "Крен камеры при стрейфах (A/D)", true);
   public final NumberSetting maxRoll = new NumberSetting("Угол наклона", "Максимальный угол наклона камеры", 3.5F, 0.5F, 10.0F, 0.5F);
   public final NumberSetting rollSpeed = new NumberSetting("Плавность наклона", "Скорость интерполяции крена", 7.0F, 1.0F, 15.0F, 0.5F);
   public final BooleanSetting breathing = new BooleanSetting("Покачивание дыхания", "Мягкое кинематографичное покачивание", true);
   public final NumberSetting breathingAmount = new NumberSetting("Сила дыхания", "Амплитуда покачивания камеры", 0.5F, 0.1F, 2.0F, 0.1F);
   private float currentRoll = 0.0F;

   public CameraDynamics() {
      super("CameraDynamics", "Кинематографичный наклон камеры при стрейфах и динамика взгляда", Category.RENDER, -1);
      this.registerSetting(this.cameraRoll);
      this.registerSetting(this.maxRoll);
      this.registerSetting(this.rollSpeed);
      this.registerSetting(this.breathing);
      this.registerSetting(this.breathingAmount);
   }

   public float getCalculatedRoll(float tickDelta) {
      if (this.isEnabled() && mc.player != null) {
         float targetRoll = 0.0F;
         if (this.cameraRoll.get()) {
            boolean left = mc.options.leftKey.isPressed();
            boolean right = mc.options.rightKey.isPressed();
            if (left && !right) {
               targetRoll = this.maxRoll.get();
            } else if (right && !left) {
               targetRoll = -this.maxRoll.get();
            }
         }

         if (this.breathing.get() && mc.player.getVelocity().horizontalLengthSquared() < 0.001) {
            float time = (float)(System.currentTimeMillis() % 10000000L / 1000.0);
            targetRoll += (float)(Math.sin(time * 2.0) * 0.4F * this.breathingAmount.get().floatValue());
         }

         float speed = this.rollSpeed.get() * (tickDelta * 0.05F);
         this.currentRoll = this.currentRoll + (targetRoll - this.currentRoll) * Math.min(1.0F, speed * 2.5F);
         return this.currentRoll;
      } else {
         this.currentRoll = 0.0F;
         return 0.0F;
      }
   }
}
