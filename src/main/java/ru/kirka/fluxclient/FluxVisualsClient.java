package ru.kirka.fluxclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.logger.FluxLogger;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.msdf.Fonts;
import ru.kirka.fluxclient.render.shader.GlProgram;

public final class FluxVisualsClient implements ClientModInitializer {
   public static final String CLIENT_NAME = "FluxClient";
   public static final String CLIENT_VERSION = "1.0.0";
   public static final String WINDOW_TITLE = "FluxClient от Kirka_int :3";
   private static FluxVisualsClient instance;

   public void onInitializeClient() {
      instance = this;
      FluxLogger.info("Инициализация FluxClient v1.0.0...");
      FluxContext.get().initialize();
      new FluxClient();

      try {
         ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            public Identifier getFabricId() {
               return Identifier.of("fluxclient", "after_shader_load");
            }

            public void reload(ResourceManager manager) {
               try {
                  Fonts.init();
               } catch (Throwable var4) {
                  FluxLogger.warn("Не удалось загрузить MSDF шрифты: " + var4.getMessage());
               }

               try {
                  GlProgram.loadAndSetupPrograms();
               } catch (Throwable var3) {
                  FluxLogger.warn("Не удалось загрузить шейдеры: " + var3.getMessage());
               }
            }
         });
         DrawUtility.initializeShaders();
      } catch (Throwable var2) {
         FluxLogger.warn("Не удалось заранее инициализировать шейдеры: " + var2.getMessage());
      }

      FluxLogger.info("Ядро клиента и графическая система успешно инициализированы.");
      FluxLogger.info("Модуль запуска готов.");
   }

   public static FluxVisualsClient getInstance() {
      return instance;
   }
}
