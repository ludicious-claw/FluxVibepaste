package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.client.input.KeyboardInput;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class Freecam extends Module {
   public final NumberSetting speed = new NumberSetting("Скорость", "Блоков за тик", 0.6F, 0.1F, 3.0F, 0.1F);
   public static boolean active = false;
   public static double camX;
   public static double camY;
   public static double camZ;
   private double saveX;
   private double saveY;
   private double saveZ;
   private float saveYaw;
   private float savePitch;

   public Freecam() {
      super("Freecam", "Свободная камера без движения тела", Category.MISC, 0);
      this.registerSetting(this.speed);
   }

   @Override
   protected void onEnable() {
      if (mc.player == null) {
         this.setEnabled(false);
      } else {
         this.saveX = mc.player.getX();
         this.saveY = mc.player.getY();
         this.saveZ = mc.player.getZ();
         this.saveYaw = mc.player.getYaw();
         this.savePitch = mc.player.getPitch();
         camX = mc.player.getX();
         camY = mc.player.getEyeY();
         camZ = mc.player.getZ();
         active = true;
      }
   }

   @Override
   protected void onDisable() {
      active = false;
      if (mc.player != null) {
         mc.player.setPos(this.saveX, this.saveY, this.saveZ);
         mc.player.setVelocity(0.0, 0.0, 0.0);
         mc.player.setYaw(this.saveYaw);
         mc.player.setPitch(this.savePitch);
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         long h = mc.getWindow().getHandle();
         float sp = this.speed.get();
         double yr = Math.toRadians(mc.player.getYaw());
         double fx = -Math.sin(yr);
         double fz = Math.cos(yr);
         double rx = -Math.cos(yr);
         double rz = -Math.sin(yr);
         double mx = 0.0;
         double my = 0.0;
         double mz = 0.0;
         if (GLFW.glfwGetKey(h, 87) == 1) {
            mx += fx;
            mz += fz;
         }

         if (GLFW.glfwGetKey(h, 83) == 1) {
            mx -= fx;
            mz -= fz;
         }

         if (GLFW.glfwGetKey(h, 68) == 1) {
            mx += rx;
            mz += rz;
         }

         if (GLFW.glfwGetKey(h, 65) == 1) {
            mx -= rx;
            mz -= rz;
         }

         if (GLFW.glfwGetKey(h, 32) == 1) {
            my++;
         }

         if (GLFW.glfwGetKey(h, 340) == 1) {
            my--;
         }

         double len = Math.sqrt(mx * mx + my * my + mz * mz);
         if (len > 0.0) {
            camX += mx / len * sp;
            camY += my / len * sp;
            camZ += mz / len * sp;
         }

         mc.player.setVelocity(0.0, 0.0, 0.0);
         mc.player.input = new KeyboardInput(mc.options);
         mc.player.input.movementForward = 0.0F;
         mc.player.input.movementSideways = 0.0F;
      }
   }
}
