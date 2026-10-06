package ru.kirka.fluxclient.feature.impl.combat;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.MultiSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.draw.MathUtility;
import ru.kirka.fluxclient.rotation.MoveCorrection;
import ru.kirka.fluxclient.rotation.Rotation;
import ru.kirka.fluxclient.rotation.RotationHandler;
import ru.kirka.fluxclient.rotation.RotationMath;
import ru.kirka.fluxclient.rotation.RotationPriority;
import ru.kirka.fluxclient.rotation.modes.FunTimeRotationMode;
import ru.kirka.fluxclient.rotation.modes.HolyWorldRotationMode;
import ru.kirka.fluxclient.rotation.modes.OneTickRotationMode;
import ru.kirka.fluxclient.rotation.modes.ReallyWorldRotationMode;
import ru.kirka.fluxclient.rotation.modes.SlimeWorldRotationMode;
import ru.kirka.fluxclient.rotation.modes.SlothRotationMode;
import ru.kirka.fluxclient.rotation.modes.SpookyTimeRotationMode;
import ru.kirka.fluxclient.target.TargetComparators;
import ru.kirka.fluxclient.target.TargetManager;
import ru.kirka.fluxclient.target.TargetSettings;
import ru.kirka.fluxclient.util.Timer;
import ru.kirka.fluxclient.util.combat.CombatUtility;
import ru.kirka.fluxclient.util.combat.FallingPlayer;
import ru.kirka.fluxclient.util.combat.PerlinNoise;

public class Aura extends Module {
   public static volatile LivingEntity target = null;
   private static volatile Aura INSTANCE = null;
   public final NumberSetting attackDistance = new NumberSetting("Дистанция атаки", "Дистанция атаки в блоках", 3.0F, 0.1F, 6.0F, 0.1F);
   public final NumberSetting aimDistance = new NumberSetting("Дистанция наведения", "Дистанция поиска цели", 3.0F, 0.1F, 50.0F, 0.5F);
   public final NumberSetting minCps = new NumberSetting("Мин. CPS", "Минимальная скорость ударов в режиме 1.8", 8.0F, 1.0F, 20.0F, 1.0F);
   public final NumberSetting maxCps = new NumberSetting("Макс. CPS", "Максимальная скорость ударов в режиме 1.8", 12.0F, 1.0F, 20.0F, 1.0F);
   public final ModeSetting rotationMode = new ModeSetting(
      "Режим ротации",
      "Режим обхода античитов",
      "Simple",
      "Simple",
      "FunTime",
      "SpookyTime",
      "HolyWorld",
      "Intave",
      "OneTick",
      "ReallyWorld",
      "SlimeWorld",
      "Sloth",
      "None"
   );
   public final ModeSetting sortingMode = new ModeSetting("Сортировка", "Приоритет выбора цели", "Дистанция", "Дистанция", "Здоровье", "Прицел (FOV)");
   public final ModeSetting moveCorrectionMode = new ModeSetting(
      "Коррекция движения", "Выравнивание движения относительно ротации", "Silent", "Silent", "Direct", "None"
   );
   public final ModeSetting styleAttack = new ModeSetting("Стиль атаки", "Механика кулдауна ударов", "1.9 (Кулдаун)", "1.9 (Кулдаун)", "1.8 (CPS)");
   public final ModeSetting criticalMode = new ModeSetting(
      "Режим критов", "Порог готовности кулдауна для критического удара", "New (0.8)", "New (0.8)", "Old (0.93)"
   );
   public final MultiSetting targets = new MultiSetting(
      "Цели",
      "Фильтр целей",
      new BooleanSetting("Игроки", true),
      new BooleanSetting("Без брони", true),
      new BooleanSetting("Враждебные мобы", false),
      new BooleanSetting("Животные", false),
      new BooleanSetting("Невидимки", true),
      new BooleanSetting("Друзья", false)
   );
   public final MultiSetting targetingOptions = new MultiSetting(
      "Опции таргетинга",
      "Дополнительные параметры наведения",
      new BooleanSetting("Hit Vector", true),
      new BooleanSetting("Offhand Hit", true),
      new BooleanSetting("Strict Targeting", true)
   );
   public final BooleanSetting onlyCriticals = new BooleanSetting("Только криты", "Атаковать только при критическом ударе", true);
   public final BooleanSetting smartCriticals = new BooleanSetting("Умные криты", "Бить с земли, если цель ваншотнется", true);
   public final BooleanSetting walls = new BooleanSetting("Сквозь стены", "Атаковать через блоки", false);
   public final BooleanSetting rayTrace = new BooleanSetting("RayTrace", "Строгая проверка луча прицела", true);
   public final BooleanSetting noHitInv = new BooleanSetting("Не бить в инвентаре", "Пауза атаки при открытом контейнере", true);
   public final BooleanSetting targeting = new BooleanSetting("Фокус цели", "Удерживать выбранную цель", true);
   public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Атаковать только когда в руке меч, топор или булава", false);
   public final BooleanSetting autoMace = new BooleanSetting("Auto Mace", "Авто-свап на булаву при падении с высоты", true);
   public final NumberSetting autoMaceFallDistance = new NumberSetting("Падение булавы", "Мин. дистанция падения для булавы", 5.0F, 5.0F, 30.0F, 0.5F);
   private final Timer attackTimer = new Timer();
   private boolean shield = false;
   private PerlinNoise noise = new PerlinNoise();
   private long rotationStartTime = 0L;
   private float noiseFactor = 0.0F;
   private int attacks = 0;
   private Rotation additional = new Rotation(0.0F, 0.0F);
   private final HolyWorldRotationMode holyWorldRotationMode = new HolyWorldRotationMode();
   private final FunTimeRotationMode funTimeRotationModeImpl = new FunTimeRotationMode();
   private final SpookyTimeRotationMode spookyTimeRotationMode = new SpookyTimeRotationMode();
   private final OneTickRotationMode oneTickRotationMode = new OneTickRotationMode();
   private final ReallyWorldRotationMode reallyWorldRotationMode = new ReallyWorldRotationMode();
   private final SlimeWorldRotationMode slimeWorldRotationMode = new SlimeWorldRotationMode();
   private final SlothRotationMode slothRotationMode = new SlothRotationMode();

