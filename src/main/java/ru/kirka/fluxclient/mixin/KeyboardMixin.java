package ru.kirka.fluxclient.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.Event;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.KeyEvent;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;
import ru.kirka.fluxclient.ui.screen.GUIScreen;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Inject(
      method = "onKey",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Keyboard;client:Lnet/minecraft/client/MinecraftClient;", ordinal = 0, shift = Shift.BEFORE),
      cancellable = true
   )
   private void onKeyInput(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      KeyEvent keyEvent = new KeyEvent(key, scancode, action, modifiers);
      EventManager.a((Event)keyEvent);
      if (FluxContext.get() != null && FluxContext.get().getEventBus() != null) {
         FluxContext.get().getEventBus().post(keyEvent);
      }

      if (action == 1) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.currentScreen instanceof GUIScreen gui) {
            Module clickGui = FluxContext.get().getModuleManager().getModule(ClickGuiModule.class);
            int bind = clickGui != null ? clickGui.getKeyBind() : 344;
            if (!gui.getSearchField().isFocused() && !gui.getNewConfigField().isFocused() && (key == 256 || key == bind)) {
               gui.close();
               ci.cancel();
               return;
            }
         } else if (mc.currentScreen == null && FluxContext.get().getModuleManager().handleKeyPress(key)) {
            ci.cancel();
         }
      }
   }
}
