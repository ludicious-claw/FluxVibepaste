package ru.kirka.fluxclient.feature.impl.render;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class Tracers extends Module {
   public final BooleanSetting players = new BooleanSetting("Игроки", "Линии к игрокам", true);
   public final BooleanSetting monsters = new BooleanSetting("Мобы", "Линии к монстрам", false);
   public final BooleanSetting animals = new BooleanSetting("Животные", "Линии к мирным мобам", false);
   public final ModeSetting origin = new ModeSetting("Откуда", "Точка начала линии", "Низ экрана", "Низ экрана", "Центр");
   public final BooleanSetting targetDot = new BooleanSetting("Маркер на цели", "Светящаяся точка в месте контакта", true);
   public final BooleanSetting distanceFade = new BooleanSetting("Затухание", "Плавное растворение по дистанции", true);
   public final NumberSetting thickness = new NumberSetting("Толщина", "Толщина линии", 1.5F, 0.5F, 4.0F, 0.5F);
   public final NumberSetting opacity = new NumberSetting("Прозрачность", "Прозрачность линий", 0.75F, 0.1F, 1.0F, 0.05F);

   public Tracers() {
      super("Tracers", "Линии к целям через весь экран", Category.RENDER, 0);
      this.registerSetting(this.players);
      this.registerSetting(this.monsters);
      this.registerSetting(this.animals);
      this.registerSetting(this.origin);
      this.registerSetting(this.targetDot);
      this.registerSetting(this.distanceFade);
      this.registerSetting(this.thickness);
      this.registerSetting(this.opacity);
   }
}
