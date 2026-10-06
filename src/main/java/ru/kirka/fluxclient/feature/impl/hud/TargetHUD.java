package ru.kirka.fluxclient.feature.impl.hud;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.combat.Aura;

public class TargetHUD extends Module {
   public final BooleanSetting crosshairTarget = new BooleanSetting("По прицелу", "Захватывать цель при наведении крестика", true);
   public final BooleanSetting auraTarget = new BooleanSetting("По Киллауре", "Захватывать цель от KillAura", true);
   public final NumberSetting maxDistance = new NumberSetting("Дистанция", "Дальность захвата по прицелу", 12.0F, 3.0F, 20.0F, 1.0F);
   public final NumberSetting fadeDelay = new NumberSetting("Задержка (сек)", "Сколько показывать цель после отведения прицела", 1.5F, 0.5F, 4.0F, 0.5F);
   public final ModeSetting style = new ModeSetting("Стиль", "Вид панели цели", "Полный", "Полный", "Компакт");
   private LivingEntity lastTarget = null;
   private long lastTargetTime = 0L;

   public TargetHUD() {
      super("TargetHUD", "Информационная панель текущей цели", Category.HUD, -1);
      this.registerSetting(this.crosshairTarget);
      this.registerSetting(this.auraTarget);
      this.registerSetting(this.maxDistance);
      this.registerSetting(this.fadeDelay);
      this.registerSetting(this.style);
   }

   public LivingEntity getTarget() {
      if (mc.player != null && mc.world != null) {
         LivingEntity current = null;
         if (this.auraTarget.get() && Aura.target != null && Aura.target.isAlive()) {
            current = Aura.target;
         }

         if (current == null && this.crosshairTarget.get()) {
            current = this.getEntityUnderCrosshair();
         }

         long now = System.currentTimeMillis();
         if (current != null) {
            this.lastTarget = current;
            this.lastTargetTime = now;
            return current;
         } else {
            return this.lastTarget != null && this.lastTarget.isAlive() && (float)(now - this.lastTargetTime) < this.fadeDelay.get() * 1000.0F
               ? this.lastTarget
               : null;
         }
      } else {
         return null;
      }
   }

   private LivingEntity getEntityUnderCrosshair() {
      if (mc.targetedEntity instanceof LivingEntity living && this.isValid(living)) {
         return living;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d lookVec = mc.player.getRotationVec(1.0F).normalize();
         double maxDist = this.maxDistance.get().floatValue();
         LivingEntity closest = null;
         double closestDist = maxDist;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && this.isValid(living)) {
               Vec3d toEntity = living.getEyePos().subtract(eyePos);
               double dist = toEntity.length();
               if (dist <= closestDist) {
                  double dot = lookVec.dotProduct(toEntity.normalize());
                  if (dot > 0.985) {
                     closestDist = dist;
                     closest = living;
                  }
               }
            }
         }

         return closest;
      }
   }

   private boolean isValid(LivingEntity entity) {
      return entity != mc.player && entity.isAlive();
   }
}
