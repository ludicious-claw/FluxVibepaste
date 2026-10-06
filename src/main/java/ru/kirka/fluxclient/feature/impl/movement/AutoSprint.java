package ru.kirka.fluxclient.feature.impl.movement;

import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoSprint extends Module {
   public final ModeSetting direction = new ModeSetting("Направление", "Когда держать спринт", "Вперёд (W)", "Вперёд (W)", "Все направления");

   public AutoSprint() {
      super("AutoSprint", "Автоматический спринт при беге", Category.MOVEMENT, 0);
      this.registerSetting(this.direction);
   }

   @Override
   protected void onDisable() {
      if (mc.options != null && mc.options.sprintKey != null) {
         mc.options.sprintKey.setPressed(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         if (mc.options != null) {
            boolean movingForward = mc.options.forwardKey.isPressed();
            boolean movingAny = movingForward || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
            boolean wantSprint = this.direction.is("Все направления") ? movingAny : movingForward;
            boolean canSprint = wantSprint && !mc.player.isSneaking() && !mc.player.isUsingItem() && mc.player.getHungerManager().getFoodLevel() > 6;
            if (canSprint) {
               mc.options.sprintKey.setPressed(true);
               if (!mc.player.isSprinting()) {
                  mc.player.setSprinting(true);
               }
            } else {
               mc.options.sprintKey.setPressed(false);
            }
         }
      }
   }
}
