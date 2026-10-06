package ru.kirka.fluxclient.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.entity.BlockEntity;

public final class WorldUtility {
   public static final List<BlockEntity> blockEntities = new CopyOnWriteArrayList<>();

   private WorldUtility() {
      throw new UnsupportedOperationException();
   }
}
