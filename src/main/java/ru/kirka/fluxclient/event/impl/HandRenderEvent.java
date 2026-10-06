package ru.kirka.fluxclient.event.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import ru.kirka.fluxclient.event.EventCancellable;

public class HandRenderEvent extends EventCancellable {
   private final Arm arm;
   private final float swingProgress;
   private final ItemStack itemStack;
   private final float equipProgress;
   private final MatrixStack matrices;

   public HandRenderEvent(Arm arm, float swingProgress, ItemStack itemStack, float equipProgress, MatrixStack matrices) {
      this.arm = arm;
      this.swingProgress = swingProgress;
      this.itemStack = itemStack;
      this.equipProgress = equipProgress;
      this.matrices = matrices;
   }

   public Arm getArm() {
      return this.arm;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
