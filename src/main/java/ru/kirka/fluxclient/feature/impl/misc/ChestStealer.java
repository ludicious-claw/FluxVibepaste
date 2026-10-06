package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class ChestStealer extends Module {
   public final NumberSetting delay = new NumberSetting("Задержка", "Тиков между предметами", 1.0F, 0.0F, 10.0F, 1.0F);
   public final BooleanSetting autoClose = new BooleanSetting("Автозакрытие", "Закрывать пустой сундук", true);
   private int timer = 0;

   public ChestStealer() {
      super("ChestStealer", "Мгновенно опустошает сундуки", Category.MISC, 0);
      this.registerSetting(this.delay);
      this.registerSetting(this.autoClose);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h) {
            if (--this.timer <= 0) {
               this.timer = (int)this.delay.get().floatValue();
               int invSize = h.getRows() * 9;

               for (int i = 0; i < invSize; i++) {
                  if (!h.getSlot(i).getStack().isEmpty()) {
                     mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                     return;
                  }
               }

               if (this.autoClose.get()) {
                  mc.player.closeHandledScreen();
               }
            }
         }
      }
   }
}
