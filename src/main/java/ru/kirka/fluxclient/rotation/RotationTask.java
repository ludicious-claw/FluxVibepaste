package ru.kirka.fluxclient.rotation;

public class RotationTask {
   private final Rotation rotation;
   private final MoveCorrection moveCorrection;
   private final float speedX;
   private final float speedY;
   private final float returnSpeed;
   private final int priority;

   public RotationTask(Rotation rotation, MoveCorrection moveCorrection, float speedX, float speedY, float returnSpeed, int priority) {
      this.rotation = rotation;
      this.moveCorrection = moveCorrection;
      this.speedX = speedX;
      this.speedY = speedY;
      this.priority = priority;
      this.returnSpeed = returnSpeed;
   }

   public RotationTask(Rotation rotation, float speedX, float speedY, long returnSpeed, int priority) {
      this(rotation, MoveCorrection.NONE, speedX, speedY, (float)returnSpeed, priority);
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public MoveCorrection getMoveCorrection() {
      return this.moveCorrection;
   }

   public float getSpeedX() {
      return this.speedX;
   }

   public float getSpeedY() {
      return this.speedY;
   }

   public float getReturnSpeed() {
      return this.returnSpeed;
   }

   public int getPriority() {
      return this.priority;
   }
}
