package ru.kirka.fluxclient.util;

import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AimHumanizer {
   private final Random rng = new Random();
   private float yaw;
   private float pitch;
   private boolean hasAngles = false;
   private int reactionTicks = 0;
   private float driftYaw;
   private float driftPitch;
   private float tremorPhase;
   private float jitterYaw;
   private float jitterPitch;
   private int jitterTicks;
   private float overYaw;
   private float overPitch;
   private float pointX = 0.5F;
   private float pointY = 0.55F;
   private float pointZ = 0.5F;
   private boolean useCentroid = true;
   private int pointTicks = 0;
   private int flickTicks = 0;
   private float flickYaw;
   private float flickPitch;
   private float userSpeedEma = 1.0F;
   private float lastCamYaw;
   private float lastCamPitch;
   private boolean camInit = false;

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void reset() {
      this.hasAngles = false;
      this.reactionTicks = 0;
      this.driftYaw = 0.0F;
      this.driftPitch = 0.0F;
      this.jitterYaw = 0.0F;
      this.jitterPitch = 0.0F;
      this.jitterTicks = 0;
      this.overYaw = 0.0F;
      this.overPitch = 0.0F;
      this.flickTicks = 0;
      this.pointTicks = 0;
      this.useCentroid = true;
   }

   public void snapTo(float yaw, float pitch) {
      this.yaw = MathHelper.wrapDegrees(yaw);
      this.pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      this.hasAngles = true;
   }

   public void onNewTarget() {
      this.reactionTicks = 1 + this.rng.nextInt(3);
      this.overYaw = 0.0F;
      this.overPitch = 0.0F;
      this.flickTicks = 0;
      this.jitterYaw = 0.0F;
      this.jitterPitch = 0.0F;
      this.jitterTicks = 0;
      this.useCentroid = true;
      this.repickPoint();
      this.pointTicks = 6 + this.rng.nextInt(7);
   }

   public void observeFreeLook(float camYaw, float camPitch) {
      if (!this.camInit) {
         this.lastCamYaw = camYaw;
         this.lastCamPitch = camPitch;
         this.camInit = true;
      } else {
         float d = (float)Math.hypot(MathHelper.wrapDegrees(camYaw - this.lastCamYaw), camPitch - this.lastCamPitch);
         this.lastCamYaw = camYaw;
         this.lastCamPitch = camPitch;
         if (!(d > 60.0F)) {
            this.userSpeedEma = this.userSpeedEma + (d - this.userSpeedEma) * 0.02F;
         }
      }
   }

   public void update(Vec3d eye, LivingEntity target, double reach, boolean throughWalls, boolean tracking, float camYaw, float camPitch) {
      if (!this.hasAngles) {
         this.snapTo(camYaw, camPitch);
      }

      if (this.reactionTicks > 0) {
         this.reactionTicks--;
      } else {
         float wanderScale = tracking ? 0.12F : MathHelper.clamp(this.userSpeedEma / 1.5F, 0.7F, 1.4F);
         float goalYaw;
         float goalPitch;
         if (tracking) {
            float[] tp = this.anglesToPoint(eye, target, reach, throughWalls);
            goalYaw = tp[0];
            goalPitch = tp[1];
         } else {
            goalYaw = approachAngle(this.yaw, camYaw, 30.0F);
            goalPitch = approachAngle(this.pitch, camPitch, 15.0F);
         }

         if (!tracking && --this.jitterTicks <= 0) {
            this.jitterTicks = 5 + this.rng.nextInt(5);
            this.jitterYaw = (this.rng.nextFloat() - 0.5F) * 3.0F * wanderScale;
            this.jitterPitch = (this.rng.nextFloat() - 0.5F) * 1.6F * wanderScale;
         }

         this.driftYaw = this.driftYaw + (this.rng.nextFloat() - 0.5F) * 0.16F;
         this.driftPitch = this.driftPitch + (this.rng.nextFloat() - 0.5F) * 0.12F;
         this.driftYaw *= 0.982F;
         this.driftPitch *= 0.982F;
         this.driftYaw = MathHelper.clamp(this.driftYaw, -1.3F, 1.3F);
         this.driftPitch = MathHelper.clamp(this.driftPitch, -0.9F, 0.9F);
         this.tremorPhase = this.tremorPhase + (0.8F + this.rng.nextFloat() * 0.7F);
         float tremorYaw = (float)Math.sin(this.tremorPhase * 1.71F) * 0.1F + (this.rng.nextFloat() - 0.5F) * 0.07F;
         float tremorPitch = (float)Math.cos(this.tremorPhase * 1.37F) * 0.08F + (this.rng.nextFloat() - 0.5F) * 0.05F;
         float noiseYaw = tracking ? (this.driftYaw + tremorYaw) * 0.12F : (this.driftYaw + tremorYaw) * wanderScale + this.jitterYaw;
         float noisePitch = tracking ? (this.driftPitch + tremorPitch) * 0.12F : (this.driftPitch + tremorPitch) * wanderScale + this.jitterPitch;
         float wantYaw = goalYaw + noiseYaw;
         float wantPitch = goalPitch + noisePitch;
         if (!tracking) {
            if (this.flickTicks > 0) {
               this.flickTicks--;
               wantYaw += this.flickYaw;
               wantPitch += this.flickPitch;
            } else if (this.rng.nextFloat() < 0.1F) {
               this.flickTicks = 1 + this.rng.nextInt(3);
               float ang = this.rng.nextFloat() * (float) (Math.PI * 2);
               float mag = 3.0F + this.rng.nextFloat() * 5.0F;
               this.flickYaw = (float)Math.cos(ang) * mag;
               this.flickPitch = (float)Math.sin(ang) * mag * 0.6F;
            }
         } else {
            this.flickTicks = 0;
         }

         float fy = (tracking ? 0.87F : 0.56F) * (0.97F + this.rng.nextFloat() * 0.06F);
         float fp = fy * (0.55F + this.rng.nextFloat() * 0.2F);
         double eyeDist = eye.distanceTo(target.getBoundingBox().getCenter());
         if (eyeDist < 1.5) {
            fy *= 0.8F;
            fp *= 0.8F;
         }

         float yawCap = tracking ? 220.0F : 110.0F;
         float pitchCap = yawCap * 0.6F;
         float dy = MathHelper.wrapDegrees(wantYaw - this.yaw);
         float dp = wantPitch - this.pitch;
         float stepYaw = MathHelper.clamp(dy * fy, -yawCap, yawCap);
         float stepPitch = MathHelper.clamp(dp * fp, -pitchCap, pitchCap);
         if (this.rng.nextFloat() < 0.05F && Math.abs(dy) > 5.0F) {
            stepYaw *= 1.1F + this.rng.nextFloat() * 0.3F;
         }

         if (this.rng.nextFloat() < 0.02F) {
            stepYaw += (this.rng.nextFloat() - 0.5F) * 1.2F;
            stepPitch += (this.rng.nextFloat() - 0.5F) * 0.8F;
         }

         if (Math.abs(dp) > 8.0F && Math.abs(this.overPitch) < 0.05F && this.rng.nextFloat() < 0.2F) {
            this.overPitch = MathHelper.clamp(dp * 0.1F, -2.0F, 2.0F);
         }

         if (Math.abs(dy) > 12.0F && Math.abs(this.overYaw) < 0.05F && this.rng.nextFloat() < 0.2F) {
            this.overYaw = MathHelper.clamp(dy * 0.1F, -3.0F, 3.0F);
         }

         float newYaw = this.yaw + stepYaw + this.overYaw;
         float newPitch = MathHelper.clamp(this.pitch + stepPitch + this.overPitch, -90.0F, 90.0F);
         this.overYaw *= 0.6F;
         this.overPitch *= 0.6F;
         if (!(Math.abs(newYaw - this.yaw) < 0.01F) || !(Math.abs(newPitch - this.pitch) < 0.01F)) {
            this.yaw = AuraUtil.snapToGCD(this.yaw, newYaw);
            this.pitch = MathHelper.clamp(AuraUtil.snapToGCD(this.pitch, newPitch), -90.0F, 90.0F);
            this.yaw = MathHelper.wrapDegrees(this.yaw);
         }
      }
   }

   private float[] anglesToPoint(Vec3d eye, LivingEntity target, double reach, boolean throughWalls) {
      Box box = target.getBoundingBox();
      if (--this.pointTicks <= 0) {
         this.useCentroid = this.rng.nextFloat() < 0.6F;
         if (!this.useCentroid) {
            this.repickPoint();
         }

         this.pointTicks = 6 + this.rng.nextInt(7);
      }

      if (!this.useCentroid) {
         Vec3d p = new Vec3d(
               MathHelper.lerp(this.pointX, box.minX, box.maxX),
               MathHelper.lerp(this.pointY, box.minY, box.maxY),
               MathHelper.lerp(this.pointZ, box.minZ, box.maxZ)
            )
            .subtract(eye);
         float[] a = anglesOf(p);
         if (AuraUtil.a(eye, a[0], a[1], reach, target, throughWalls)) {
            return a;
         }
      }

      Vec3d off = AuraUtil.a(eye, target, reach, throughWalls);
      return off == Vec3d.ZERO ? anglesOf(box.getCenter().subtract(eye)) : anglesOf(off);
   }

   private void repickPoint() {
      for (int i = 0; i < 4; i++) {
         float nx = 0.2F + this.rng.nextFloat() * 0.6F;
         float ny = 0.15F + this.rng.nextFloat() * 0.7F;
         float nz = 0.2F + this.rng.nextFloat() * 0.6F;
         float dx = nx - this.pointX;
         float dy = ny - this.pointY;
         float dz = nz - this.pointZ;
         if (dx * dx + dy * dy + dz * dz > 0.0324F) {
            this.pointX = nx;
            this.pointY = ny;
            this.pointZ = nz;
            return;
         }
      }
   }

   private static float[] anglesOf(Vec3d d) {
      float yawTo = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
      float pitchTo = (float)(-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
      return new float[]{yawTo, MathHelper.clamp(pitchTo, -90.0F, 90.0F)};
   }

   private static float approachAngle(float cur, float target, float maxStep) {
      float d = MathHelper.wrapDegrees(target - cur);
      return cur + MathHelper.clamp(d, -maxStep, maxStep);
   }
}
