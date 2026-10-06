package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AntiInvisible extends Module {
   public final NumberSetting opacity = new NumberSetting("Прозрачность", "Прозрачность невидимых игроков и сущностей (%)", 60.0F, 10.0F, 100.0F, 5.0F);

   public AntiInvisible() {
      super("AntiInvisible", "Делает полностью видимыми невидимых игроков и мобов с настраиваемой прозрачностью", Category.RENDER, -1);
      this.registerSetting(this.opacity);
   }
}
