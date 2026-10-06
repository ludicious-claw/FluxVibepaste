package ru.kirka.fluxclient.render.geometry.obj;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector3f;

public class RenderVec3d extends Vec3d {
   private final Vec3d prev;

   public RenderVec3d(double x, double y, double z, Vec3d prev) {
      super(x, y, z);
      this.prev = prev;
   }

   public RenderVec3d(Vector3f vec, Vec3d prev) {
      super(vec.x, vec.y, vec.z);
      this.prev = prev;
   }

   public RenderVec3d(Vec3i vec, Vec3d prev) {
      super(vec.getX(), vec.getY(), vec.getZ());
      this.prev = prev;
   }

   public Vec3d getPrev() {
      return this.prev;
   }
}
