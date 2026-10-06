package ru.kirka.fluxclient.feature.impl.combat;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class Hitboxes extends Module {
   public final NumberSetting expand = new NumberSetting("Размер", "Увеличение хитбокса в блоках", 0.35F, 0.05F, 1.5F, 0.05F);
   public final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", "Расширять хитбоксы только у игроков", false);

   public Hitboxes() {
      super("Hitboxes", "Расширение хитбоксов врагов для более легкого попадания", Category.COMBAT, -1);
      this.registerSetting(this.expand);
      this.registerSetting(this.onlyPlayers);
   }
}
