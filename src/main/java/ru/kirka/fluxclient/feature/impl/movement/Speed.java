package ru.kirka.fluxclient.feature.impl.movement;

import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.util.MoveUtil;

public class Speed extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Тип ускорения", "BHop", "BHop", "Strafe", "Ground", "Vanilla");
   public final NumberSetting speed = new NumberSetting("Скорость", "Множитель скорости перемещения", 1.5F, 0.2F, 5.0F, 0.1F);
   public final BooleanSetting autoJump = new BooleanSetting("Авто-прыжок", "Автоматически прыгать при беге для набора ускорения", true);
   public final BooleanSetting inWater = new BooleanSetting("В воде", "Работать при погружении в воду или лаву", false);

   public Speed() {
      super("Speed", "Существенно повышает скорость перемещения игрока", Category.MOVEMENT, 0);
      this.registerSetting(this.mode);
      this.registerSetting(this.speed);
      this.registerSetting(this.autoJump);
      this.registerSetting(this.inWater);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         Module fly = FluxContext.get().getModuleManager().getModule(Fly.class);
         if (fly == null || !fly.isEnabled()) {
            if (!mc.player.getAbilities().flying) {
               if (this.inWater.get() || !mc.player.isTouchingWater() && !mc.player.isInLava()) {
                  if (MoveUtil.isMoving()) {
                     if (!mc.player.isSprinting() && mc.player.getHungerManager().getFoodLevel() > 6) {
                        mc.player.setSprinting(true);
                     }

                     double baseSpeed = MoveUtil.getBaseMoveSpeed();
                     if (this.mode.is("BHop")) {
                        if (mc.player.isOnGround()) {
                           if (this.autoJump.get()) {
                              mc.player.setVelocity(mc.player.getVelocity().x, MoveUtil.getJumpMotion(), mc.player.getVelocity().z);
                           }

                           MoveUtil.strafe(baseSpeed * this.speed.get().floatValue() * 1.05);
                        } else {
                           double currentSpeed = MoveUtil.getHorizontalSpeed();
                           double targetSpeed = Math.max(currentSpeed, baseSpeed * this.speed.get().floatValue() * 0.95);
                           MoveUtil.strafe(targetSpeed);
                        }
                     } else if (this.mode.is("Strafe")) {
                        if (this.autoJump.get() && mc.player.isOnGround()) {
                           mc.player.setVelocity(mc.player.getVelocity().x, MoveUtil.getJumpMotion(), mc.player.getVelocity().z);
                        }

                        MoveUtil.strafe(baseSpeed * this.speed.get().floatValue());
                     } else if (this.mode.is("Ground")) {
                        if (mc.player.isOnGround()) {
                           MoveUtil.strafe(baseSpeed * this.speed.get().floatValue());
                        }
                     } else if (this.mode.is("Vanilla")) {
                        Vec3d vel = mc.player.getVelocity();
                        mc.player.setVelocity(vel.x * this.speed.get().floatValue(), vel.y, vel.z * this.speed.get().floatValue());
                     }
                  }
               }
            }
         }
      }
   }
}
