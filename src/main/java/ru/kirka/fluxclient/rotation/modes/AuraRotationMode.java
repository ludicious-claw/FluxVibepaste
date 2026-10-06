package ru.kirka.fluxclient.rotation.modes;

import net.minecraft.entity.LivingEntity;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.rotation.MoveCorrection;
import ru.kirka.fluxclient.rotation.RotationHandler;

public interface AuraRotationMode {
   void rotate(Aura var1, RotationHandler var2, LivingEntity var3, MoveCorrection var4);

   default void reset() {
   }
}
