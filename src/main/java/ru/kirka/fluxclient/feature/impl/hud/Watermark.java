package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class Watermark extends Module {
   public Watermark() {
      super("Watermark", "Информационный бар игрока (FPS, пинг, координаты)", Category.HUD, -1);
      this.setEnabled(true);
   }
}
