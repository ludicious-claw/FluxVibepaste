package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class FullBright extends Module {
   public final NumberSetting gamma = new NumberSetting("Яркость", "Множитель гаммы освещения", 16.0F, 1.0F, 25.0F, 1.0F);

   public FullBright() {
      super("FullBright", "Максимальная яркость без факелов через гамму", Category.RENDER, 66);
      this.registerSetting(this.gamma);
   }
}
