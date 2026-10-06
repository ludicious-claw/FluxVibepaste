package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class PotionsHUD extends Module {
   public final ModeSetting style = new ModeSetting("Стиль", "Вид списка эффектов", "Список", "Список", "Компакт");
   public final BooleanSetting background = new BooleanSetting("Фон", "Рисовать подложку панели", true);
   public final BooleanSetting preview = new BooleanSetting("Превью", "Показывать превью в чате, чтобы двигать панель", true);

   public PotionsHUD() {
      super("PotionsHUD", "Отображение активных эффектов зелий и их времени", Category.HUD, -1);
      this.registerSetting(this.style);
      this.registerSetting(this.background);
      this.registerSetting(this.preview);
   }
}
