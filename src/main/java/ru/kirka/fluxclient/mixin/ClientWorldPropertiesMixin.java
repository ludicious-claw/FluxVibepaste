package ru.kirka.fluxclient.mixin;

import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.Ambience;
import ru.kirka.fluxclient.feature.impl.render.TimeChanger;

@Mixin(Properties.class)
public class ClientWorldPropertiesMixin {
   @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
   private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
      Ambience amb = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(Ambience.class);
      if (amb != null && amb.isEnabled() && amb.customTime.get()) {
         cir.setReturnValue((long)amb.time.get().floatValue());
      } else {
         TimeChanger tc = FluxContext.get().getModuleManager() == null ? null : FluxContext.get().getModuleManager().getModule(TimeChanger.class);
         if (tc != null && tc.isEnabled()) {
            cir.setReturnValue(tc.getTime());
         }
      }
   }
}
