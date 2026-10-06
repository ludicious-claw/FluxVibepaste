package ru.kirka.fluxclient.feature.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.HandRenderEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class ViewModel extends Module {
   public final NumberSetting mainTranslateX = new NumberSetting("Правая X", "Смещение правой руки по X", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting mainTranslateY = new NumberSetting("Правая Y", "Смещение правой руки по Y", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting mainTranslateZ = new NumberSetting("Правая Z", "Смещение правой руки по Z", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting mainRotateX = new NumberSetting("Правая Rot X", "Вращение правой руки по X", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting mainRotateY = new NumberSetting("Правая Rot Y", "Вращение правой руки по Y", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting mainRotateZ = new NumberSetting("Правая Rot Z", "Вращение правой руки по Z", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting mainScale = new NumberSetting("Правая масштаб", "Масштаб правой руки", 1.0F, 0.2F, 2.0F, 0.05F);
   public final NumberSetting offTranslateX = new NumberSetting("Левая X", "Смещение левой руки по X", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting offTranslateY = new NumberSetting("Левая Y", "Смещение левой руки по Y", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting offTranslateZ = new NumberSetting("Левая Z", "Смещение левой руки по Z", 0.0F, -2.0F, 2.0F, 0.05F);
   public final NumberSetting offRotateX = new NumberSetting("Левая Rot X", "Вращение левой руки по X", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting offRotateY = new NumberSetting("Левая Rot Y", "Вращение левой руки по Y", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting offRotateZ = new NumberSetting("Левая Rot Z", "Вращение левой руки по Z", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting offScale = new NumberSetting("Левая масштаб", "Масштаб левой руки", 1.0F, 0.2F, 2.0F, 0.05F);
   private final EventListener<HandRenderEvent> onHandRender = event -> {
      MatrixStack matrices = event.getMatrices();
      boolean isMain = event.getArm() == Arm.RIGHT;
      float translateX = isMain ? this.mainTranslateX.get() : this.offTranslateX.get();
      float translateY = isMain ? this.mainTranslateY.get() : this.offTranslateY.get();
      float translateZ = isMain ? this.mainTranslateZ.get() : this.offTranslateZ.get();
      float rotateX = isMain ? this.mainRotateX.get() : this.offRotateX.get();
      float rotateY = isMain ? this.mainRotateY.get() : this.offRotateY.get();
      float rotateZ = isMain ? this.mainRotateZ.get() : this.offRotateZ.get();
      float scale = isMain ? this.mainScale.get() : this.offScale.get();
      float direction = isMain ? 1.0F : -1.0F;
      matrices.translate(translateX * direction, translateY, translateZ);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
      if (scale != 1.0F) {
         matrices.scale(scale, scale, scale);
      }
   };

   public ViewModel() {
      super("ViewModel", "Кастомизация позиции, масштаба и поворота предметов в руках от 1-го лица", Category.RENDER, -1);
      this.registerSetting(this.mainTranslateX);
      this.registerSetting(this.mainTranslateY);
      this.registerSetting(this.mainTranslateZ);
      this.registerSetting(this.mainRotateX);
      this.registerSetting(this.mainRotateY);
      this.registerSetting(this.mainRotateZ);
      this.registerSetting(this.mainScale);
      this.registerSetting(this.offTranslateX);
      this.registerSetting(this.offTranslateY);
      this.registerSetting(this.offTranslateZ);
      this.registerSetting(this.offRotateX);
      this.registerSetting(this.offRotateY);
      this.registerSetting(this.offRotateZ);
      this.registerSetting(this.offScale);
   }
}
