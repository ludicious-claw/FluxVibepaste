package ru.kirka.fluxclient.feature.impl.render;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.hud.NotificationManager;

public class Optimizations extends Module {
   private static Optimizations instance;
   public final BooleanSetting entityCulling = new BooleanSetting("Entity Culling", "Отсечение рендеринга далеких и скрытых сущностей (+20-40 FPS)", true);
   public final NumberSetting maxEntityDistance = new NumberSetting(
      "Дистанция сущностей", "Максимальная дистанция прорисовки мобов", 48.0F, 16.0F, 128.0F, 4.0F
   );
   public final BooleanSetting noEntityShadows = new BooleanSetting("Отключить тени сущностей", "Убирает тени под мобами (+10-15 FPS)", true);
   public final BooleanSetting limitParticles = new BooleanSetting("Лимит частиц", "Убирает тяжелые частицы дыма, взрывов и лавы", true);
   public final BooleanSetting fastGUI = new BooleanSetting("Быстрый GUI", "Снижает нагрузку шейдеров размытия в меню для плавного ClickGUI", true);
   public final BooleanSetting fastWorld = new BooleanSetting("Оптимизация мира", "Быстрый рендер неба, тумана и фонового освещения", true);
   public final BooleanSetting memoryCleaner = new BooleanSetting("Очистка RAM", "Автоматический сброс мусора JVM при нехватке памяти", true);
   private long lastGcTime = 0L;

   public Optimizations() {
      super("Optimizations", "Максимальная оптимизация Minecraft для высокого и стабильного FPS", Category.RENDER, -1);
      instance = this;
      this.registerSetting(this.entityCulling);
      this.registerSetting(this.maxEntityDistance);
      this.registerSetting(this.noEntityShadows);
      this.registerSetting(this.limitParticles);
      this.registerSetting(this.fastGUI);
      this.registerSetting(this.fastWorld);
      this.registerSetting(this.memoryCleaner);
      this.setEnabled(true);
   }

   public static Optimizations getInstance() {
      return instance;
   }

   public static boolean isOptimizationActive() {
      return instance != null && instance.isEnabled();
   }

   public static boolean shouldCull(Entity entity) {
      if (!isOptimizationActive() || !instance.entityCulling.get()) {
         return false;
      } else if (mc.player == null || entity == null || entity == mc.player) {
         return false;
      } else if (entity instanceof PlayerEntity) {
         double d = mc.player.squaredDistanceTo(entity);
         float limit = instance.maxEntityDistance.get() * 1.5F;
         return d > limit * limit;
      } else {
         double maxDistSq = instance.maxEntityDistance.get() * instance.maxEntityDistance.get();
         return mc.player.squaredDistanceTo(entity) > maxDistSq;
      }
   }

   public static boolean isFastGuiActive() {
      return isOptimizationActive() && instance.fastGUI.get();
   }

   @Override
   public void onEnable() {
      this.applySettings();
      NotificationManager.notifyInfo("Optimizations", "Режим максимального FPS включен!");
   }

   @Override
   public void onDisable() {
      if (mc.options != null && mc.options.getEntityShadows() != null) {
         mc.options.getEntityShadows().setValue(true);
      }

      NotificationManager.notifyInfo("Optimizations", "Оптимизации отключены");
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         this.applySettings();
         if (this.memoryCleaner.get()) {
            long now = System.currentTimeMillis();
            if (now - this.lastGcTime > 60000L) {
               Runtime rt = Runtime.getRuntime();
               long used = rt.totalMemory() - rt.freeMemory();
               long max = rt.maxMemory();
               if ((double)used / max > 0.82) {
                  System.gc();
                  this.lastGcTime = now;
               }
            }
         }
      }
   }

   private void applySettings() {
      if (mc.options != null) {
         if (this.noEntityShadows.get()) {
            SimpleOption<Boolean> shadows = mc.options.getEntityShadows();
            if (shadows != null && (Boolean)shadows.getValue()) {
               shadows.setValue(false);
            }
         }
      }
   }
}
