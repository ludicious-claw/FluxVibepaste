package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.hud.ArmorHUDWindow;

public class ArmorHUD extends Module {
   public final BooleanSetting showPercent = new BooleanSetting("Проценты", "Показывать % прочности", true);
   public final BooleanSetting background = new BooleanSetting("Подложка", "Тёмный фон ячеек", true);

   public ArmorHUD() {
      super("ArmorHUD", "Состояние брони с полосками прочности", Category.HUD, 0);
      this.registerSetting(this.showPercent);
      this.registerSetting(this.background);
   }

   public void render() {
      ArmorHUDWindow.render(this);
   }
}
