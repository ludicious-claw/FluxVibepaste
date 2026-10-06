package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class TimeChanger extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Пресет времени", "День", "День", "Ночь", "Рассвет", "Закат", "Полночь", "Кастом");
   public final NumberSetting customTime = new NumberSetting("Время", "Значение тиков в режиме Кастом", 6000.0F, 0.0F, 24000.0F, 500.0F);

   public TimeChanger() {
      super("TimeChanger", "Визуальное изменение времени суток на клиенте", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.customTime);
   }

   public long getTime() {
      if (this.mode.is("День")) {
         return 6000L;
      } else if (this.mode.is("Закат")) {
         return 12000L;
      } else if (this.mode.is("Ночь")) {
         return 14000L;
      } else if (this.mode.is("Полночь")) {
         return 18000L;
      } else {
         return this.mode.is("Рассвет") ? 23000L : (long)this.customTime.get().floatValue();
      }
   }
}
