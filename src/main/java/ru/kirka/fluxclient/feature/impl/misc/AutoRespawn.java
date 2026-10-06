package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.client.gui.screen.DeathScreen;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoRespawn extends Module {
   public AutoRespawn() {
      super("AutoRespawn", "Мгновенное возрождение после смерти", Category.MISC, 0);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
   }
}
