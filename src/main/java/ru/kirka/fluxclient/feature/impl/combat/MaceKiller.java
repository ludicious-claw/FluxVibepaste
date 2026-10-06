package ru.kirka.fluxclient.feature.impl.combat;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.util.FriendUtil;
import ru.kirka.fluxclient.util.combat.CombatUtility;

public class MaceKiller extends Module {
   public final BooleanSetting chatLogs = new BooleanSetting("Логи в чат", "Выводить информационные сообщения модуля в чат", true);
   public final BooleanSetting maceProtect = new BooleanSetting("Защита от булавы", "Автоматически доставать щит и блокировать при угрозе булавы сверху", true);
   public final BooleanSetting targetPlayers = new BooleanSetting("Игроки", "Атаковать других игроков", true);
   public final BooleanSetting targetMobs = new BooleanSetting("Мобы", "Атаковать мобов и монстров (зомби, криперы, боссы и др.)", true);
   public final NumberSetting approachDistance = new NumberSetting(
      "Дистанция подхода", "Подходить к цели на это расстояние перед взлетом", 4.0F, 2.0F, 8.0F, 0.5F
   );
   public final BooleanSetting autoWindCharge = new BooleanSetting("Авто-заряд ветра", "Автоматически кидать заряды ветра под себя для взлета", true);
   public final BooleanSetting airStrafe = new BooleanSetting("WASD в воздухе", "Стрейфиться к цели в воздухе стандартным движением WASD", true);
   public final NumberSetting targetRange = new NumberSetting("Дистанция поиска", "Максимальная дистанция обнаружения цели", 25.0F, 5.0F, 50.0F, 1.0F);
   public final NumberSetting launchDelay = new NumberSetting(
      "Задержка взлета", "Задержка в тиках между приземлением и следующим взлетом", 12.0F, 4.0F, 40.0F, 1.0F
   );
   private MaceKiller.State state = MaceKiller.State.GROUND_WAIT;
   private int stateTicks = 0;
   private int groundCooldown = 0;
   private double launchY = 0.0;
   private LivingEntity currentTarget = null;
   private boolean isBlockingMace = false;
   private LivingEntity currentThreat = null;
   private boolean noWindChargeNotified = false;
   private boolean noMaceNotified = false;
   private boolean hitLandedInFall = false;

   public MaceKiller() {
      super("MaceKiller", "Автоматический бой булавой: подход, взлет ветром, прямое наведение и авто-щит", Category.COMBAT, -1);
      this.registerSetting(this.chatLogs);
      this.registerSetting(this.maceProtect);
      this.registerSetting(this.targetPlayers);
      this.registerSetting(this.targetMobs);
      this.registerSetting(this.approachDistance);
      this.registerSetting(this.autoWindCharge);
      this.registerSetting(this.airStrafe);
      this.registerSetting(this.targetRange);
      this.registerSetting(this.launchDelay);
   }

   @Override
   protected void onEnable() {
      this.state = MaceKiller.State.GROUND_WAIT;
      this.stateTicks = 0;
      this.groundCooldown = (int)this.launchDelay.get().floatValue();
      this.currentTarget = null;
      this.currentThreat = null;
      this.isBlockingMace = false;
      this.noWindChargeNotified = false;
      this.noMaceNotified = false;
      this.hitLandedInFall = false;
      this.logChat("§aMaceKiller активирован.");
      this.ensureAutoTotemEnabled();
   }

