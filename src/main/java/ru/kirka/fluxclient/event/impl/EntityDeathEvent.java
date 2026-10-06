package ru.kirka.fluxclient.event.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;
import ru.kirka.fluxclient.event.Event;

public class EntityDeathEvent implements Event {
   private final LivingEntity entity;
   private final DamageSource source;

   public EntityDeathEvent(LivingEntity entity, DamageSource source) {
      this.entity = entity;
      this.source = source;
   }

   @Nullable
   public LivingEntity getKillerEntity() {
      return this.entity.getAttacker();
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public DamageSource getSource() {
      return this.source;
   }
}
