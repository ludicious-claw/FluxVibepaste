package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.Render3DUtil;

public class Trajectories extends Module {
   public final ModeSetting style = new ModeSetting("Стиль линии", "Внешний вид траектории", "Неоновый градиент", "Неоновый градиент", "Сплошная", "Пунктир");
   public final NumberSetting lineWidth = new NumberSetting("Толщина", "Толщина линии траектории", 2.0F, 0.5F, 5.0F, 0.5F);
   public final ColorSetting pathColor = new ColorSetting("Цвет дуги", "Основной цвет траектории", new Color(168, 85, 247, 240));
   public final ColorSetting hitColor = new ColorSetting("Цвет попадания", "Цвет маркера точки приземления", new Color(239, 68, 68, 255));
   public final BooleanSetting hitMarker = new BooleanSetting("Маркер посадки", "Светящаяся мишень в месте падения", true);
   public final BooleanSetting targetBox = new BooleanSetting("Бокс при ударе", "Подсветка цели, если траектория попадет в моба", true);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Видно траекторию сквозь препятствия", true);
   public final BooleanSetting worldProjectiles = new BooleanSetting("Летящие снаряды", "Траектории уже летящих перлов и стрел в мире", true);

   public Trajectories() {
      super("Trajectories", "Баллистическая 3D траектория стрел, перлов и трезубцев с точкой падения", Category.RENDER, -1);
      this.registerSetting(this.style);
      this.registerSetting(this.lineWidth);
      this.registerSetting(this.pathColor);
      this.registerSetting(this.hitColor);
      this.registerSetting(this.hitMarker);
      this.registerSetting(this.targetBox);
      this.registerSetting(this.throughWalls);
      this.registerSetting(this.worldProjectiles);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.player != null && mc.world != null) {
         if (this.worldProjectiles.get()) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof ProjectileEntity proj && entity.isAlive() && !entity.isOnGround()) {
                  this.renderProjectileSimulation(matrices, cameraPos, camRot, proj);
               }
            }
         }

         ItemStack stack = this.getHeldProjectileItem();
         if (!stack.isEmpty()) {
            Trajectories.ProjectileProperties props = this.getProperties(stack);
            if (props != null) {
               float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
               double posX = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
               double posY = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()) + mc.player.getEyeHeight(mc.player.getPose()) - 0.1;
               double posZ = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());
               float yaw = MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
               float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());
               float radYaw = (float)Math.toRadians(yaw);
               float radPitch = (float)Math.toRadians(pitch);
               double motionX = -Math.sin(radYaw) * Math.cos(radPitch) * props.velocity;
               double motionY = -Math.sin(radPitch) * props.velocity;
               double motionZ = Math.cos(radYaw) * Math.cos(radPitch) * props.velocity;
               List<Vec3d> path = new ArrayList<>();
               Vec3d currentPos = new Vec3d(posX, posY, posZ);
               path.add(currentPos);
               HitResult collision = null;
               int maxSteps = 120;

               for (int step = 0; step < maxSteps; step++) {
                  Vec3d nextPos = currentPos.add(motionX, motionY, motionZ);
                  BlockHitResult bhr = mc.world.raycast(new RaycastContext(currentPos, nextPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                  Box stepBox = new Box(currentPos, nextPos).expand(1.0);
                  EntityHitResult ehr = ProjectileUtil.raycast(
                     mc.player, currentPos, nextPos, stepBox, entityx -> !entityx.isSpectator() && entityx.canHit(), currentPos.squaredDistanceTo(nextPos)
                  );
                  if (ehr != null) {
                     collision = ehr;
                     path.add(ehr.getPos());
                     break;
                  }

                  if (bhr != null && bhr.getType() != Type.MISS) {
                     collision = bhr;
                     path.add(bhr.getPos());
                     break;
                  }

                  path.add(nextPos);
                  currentPos = nextPos;
                  motionX *= props.drag;
                  motionY = motionY * props.drag - props.gravity;
                  motionZ *= props.drag;
               }

               if (path.size() >= 2) {
                  this.renderTrajectoryPath(matrices, cameraPos, path);
                  if (this.hitMarker.get() && collision != null) {
                     this.renderLandingMarker(matrices, cameraPos, camRot, collision);
                  }
               }
            }
         }
      }
   }

   private void renderTrajectoryPath(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      if (this.throughWalls.get()) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }

      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      GL11.glLineWidth(this.lineWidth.get());
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder b = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f m = matrices.peek().getPositionMatrix();
      Color base = this.pathColor.get();
      Color hit = this.hitColor.get();
      int total = path.size();

      for (int i = 0; i < total; i++) {
         Vec3d pt = path.get(i);
         float progress = (float)i / (total - 1);
         int cr = (int)(base.getRed() + (hit.getRed() - base.getRed()) * progress);
         int cg = (int)(base.getGreen() + (hit.getGreen() - base.getGreen()) * progress);
         int cb = (int)(base.getBlue() + (hit.getBlue() - base.getBlue()) * progress);
         int ca = (int)(base.getAlpha() * (0.5F + progress * 0.5F));
         b.vertex(m, (float)(pt.x - cameraPos.x), (float)(pt.y - cameraPos.y), (float)(pt.z - cameraPos.z)).color(cr, cg, cb, ca);
      }

      BufferRenderer.drawWithGlobalProgram(b.end());
      GL11.glLineWidth(1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void renderLandingMarker(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot, HitResult hit) {
      Vec3d impact = hit.getPos();
      Color markerCol = this.hitColor.get();
      if (hit instanceof EntityHitResult ehr && this.targetBox.get()) {
         Entity target = ehr.getEntity();
         Box bb = target.getBoundingBox().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
         Color fill = new Color(markerCol.getRed(), markerCol.getGreen(), markerCol.getBlue(), 60);
         Render3DUtil.drawBox(matrices, bb, markerCol, fill, this.lineWidth.get(), this.throughWalls.get());
      } else {
         Direction side = hit instanceof BlockHitResult bhr ? bhr.getSide() : Direction.UP;
         Vec3d localPos = impact.subtract(cameraPos);
         float radius = 0.45F;
         matrices.push();
         matrices.translate(localPos.x, localPos.y + 0.015, localPos.z);
         if (side != Direction.UP && side != Direction.DOWN) {
            Render3DUtil.drawRadialGlow(matrices, Vec3d.ZERO, camRot, radius, markerCol);
         } else {
            Render3DUtil.drawRing(matrices, Vec3d.ZERO, Math.max(0.0F, radius - 0.08F), radius, markerCol, 24);
            Render3DUtil.drawRing(matrices, Vec3d.ZERO, 0.0F, radius * 0.25F, new Color(255, 255, 255, 240), 16);
         }

         matrices.pop();
      }
   }

   private ItemStack getHeldProjectileItem() {
      ItemStack main = mc.player.getMainHandStack();
      if (this.isProjectileItem(main)) {
         return main;
      } else {
         ItemStack off = mc.player.getOffHandStack();
         return this.isProjectileItem(off) ? off : ItemStack.EMPTY;
      }
   }

   private boolean isProjectileItem(ItemStack st) {
      return st.getItem() instanceof BowItem
         || st.getItem() instanceof CrossbowItem
         || st.isOf(Items.ENDER_PEARL)
         || st.isOf(Items.SNOWBALL)
         || st.isOf(Items.EGG)
         || st.isOf(Items.SPLASH_POTION)
         || st.isOf(Items.LINGERING_POTION)
         || st.isOf(Items.TRIDENT)
         || st.isOf(Items.WIND_CHARGE);
   }

   private Trajectories.ProjectileProperties getProperties(ItemStack st) {
      if (st.getItem() instanceof BowItem) {
         int useTime = mc.player.getItemUseTime();
         float pull = useTime <= 0 ? 1.0F : BowItem.getPullProgress(useTime);
         if (pull < 0.1F) {
            pull = 1.0F;
         }

         return new Trajectories.ProjectileProperties(pull * 3.0, 0.05, 0.99);
      } else if (st.getItem() instanceof CrossbowItem) {
         return new Trajectories.ProjectileProperties(3.15, 0.05, 0.99);
      } else if (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG)) {
         return new Trajectories.ProjectileProperties(1.5, 0.03, 0.99);
      } else if (st.isOf(Items.SPLASH_POTION) || st.isOf(Items.LINGERING_POTION)) {
         return new Trajectories.ProjectileProperties(0.5, 0.05, 0.99);
      } else if (st.isOf(Items.TRIDENT)) {
         return new Trajectories.ProjectileProperties(2.5, 0.05, 0.99);
      } else {
         return st.isOf(Items.WIND_CHARGE) ? new Trajectories.ProjectileProperties(1.5, 0.0, 1.0) : null;
      }
   }

   private void renderProjectileSimulation(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot, ProjectileEntity proj) {
      Vec3d vel = proj.getVelocity();
      if (!(vel.lengthSquared() < 0.001)) {
         double motionX = vel.x;
         double motionY = vel.y;
         double motionZ = vel.z;
         double drag = 0.99;
         double gravity = 0.03;
         if (proj instanceof PersistentProjectileEntity) {
            gravity = 0.05;
         }

         List<Vec3d> path = new ArrayList<>();
         Vec3d currentPos = proj.getPos();
         path.add(currentPos);
         HitResult collision = null;

         for (int step = 0; step < 100; step++) {
            Vec3d nextPos = currentPos.add(motionX, motionY, motionZ);
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(currentPos, nextPos, ShapeType.COLLIDER, FluidHandling.NONE, proj));
            Box stepBox = new Box(currentPos, nextPos).expand(0.5);
            EntityHitResult ehr = ProjectileUtil.raycast(
               proj, currentPos, nextPos, stepBox, e -> !e.isSpectator() && e.canHit() && e != proj.getOwner(), currentPos.squaredDistanceTo(nextPos)
            );
            if (ehr != null) {
               collision = ehr;
               path.add(ehr.getPos());
               break;
            }

            if (bhr != null && bhr.getType() != Type.MISS) {
               collision = bhr;
               path.add(bhr.getPos());
               break;
            }

            path.add(nextPos);
            currentPos = nextPos;
            motionX *= drag;
            motionY = motionY * drag - gravity;
            motionZ *= drag;
         }

         if (path.size() >= 2) {
            this.renderTrajectoryPath(matrices, cameraPos, path);
            if (this.hitMarker.get() && collision != null) {
               this.renderLandingMarker(matrices, cameraPos, camRot, collision);
            }
         }
      }
   }

   private record ProjectileProperties(double velocity, double gravity, double drag) {
   }
}
