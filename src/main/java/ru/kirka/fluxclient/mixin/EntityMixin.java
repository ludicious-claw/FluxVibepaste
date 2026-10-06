package ru.kirka.fluxclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.combat.Hitboxes;
import ru.kirka.fluxclient.feature.impl.misc.SeeInvinsibles;
import ru.kirka.fluxclient.feature.impl.render.AntiInvisible;

@Mixin(Entity.class)
public class EntityMixin {
   @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
   private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
      Hitboxes hitboxes = FluxContext.get().getModuleManager().getModule(Hitboxes.class);
      if (hitboxes != null && hitboxes.isEnabled()) {
         Entity entity = (Entity)(Object)this;
         MinecraftClient mc = MinecraftClient.getInstance();
         if (entity != mc.player && (!hitboxes.onlyPlayers.get() || entity instanceof PlayerEntity)) {
            cir.setReturnValue(hitboxes.expand.get());
         }
      }
   }

   @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
   private void onIsInvisibleTo(PlayerEntity observer, CallbackInfoReturnable<Boolean> cir) {
      SeeInvinsibles seeInvinsibles = FluxContext.get().getModuleManager().getModule(SeeInvinsibles.class);
      AntiInvisible antiInvisible = FluxContext.get().getModuleManager().getModule(AntiInvisible.class);
      Entity entity = (Entity)(Object)this;
      MinecraftClient mc = MinecraftClient.getInstance();
      if ((seeInvinsibles != null && seeInvinsibles.isEnabled() || antiInvisible != null && antiInvisible.isEnabled())
         && entity instanceof LivingEntity
         && observer == mc.player) {
         cir.setReturnValue(false);
      }
   }
}
