package ru.kirka.fluxclient.mixin;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.kirka.fluxclient.util.EntityRenderStateAddition;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateAddition {
   @Unique
   private Entity flux$entity;

   @Unique
   @Override
   public void flux$setEntity(Entity entity) {
      this.flux$entity = entity;
   }

   @Unique
   @Override
   public Entity flux$getEntity() {
      return this.flux$entity;
   }
}
