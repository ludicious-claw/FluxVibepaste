package ru.kirka.fluxclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntitySprintAccessor {
   @Accessor("lastSprinting")
   boolean flux$getWasSprinting();

   @Accessor("lastSprinting")
   void flux$setWasSprinting(boolean var1);
}
