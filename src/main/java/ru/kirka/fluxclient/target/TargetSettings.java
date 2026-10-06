package ru.kirka.fluxclient.target;

import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import ru.kirka.fluxclient.util.FriendUtil;

public class TargetSettings {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private boolean targetPlayers = true;
   private boolean targetAnimals = false;
   private boolean targetMobs = false;
   private boolean targetInvisibles = false;
   private boolean targetNakedPlayers = true;
   private boolean targetFriends = false;
   private boolean targetArmorStands = false;
   private float requiredRange = -1.0F;
   private Comparator<Entity> targetComparator = TargetComparators.DISTANCE;

   public boolean isEntityValid(Entity entity) {
      if (mc.player != null && mc.world != null && entity != null) {
         if (!(entity instanceof LivingEntity living && entity != mc.player)) {
            return false;
         } else if (living.isDead() || !living.isAlive()) {
            return false;
         } else if (!this.isWithinRange(entity)) {
            return false;
         } else if (entity instanceof ArmorStandEntity) {
            return this.targetArmorStands;
         } else if (!this.targetInvisibles && entity.isInvisible()) {
            return false;
         } else if (entity instanceof PlayerEntity player) {
            boolean isFriend = FriendUtil.isFriend(player.getName().getString());
            if (!this.targetFriends && isFriend) {
               return false;
            } else {
               boolean isNaked = this.isPlayerNaked(player);
               if (!this.targetPlayers && !this.targetNakedPlayers) {
                  return false;
               } else if (this.targetPlayers && this.targetNakedPlayers) {
                  return true;
               } else {
                  return this.targetNakedPlayers ? isNaked : !isNaked;
               }
            }
         } else {
            return !(entity instanceof AnimalEntity) && !(entity instanceof PassiveEntity)
               ? (entity instanceof MobEntity || entity instanceof HostileEntity || entity instanceof SlimeEntity) && this.targetMobs
               : this.targetAnimals;
         }
      } else {
         return false;
      }
   }

   public boolean isWithinRange(Entity entity) {
      return this.getRequiredRange() <= 0.0F || mc.player != null && entity.distanceTo(mc.player) <= this.getRequiredRange();
   }

   private boolean isPlayerNaked(PlayerEntity player) {
      for (ItemStack armorStack : player.getArmorItems()) {
         if (armorStack != null && !armorStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public boolean isTargetPlayers() {
      return this.targetPlayers;
   }

   public boolean isTargetAnimals() {
      return this.targetAnimals;
   }

   public boolean isTargetMobs() {
      return this.targetMobs;
   }

   public boolean isTargetInvisibles() {
      return this.targetInvisibles;
   }

   public boolean isTargetNakedPlayers() {
      return this.targetNakedPlayers;
   }

   public boolean isTargetFriends() {
      return this.targetFriends;
   }

   public boolean isTargetArmorStands() {
      return this.targetArmorStands;
   }

   public float getRequiredRange() {
      return this.requiredRange;
   }

   public Comparator<Entity> getTargetComparator() {
      return this.targetComparator;
   }

   public static class Builder {
      private final TargetSettings settings = new TargetSettings();

      public TargetSettings.Builder targetPlayers(boolean targetPlayers) {
         this.settings.targetPlayers = targetPlayers;
         return this;
      }

      public TargetSettings.Builder targetAnimals(boolean targetAnimals) {
         this.settings.targetAnimals = targetAnimals;
         return this;
      }

      public TargetSettings.Builder targetMobs(boolean targetMobs) {
         this.settings.targetMobs = targetMobs;
         return this;
      }

      public TargetSettings.Builder targetInvisibles(boolean targetInvisibles) {
         this.settings.targetInvisibles = targetInvisibles;
         return this;
      }

      public TargetSettings.Builder targetNakedPlayers(boolean targetNakedPlayers) {
         this.settings.targetNakedPlayers = targetNakedPlayers;
         return this;
      }

      public TargetSettings.Builder targetFriends(boolean targetFriends) {
         this.settings.targetFriends = targetFriends;
         return this;
      }

      public TargetSettings.Builder targetArmorStands(boolean targetArmorStands) {
         this.settings.targetArmorStands = targetArmorStands;
         return this;
      }

      public TargetSettings.Builder requiredRange(float requiredRange) {
         this.settings.requiredRange = requiredRange;
         return this;
      }

      public TargetSettings.Builder sortBy(Comparator<Entity> comparator) {
         this.settings.targetComparator = comparator;
         return this;
      }

      public TargetSettings build() {
         return this.settings;
      }
   }
}
