package ru.kirka.fluxclient.util;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class BotNavigator {
   private Vec3d lastPos;
   private int stuckTicks;
   private int unstickTicks;
   private int unstickDir = 1;
   private final Random rng = new Random();

   public void reset() {
      this.lastPos = null;
      this.stuckTicks = 0;
      this.unstickTicks = 0;
   }

   public static float turnToward(float current, float target, float maxStep) {
      float diff = MathHelper.wrapDegrees(target - current);
      if (diff > maxStep) {
         diff = maxStep;
      } else if (diff < -maxStep) {
         diff = -maxStep;
      }

      return MathHelper.wrapDegrees(current + diff);
   }

   public boolean moveTo(MinecraftClient mc, double x, double z, double stopDist, boolean sprint, boolean jumpAllowed) {
      if (mc != null && mc.player != null && mc.world != null && mc.options != null) {
         double dx = x - mc.player.getX();
         double dz = z - mc.player.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist <= stopDist) {
            this.stop(mc);
            this.reset();
            return true;
         } else {
            float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float yaw = turnToward(mc.player.getYaw(), targetYaw, 30.0F);
            float remain = Math.abs(MathHelper.wrapDegrees(targetYaw - yaw));
            if (remain > 3.0F) {
               yaw += (this.rng.nextFloat() - 0.5F) * 1.2F;
            }

            mc.player.setYaw(MathHelper.wrapDegrees(yaw));
            Vec3d now = mc.player.getPos();
            boolean moved;
            if (this.lastPos == null) {
               moved = true;
            } else {
               double dxn = now.x - this.lastPos.x;
               double dzn = now.z - this.lastPos.z;
               moved = dxn * dxn + dzn * dzn > 0.0064;
            }

            this.lastPos = now;
            if (moved && this.stuckTicks > 0) {
               this.stuckTicks--;
            }

            boolean onGround = mc.player.isOnGround();
            boolean headroom = this.isAir(mc, mc.player.getBlockPos().up(2));
            if (this.unstickTicks > 0) {
               this.unstickTicks--;
               mc.options.forwardKey.setPressed(true);
               mc.options.sprintKey.setPressed(false);
               mc.options.leftKey.setPressed(this.unstickDir < 0);
               mc.options.rightKey.setPressed(this.unstickDir > 0);
               mc.options.jumpKey.setPressed(jumpAllowed && onGround && headroom);
               return false;
            } else {
               if (!moved) {
                  this.stuckTicks++;
                  if (this.stuckTicks > 25) {
                     this.stuckTicks = 0;
                     this.unstickTicks = 30;
                     this.unstickDir = -this.unstickDir;
                     return false;
                  }
               }

               boolean blocked = mc.player.horizontalCollision || this.obstacleAhead(mc);
               boolean holeAhead = !this.groundAhead(mc);
               mc.options.leftKey.setPressed(false);
               mc.options.rightKey.setPressed(false);
               if (holeAhead && onGround) {
                  mc.options.forwardKey.setPressed(false);
                  mc.options.sprintKey.setPressed(false);
                  mc.options.leftKey.setPressed(true);
                  mc.options.jumpKey.setPressed(false);
                  return false;
               } else {
                  mc.options.forwardKey.setPressed(true);
                  mc.options.sprintKey.setPressed(sprint && dist > 4.0 && mc.player.getHungerManager().getFoodLevel() > 6);
                  if (blocked) {
                     if (jumpAllowed && onGround && headroom) {
                        mc.options.jumpKey.setPressed(true);
                     } else {
                        mc.options.jumpKey.setPressed(false);
                        if (this.unstickDir < 0) {
                           mc.options.leftKey.setPressed(true);
                        } else {
                           mc.options.rightKey.setPressed(true);
                        }
                     }
                  } else {
                     mc.options.jumpKey.setPressed(false);
                  }

                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public void stop(MinecraftClient mc) {
      if (mc != null && mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }
   }

   private boolean obstacleAhead(MinecraftClient mc) {
      try {
         float yaw = mc.player.getYaw() * (float) (Math.PI / 180.0);
         double fx = -Math.sin(yaw);
         double fz = Math.cos(yaw);
         BlockPos feet = mc.player.getBlockPos();
         int ax = (int)Math.floor(mc.player.getX() + fx * 1.2);
         int az = (int)Math.floor(mc.player.getZ() + fz * 1.2);
         return this.isSolid(mc, new BlockPos(ax, feet.getY(), az)) || this.isSolid(mc, new BlockPos(ax, feet.getY() + 1, az));
      } catch (Exception var10) {
         return false;
      }
   }

   private boolean groundAhead(MinecraftClient mc) {
      try {
         float yaw = mc.player.getYaw() * (float) (Math.PI / 180.0);
         double fx = -Math.sin(yaw);
         double fz = Math.cos(yaw);
         int ax = (int)Math.floor(mc.player.getX() + fx * 1.2);
         int az = (int)Math.floor(mc.player.getZ() + fz * 1.2);
         int feetY = mc.player.getBlockPos().getY();

         for (int d = 1; d <= 4; d++) {
            if (this.isSolid(mc, new BlockPos(ax, feetY - d, az))) {
               return true;
            }
         }

         return false;
      } catch (Exception var11) {
         return true;
      }
   }

   private boolean isSolid(MinecraftClient mc, BlockPos p) {
      try {
         if (mc.world.isOutOfHeightLimit(p.getY())) {
            return false;
         } else {
            return !mc.world.isChunkLoaded(p) ? false : mc.world.getBlockState(p).isSolidBlock(mc.world, p);
         }
      } catch (Exception var4) {
         return false;
      }
   }

   private boolean isAir(MinecraftClient mc, BlockPos p) {
      try {
         if (mc.world.isOutOfHeightLimit(p.getY())) {
            return false;
         } else {
            return !mc.world.isChunkLoaded(p) ? false : mc.world.getBlockState(p).isAir();
         }
      } catch (Exception var4) {
         return false;
      }
   }
}