   public Aura() {
      super("Aura", "Автоматически атакует цели рядом с вами", Category.COMBAT, 82);
      INSTANCE = this;
      this.registerSetting(this.attackDistance);
      this.registerSetting(this.aimDistance);
      this.registerSetting(this.rotationMode);
      this.registerSetting(this.moveCorrectionMode);
      this.registerSetting(this.styleAttack);
      this.registerSetting(this.criticalMode);
      this.registerSetting(this.targets);
      this.registerSetting(this.targetingOptions);
      this.registerSetting(this.minCps);
      this.registerSetting(this.maxCps);
      this.registerSetting(this.onlyCriticals);
      this.registerSetting(this.smartCriticals);
      this.registerSetting(this.walls);
      this.registerSetting(this.rayTrace);
      this.registerSetting(this.noHitInv);
      this.registerSetting(this.targeting);
      this.registerSetting(this.onlyWeapon);
      this.registerSetting(this.autoMace);
      this.registerSetting(this.autoMaceFallDistance);
   }

   public static Aura getInstance() {
      return INSTANCE;
   }

   public static float[] pollSilentRotation() {
      if (INSTANCE == null || !INSTANCE.isEnabled()) {
         return null;
      } else {
         return target != null && target.isAlive() ? RotationHandler.getInstance().pollSilentRotation() : null;
      }
   }

   public LivingEntity getTarget() {
      return target;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.rotationStartTime = System.currentTimeMillis();
      this.noise = new PerlinNoise();
      this.noiseFactor = 1.0F;
      this.resetRotationModes();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      TargetManager.getInstance().reset();
      this.resetRotationModes();
      target = null;
   }

