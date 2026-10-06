package ru.kirka.fluxclient.rotation;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
   public static final Rotation ZERO = new Rotation(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public Rotation difference(Rotation other) {
      float diffYaw = angleDifference(this.yaw, other.yaw);
      float diffPitch = angleDifference(this.pitch, other.pitch);
      return new Rotation(diffYaw, diffPitch);
   }

   public float differenceValue(Rotation other) {
      float diffYaw = angleDifference(this.yaw, other.yaw);
      float diffPitch = angleDifference(this.pitch, other.pitch);
      return Math.abs(diffYaw) + Math.abs(diffPitch);
   }

   public Vec3d getRotationVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float f1 = -this.yaw * (float) (Math.PI / 180.0);
      float f2 = MathHelper.cos(f1);
      float f3 = MathHelper.sin(f1);
      float f4 = MathHelper.cos(f);
      float f5 = MathHelper.sin(f);
      return new Vec3d(f3 * f4, -f5, f2 * f4);
   }

   public void modify(float yaw, float pitch) {
      this.yaw += yaw;
      this.pitch += pitch;
   }

   public static float angleDifference(float a, float b) {
      float diff = (b - a + 180.0F) % 360.0F - 180.0F;
      return diff < -180.0F ? diff + 360.0F : diff;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Rotation other) ? false : Float.compare(this.yaw, other.yaw) == 0 && Float.compare(this.pitch, other.pitch) == 0;
      }
   }

   @Override
   public int hashCode() {
      return 31 * Float.floatToIntBits(this.yaw) + Float.floatToIntBits(this.pitch);
   }

   @Override
   public String toString() {
      return "Rotation(yaw=" + this.yaw + ", pitch=" + this.pitch + ")";
   }
}
