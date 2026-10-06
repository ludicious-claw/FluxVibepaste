package ru.kirka.fluxclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
   public ClickGuiScreen() {
      super(Text.literal("FluxVisuals ClickGUI"));
   }

   protected void init() {
      super.init();
      ClickGuiWindow.onOpen();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && ClickGuiWindow.isSettingsOpen()) {
         ClickGuiWindow.closeSettings();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (!ClickGuiWindow.isFinishingClose()) {
         ClickGuiWindow.requestClose();
      }
   }
}
