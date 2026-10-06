package ru.kirka.fluxclient.render.mixins;

import net.minecraft.entity.Entity;

public interface EntityRenderStateAddition {
   void flux$setEntity(Entity var1);

   Entity flux$getEntity();
}
