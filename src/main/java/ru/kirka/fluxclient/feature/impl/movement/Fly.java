package ru.kirka.fluxclient.feature.impl.movement;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.util.MoveUtil;

public class Fly extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Режим работы полета", "Motion", "Motion", "Creative", "Glide");
   public final NumberSetting speed = new NumberSetting("Скорость", "Горизонтальная скорость полета", 1.0F, 0.1F, 5.0F, 0.1F);
   public final NumberSetting verticalSpeed = new NumberSetting("Вертикальная", "Скорость подъема и спуска", 0.6F, 0.1F, 3.0F, 0.1F);
   public final BooleanSetting antiKick = new BooleanSetting("AntiKick", "Периодический микро-сброс для обхода кика за полет", true);

   public Fly() {
      super("Fly", "Позволяет свободно летать в воздухе", Category.MOVEMENT, 0);
      this.registerSetting(this.mode);
      this.registerSetting(this.speed);
      this.registerSetting(this.verticalSpeed);
      this.registerSetting(this.antiKick);
   }

   @Override
   protected void onEnable() {
      if (mc.player != null && this.mode.is("Creative")) {
         mc.player.getAbilities().allowFlying = true;
         mc.player.getAbilities().flying = true;
      }
   }

   @Override
   protected void onDisable() {
      if (mc.player != null) {
         if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
         }

         mc.player.getAbilities().setFlySpeed(0.05F);
         mc.player.fallDistance = 0.0F;
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         mc.player.fallDistance = 0.0F;
         if (this.mode.is("Creative")) {
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(0.05F * this.speed.get());
         } else {
            if (!mc.player.isCreative() && !mc.player.isSpectator() && mc.player.getAbilities().flying) {
               mc.player.getAbilities().flying = false;
            }

            mc.player.getAbilities().setFlySpeed(0.05F);
            if (this.mode.is("Glide")) {
               if (MoveUtil.isMoving()) {
                  MoveUtil.strafe(this.speed.get().floatValue() * 0.7);
               } else {
                  MoveUtil.stop();
               }

               double yVel = -0.05;
               if (mc.options.jumpKey.isPressed()) {
                  yVel = this.verticalSpeed.get().floatValue() * 0.5;
               } else if (mc.options.sneakKey.isPressed()) {
                  yVel = -this.verticalSpeed.get();
               }

               mc.player.setVelocity(mc.player.getVelocity().x, yVel, mc.player.getVelocity().z);
            } else {
               if (this.mode.is("Motion")) {
                  if (MoveUtil.isMoving()) {
                     MoveUtil.strafe(this.speed.get().floatValue());
                  } else {
                     MoveUtil.stop();
                  }

                  double yVel = 0.0;
                  if (mc.options.jumpKey.isPressed()) {
                     yVel += this.verticalSpeed.get().floatValue();
                  } else if (mc.options.sneakKey.isPressed()) {
                     yVel -= this.verticalSpeed.get().floatValue();
                  } else if (this.antiKick.get() && mc.player.age % 25 == 0) {
                     yVel = -0.04;
                  }

                  mc.player.setVelocity(mc.player.getVelocity().x, yVel, mc.player.getVelocity().z);
               }
            }
         }
      }
   }
}
