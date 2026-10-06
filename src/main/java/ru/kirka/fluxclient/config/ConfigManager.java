package ru.kirka.fluxclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.MultiSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.config.impl.TextSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.logger.FluxLogger;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;

public final class ConfigManager {
   private final File configsFolder;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private String currentConfigName = "Default";
   private boolean loading = false;

   public ConfigManager() {
      this.configsFolder = new File(MinecraftClient.getInstance().runDirectory, "fluxclient/configs");
      if (!this.configsFolder.exists()) {
         this.configsFolder.mkdirs();
      }
   }

   public List<String> getAvailableConfigs() {
      List<String> list = new ArrayList<>();
      File[] files = this.configsFolder.listFiles((dir, name) -> name.endsWith(".flux"));
      if (files != null) {
         for (File f : files) {
            list.add(f.getName().replace(".flux", ""));
         }
      }

      if (list.isEmpty()) {
         list.add("Default");
      }

      return list;
   }

   public void openFolder() {
      Util.getOperatingSystem().open(this.configsFolder);
   }

   public void saveConfig(String name) {
      if (!this.loading) {
         this.currentConfigName = name;
         File file = new File(this.configsFolder, name + ".flux");
         JsonObject root = new JsonObject();

         for (Module module : FluxContext.get().getModuleManager().getModules()) {
            JsonObject modObj = new JsonObject();
            boolean isClickGui = module instanceof ClickGuiModule || module.getName().equalsIgnoreCase("ClickGUI");
            modObj.addProperty("enabled", !isClickGui && module.isEnabled());
            modObj.addProperty("bind", module.getKeyBind());
            JsonObject settingsObj = new JsonObject();

            for (Setting<?> s : module.getSettings()) {
               if (s instanceof BooleanSetting b) {
                  settingsObj.addProperty(b.getName(), b.get());
               } else if (s instanceof NumberSetting n) {
                  settingsObj.addProperty(n.getName(), n.get());
               } else if (s instanceof ModeSetting m) {
                  settingsObj.addProperty(m.getName(), m.get());
               } else if (s instanceof ColorSetting c) {
                  settingsObj.addProperty(c.getName(), c.get().getRGB());
               } else if (s instanceof TextSetting t) {
                  settingsObj.addProperty(t.getName(), t.get());
               } else if (s instanceof MultiSetting ms) {
                  JsonObject sub = new JsonObject();

                  for (BooleanSetting b : ms.getOptions()) {
                     sub.addProperty(b.getName(), b.get());
                  }

                  settingsObj.add(ms.getName(), sub);
               }
            }

            modObj.add("settings", settingsObj);
            root.add(module.getName(), modObj);
         }

         try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
            FluxLogger.info("Конфиг сохранён: " + file.getName());
         } catch (Exception var22) {
            FluxLogger.error("Ошибка сохранения конфига: " + name, var22);
         }
      }
   }

   public void loadConfig(String name) {
      this.currentConfigName = name;
      this.loading = true;

      try {
         File file = new File(this.configsFolder, name + ".flux");
         if (!file.exists()) {
            this.saveConfig(name);
            return;
         }

         try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (Module module : FluxContext.get().getModuleManager().getModules()) {
               if (root.has(module.getName())) {
                  JsonObject modObj = root.getAsJsonObject(module.getName());
                  boolean isClickGui = module instanceof ClickGuiModule || module.getName().equalsIgnoreCase("ClickGUI");
                  if (!isClickGui && modObj.has("enabled")) {
                     module.setEnabled(modObj.get("enabled").getAsBoolean());
                  }

                  if (modObj.has("bind")) {
                     module.setKeyBind(modObj.get("bind").getAsInt());
                  }

                  if (modObj.has("settings")) {
                     JsonObject setObj = modObj.getAsJsonObject("settings");

                     for (Setting<?> s : module.getSettings()) {
                        if (setObj.has(s.getName())) {
                           if (s instanceof BooleanSetting b) {
                              b.set(setObj.get(b.getName()).getAsBoolean());
                           } else if (s instanceof NumberSetting n) {
                              n.set(setObj.get(n.getName()).getAsFloat());
                           } else if (s instanceof ModeSetting m) {
                              m.set(setObj.get(m.getName()).getAsString());
                           } else if (s instanceof ColorSetting c) {
                              c.set(new Color(setObj.get(c.getName()).getAsInt(), true));
                           } else if (s instanceof TextSetting t) {
                              t.set(setObj.get(t.getName()).getAsString());
                           } else if (s instanceof MultiSetting ms && setObj.has(ms.getName())) {
                              JsonObject sub = setObj.getAsJsonObject(ms.getName());

                              for (BooleanSetting b : ms.getOptions()) {
                                 if (sub.has(b.getName())) {
                                    b.set(sub.get(b.getName()).getAsBoolean());
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            FluxLogger.info("Конфиг загружен: " + file.getName());
         } catch (Exception var28) {
            FluxLogger.error("Ошибка загрузки конфига: " + name, var28);
         }
      } finally {
         this.loading = false;
      }
   }

   public void deleteConfig(String name) {
      File file = new File(this.configsFolder, name + ".flux");
      if (file.exists()) {
         file.delete();
      }
   }

   public boolean isLoading() {
      return this.loading;
   }

   public String getCurrentConfigName() {
      return this.currentConfigName;
   }

   public void load() {
      this.loadConfig("Default");
   }

   public void save() {
      this.saveConfig(this.currentConfigName);
   }
}
