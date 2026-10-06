package ru.kirka.fluxclient.util.combat;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.util.ServerUtility;

public final class CombatUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private CombatUtility() {
   }

   public static float getFallDistance(LivingEntity target) {
      if (mc.player != null && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
         return 0.7F;
      } else {
         int maceSlot = findMaceHotbarSlot();
         if (maceSlot != -1) {
            return 0.7F;
         } else if (!ServerUtility.isFS() && !ServerUtility.isST()) {
            return 0.0F;
         } else {
            Aura aura = Aura.getInstance();
            if (aura != null && aura.getAttacks() % 10 == 0) {
               return 0.4F;
            } else {
               return aura != null && aura.getAttacks() % 5 == 0 ? 0.2F : 0.0F;
            }
         }
      }
   }

   public static int findMaceHotbarSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) {
               return i;
            }
         }

         return -1;
      }
   }

   public static int findAxeHotbarSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean canPerformCriticalHit(LivingEntity target, boolean ignoreSprint) {
      if (mc.world != null && mc.player != null) {
         Aura aura = Aura.getInstance();
         return canDealCriticalDamage(target, ignoreSprint)
            || mc.player.isClimbing()
            || mc.currentScreen instanceof HandledScreen
            || mc.player.isTouchingWater() && mc.player.fallDistance <= 0.0F
            || mc.player.hasVehicle()
            || mc.player.isSubmergedInWater()
            || aura != null && aura.isFastPvp()
            || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            || mc.player.hasStatusEffect(StatusEffects.SLOWNESS)
            || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
      } else {
         return false;
      }
   }

   public static boolean canDealCriticalDamage(LivingEntity target, boolean ignoreSprint) {
      if (mc.world != null && mc.player != null && target != null) {
         boolean sprintCheck = ignoreSprint || !mc.player.isSprinting();
         boolean falling = !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.hasVehicle()
            && !mc.player.isSubmergedInWater()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !mc.player.hasStatusEffect(StatusEffects.SLOWNESS)
            && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
         if (!sprintCheck || !falling) {
            return false;
         } else {
            return mc.player.getMainHandStack().getItem() instanceof MaceItem
               ? mc.player.fallDistance > 1.0F
               : mc.player.fallDistance > getFallDistance(target);
         }
      } else {
         return false;
      }
   }

   public static boolean shouldBreakShield(LivingEntity target) {
      return target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem;
   }

   public static boolean canBreakShield(LivingEntity target) {
      if (mc.player != null && !mc.player.isDead() && !target.isDead()) {
         int axeSlot = findAxeHotbarSlot();
         if (axeSlot == -1) {
            return false;
         } else {
            Vec3d facingVector = target.getRotationVector();
            Vec3d deltaPos = new Vec3d(target.getX() - mc.player.getX(), 0.0, target.getZ() - mc.player.getZ());
            return deltaPos.dotProduct(facingVector) < 0.0;
         }
      } else {
         return false;
      }
   }

   public static void tryBreakShield(LivingEntity target) {
      if (mc.player != null && mc.interactionManager != null && mc.player.networkHandler != null) {
         int axeSlot = findAxeHotbarSlot();
         if (axeSlot != -1 && target instanceof PlayerEntity && target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
         }
      }
   }

   public static boolean stalin(LivingEntity target) {
      if (mc.world == null) {
         return false;
      } else {
         Vec3d pos = target.getPos();
         Box hitbox = target.getBoundingBox();
         float off = 0.05F;
         return !isAir(hitbox.minX - off, pos.y, hitbox.minZ - off)
            || !isAir(hitbox.maxX + off, pos.y, hitbox.minZ - off)
            || !isAir(hitbox.minX - off, pos.y, hitbox.maxZ + off)
            || !isAir(hitbox.maxX + off, pos.y, hitbox.maxZ + off);
      }
   }

   private static boolean isAir(double x, double y, double z) {
      return mc.world == null ? true : mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir();
   }

   public static boolean canTraceWithBlock(double rayTraceDistance, float yaw, float pitch, Entity entity, Entity target, boolean checkBlocks) {
      if (target != null && entity != null && mc.world != null) {
         float partialTicks = mc.getRenderTickCounter().getTickDelta(false);
         Vec3d startPos = entity.getCameraPosVec(partialTicks);
         Vec3d endPos = target.getBoundingBox().getCenter();
         if (checkBlocks) {
            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(startPos, endPos, ShapeType.OUTLINE, FluidHandling.NONE, entity));
            if (blockHit != null && blockHit.getType() == Type.BLOCK) {
               double blockDistSq = blockHit.getPos().squaredDistanceTo(startPos);
               double targetDistSq = endPos.squaredDistanceTo(startPos);
               if (blockDistSq < targetDistSq) {
                  return false;
               }
            }
         }

         Vec3d direction = getVectorForRotation(pitch, yaw);
         Vec3d rayEnd = startPos.add(direction.multiply(rayTraceDistance));
         Box searchBox = entity.getBoundingBox().stretch(direction.multiply(rayTraceDistance)).expand(1.0);
         return tracedTo(entity, startPos, rayEnd, searchBox, e -> !e.isSpectator() && e.canHit(), rayTraceDistance * rayTraceDistance, target);
      } else {
         return false;
      }
   }

   public static boolean tracedTo(Entity shooter, Vec3d startVec, Vec3d endVec, Box boundingBox, Predicate<Entity> filter, double distance, Entity target) {
      World world = shooter.getWorld();
      double d0 = distance;

      for (Entity entity1 : world.getOtherEntities(shooter, boundingBox, filter)) {
         Box box = entity1.getBoundingBox().expand(entity1.getTargetingMargin());
         Optional<Vec3d> optional = box.raycast(startVec, endVec);
         if (box.contains(startVec)) {
            if (d0 >= 0.0) {
               if (entity1 == target) {
                  return true;
               }

               d0 = 0.0;
            }
         } else if (optional.isPresent()) {
            Vec3d vec3d1 = optional.get();
            double d1 = startVec.squaredDistanceTo(vec3d1);
            if (entity1.getRootVehicle() == shooter.getRootVehicle()) {
               if (d0 == 0.0 && entity1 == target) {
                  return true;
               }
            } else {
               if (entity1 == target) {
                  return true;
               }

               d0 = d1;
            }
         }
      }

      return false;
   }

   public static Vec3d getVectorForRotation(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float f1 = -yaw * (float) (Math.PI / 180.0);
      float f2 = MathHelper.cos(f1);
      float f3 = MathHelper.sin(f1);
      float f4 = MathHelper.cos(f);
      float f5 = MathHelper.sin(f);
      return new Vec3d(f3 * f4, -f5, f2 * f4);
   }
}
