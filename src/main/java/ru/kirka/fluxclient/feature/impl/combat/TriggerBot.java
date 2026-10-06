package ru.kirka.fluxclient.feature.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class TriggerBot extends Module {
   public final BooleanSetting onlyCrits = new BooleanSetting("Только криты", "Атаковать только на спаде прыжка", false);
   public final BooleanSetting targetPlayers = new BooleanSetting("Игроки", "Атаковать игроков", true);
   public final BooleanSetting targetMonsters = new BooleanSetting("Мобы", "Атаковать монстров", true);
   public final BooleanSetting targetAnimals = new BooleanSetting("Животные", "Атаковать животных", false);

   public TriggerBot() {
      super("TriggerBot", "Авто-атака при прямом наведении прицела на цель", Category.COMBAT, -1);
      this.registerSetting(this.onlyCrits);
      this.registerSetting(this.targetPlayers);
      this.registerSetting(this.targetMonsters);
      this.registerSetting(this.targetAnimals);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.targetedEntity instanceof LivingEntity living && this.isValidTarget(living)) {
            if (mc.player.getAttackCooldownProgress(0.5F) < 0.95F) {
               return;
            }

            if (this.onlyCrits.get()) {
               boolean isFalling = mc.player.fallDistance > 0.06F || !mc.player.isOnGround() && mc.player.getVelocity().y < -0.06;
               if (!isFalling) {
                  return;
               }
            }

            mc.interactionManager.attackEntity(mc.player, living);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();
         }
      }
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (!entity.isAlive() || entity == mc.player) {
         return false;
      } else if (entity instanceof PlayerEntity) {
         return this.targetPlayers.get();
      } else if (entity instanceof HostileEntity) {
         return this.targetMonsters.get();
      } else {
         return entity instanceof PassiveEntity ? this.targetAnimals.get() : true;
      }
   }
}
