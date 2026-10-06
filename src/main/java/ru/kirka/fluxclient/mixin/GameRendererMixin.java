package ru.kirka.fluxclient.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.Event;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.feature.impl.combat.AimAssistant;
import ru.kirka.fluxclient.feature.impl.combat.Aura;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderTail(RenderTickCounter tickCounter, boolean renderLevel, CallbackInfo ci) {
      AimAssistant aimAssistant = FluxContext.get().getModuleManager().getModule(AimAssistant.class);
      if (aimAssistant != null) {
         aimAssistant.onFrame();
      }

      try {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null) {
            aura.onFrame();
         }
      } catch (Exception var6) {
      }

      EventManager.a((Event)(new GlobalEvent()));
   }
}
