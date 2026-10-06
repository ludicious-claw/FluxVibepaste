package ru.kirka.fluxclient.feature.impl.render;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.core.logger.FluxLogger;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.hud.NotificationManager;

public class WorldReflections extends Module {
   public final ModeSetting profile = new ModeSetting(
      "Профиль", "Качество шейдеров и освещения", "Высокие (High)", "Высокие (High)", "Ультра (PBR)", "Баланс (Medium)", "Производительность (Low)"
   );
   public final NumberSetting brightness = new NumberSetting("Яркость (Exposure)", "Общая яркость и освещенность картинки", 1.25F, 0.8F, 2.0F, 0.05F);
   public final BooleanSetting puddles = new BooleanSetting("Лужи при дожде (SSR)", "Зеркальные PBR-лужи с отражением мира", true);
   public final BooleanSetting godRays = new BooleanSetting("Лучи солнца", "Кинематографичные объемные световые лучи", true);
   public final BooleanSetting reflections = new BooleanSetting("Отражения мира и воды", "Полноценные Screen-Space Reflections (SSR)", true);
   public final BooleanSetting openIrisMenu = new BooleanSetting("Настройки шейдеров", "Открыть графическую панель Iris Shaders", false);
   private String lastProfile = null;
   private float lastBrightness = -1.0F;
   private boolean lastPuddles = true;
   private boolean lastGodRays = true;
   private boolean lastReflections = true;

   public WorldReflections() {
      super("WorldReflections", "Полноценные шейдеры, PBR-отражения луж при дожде и лучи солнца", Category.RENDER, -1);
      this.registerSetting(this.profile);
      this.registerSetting(this.brightness);
      this.registerSetting(this.puddles);
      this.registerSetting(this.godRays);
      this.registerSetting(this.reflections);
      this.registerSetting(this.openIrisMenu);
   }

   @Override
   public void onEnable() {
      if (!isIrisAvailable()) {
         NotificationManager.notifyInfo("WorldReflections", "Iris Shaders не найден в папке mods!");
      } else {
         this.applyShaderConfig();
         setIrisShadersActive(true);
         NotificationManager.notifyInfo("WorldReflections", "Шейдеры и PBR-отражения включены!");
      }
   }

   @Override
   public void onDisable() {
      if (isIrisAvailable()) {
         setIrisShadersActive(false);
         NotificationManager.notifyInfo("WorldReflections", "Шейдеры отключены");
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         if (!this.isEnabled()) {
            if (isIrisAvailable() && isShadersInUse()) {
               setIrisShadersActive(false);
            }
         } else {
            if (this.openIrisMenu.get()) {
               this.openIrisMenu.set(false);
               this.openIrisScreen();
            }

            boolean p = this.puddles.get();
            boolean g = this.godRays.get();
            boolean r = this.reflections.get();
            float b = this.brightness.get();
            String prof = this.profile.get();
            if (this.lastProfile == null) {
               this.lastProfile = prof;
               this.lastBrightness = b;
               this.lastPuddles = p;
               this.lastGodRays = g;
               this.lastReflections = r;
            } else {
               if (p != this.lastPuddles
                  || g != this.lastGodRays
                  || r != this.lastReflections
                  || Math.abs(b - this.lastBrightness) > 0.01F
                  || !prof.equals(this.lastProfile)) {
                  this.lastPuddles = p;
                  this.lastGodRays = g;
                  this.lastReflections = r;
                  this.lastBrightness = b;
                  this.lastProfile = prof;
                  this.applyShaderConfig();
                  if (isShadersInUse()) {
                     reloadIris();
                  }
               }
            }
         }
      }
   }

