package ru.kirka.fluxclient.mixin;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.FullBright;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
   @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
   private Object onGetGamma(SimpleOption<Double> option) {
      FullBright fullBright = FluxContext.get().getModuleManager().getModule(FullBright.class);
      return fullBright != null && fullBright.isEnabled() ? (double)fullBright.gamma.get().floatValue() : option.getValue();
   }
}
