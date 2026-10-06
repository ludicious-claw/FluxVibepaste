package ru.kirka.fluxclient.rotation.modes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.render.draw.MathUtility;
import ru.kirka.fluxclient.rotation.Rotation;
import ru.kirka.fluxclient.rotation.RotationMath;
import ru.kirka.fluxclient.rotation.RotationPointUtil;
import ru.kirka.fluxclient.util.combat.CombatUtility;

public final class AuraRotationSupport {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private AuraRotationSupport() {
   }

   public static Rotation rotationToTarget(Aura aura, LivingEntity target) {
      return RotationMath.getRotationTo(aimPoint(aura, target));
   }

   public static Vec3d aimPoint(Aura aura, LivingEntity target) {
      Vec3d predicted = target.getPos();
      Box box = target.getBoundingBox();
      if (aura != null && aura.isHitVectorModeEnabled()) {
         return RotationPointUtil.nearestPoint(box, mc.player != null ? mc.player.getEyePos() : box.getCenter());
      } else {
         double interpY = mc.player != null ? MathUtility.interpolate(mc.player.getY(), target.getY(), 0.5) : target.getY();
         return new Vec3d(predicted.x, MathHelper.clamp(interpY, box.minY, box.maxY), predicted.z);
      }
   }

   public static Vec3d targetDelta(LivingEntity target, double yScale) {
      if (mc.player == null) {
         return Vec3d.ZERO;
      } else {
         Vec3d center = target.getBoundingBox().getCenter();
         Vec3d point = new Vec3d(center.x, target.getY() + target.getEyeHeight(target.getPose()) * yScale, center.z);
         return point.subtract(mc.player.getEyePos());
      }
   }

   public static Rotation rotationFromDelta(Vec3d delta) {
      return new Rotation(RotationPointUtil.yawTo(delta), RotationPointUtil.pitchTo(delta));
   }

   public static Rotation rotationDelta(Rotation base, Vec3d delta) {
      Rotation target = rotationFromDelta(delta);
      return new Rotation(MathHelper.wrapDegrees(target.getYaw() - base.getYaw()), target.getPitch() - base.getPitch());
   }

   public static boolean canSeePoint(Vec3d point) {
      if (mc.player != null && mc.world != null) {
         BlockHitResult hit = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), point, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         return hit == null || hit.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static boolean canTrace(Aura aura, LivingEntity target, Rotation rotation, boolean checkBlocks) {
      return CombatUtility.canTraceWithBlock(aura.getAttackDistanceValue(), rotation.getYaw(), rotation.getPitch(), mc.player, target, checkBlocks);
   }

   public static float random(float min, float max) {
      return MathUtility.random(min, max);
   }

   public static float lerp(float delta, float start, float end) {
      return start + (end - start) * delta;
   }

   public static void sendRotationPackets(Rotation rotation) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player
            .networkHandler
            .sendPacket(
               new Full(
                  mc.player.getX(),
                  mc.player.getY(),
                  mc.player.getZ(),
                  rotation.getYaw(),
                  rotation.getPitch(),
                  mc.player.isOnGround(),
                  mc.player.horizontalCollision
               )
            );
         mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, rotation.getYaw(), rotation.getPitch()));
      }
   }
}
