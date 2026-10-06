package ru.kirka.fluxclient.feature.impl.hud;

import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.util.music.MusicTracker;

public class Music extends Module {
   public final BooleanSetting dynamicIsland = new BooleanSetting("Dynamic Island", "Отображение в стиле Apple Dynamic Island", true);
   public final BooleanSetting showVisualizer = new BooleanSetting("Эквалайзер", "Анимированные волны воспроизведения", true);
   public final BooleanSetting showCover = new BooleanSetting("Обложка", "Показывать обложку альбома или логотип", true);
   public final BooleanSetting showLyrics = new BooleanSetting("Текст песни", "Отображать строчки текста песни", true);
   public final BooleanSetting showProgress = new BooleanSetting("Прогресс", "Полоса прогресса воспроизведения", true);
   public final BooleanSetting blur = new BooleanSetting("Размытие", "Эффект матового стекла острова", true);
   public final BooleanSetting glow = new BooleanSetting("Свечение", "Мягкое свечение в стиле трека", true);
   public final BooleanSetting testMode = new BooleanSetting("Демо-режим", "Показывать тестовый трек для настройки", false);
   public final NumberSetting scale = new NumberSetting("Размер", "Масштаб острова", 1.0F, 0.8F, 1.3F, 0.05F);

   public Music() {
      super("Music", "Apple Dynamic Island для Spotify, Яндекс Музыки и треков на ПК", Category.HUD, -1);
      this.setEnabled(true);
      this.registerSetting(this.dynamicIsland);
      this.registerSetting(this.showVisualizer);
      this.registerSetting(this.showCover);
      this.registerSetting(this.showLyrics);
      this.registerSetting(this.showProgress);
      this.registerSetting(this.blur);
      this.registerSetting(this.glow);
      this.registerSetting(this.testMode);
      this.registerSetting(this.scale);
      MusicTracker.getInstance().start();
   }

   @Override
   public void onEnable() {
      super.onEnable();
      MusicTracker.getInstance().start();
   }
}
