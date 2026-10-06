package ru.kirka.fluxclient.feature.impl.combat;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class Velocity extends Module {
   public final NumberSetting horizontal = new NumberSetting("Горизонтальная", "Процент отдачи по горизонтали", 0.0F, 0.0F, 100.0F, 5.0F);
   public final NumberSetting vertical = new NumberSetting("Вертикальная", "Процент отдачи по вертикали", 0.0F, 0.0F, 100.0F, 5.0F);
   public final BooleanSetting explosions = new BooleanSetting("Взрывы", "Гасить отдачу от кристаллов и динамита", true);

   public Velocity() {
      super("Velocity", "Полная или частичная нейтрализация отталкивания в PvP", Category.COMBAT, -1);
      this.registerSetting(this.horizontal);
      this.registerSetting(this.vertical);
      this.registerSetting(this.explosions);
   }
}
