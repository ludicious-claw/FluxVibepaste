package ru.kirka.fluxclient.feature.impl.misc;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class FakePlayer extends Module {
   private OtherClientPlayerEntity fake;
   private final int fakeId = -4242;

   public FakePlayer() {
      super("FakePlayer", "Клон игрока для тестов аур и ESP", Category.MISC, 0);
   }

   @Override
   protected void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.fake = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "FakePlayer"));
         this.fake.copyPositionAndRotation(mc.player);
         this.fake.setBodyYaw(mc.player.getBodyYaw());
         this.fake.setHeadYaw(mc.player.getHeadYaw());
         this.fake.equipStack(EquipmentSlot.HEAD, ((ItemStack)mc.player.getInventory().armor.get(3)).copy());
         this.fake.equipStack(EquipmentSlot.CHEST, ((ItemStack)mc.player.getInventory().armor.get(2)).copy());
         this.fake.equipStack(EquipmentSlot.LEGS, ((ItemStack)mc.player.getInventory().armor.get(1)).copy());
         this.fake.equipStack(EquipmentSlot.FEET, ((ItemStack)mc.player.getInventory().armor.get(0)).copy());
         this.fake.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
         this.fake.setId(-4242);
         this.fake.setHealth(20.0F);
         mc.world.addEntity(this.fake);
      } else {
         this.setEnabled(false);
      }
   }

   @Override
   protected void onDisable() {
      if (mc.world != null && this.fake != null) {
         mc.world.removeEntity(-4242, RemovalReason.DISCARDED);
      }

      this.fake = null;
   }
}
