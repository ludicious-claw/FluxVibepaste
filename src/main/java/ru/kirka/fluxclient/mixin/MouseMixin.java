package ru.kirka.fluxclient.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.core.Event;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.event.ClickEvent;
import ru.kirka.fluxclient.event.KeyEvent;
import ru.kirka.fluxclient.event.ScrollEvent;

@Mixin(Mouse.class)
public class MouseMixin {
   @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
   public void onMouseButton(long window, int button, int action, int modifiers, CallbackInfo ci) {
      if (Interface.mc.currentScreen == null) {
         EventManager.a((Event)(new KeyEvent(button >= 0 && button <= 7 ? -100 + button : button, 0, action, modifiers)));
      }

      if (action == 1) {
         ClickEvent event = new ClickEvent(Interface.mc.mouse.getX() / 2.0, Interface.mc.mouse.getY() / 2.0, button, ClickEvent.Action.PRESS);
         EventManager.a((Event)event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      } else if (action == 0) {
         ClickEvent event2 = new ClickEvent(Interface.mc.mouse.getX() / 2.0, Interface.mc.mouse.getY() / 2.0, button, ClickEvent.Action.RELEASE);
         EventManager.a((Event)event2);
         if (event2.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
   public void onCursorPos(long window, double x, double y, CallbackInfo ci) {
      if (Interface.mc != null && Interface.mc.mouse != null) {
         ClickEvent event = new ClickEvent(Interface.mc.mouse.getX() / 2.0, Interface.mc.mouse.getY() / 2.0, 0, ClickEvent.Action.DRAG);
         EventManager.a((Event)event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "onMouseScroll", at = @At("RETURN"))
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      EventManager.a((Event)(new ScrollEvent(horizontal, vertical)));
   }
}
