package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class CustomCrosshair extends Module {
   public final ModeSetting style = new ModeSetting("Стиль", "Вид прицела", "Линии", "Линии", "Точка", "Круг", "Уголки");
   public final NumberSetting gap = new NumberSetting("Зазор", "Отступ от центра", 4.0F, 0.0F, 12.0F, 1.0F);
   public final NumberSetting len = new NumberSetting("Длина", "Длина лучей", 6.0F, 2.0F, 20.0F, 1.0F);
   public final NumberSetting thick = new NumberSetting("Толщина", "Толщина линий", 2.0F, 1.0F, 5.0F, 0.5F);
   public final BooleanSetting targetGlow = new BooleanSetting("Подсветка цели", "Менять цвет когда KillAura держит цель", true);

   public CustomCrosshair() {
      super("CustomCrosshair", "Кастомный прицел в стиле клиента", Category.RENDER, 0);
      this.registerSetting(this.style);
      this.registerSetting(this.gap);
      this.registerSetting(this.len);
      this.registerSetting(this.thick);
      this.registerSetting(this.targetGlow);
   }
}
