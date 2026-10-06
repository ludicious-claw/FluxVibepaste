package ru.kirka.fluxclient.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import ru.kirka.fluxclient.render.draw.MathUtility;
import ru.kirka.fluxclient.util.Timer;

public class RotationHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final RotationHandler INSTANCE = new RotationHandler();
   private Rotation currentRotation = Rotation.ZERO;
   private final Rotation serverRotation = new Rotation(0.0F, 0.0F);
   private Rotation prevRotation = Rotation.ZERO;
   private Rotation renderRotation = Rotation.ZERO;
   private RotationState state = RotationState.IDLE;
   @Nullable
   private RotationTask currentTask;
   private final Timer rotationIdle = new Timer();

   public static RotationHandler getInstance() {
      return INSTANCE;
   }

   public boolean isIdling() {
      return this.state == RotationState.IDLE;
   }

   public float[] pollSilentRotation() {
      return this.isIdling() ? null : new float[]{this.currentRotation.getYaw(), this.currentRotation.getPitch()};
   }

   public void update() {
      this.prevRotation = this.currentRotation;
      if (mc.player == null) {
         this.state = RotationState.IDLE;
         this.currentTask = null;
      } else {
         if (this.currentTask == null) {
            this.currentRotation = this.getPlayerRotation();
            this.state = RotationState.IDLE;
         } else if (this.rotationIdle.finished(70L)) {
            if (this.getPlayerRotation().differenceValue(this.currentRotation) < 1.0F) {
               this.state = RotationState.IDLE;
               this.currentTask = null;
            } else {
               this.state = RotationState.ROTATING_BACK;
               mc.player.setYaw(RotationMath.adjustAngle(this.currentRotation.getYaw(), mc.player.getYaw()));
               this.currentRotation = RotationMath.correctRotation(
                  new Rotation(
                     this.moveTowardsAngle(this.currentRotation.getYaw(), this.getPlayerRotation().getYaw(), this.currentTask.getReturnSpeed()),
                     this.moveTowardsAngle(this.currentRotation.getPitch(), this.getPlayerRotation().getPitch(), this.currentTask.getReturnSpeed())
                  )
               );
            }
         } else {
            this.state = RotationState.ROTATING;
            this.currentRotation = RotationMath.correctRotation(
               new Rotation(
                  this.moveTowardsAngle(this.currentRotation.getYaw(), this.currentTask.getRotation().getYaw(), this.currentTask.getSpeedX()),
                  this.moveTowardsAngle(this.currentRotation.getPitch(), this.currentTask.getRotation().getPitch(), this.currentTask.getSpeedY())
               )
            );
         }
      }
   }

   public void updateRender(float partialTicks) {
      if (mc.player != null) {
         float yaw = MathUtility.interpolate(this.prevRotation.getYaw(), this.currentRotation.getYaw(), partialTicks);
         float pitch = this.prevRotation.getPitch() + (this.currentRotation.getPitch() - this.prevRotation.getPitch()) * partialTicks;
         if (pitch <= -85.0F) {
            pitch = 0.0F;
         }

         this.renderRotation = new Rotation(yaw, pitch);
      }
   }

   public void rotate(Rotation rotation, MoveCorrection moveCorrection, float yawSpeed, float pitchSpeed, float returnSpeed, RotationPriority priority) {
      int priorityValue = priority.getPriority();
      if (this.currentTask == null || this.currentTask.getPriority() <= priorityValue || this.state != RotationState.ROTATING) {
         rotation.setYaw(
            RotationMath.adjustAngle(this.currentTask == null ? this.getPlayerRotation().getYaw() : this.currentTask.getRotation().getYaw(), rotation.getYaw())
         );
         this.currentTask = new RotationTask(rotation, moveCorrection, yawSpeed, pitchSpeed, returnSpeed, priorityValue);
         this.rotationIdle.reset();
      }
   }

   public void rotate(Rotation rotation, MoveCorrection moveCorrection, float yawSpeed, float pitchSpeed, float returnSpeed) {
      this.rotate(rotation, moveCorrection, yawSpeed, pitchSpeed, returnSpeed, RotationPriority.NORMAL);
   }

   public void rotate(Rotation rotation, RotationPriority priority) {
      this.rotate(rotation, MoveCorrection.DIRECT, 180.0F, 180.0F, 180.0F, priority);
   }

   public void rotate(Rotation rotation) {
      this.rotate(rotation, MoveCorrection.DIRECT, 180.0F, 180.0F, 180.0F, RotationPriority.NORMAL);
   }

   private float moveTowardsAngle(float current, float target, float speed) {
      float difference = RotationMath.getAngleDifference(current, target);
      return Math.abs(difference) <= speed ? target : current + Math.signum(difference) * speed;
   }

   public void rotateTowards(Entity entity, long yawSpeed, long pitchSpeed, long returnSpeed, RotationPriority priority, MoveCorrection moveCorrection) {
      if (entity != null && mc.player != null) {
         double posX = entity.getX();
         double posY = entity.getY() + entity.getEyeHeight(entity.getPose());
         double posZ = entity.getZ();
         double deltaX = posX - mc.player.getX();
         double deltaY = posY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
         double deltaZ = posZ - mc.player.getZ();
         double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
         Rotation targetRotation = new Rotation(yaw, pitch);
         this.rotate(targetRotation, moveCorrection, (float)yawSpeed, (float)pitchSpeed, (float)returnSpeed, priority);
      }
   }

   public Rotation getRotation(LivingEntity entity) {
      return new Rotation(entity.getYaw(), entity.getPitch());
   }

   public Rotation getPlayerRotation() {
      return mc.player == null ? Rotation.ZERO : new Rotation(mc.player.getYaw(), mc.player.getPitch());
   }

   public Rotation getCurrentRotation() {
      return this.currentRotation;
   }

   public Rotation getServerRotation() {
      return this.serverRotation;
   }

   public Rotation getPrevRotation() {
      return this.prevRotation;
   }

   public Rotation getRenderRotation() {
      return this.renderRotation;
   }

   public RotationState getState() {
      return this.state;
   }

   public Timer getRotationIdle() {
      return this.rotationIdle;
   }

   public void setCurrentRotation(Rotation currentRotation) {
      this.currentRotation = currentRotation;
   }

   public void setPrevRotation(Rotation prevRotation) {
      this.prevRotation = prevRotation;
   }

   public void setRenderRotation(Rotation renderRotation) {
      this.renderRotation = renderRotation;
   }

   public void setState(RotationState state) {
      this.state = state;
   }

   public void setCurrentTask(@Nullable RotationTask currentTask) {
      this.currentTask = currentTask;
   }

   @Nullable
   public RotationTask getCurrentTask() {
      return this.currentTask;
   }
}
