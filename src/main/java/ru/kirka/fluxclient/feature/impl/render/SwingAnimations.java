package ru.kirka.fluxclient.feature.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.combat.Aura;

public class SwingAnimations extends Module {
   public final ModeSetting mode = new ModeSetting(
      "Режим",
      "Анимация удара и взмаха",
      "Block Hit",
      "Block Hit",
      "Bonk",
      "Rotate 360",
      "From Me",
      "Мод 1",
      "Мод 2",
      "Мод 3",
      "Мод 4",
      "Мод 5",
      "Smooth",
      "Slide",
      "Spin"
   );
   public final NumberSetting intensity = new NumberSetting("Интенсивность", "Сила анимации взмаха", 1.0F, 0.2F, 2.0F, 0.05F);
   public final NumberSetting speed = new NumberSetting("Скорость", "Скорость анимации взмаха", 1.0F, 0.5F, 3.0F, 0.05F);
   public final BooleanSetting considerAura = new BooleanSetting("Только с целью", "Анимация только когда у KillAura есть активная цель", false);
   public final NumberSetting deltaAngle = new NumberSetting("Угол поворота", "Угол замаха (Мод 1)", 75.0F, 0.0F, 360.0F, 1.0F);
   public final NumberSetting deltaTip = new NumberSetting("Наклон кончика", "Наклон клинка (кроме Мод 5)", -20.0F, -90.0F, 90.0F, 1.0F);
   public final NumberSetting deltaPower = new NumberSetting("Сила взмаха", "Интенсивность дельта-анимации", 5.0F, 1.0F, 10.0F, 0.5F);

   public SwingAnimations() {
      super("SwingAnimations", "Кастомные анимации руки при ударе и ломании", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.intensity);
      this.registerSetting(this.speed);
      this.registerSetting(this.considerAura);
      this.registerSetting(this.deltaAngle);
      this.registerSetting(this.deltaTip);
      this.registerSetting(this.deltaPower);
   }

   public void apply(MatrixStack matrices, float swingProgress, Hand hand) {
      if (this.isEnabled() && !(swingProgress <= 0.001F)) {
         if (!this.considerAura.get() || Aura.target != null) {
            String current = this.mode.get();
            if (current.startsWith("Мод")) {
               this.applyDelta(matrices, swingProgress, hand);
            } else {
               float progress = Math.min(1.0F, swingProgress * this.speed.get());
               float side = hand == Hand.MAIN_HAND ? 1.0F : -1.0F;
               float strength = this.intensity.get();
               switch (current) {
                  case "Block Hit":
                     applyRockstarTransform(
                        matrices,
                        swingProgress,
                        0.0F,
                        -0.05F,
                        -0.7F,
                        1.05F * strength,
                        -0.7F * strength,
                        -1.1F * strength,
                        -120.0F,
                        MathHelper.lerp(swingProgressBack(progress), -135.0F, -180.0F) * strength,
                        -60.0F
                     );
                     break;
                  case "Bonk":
                     applyRockstarTransform(
                        matrices,
                        swingProgress,
                        0.0F,
                        -0.4F,
                        -0.65F,
                        0.0F,
                        0.0F,
                        0.0F,
                        MathHelper.lerp(swingProgressBack(progress), 0.0F, -45.0F) * strength,
                        0.0F,
                        0.0F
                     );
                     break;
                  case "Rotate 360":
                     applyRockstarTransform(
                        matrices, swingProgress, 0.0F, -0.4F, -0.65F, 0.0F, 0.0F, 0.0F, MathHelper.lerp(progress, 0.0F, -360.0F) * strength, 0.0F, 0.0F
                     );
                     break;
                  case "From Me":
                     float p = swingProgressBack(progress);
                     applyRockstarTransform(
                        matrices,
                        swingProgress,
                        0.0F,
                        0.0F,
                        -1.1F,
                        0.2F * strength,
                        0.0F,
                        MathHelper.lerp(p, -0.1F, -0.3F) * strength,
                        MathHelper.lerp(p, -135.0F, -180.0F) * strength,
                        45.0F,
                        60.0F
                     );
                     break;
                  case "Slide": {
                     float eased = (float)Math.sin(progress * Math.PI);
                     matrices.translate(side * eased * 0.16F * strength, eased * 0.08F * strength, -eased * 0.08F * strength);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * eased * 24.0F * strength));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-eased * 32.0F * strength));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * eased * 12.0F * strength));
                     break;
                  }
                  case "Spin": {
                     float eased = progress * progress * (3.0F - 2.0F * progress);
                     matrices.translate(side * eased * 0.08F * strength, 0.0F, -eased * 0.12F * strength);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * eased * 180.0F * strength));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-eased * 45.0F * strength));
                     break;
                  }
                  default:
                     float wave = (float)Math.sin(progress * Math.PI);
                     matrices.translate(side * wave * 0.06F * strength, wave * 0.035F * strength, -wave * 0.04F * strength);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-wave * 22.0F * strength));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * wave * 14.0F * strength));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * 8.0F * strength));
               }
            }
         }
      }
   }

   private static float swingProgressBack(float progress) {
      return MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI);
   }

   private static void applyRockstarTransform(
      MatrixStack matrices, float progress, float ax, float ay, float az, float mx, float my, float mz, float rx, float ry, float rz
   ) {
      matrices.translate(ax, ay, az);
      matrices.translate(mx, my, mz);
      matrices.multiply(new Quaternionf().rotationXYZ((float)Math.toRadians(rx), (float)Math.toRadians(ry), (float)Math.toRadians(rz)));
      matrices.translate(-ax, -ay, -az);
   }

   private void applyDelta(MatrixStack matrices, float swingProgress, Hand hand) {
      if (hand == Hand.MAIN_HAND) {
         float arm = 1.0F;
         float anim = (float)Math.sin(swingProgress * 3.1415936112270124);
         float power = this.deltaPower.get() * 10.0F;
         String current = this.mode.get();
         matrices.translate(arm * (current.equals("Мод 5") ? 0.5F : 0.72F), -0.5F, current.equals("Мод 5") ? -0.72F : -1.0F);
         if (!current.equals("Мод 5")) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.deltaTip.get()));
         }

         switch (current) {
            case "Мод 1":
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * 90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * -70.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.deltaAngle.get() - power * anim));
               break;
            case "Мод 2":
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * 90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * -65.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-65.0F + power * anim));
               break;
            case "Мод 3":
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * -90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * 60.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * power * anim));
               break;
            case "Мод 4":
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * 90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * -75.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45.0F - power * anim));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * power * anim * 0.5F));
               break;
            case "Мод 5":
               float strength = power / 80.0F;
               float swing = anim * anim;
               float twist = (float)Math.sin(swingProgress * swingProgress * 3.1415936112270124);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * (45.0F + twist * -20.0F * strength)));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm * swing * -22.0F * strength));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swing * -85.0F * strength));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm * -45.0F));
         }
      }
   }
}