   @Override
   public void onTick() {
      if (mc.player == null || mc.world == null) {
         target = null;
      } else if (!this.noHitInv.get() || !(mc.currentScreen instanceof HandledScreen)) {
         TargetSettings.Builder builder = new TargetSettings.Builder()
            .targetPlayers(this.targets.isEnabled("Игроки"))
            .targetNakedPlayers(this.targets.isEnabled("Без брони"))
            .targetMobs(this.targets.isEnabled("Враждебные мобы"))
            .targetAnimals(this.targets.isEnabled("Животные"))
            .targetInvisibles(this.targets.isEnabled("Невидимки"))
            .targetFriends(this.targets.isEnabled("Друзья"))
            .requiredRange(this.aimDistance.get());
         if (this.sortingMode.is("Дистанция")) {
            builder.sortBy(TargetComparators.DISTANCE);
         } else if (this.sortingMode.is("Здоровье")) {
            builder.sortBy(TargetComparators.HEALTH);
         } else if (this.sortingMode.is("Прицел (FOV)")) {
            builder.sortBy(TargetComparators.FOV);
         }

         TargetSettings settings = builder.build();
         LivingEntity currentTarget = TargetManager.getInstance().getLivingTarget();
         boolean keepForcedTarget = this.targeting.get()
            && currentTarget != null
            && settings.isEntityValid(currentTarget)
            && mc.player.getEyePos().distanceTo(currentTarget.getBoundingBox().getCenter()) <= this.aimDistance.get().floatValue()
            && currentTarget.isAlive();
         if (!keepForcedTarget) {
            TargetManager.getInstance().update(settings);
            currentTarget = TargetManager.getInstance().getLivingTarget();
         }

         target = currentTarget;
         if (target != null) {
            this.rotateHead(target);
            if (this.shouldAttackEntity(target)) {
               this.attack(target);
            }
         } else {
            this.rotationStartTime = System.currentTimeMillis();
            this.noise = new PerlinNoise();
            this.noiseFactor = 1.0F;
            this.resetRotationModes();
         }

         this.funTimeRotationModeImpl.onTick();
         this.spookyTimeRotationMode.onTick();
         RotationHandler.getInstance().update();
      }
   }

