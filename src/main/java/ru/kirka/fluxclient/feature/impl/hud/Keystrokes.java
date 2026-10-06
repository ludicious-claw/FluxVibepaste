package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.hud.KeystrokesWindow;

public class Keystrokes extends Module {
   public final BooleanSetting showCPS = new BooleanSetting("CPS", "Счётчик кликов под кнопками мыши", true);
   public final BooleanSetting showMouse = new BooleanSetting("Кнопки мыши", "Показывать ЛКМ/ПКМ", true);
   public final BooleanSetting background = new BooleanSetting("Подложка", "Тёмный фон клавиш", true);

   public Keystrokes() {
      super("Keystrokes", "Анимированные клавиши WASD и мышь", Category.HUD, 0);
      this.registerSetting(this.showCPS);
      this.registerSetting(this.showMouse);
      this.registerSetting(this.background);
   }

   public void render() {
      KeystrokesWindow.render(this);
   }
}
