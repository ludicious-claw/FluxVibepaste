package ru.kirka.fluxclient.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.impl.SoundEvent;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
   @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
   private void onPlaySound(SoundInstance sound, CallbackInfo ci) {
      if (FluxContext.get() != null && FluxContext.get().getEventBus() != null) {
         FluxContext.get().getEventBus().post(new SoundEvent(sound));
      }
   }
}