   public void onFrame() {
      if (this.isEnabled()) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
         this.holyWorldRotationMode.onRender3D(tickDelta);
         RotationHandler.getInstance().updateRender(tickDelta);
      }
   }

   private boolean shouldAttackEntity(LivingEntity targetedEntity) {
      if (!this.isCooledDown()) {
         return false;
      } else if (this.onlyWeapon.get() && !isHoldingWeapon()) {
         return false;
      } else if (this.inRange(targetedEntity)) {
         return false;
      } else {
         if (!this.walls.get() && this.isSpookyTimeRotationSelected()) {
            BlockHitResult blockHit = mc.world
               .raycast(
                  new RaycastContext(
                     mc.player.getEyePos(),
                     mc.player.getEyePos().add(mc.player.getRotationVec(1.0F).multiply(this.attackDistance.get().floatValue())),
                     ShapeType.OUTLINE,
                     FluidHandling.NONE,
                     mc.player
                  )
               );
            if (blockHit != null && blockHit.getType() == Type.BLOCK) {
               return false;
            }
         }

         boolean canSeeTarget = this.canSeeTarget(targetedEntity);
         return !canSeeTarget && this.rayTrace.get() ? false : this.canAttackWithCriticalSettings(targetedEntity);
      }
   }

   private boolean canAttackWithCriticalSettings(LivingEntity targetedEntity) {
      return !this.shouldWaitForCritical(targetedEntity) || CombatUtility.canPerformCriticalHit(targetedEntity, true);
   }

   private boolean shouldWaitForCritical(LivingEntity targetedEntity) {
      return !this.onlyCriticals.get() && !this.smartCriticals.get()
         ? false
         : this.onlyCriticals.get() && !this.smartCriticals.get() || this.shouldHoldForSmartCritical(targetedEntity);
   }

   private boolean shouldHoldForSmartCritical(LivingEntity targetedEntity) {
      return this.canPrepareSmartCritical() && this.isCriticalRequired(targetedEntity);
   }

   private boolean canPrepareSmartCritical() {
      return mc.player != null
         && mc.world != null
         && !mc.player.isOnGround()
         && !mc.player.isGliding()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasVehicle()
         && !mc.player.isSubmergedInWater();
   }

   private boolean isCriticalRequired(LivingEntity targetedEntity) {
      float damage = this.calculateDamage(targetedEntity, false);
      return damage + 0.25F < targetedEntity.getHealth();
   }

   public boolean isCooledDown() {
      if (mc.player == null) {
         return false;
      } else {
         long fastDelay = this.getAttackDelayMs();
         float cooldownThreshold = this.criticalMode.is("New (0.8)") ? 0.8F : 0.93F;
         return mc.player.getAttackCooldownProgress(1.5F) > cooldownThreshold && this.attackTimer.finished(500L)
            || this.styleAttack.is("1.8 (CPS)") && this.attackTimer.finished(fastDelay);
      }
   }

   public float calculateDamage(LivingEntity targetedEntity, boolean includeCritical) {
      if (mc.player != null && targetedEntity != null) {
         float baseDamage = 1.0F;
         ItemStack held = mc.player.getMainHandStack();
         if (held.getItem() instanceof SwordItem || held.getItem() instanceof AxeItem || held.getItem() instanceof MaceItem) {
            baseDamage = 7.0F;
         }

         float cooldown = mc.player.getAttackCooldownProgress(0.0F);
         float damage = baseDamage * (0.2F + cooldown * cooldown * 0.8F);
         if (includeCritical) {
            damage *= 1.5F;
         }

         float armor = targetedEntity.getArmor();
         float armorPart = MathHelper.clamp(armor - damage / 4.0F, armor * 0.2F, 20.0F);
         damage *= 1.0F - armorPart / 25.0F;
         return Math.max(0.0F, damage);
      } else {
         return 0.0F;
      }
   }

   private void attack(LivingEntity targetedEntity) {
      this.attackTargetWithAutoMace(targetedEntity);
   }

   private void attackTargetWithAutoMace(LivingEntity targetedEntity) {
      if (!this.shouldUseAutoMace(targetedEntity)) {
         this.attackTarget(targetedEntity);
      } else if (mc.player.getMainHandStack().isOf(Items.MACE)) {
         this.attackTarget(targetedEntity);
      } else {
         int previousSlot = mc.player.getInventory().selectedSlot;
         int maceHotbarSlot = CombatUtility.findMaceHotbarSlot();
         if (maceHotbarSlot != -1) {
            this.attackWithMaceSlot(targetedEntity, maceHotbarSlot, previousSlot);
         } else {
            this.attackTarget(targetedEntity);
         }
      }
   }

   private void attackWithMaceSlot(LivingEntity targetedEntity, int maceSlot, int previousSlot) {
      if (maceSlot == previousSlot) {
         this.attackTarget(targetedEntity);
      } else {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
         this.attackTarget(targetedEntity);
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
      }
   }

   private boolean shouldUseAutoMace(LivingEntity targetedEntity) {
      float requiredFallDistance = Math.max(5.0F, this.autoMaceFallDistance.get());
      return mc.player != null
         && targetedEntity != null
         && this.autoMace.get()
         && !mc.player.isOnGround()
         && !mc.player.isGliding()
         && !mc.player.isTouchingWater()
         && mc.player.getVelocity().y <= -0.35
         && mc.player.fallDistance >= requiredFallDistance;
   }

   private void attackTarget(LivingEntity targetedEntity) {
      if (mc.interactionManager != null && mc.player != null) {
         this.shield = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD);
         if (this.shield) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
         }

         if (CombatUtility.shouldBreakShield(targetedEntity) && CombatUtility.canBreakShield(targetedEntity)) {
            CombatUtility.tryBreakShield(targetedEntity);
         }

         mc.interactionManager.attackEntity(mc.player, targetedEntity);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (this.shield) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         }

         this.additional = new Rotation(MathUtility.random(5.0, 20.0), MathUtility.random(5.0, 10.0));
         this.attackTimer.reset();
         this.attacks++;
      }
   }

   private void rotateHead(LivingEntity targetedEntity) {
      if ((!this.onlyWeapon.get() || isHoldingWeapon()) && !this.rotationMode.is("None")) {
         MoveCorrection moveCorrection;
         if (this.moveCorrectionMode.is("Silent")) {
            moveCorrection = MoveCorrection.SILENT;
         } else if (this.moveCorrectionMode.is("Direct")) {
            moveCorrection = MoveCorrection.DIRECT;
         } else {
            moveCorrection = MoveCorrection.NONE;
         }

         RotationHandler handler = RotationHandler.getInstance();
         String mode = this.rotationMode.get();
         if (mode.equals("Simple")) {
            Rotation rot = RotationMath.getRotationTo(RotationMath.getNearestPoint(targetedEntity));
            handler.rotate(rot, moveCorrection, 180.0F, 180.0F, 180.0F, RotationPriority.TO_TARGET);
         } else if (mode.equals("HolyWorld")) {
            this.holyWorldRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("FunTime")) {
            this.funTimeRotationModeImpl.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("SpookyTime")) {
            this.spookyTimeRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("OneTick")) {
            this.oneTickRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("ReallyWorld")) {
            this.reallyWorldRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("SlimeWorld")) {
            this.slimeWorldRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("Sloth")) {
            this.slothRotationMode.rotate(this, handler, targetedEntity, moveCorrection);
         } else if (mode.equals("Intave")) {
            this.rotateIntave(targetedEntity, handler, moveCorrection);
         }
      }
   }

   private void rotateIntave(LivingEntity targetedEntity, RotationHandler handler, MoveCorrection moveCorrection) {
      if (mc.player.age % 500 == 0) {
         this.noise = new PerlinNoise();
         this.noiseFactor = 1.0F;
      }

      Vec3d nearY = RotationMath.getNearestPoint(targetedEntity);
      Box box = targetedEntity.getBoundingBox();
      Rotation targetRot = RotationMath.getRotationTo(
         new Vec3d(nearY.x, MathHelper.clamp(MathUtility.interpolate(mc.player.getY(), targetedEntity.getY(), 0.5), box.minY, box.maxY), nearY.z)
      );
      boolean idle = this.attackTimer.finished(300L);
      float targetYaw = targetRot.getYaw();
      float targetPitch = targetRot.getPitch();
      Rotation currentRot = handler.getCurrentRotation();
      float currentYaw = currentRot.getYaw();
      float currentPitch = currentRot.getPitch();
      float yawDiff = RotationMath.getAngleDifference(currentYaw, targetYaw);
      float pitchDiff = RotationMath.getAngleDifference(currentPitch, targetPitch);
      if (idle) {
         if (this.shouldPreventSprinting()) {
            targetYaw += 5.0F;
            targetPitch -= 10.0F;
         } else {
            targetYaw -= 5.0F;
         }
      }

      float yawSpeed = Math.max((90.0F - Math.abs(yawDiff)) / (idle ? (mc.player.fallDistance > 0.0F ? 20.0F : 60.0F) : 40.0F), MathUtility.random(1.0, 5.0))
         * MathUtility.random(0.9, 1.1);
      float pitchSpeed = Math.abs(pitchDiff) / (idle ? (mc.player.fallDistance > 0.0F ? 60.0F : 100.0F) : 30.0F) * MathUtility.random(0.9, 1.1);
      long timeElapsed = System.currentTimeMillis() - this.rotationStartTime;
      float yawNoise = (float)this.noise.noise(timeElapsed * 5.0E-4);
      float pitchNoise = (float)this.noise.noise(timeElapsed * 5.0E-4, 10.0);
      float yawOffset = yawNoise * 25.0F * this.noiseFactor;
      float pitchOffset = pitchNoise * 25.0F * this.noiseFactor;
      float totalDiff = Math.abs(yawDiff) + Math.abs(pitchDiff);
      if (totalDiff < 10.0F) {
         this.noiseFactor = Math.max(0.0F, this.noiseFactor - 0.05F);
      }

      handler.rotate(
         new Rotation(targetYaw + yawOffset, MathHelper.clamp(targetPitch + pitchOffset, -90.0F, 90.0F)),
         moveCorrection,
         yawSpeed * 25.0F,
         pitchSpeed * 25.0F,
         MathUtility.random(5.0, 50.0),
         RotationPriority.TO_TARGET
      );
   }

   private boolean canSeeTarget(LivingEntity target) {
      Rotation currentRotation = RotationHandler.getInstance().getCurrentRotation();
      return CombatUtility.canTraceWithBlock(
         this.attackDistance.get().floatValue(), currentRotation.getYaw(), currentRotation.getPitch(), mc.player, target, !this.noHitInv.get()
      );
   }

   private long getAttackDelayMs() {
      int min = Math.round(this.minCps.get());
      int max = Math.round(this.maxCps.get());
      if (min > max) {
         int swap = min;
         min = max;
         max = swap;
      }

      int cps = (int)Math.floor(MathUtility.random(min, max + 1));
      return Math.max(1L, 1000L / Math.max(1, cps));
   }

   public boolean shouldPreventSprinting() {
      LivingEntity cur = target;
      if (cur == null || mc.player == null) {
         return false;
      } else if (this.styleAttack.is("1.8 (CPS)")) {
         return false;
      } else {
         boolean predict = !mc.player.isOnGround() && FallingPlayer.fromPlayer(mc.player).findFall(CombatUtility.getFallDistance(cur));
         return this.shouldWaitForCritical(cur) && (predict || CombatUtility.canPerformCriticalHit(cur, true) || !this.attackTimer.finished(50L));
      }
   }

   private boolean inRange(LivingEntity target) {
      return mc.player == null ? true : mc.player.getEyePos().distanceTo(RotationMath.getNearestPoint(target)) > this.attackDistance.get().floatValue();
   }

   public boolean isRotationTargetValid(LivingEntity target) {
      return mc.player != null && mc.world != null && target != null && target.isAlive() && !this.inRange(target);
   }

   public boolean isReadyToAttackNow() {
      return this.isCooledDown();
   }

   public float getAttackDistanceValue() {
      return this.attackDistance.get();
   }

   public boolean isWallsEnabled() {
      return this.walls.get();
   }

   public boolean isNoHitInvEnabled() {
      return this.noHitInv.get();
   }

   public boolean isHitVectorModeEnabled() {
      return this.targetingOptions.isEnabled("Hit Vector");
   }

   public boolean isHolyWorldRotationSelected() {
      return this.rotationMode.is("HolyWorld");
   }

   public boolean isSpookyTimeRotationSelected() {
      return this.rotationMode.is("SpookyTime");
   }

   public boolean isFunTimeRotationSelected() {
      return this.rotationMode.is("FunTime");
   }

   public ModeSetting getFastPvp() {
      return this.styleAttack;
   }

   public boolean isFastPvp() {
      return this.styleAttack.is("1.8 (CPS)");
   }

   public int getAttacks() {
      return this.attacks;
   }

   public static boolean isHoldingWeapon() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack held = mc.player.getMainHandStack();
         return !held.isEmpty()
            && (
               held.getItem() instanceof SwordItem
                  || held.getItem() instanceof AxeItem
                  || held.getItem() instanceof TridentItem
                  || held.getItem() instanceof MaceItem
            );
      }
   }

   private void resetRotationModes() {
      this.holyWorldRotationMode.reset();
      this.funTimeRotationModeImpl.reset();
      this.spookyTimeRotationMode.reset();
      this.oneTickRotationMode.reset();
      this.reallyWorldRotationMode.reset();
      this.slimeWorldRotationMode.reset();
      this.slothRotationMode.reset();
   }
}