   private void applyShaderConfig() {
      try {
         File gameDir = mc.runDirectory;
         if (gameDir == null) {
            return;
         }

         Path spTxt = gameDir.toPath().resolve("shaderpacks").resolve("ComplementaryReimagined_r5.9.3.zip.txt");
         if (!Files.exists(spTxt.getParent())) {
            Files.createDirectories(spTxt.getParent());
         }

         int puddleLevel = this.puddles.get() ? 2 : 0;
         int lightShaft = this.godRays.get() ? 2 : 0;
         int waterReflect = this.reflections.get() ? 2 : 1;
         int blockReflect = this.reflections.get() ? 3 : 0;
         String exposure = this.profile.get();

         String p = switch (exposure) {
            case "Ультра (PBR)" -> "ULTRA";
            case "Баланс (Medium)" -> "MEDIUM";
            case "Производительность (Low)" -> "LOW";
            default -> "HIGH";
         };
         exposure = String.format(Locale.US, "%.2f", this.brightness.get());
         String configContent = "profile="
            + p
            + "\nRAIN_PUDDLES="
            + puddleLevel
            + "\nWATER_REFLECT_QUALITY="
            + waterReflect
            + "\nBLOCK_REFLECT_QUALITY="
            + blockReflect
            + "\nLIGHTSHAFT_QUALI_DEFINE="
            + lightShaft
            + "\nSSAO_QUALI_DEFINE=2\nWORLD_SPACE_REFLECTIONS=-1\nENTITY_SHADOW=1\nTM_EXPOSURE="
            + exposure
            + "\nAMBIENT_MULT=120\n";
         Files.writeString(spTxt, configContent);
         String appData = System.getenv("APPDATA");
         if (appData != null) {
            Path appDataSpTxt = Path.of(appData, ".fluxclient", "shaderpacks", "ComplementaryReimagined_r5.9.3.zip.txt");
            if (Files.exists(appDataSpTxt.getParent())) {
               Files.writeString(appDataSpTxt, configContent);
            }
         }
      } catch (Throwable var12) {
         FluxLogger.error("Failed to write shaderpack config: " + var12.getMessage());
      }
   }

   public static boolean isIrisAvailable() {
      try {
         Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   public static boolean isShadersInUse() {
      try {
         Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Object instance = apiClass.getMethod("getInstance").invoke(null);
         if (instance != null) {
            return (Boolean)apiClass.getMethod("isShaderPackInUse").invoke(instance);
         }
      } catch (Throwable var2) {
      }

      return false;
   }

   public static void reloadIris() {
      if (mc != null && mc.getWindow() != null) {
         try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            irisClass.getMethod("reload").invoke(null);
         } catch (Throwable var1) {
            FluxLogger.error("Failed to reload Iris: " + var1.getMessage());
         }
      }
   }

   public static void setIrisShadersActive(boolean active) {
      try {
         Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
         Object irisConfig = irisClass.getMethod("getIrisConfig").invoke(null);
         if (irisConfig != null) {
            if (active) {
               irisConfig.getClass().getMethod("setShaderPackName", String.class).invoke(irisConfig, "ComplementaryReimagined_r5.9.3.zip");
            }

            irisConfig.getClass().getMethod("setShadersEnabled", boolean.class).invoke(irisConfig, active);
            irisConfig.getClass().getMethod("save").invoke(irisConfig);
         }

         if (mc != null && mc.getWindow() != null) {
            try {
               Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               Object instance = apiClass.getMethod("getInstance").invoke(null);
               if (instance != null) {
                  Object config = apiClass.getMethod("getConfig").invoke(instance);
                  if (config != null) {
                     config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, active);
                     return;
                  }
               }
            } catch (Throwable var6) {
            }

            irisClass.getMethod("reload").invoke(null);
         }
      } catch (Throwable var7) {
         FluxLogger.error("IrisApi toggle error: " + var7.getMessage());
      }
   }

   public void openIrisScreen() {
      try {
         Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Object instance = apiClass.getMethod("getInstance").invoke(null);
         if (instance != null && apiClass.getMethod("openMainIrisScreenObj", Object.class).invoke(instance, mc.currentScreen) instanceof Screen sc) {
            mc.setScreen(sc);
         }
      } catch (Throwable var5) {
         FluxLogger.error("Failed to open Iris screen: " + var5.getMessage());
      }
   }
}
