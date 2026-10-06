package ru.kirka.fluxclient.feature.impl.render;

import net.minecraft.client.MinecraftClient;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.ClientPlayerTickEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class NoRender extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private double oldFovEffectScale = 1.0;
   public final BooleanSetting hurtCam = new BooleanSetting("Тряска камеры", "Отключить покачивание камеры при получении урона", true);
   public final BooleanSetting fire = new BooleanSetting("Огонь", "Убрать пламя на экране при горении", true);
   public final BooleanSetting cameraClip = new BooleanSetting("Клип камеры", "Прохождение камеры от третьего лица сквозь блоки", true);
   public final BooleanSetting water = new BooleanSetting("Вода", "Убрать визуальный подводный туман и эффект погружения", true);
   public final BooleanSetting pumpkin = new BooleanSetting("Тыква", "Убрать черную виньетку вырезанной тыквы", true);
   public final BooleanSetting blindness = new BooleanSetting("Слепота", "Убрать эффект слепоты на экране", true);
   public final BooleanSetting darkness = new BooleanSetting("Тьма", "Убрать эффект темного пульсирования", true);
   public final BooleanSetting nausea = new BooleanSetting("Тошнота", "Убрать искажение экрана при тошноте", true);
   public final BooleanSetting portal = new BooleanSetting("Портал", "Убрать эффект искажения портала", true);
   public final BooleanSetting scoreboard = new BooleanSetting("Скорборд", "Скрыть боковую панель счета", false);
   public final BooleanSetting bossBar = new BooleanSetting("Боссбар", "Скрыть полосы боссов сверху экрана", false);
   public final BooleanSetting breakParticles = new BooleanSetting("Частицы блоков", "Отключить частицы разрушения блоков", false);
   public final BooleanSetting fov = new BooleanSetting("Статичный FOV", "Отключить изменение угла обзора от скорости", true);
   public final BooleanSetting weather = new BooleanSetting("Погода", "Отключить дождь и снег", false);
   public final BooleanSetting beaconSound = new BooleanSetting("Звук маяка", "Отключить гул маяка", false);
   public final BooleanSetting phantomSound = new BooleanSetting("Звук фантомов", "Отключить крики фантомов", false);
   public final BooleanSetting weatherSound = new BooleanSetting("Звук погоды", "Отключить шум дождя и грома", false);
   private final EventListener<ClientPlayerTickEvent> onTickEvent = event -> {
      if (this.fov.get() && mc.options != null) {
         mc.options.getFovEffectScale().setValue(0.0);
      }
   };

   public NoRender() {
      super("NoRender", "Отключение мешающих оверлеев, частиц и эффектов", Category.RENDER, -1);
      this.registerSetting(this.hurtCam);
      this.registerSetting(this.fire);
      this.registerSetting(this.cameraClip);
      this.registerSetting(this.water);
      this.registerSetting(this.pumpkin);
      this.registerSetting(this.blindness);
      this.registerSetting(this.darkness);
      this.registerSetting(this.nausea);
      this.registerSetting(this.portal);
      this.registerSetting(this.scoreboard);
      this.registerSetting(this.bossBar);
      this.registerSetting(this.breakParticles);
      this.registerSetting(this.fov);
      this.registerSetting(this.weather);
      this.registerSetting(this.beaconSound);
      this.registerSetting(this.phantomSound);
      this.registerSetting(this.weatherSound);
   }

   @Override
   public void onEnable() {
      if (mc.options != null) {
         this.oldFovEffectScale = (Double)mc.options.getFovEffectScale().getValue();
      }

      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         mc.options.getFovEffectScale().setValue(this.oldFovEffectScale);
      }

      super.onDisable();
   }
}
