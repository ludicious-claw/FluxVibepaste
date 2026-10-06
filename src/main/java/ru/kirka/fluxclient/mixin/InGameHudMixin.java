package ru.kirka.fluxclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.core.Event;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.impl.HudRenderEvent;
import ru.kirka.fluxclient.event.impl.PostHudRenderEvent;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.feature.impl.render.CustomCrosshair;
import ru.kirka.fluxclient.feature.impl.render.NoRender;
import ru.kirka.fluxclient.render.context.CustomDrawContext;

@Mixin(InGameHud.class)
public class InGameHudMixin {
   @Inject(method = "render", at = @At("HEAD"))
   public void headRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (FluxClient.getInstance() != null) {
         FluxClient.getInstance().getDraw2DProcessor().e().a(context.getMatrices());
         EventManager.a((Event)(new DrawEvent(context, tickCounter.getTickDelta(false), DrawEvent.Type.D2D)));
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      FluxContext.get().getEventBus().post(new PreHudRenderEvent(customContext, tickCounter.getTickDelta(false)));
   }

   @Inject(method = "render", at = @At("TAIL"))
   public void tailRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customContext = CustomDrawContext.of(context);
      FluxContext.get().getEventBus().post(new HudRenderEvent(customContext, tickCounter.getTickDelta(false)));
      FluxContext.get().getEventBus().post(new PostHudRenderEvent(customContext, tickCounter.getTickDelta(false)));
   }

   @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
      NoRender noRender = FluxContext.get().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.pumpkin.get() && texture.getPath().contains("pumpkinblur")) {
         ci.cancel();
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void cancelVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomCrosshair cc = FluxContext.get().getModuleManager().getModule(CustomCrosshair.class);
      if (cc != null && cc.isEnabled()) {
         ci.cancel();
      }
   }
}
