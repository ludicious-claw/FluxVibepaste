package ru.kirka.fluxclient.mixin;

import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.DeathEvent;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {
   @Inject(method = "<init>", at = @At("TAIL"))
   private void onDeathScreenInit(CallbackInfo ci) {
      try {
         FluxContext.get().getEventBus().post(new DeathEvent());
      } catch (Exception var3) {
      }
   }
}
