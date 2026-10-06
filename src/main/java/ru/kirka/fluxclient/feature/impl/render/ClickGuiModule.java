package ru.kirka.fluxclient.feature.impl.render;

import net.minecraft.client.MinecraftClient;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.ui.screen.GUIScreen;

public class ClickGuiModule extends Module {
   public ClickGuiModule() {
      super("ClickGUI", "Графическое меню управления функциями клиента", Category.RENDER, 344);
   }

   @Override
   protected void onEnable() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null && client.currentScreen == null) {
         FluxClient.getInstance().openGuiScreen();
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.currentScreen instanceof GUIScreen gui) {
         gui.startClosing();
      }
   }
}
