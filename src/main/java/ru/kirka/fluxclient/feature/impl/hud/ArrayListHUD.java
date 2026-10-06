package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.hud.ArrayListWindow;

public class ArrayListHUD extends Module {
   public final ModeSetting sorting = new ModeSetting("Сортировка", "Порядок строк списка", "По длине", "По длине", "По алфавиту");
   public final BooleanSetting background = new BooleanSetting("Подложка", "Тёмный фон под строками", true);
   public final BooleanSetting animation = new BooleanSetting("Анимация", "Плавное появление и скрытие строк", true);

   public ArrayListHUD() {
      super("ArrayList", "Список включённых модулей (ImGui HUD)", Category.HUD, 0);
      this.registerSetting(this.sorting);
      this.registerSetting(this.background);
      this.registerSetting(this.animation);
   }

   public void render() {
      ArrayListWindow.render(this);
   }
}
