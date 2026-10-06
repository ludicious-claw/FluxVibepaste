package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoEat extends Module {
   public final NumberSetting hunger = new NumberSetting("Порог голода", "Начинать есть при голоде <=", 14.0F, 1.0F, 19.0F, 1.0F);
   private boolean eating = false;

   public AutoEat() {
      super("AutoEat", "Сам ест обычную еду при низком голоде", Category.MISC, 0);
      this.registerSetting(this.hunger);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         if (mc.player.getHungerManager().getFoodLevel() > this.hunger.get()) {
            this.stop();
         } else {
            int slot = this.findFood();
            if (slot < 0) {
               this.stop();
            } else {
               mc.player.getInventory().selectedSlot = slot;
               mc.options.useKey.setPressed(true);
               this.eating = true;
            }
         }
      }
   }

   private int findFood() {
      int best = -1;
      float bestScore = 0.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack st = mc.player.getInventory().getStack(i);
         FoodComponent food = (FoodComponent)st.get(DataComponentTypes.FOOD);
         if (!st.isEmpty() && food != null && st.getItem() != Items.GOLDEN_APPLE && st.getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
            float score = food.nutrition() + food.saturation();
            if (score > bestScore) {
               bestScore = score;
               best = i;
            }
         }
      }

      return best;
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