   @Override
   protected void onDisable() {
      this.releaseMovement();
      this.stopBlocking();
      this.currentTarget = null;
      this.currentThreat = null;
      this.isBlockingMace = false;
      this.logChat("§cMaceKiller деактивирован.");
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen instanceof HandledScreen) {
            this.releaseMovement();
            this.stopBlocking();
         } else {
            this.ensureAutoTotemEnabled();
            if (this.maceProtect.get()) {
               LivingEntity threat = this.findMaceThreat();
               if (threat != null) {
                  this.handleMaceDefense(threat);
                  return;
               }

               if (this.isBlockingMace) {
                  this.stopMaceDefense();
               }
            } else if (this.isBlockingMace) {
               this.stopMaceDefense();
            }

            int maceSlot = CombatUtility.findMaceHotbarSlot();
            if (maceSlot == -1) {
               int invMace = this.findItemInInventory(Items.MACE);
               if (invMace == -1) {
                  if (!this.noMaceNotified) {
                     this.logChat("§cВ инвентаре не найдена булава (Mace)!");
                     this.noMaceNotified = true;
                  }

                  this.releaseMovement();
                  return;
               }

               this.swapSlotToHotbar(invMace, 0);
               maceSlot = 0;
            }

            this.noMaceNotified = false;
            this.currentTarget = this.findTarget(this.targetRange.get().doubleValue());
            if (this.currentTarget != null && this.currentTarget.isAlive()) {
               this.stateTicks++;
               switch (this.state) {
                  case GROUND_WAIT:
                     this.handleGroundWait(maceSlot);
                     break;
                  case APPROACHING:
                     this.handleApproaching(maceSlot);
                     break;
                  case LAUNCHING:
                     this.handleLaunching(maceSlot);
                     break;
                  case AIR_ASCENDING:
                     this.handleAirAscending(maceSlot);
                     break;
                  case AIR_DESCENDING:
                     this.handleAirDescending(maceSlot);
               }
            } else {
               if (this.state != MaceKiller.State.GROUND_WAIT) {
                  this.state = MaceKiller.State.GROUND_WAIT;
                  this.releaseMovement();
               }
            }
         }
      }
   }

   private void handleGroundWait(int maceSlot) {
      this.releaseMovement();
      this.selectHotbarSlot(maceSlot);
      if (this.groundCooldown > 0) {
         this.groundCooldown--;
      } else if (!mc.player.isOnGround()) {
         if (mc.player.getVelocity().y < -0.1) {
            this.transitionTo(MaceKiller.State.AIR_DESCENDING);
         }
      } else {
         double dist = mc.player.distanceTo(this.currentTarget);
         double maxApproach = this.approachDistance.get().doubleValue();
         if (dist > maxApproach) {
            this.transitionTo(MaceKiller.State.APPROACHING);
         } else {
            this.prepareLaunchOrFight(maceSlot);
         }
      }
   }

   private void handleApproaching(int maceSlot) {
      this.selectHotbarSlot(maceSlot);
      double dist = mc.player.distanceTo(this.currentTarget);
      double maxApproach = this.approachDistance.get().doubleValue();
      this.smoothRotateTowards(this.currentTarget, 14.0F, 8.0F);
      if (dist <= maxApproach) {
         this.releaseMovement();
         this.logChat(String.format("§6[Подход] §fПодошел к цели §e%s §fна %.1f б. Начинаю атаку булавой!", this.currentTarget.getName().getString(), dist));
         this.prepareLaunchOrFight(maceSlot);
      } else {
         mc.options.forwardKey.setPressed(true);
         mc.options.sprintKey.setPressed(mc.player.getHungerManager().getFoodLevel() > 6);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         if (mc.player.horizontalCollision && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
         } else {
            mc.options.jumpKey.setPressed(false);
         }
      }
   }

   private void prepareLaunchOrFight(int maceSlot) {
      if (!this.autoWindCharge.get()) {
         this.tryAttackEntity(this.currentTarget, maceSlot);
      } else {
         int windChargeSlot = this.ensureWindChargeInHotbar();
         if (windChargeSlot == -1) {
            if (!this.noWindChargeNotified) {
               this.logChat("§cЗаряды ветра (Wind Charge) закончились! Веду ближний бой булавой.");
               this.noWindChargeNotified = true;
            }

            this.tryAttackEntity(this.currentTarget, maceSlot);
         } else {
            this.noWindChargeNotified = false;
            this.transitionTo(MaceKiller.State.LAUNCHING);
         }
      }
   }

   private void handleLaunching(int maceSlot) {
      int windChargeSlot = this.ensureWindChargeInHotbar();
      if (windChargeSlot == -1) {
         this.transitionTo(MaceKiller.State.GROUND_WAIT);
      } else {
         mc.player.setPitch(89.5F);
         this.selectHotbarSlot(windChargeSlot);
         mc.player.jump();
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.selectHotbarSlot(maceSlot);
         this.launchY = mc.player.getY();
         this.hitLandedInFall = false;
         this.logChat("§6[Взлёт] §fЗаряд ветра брошен под ноги! Набираю высоту...");
         this.transitionTo(MaceKiller.State.AIR_ASCENDING);
      }
   }

   private void handleAirAscending(int maceSlot) {
      this.selectHotbarSlot(maceSlot);
      if (this.currentTarget != null) {
         this.smoothRotateTowards(this.currentTarget, 18.0F, 12.0F);
         if (this.airStrafe.get()) {
            mc.options.forwardKey.setPressed(true);
            mc.options.sprintKey.setPressed(true);
         }
      }

      double vy = mc.player.getVelocity().y;
      boolean apexReached = vy <= 0.08 || mc.player.getY() - this.launchY >= 2.5 && vy <= 0.2;
      if (apexReached || this.stateTicks >= 14) {
         this.logChat("§6[Пик высоты] §fВзлёт завершён! Падаю на цель: §e" + (this.currentTarget != null ? this.currentTarget.getName().getString() : "враг"));
         this.transitionTo(MaceKiller.State.AIR_DESCENDING);
      }
   }

   private void handleAirDescending(int maceSlot) {
      this.selectHotbarSlot(maceSlot);
      if (this.currentTarget != null) {
         this.smoothRotateTowards(this.currentTarget, 22.0F, 16.0F);
         if (this.airStrafe.get()) {
            mc.options.forwardKey.setPressed(true);
            mc.options.sprintKey.setPressed(true);
         }

         this.tryAttackEntity(this.currentTarget, maceSlot);
      }

      if (mc.player.isOnGround()) {
         this.releaseMovement();
         this.groundCooldown = (int)this.launchDelay.get().floatValue();
         this.transitionTo(MaceKiller.State.GROUND_WAIT);
      }
   }

   private void tryAttackEntity(LivingEntity target, int maceSlot) {
      if (mc.player != null && mc.interactionManager != null && target != null && target.isAlive()) {
         this.selectHotbarSlot(maceSlot);
         double distance = mc.player.distanceTo(target);
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d targetCenter = target.getBoundingBox().getCenter();
         double eyeDistance = eyePos.distanceTo(targetCenter);
         if (distance <= 3.5 || eyeDistance <= 3.8) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5F);
            if (cooldown >= 0.85F) {
               mc.interactionManager.attackEntity(mc.player, target);
               mc.player.swingHand(Hand.MAIN_HAND);
               if (!this.hitLandedInFall) {
                  this.hitLandedInFall = true;
                  double fallDist = Math.max((double)mc.player.fallDistance, this.launchY > 0.0 ? this.launchY - mc.player.getY() : 0.0);
                  this.logChat(
                     String.format("§a[Удар булавой] §fСокрушительный удар по §e%s §f(падение: §c%.1f §fб.)!", target.getName().getString(), fallDist)
                  );
               }
            }
         }
      }
   }

   private void transitionTo(MaceKiller.State newState) {
      this.state = newState;
      this.stateTicks = 0;
   }

   private void smoothRotateTowards(Entity target, float maxStepYaw, float maxStepPitch) {
      if (mc.player != null && target != null) {
         Vec3d eyes = mc.player.getEyePos();
         Vec3d targetCenter = target.getBoundingBox().getCenter();
         double dx = targetCenter.x - eyes.x;
         double dy = targetCenter.y - eyes.y;
         double dz = targetCenter.z - eyes.z;
         double horiz = Math.hypot(dx, dz);
         float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
         float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, horiz)));
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         float diffYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
         float diffPitch = targetPitch - currentPitch;
         float factorYaw = Math.min(Math.abs(diffYaw) / 30.0F, 1.0F);
         float factorPitch = Math.min(Math.abs(diffPitch) / 30.0F, 1.0F);
         float speedYaw = 6.0F + factorYaw * (maxStepYaw - 6.0F);
         float speedPitch = 4.0F + factorPitch * (maxStepPitch - 4.0F);
         float stepYaw = MathHelper.clamp(diffYaw * 0.4F, -speedYaw, speedYaw);
         float stepPitch = MathHelper.clamp(diffPitch * 0.35F, -speedPitch, speedPitch);
         mc.player.setYaw(currentYaw + stepYaw);
         mc.player.setPitch(MathHelper.clamp(currentPitch + stepPitch, -90.0F, 90.0F));
      }
   }

   private void releaseMovement() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }
   }

   private LivingEntity findMaceThreat() {
      if (mc.world != null && mc.player != null) {
         LivingEntity bestThreat = null;
         double bestDist = Double.MAX_VALUE;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living
               && living != mc.player
               && living.isAlive()
               && !(living.getHealth() <= 0.0F)
               && !living.isSpectator()
               && !(living instanceof PlayerEntity player && (player.isCreative() || FriendUtil.isFriend(player.getName().getString())))) {
               boolean holdingMace = living.getMainHandStack().isOf(Items.MACE) || living.getOffHandStack().isOf(Items.MACE);
               if (holdingMace) {
                  double dy = living.getY() - mc.player.getY();
                  if (!(dy < 1.2) && !(dy > 30.0)) {
                     double dx = living.getX() - mc.player.getX();
                     double dz = living.getZ() - mc.player.getZ();
                     double horiz = Math.hypot(dx, dz);
                     if (!(horiz > 5.5) && !(horiz >= bestDist)) {
                        boolean isFalling = living.getVelocity().y < -0.05 || living.fallDistance > 0.3F;
                        if (isFalling) {
                           bestDist = horiz;
                           bestThreat = living;
                        }
                     }
                  }
               }
            }
         }

         return bestThreat;
      } else {
         return null;
      }
   }

   private void handleMaceDefense(LivingEntity threat) {
      this.currentThreat = threat;
      this.releaseMovement();
      this.smoothRotateTowards(threat, 22.0F, 16.0F);
      this.ensureShieldEquipped();
      if (!this.isBlockingMace) {
         this.isBlockingMace = true;
         this.logChat("§c[MaceProtect] §eУгроза! Враг §c" + threat.getName().getString() + " §eлетит сверху с булавой! Достаю щит и блокирую (ПКМ)!");
      }

      mc.options.useKey.setPressed(true);
   }

   private void stopMaceDefense() {
      if (this.isBlockingMace) {
         this.isBlockingMace = false;
         this.stopBlocking();
         if (this.currentThreat != null) {
            this.logChat("§a[MaceProtect] §aУдар булавой цели §e" + this.currentThreat.getName().getString() + " §aуспешно отражен щитом! Опускаю щит.");
            this.currentThreat = null;
         }
      }
   }

   private void stopBlocking() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }
   }

   private void ensureShieldEquipped() {
      if (mc.player != null && mc.interactionManager != null) {
         if (!mc.player.getOffHandStack().isOf(Items.SHIELD)) {
            int hotbarShield = this.findItemInHotbar(Items.SHIELD);
            if (hotbarShield != -1) {
               this.selectHotbarSlot(hotbarShield);
            } else {
               int invShield = this.findItemInInventory(Items.SHIELD);
               if (invShield != -1) {
                  this.swapSlotToOffhand(invShield);
               }
            }
         }
      }
   }

   private void ensureAutoTotemEnabled() {
      AutoTotem autoTotem = this.getModule(AutoTotem.class);
      if (autoTotem != null && !autoTotem.isEnabled()) {
         autoTotem.setEnabled(true);
         this.logChat("§aAutoTotem автоматически активирован.");
      }
   }

   private <T extends Module> T getModule(Class<T> clazz) {
      return FluxContext.get() != null && FluxContext.get().getModuleManager() != null ? FluxContext.get().getModuleManager().getModule(clazz) : null;
   }

   private int ensureWindChargeInHotbar() {
      int hotbarSlot = this.findItemInHotbar(Items.WIND_CHARGE);
      if (hotbarSlot != -1) {
         return hotbarSlot;
      } else {
         int invSlot = this.findItemInInventory(Items.WIND_CHARGE);
         if (invSlot != -1) {
            int targetHotbar = this.findReplaceableHotbarSlot();
            this.swapSlotToHotbar(invSlot, targetHotbar);
            return targetHotbar;
         } else {
            return -1;
         }
      }
   }

   private int findReplaceableHotbarSlot() {
      if (mc.player == null) {
         return 1;
      } else {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.MACE) && !stack.isOf(Items.SHIELD) && !stack.isOf(Items.TOTEM_OF_UNDYING)) {
               return i;
            }
         }

         return 1;
      }
   }

   private int findItemInHotbar(Item item) {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   private int findItemInInventory(Item item) {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   private void selectHotbarSlot(int slot) {
      if (mc.player != null && slot >= 0 && slot < 9 && mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
      }
   }

   private void swapSlotToOffhand(int screenSlot) {
      if (mc.player != null && mc.interactionManager != null) {
         try {
            int syncId = mc.player.playerScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, screenSlot, 40, SlotActionType.SWAP, mc.player);
         } catch (Exception var3) {
         }
      }
   }

   private void swapSlotToHotbar(int invSlot, int targetHotbarSlot) {
      if (mc.player != null && mc.interactionManager != null) {
         try {
            int syncId = mc.player.playerScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, invSlot, targetHotbarSlot, SlotActionType.SWAP, mc.player);
         } catch (Exception var4) {
         }
      }
   }

   private LivingEntity findTarget(double maxDistance) {
      if (mc.world != null && mc.player != null) {
         LivingEntity best = null;
         double bestDistSq = maxDistance * maxDistance;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living
               && living != mc.player
               && living.isAlive()
               && !(living.getHealth() <= 0.0F)
               && !(living instanceof ArmorStandEntity)
               && !(
                  living instanceof PlayerEntity player
                     ? !this.targetPlayers.get() || player.isSpectator() || player.isCreative() || FriendUtil.isFriend(player.getName().getString())
                     : !(living instanceof MobEntity) && !(living instanceof HostileEntity) && !(living instanceof SlimeEntity) || !this.targetMobs.get()
               )) {
               double distSq = mc.player.squaredDistanceTo(living);
               if (distSq < bestDistSq) {
                  bestDistSq = distSq;
                  best = living;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void logChat(String message) {
      try {
         if (!this.chatLogs.get()) {
            return;
         }

         if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§6[MaceKiller] §f" + message));
         }
      } catch (Exception var3) {
      }
   }

   private static enum State {
      GROUND_WAIT,
      APPROACHING,
      LAUNCHING,
      AIR_ASCENDING,
      AIR_DESCENDING;
   }
}
