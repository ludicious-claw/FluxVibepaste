package ru.kirka.fluxclient.util;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MaceUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private MaceUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static boolean hasMace() {
      return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   public static boolean a() {
      return hasMace();
   }

   public static Optional<Vec3d> predictLanding(ClientPlayerEntity player, World world) {
      return a(player, world);
   }

   public static Optional<Vec3d> a(ClientPlayerEntity player, World world) {
      if (player == null || world == null || world != mc.world) {
         return Optional.empty();
      } else if (player.isOnGround()) {
         return Optional.empty();
      } else if (player.getAbilities().flying || player.isGliding() || player.hasVehicle() || player.isClimbing()) {
         return Optional.empty();
      } else if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
         return Optional.empty();
      } else if (!player.isTouchingWater() && !player.isInLava()) {
         double gravity = 0.08;
         if (player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01;
         }

         Box baseBox = player.getBoundingBox();
         double ox = 0.0;
         double oy = 0.0;
         double oz = 0.0;
         Vec3d velocity = player.getVelocity();

         for (int tick = 0; tick < 400; tick++) {
            double vx = velocity.x * 0.9799995613681012;
            double vy = (velocity.y - gravity) * 0.9799995613681012;
            double vz = velocity.z * 0.9799995613681012;
            Box simBox = baseBox.offset(ox, oy, oz);
            Vec3d step = new Vec3d(vx, vy, vz);
            Vec3d allowed = adjustForCollisions(player, step, simBox);
            if (step.y < -9.999995680744685E-5 && allowed.y > step.y + 9.999995999686725E-5) {
               return Optional.of(player.getPos().add(ox + allowed.x, oy + allowed.y, oz + allowed.z));
            }

            ox += allowed.x;
            oy += allowed.y;
            oz += allowed.z;
            if (isWaterLogged(world, baseBox.offset(ox, oy, oz))) {
               return Optional.of(player.getPos().add(ox, oy, oz));
            }

            if (Math.abs(allowed.x - step.x) > 9.999995999686725E-5) {
               vx = 0.0;
            }

            if (Math.abs(allowed.y - step.y) > 9.999995999686725E-5) {
               vy = 0.0;
            }

            if (Math.abs(allowed.z - step.z) > 9.999995999686725E-5) {
               vz = 0.0;
            }

            velocity = new Vec3d(vx, vy, vz);
            if (velocity.lengthSquared() < 1.0000000090069629E-12 && step.y >= -9.999995680744685E-5) {
               break;
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public static boolean willLandSoon() {
      return b();
   }

   public static boolean b() {
      if (mc.world != null && mc.player != null) {
         ClientPlayerEntity player = mc.player;
         double gravity = 0.08;
         if (player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01;
         }

         double offsetY = 0.0;
         Vec3d velocity = player.getVelocity();

         for (int tick = 0; tick < 2; tick++) {
            double nextVy = (velocity.y - gravity) * 0.9799995613681012;
            Vec3d step = new Vec3d(0.0, nextVy, 0.0);
            Box box = player.getBoundingBox().offset(0.0, offsetY, 0.0);
            Vec3d allowed = adjustForCollisions(player, step, box);
            if (nextVy < 0.0 && isSoftLanding(mc.world, box.offset(allowed.x, allowed.y, allowed.z))) {
               return true;
            }

            offsetY += allowed.y;
            velocity = new Vec3d(velocity.x, allowed.y, velocity.z);
         }

         return false;
      } else {
         return false;
      }
   }

   private static Vec3d adjustForCollisions(ClientPlayerEntity player, Vec3d movement, Box box) {
      double x = axisMove(player, box, movement.x, 0);
      Box afterX = box.offset(x, 0.0, 0.0);
      double z = axisMove(player, afterX, movement.z, 2);
      Box afterZ = afterX.offset(0.0, 0.0, z);
      double y = axisMove(player, afterZ, movement.y, 1);
      return new Vec3d(x, y, z);
   }

   private static double axisMove(ClientPlayerEntity player, Box box, double delta, int axis) {
      if (delta == 0.0) {
         return 0.0;
      } else {
         Box target = shift(box, delta, axis);
         if (mc.world.isBlockSpaceEmpty(player, target)) {
            return delta;
         } else {
            double lo = 0.0;
            double hi = delta;

            for (int i = 0; i < 16; i++) {
               double mid = (lo + hi) * 0.5;
               if (mc.world.isBlockSpaceEmpty(player, shift(box, mid, axis))) {
                  lo = mid;
               } else {
                  hi = mid;
               }
            }

            return lo;
         }
      }
   }

   private static Box shift(Box box, double delta, int axis) {
      return switch (axis) {
         case 0 -> box.offset(delta, 0.0, 0.0);
         case 1 -> box.offset(0.0, delta, 0.0);
         default -> box.offset(0.0, 0.0, delta);
      };
   }

   private static boolean isWaterLogged(World world, Box box) {
      for (BlockPos pos : BlockPos.iterate(BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
         FluidState fluid = world.getBlockState(pos).getFluidState();
         if (!fluid.isEmpty() && (fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isSoftLanding(World world, Box box) {
      BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
      BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);

      for (BlockPos pos : BlockPos.iterate(min, max)) {
         BlockState state = world.getBlockState(pos);
         if (!state.isOf(Blocks.COBWEB) && !state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            FluidState fluid = state.getFluidState();
            if (fluid.isEmpty() || !fluid.isIn(FluidTags.WATER) && !fluid.isIn(FluidTags.LAVA)) {
               continue;
            }

            return true;
         }

         return true;
      }

      return false;
   }
}
