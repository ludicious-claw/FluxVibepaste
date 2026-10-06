package ru.kirka.fluxclient.feature.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.util.AuraUtil;

public class AimAssistant extends Module {
   public final BooleanSetting targetPlayers = new BooleanSetting("Игроки", "Наводиться на игроков", true);
   public final BooleanSetting targetAnimals = new BooleanSetting("Животные", "Наводиться на животных", false);
   public final BooleanSetting targetMonsters = new BooleanSetting("Мобы", "Наводиться на мобов", false);
   public final BooleanSetting throughWalls = new BooleanSetting("Наводить за стеной", "Доводка сквозь блоки", false);
   public final NumberSetting threshold = new NumberSetting("Порог", "Скорость доводки", 5.0F, 1.0F, 5.0F, 0.25F);
   public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только с мечом/топором/булавой в руке", true);
   private LivingEntity target;
   private Vec3d targetPos;

   public AimAssistant() {
      super("AimAssistant", "Мягко доводит прицел до цели", Category.COMBAT, -1);
      this.registerSetting(this.targetPlayers);
      this.registerSetting(this.targetAnimals);
      this.registerSetting(this.targetMonsters);
      this.registerSetting(this.throughWalls);
      this.registerSetting(this.threshold);
      this.registerSetting(this.onlyWeapon);
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   @Override
   protected void onDisable() {
      this.target = null;
      this.targetPos = null;
   }

   @Override
   public void onTick() {
      LivingEntity found = this.findTarget();
      if (found != this.target) {
         this.targetPos = null;
      }

      this.target = found;
   }

   public void onFrame() {
      if (this.isEnabled() && mc.player != null && mc.world != null) {
         if (mc.currentScreen == null) {
            if (this.isValidTarget(this.target) && !mc.player.isUsingItem()) {
               if (!this.onlyWeapon.get() || this.hasWeapon()) {
                  Vec3d position = AuraUtil.bestAimOffset(mc.player.getEyePos(), this.target, 3.0, this.throughWalls.get());
                  if (position == Vec3d.ZERO) {
                     return;
                  }

                  this.targetPos = this.targetPos == null ? position : this.targetPos.lerp(position, 0.2000000448441151);
                  float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(this.targetPos.z, this.targetPos.x)) - 90.0);
                  float pitch = (float)(-Math.toDegrees(Math.atan2(this.targetPos.y, Math.hypot(this.targetPos.x, this.targetPos.z))));
                  float deltaYaw = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
                  float deltaPitch = pitch - mc.player.getPitch();
                  if (Math.abs(deltaPitch) <= 13.0F
                     && Math.abs(deltaYaw) < 8.0F
                     && AuraUtil.rayHit(mc.player.getYaw(), mc.player.getPitch(), 3.0, this.target, this.throughWalls.get())) {
                     deltaPitch = 0.0F;
                  }

                  float frame = mc.getRenderTickCounter().getLastFrameDuration();
                  float ease = MathHelper.clamp((float)Math.hypot(deltaYaw, deltaPitch) / 4.0F, 0.0F, 1.0F);
                  float speed = this.threshold.get() * frame * ease;
                  if (speed <= 0.0F) {
                     return;
                  }

                  float step = Math.min(1.0F, speed / Math.max(Math.abs(deltaYaw), Math.abs(deltaPitch) * 2.0F));
                  mc.player.setYaw(mc.player.getYaw() + deltaYaw * step);
                  if (deltaPitch != 0.0F) {
                     mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + deltaPitch * step, -90.0F, 90.0F));
                  }
               }
            }
         }
      }
   }

   private LivingEntity findTarget() {
      if (mc.player != null && mc.world != null) {
         Vec3d eye = mc.player.getEyePos();
         Vec3d look = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
         LivingEntity best = null;
         double bestAngle = Double.MAX_VALUE;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && this.isValidTarget(living) && (this.throughWalls.get() || AuraUtil.surfaceVisible(eye, living, 4.0))) {
               double angle = Math.acos(MathHelper.clamp((float)look.dotProduct(living.getBoundingBox().getCenter().subtract(eye).normalize()), -1.0F, 1.0F));
               if (angle < bestAngle) {
                  bestAngle = angle;
                  best = living;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private boolean hasWeapon() {
      Item item = mc.player.getMainHandStack().getItem();
      return item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem;
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (entity != null && entity.isAlive() && !entity.isRemoved() && entity != mc.player) {
         return !AuraUtil.inReach(entity, 4.0 + mc.player.getVelocity().length() * 3.0) ? false : this.isTargetAllowed(entity);
      } else {
         return false;
      }
   }

   private boolean isTargetAllowed(LivingEntity entity) {
      if (entity instanceof PlayerEntity) {
         return this.targetPlayers.get();
      } else {
         return entity instanceof MobEntity ? this.targetMonsters.get() : entity instanceof AnimalEntity && this.targetAnimals.get();
      }
   }
}
