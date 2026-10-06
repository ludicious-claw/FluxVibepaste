package ru.kirka.fluxclient.target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class TargetManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final TargetManager INSTANCE = new TargetManager();
   @Nullable
   private Entity currentTarget = null;
   private final List<String> target = new ArrayList<>();

   public static TargetManager getInstance() {
      return INSTANCE;
   }

   public void update(TargetSettings targetSettings) {
      this.currentTarget = this.getBestTarget(targetSettings);
   }

   @Nullable
   public Entity getBestTarget(TargetSettings settings) {
      if (mc.world == null) {
         return null;
      } else {
         Comparator<Entity> comparator = Comparator.<Entity, Boolean>comparing(e -> !this.target.contains(e.getName().getString()))
            .thenComparing(settings.getTargetComparator());
         return StreamSupport.<Entity>stream(mc.world.getEntities().spliterator(), false).filter(settings::isEntityValid).min(comparator).orElse(null);
      }
   }

   public void reset() {
      this.currentTarget = null;
   }

   public boolean isTarget(String name) {
      return this.target.contains(name);
   }

   public void addTarget(String name) {
      if (!this.target.contains(name)) {
         this.target.add(name);
      }
   }

   public void removeTarget(String name) {
      this.target.remove(name);
   }

   public void clearTarget() {
      this.target.clear();
   }

   @Nullable
   public LivingEntity getLivingTarget() {
      return this.currentTarget instanceof LivingEntity living ? living : null;
   }

   @Nullable
   public Entity getCurrentTarget() {
      return this.currentTarget;
   }

   public List<String> getTarget() {
      return this.target;
   }
}
