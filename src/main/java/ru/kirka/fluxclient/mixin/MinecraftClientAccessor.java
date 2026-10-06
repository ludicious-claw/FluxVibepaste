package ru.kirka.fluxclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
   @Invoker("doAttack")
   boolean flux$invokeDoAttack();

   @Mutable
   @Accessor("session")
   void flux$setSession(Session var1);
}
