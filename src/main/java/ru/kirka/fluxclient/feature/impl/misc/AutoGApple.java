package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoGApple extends Module {
   public final NumberSetting hpNormal = new NumberSetting("HP для яблока", "Обычное золото при HP <=", 10.0F, 1.0F, 20.0F, 0.5F);
   public final NumberSetting hpEnch = new NumberSetting("HP для зачара", "Зачарованное при HP <=", 6.0F, 1.0F, 20.0F, 0.5F);
   private boolean eating = false;

   public AutoGApple() {
      super("AutoGApple", "Сам ест золотое / зачарованное яблоко при мало HP", Category.MISC, 0);
      this.registerSetting(this.hpNormal);
      this.registerSetting(this.hpEnch);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         float hp = mc.player.getHealth();
         boolean wantEnch = hp <= this.hpEnch.get();
         boolean wantNorm = hp <= this.hpNormal.get();
         if (!wantEnch && !wantNorm) {
            this.stop();
         } else {
            int slot = wantEnch ? this.find(Items.ENCHANTED_GOLDEN_APPLE) : -1;
            if (slot < 0) {
               slot = wantNorm ? this.find(Items.GOLDEN_APPLE) : -1;
            }

            if (slot < 0) {
               this.stop();
            } else if (slot >= 9) {
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            } else {
               mc.player.getInventory().selectedSlot = slot;
               mc.options.useKey.setPressed(true);
               this.eating = true;
            }
         }
      } else {
         this.stop();
      }
   }

   private int find(Item item) {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void stop() {
      if (this.eating) {
         mc.options.useKey.setPressed(false);
         this.eating = false;
      }
   }

   @Override
   protected void onDisable() {
      this.stop();
   }
}
