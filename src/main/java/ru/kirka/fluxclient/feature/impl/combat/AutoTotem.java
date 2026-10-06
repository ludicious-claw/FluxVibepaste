package ru.kirka.fluxclient.feature.impl.combat;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoTotem extends Module {
   public final NumberSetting healthThreshold = new NumberSetting("Порог здоровья", "Здоровье для свапа тотема", 16.0F, 1.0F, 20.0F, 0.5F);
   public final BooleanSetting smartSwap = new BooleanSetting("Умный режим", "Не убирать яблоко/щит, если здоровье полное", true);
   private int delayTicks = 0;

   public AutoTotem() {
      super("AutoTotem", "Автоматически кладет тотем бессмертия в левую руку", Category.COMBAT, -1);
      this.registerSetting(this.healthThreshold);
      this.registerSetting(this.smartSwap);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.delayTicks > 0) {
            this.delayTicks--;
         } else {
            ItemStack offhand = mc.player.getOffHandStack();
            float currentHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (!offhand.isOf(Items.TOTEM_OF_UNDYING)) {
               if (!this.smartSwap.get()
                  || !(currentHp > this.healthThreshold.get())
                  || !offhand.isOf(Items.GOLDEN_APPLE) && !offhand.isOf(Items.ENCHANTED_GOLDEN_APPLE) && !offhand.isOf(Items.SHIELD)) {
                  int totemSlot = this.findTotemSlot();
                  if (totemSlot != -1) {
                     this.swapToOffhand(totemSlot);
                     this.delayTicks = 2;
                  }
               }
            }
         }
      }
   }

   private int findTotemSlot() {
      for (int i = 9; i <= 44; i++) {
         ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
         if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private void swapToOffhand(int slot) {
      int syncId = mc.player.playerScreenHandler.syncId;
      mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
   }
}
