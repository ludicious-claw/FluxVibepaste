package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.render.ScaleUtil;
import ru.kirka.fluxclient.ui.element.DragInfo;

public class ArmorWidget extends Widget implements Interface {
   public ArmorWidget() {
      super(new DragInfo("Броня", 0.0F, 0.0F, 0.0F, 0.0F));
      this.j().setWidget(this);
   }

   @Override
   public void a(DrawEvent event) {
      if (event.is2D() && mc.player != null && !mc.options.hudHidden && !mc.player.isSpectator()) {
         EquipmentSlot[] armorSlots = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
         int count = 0;

         for (EquipmentSlot slot : armorSlots) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) {
               count++;
            }
         }

         if (count > 0) {
            ScaleUtil.b(event.getDrawContext());
            event.getDrawContext().getMatrices().push();
            int startX = mc.getWindow().getScaledWidth() / 2 - 91 + 182 + 4;
            int startY = mc.getWindow().getScaledHeight() - 22;
            int barX = startX + (mc.player.getMainArm() == Arm.LEFT && !mc.player.getOffHandStack().isEmpty() ? 30 : 0);
            event.getDrawContext()
               .drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/hotbar"), 182, 22, 0, 0, barX, startY, count * 20 + 1, 22);
            int index = 0;

            for (EquipmentSlot slot2 : armorSlots) {
               ItemStack stack = mc.player.getEquippedStack(slot2);
               if (!stack.isEmpty()) {
                  int x = barX + 3 + index * 20;
                  int y = startY + 3;
                  event.getDrawContext().drawItem(stack, x, y);
                  event.getDrawContext().drawStackOverlay(mc.textRenderer, stack, x, y);
                  index++;
               }
            }

            event.getDrawContext().getMatrices().pop();
            ScaleUtil.c(event.getDrawContext());
         }
      }

      super.a(event);
   }
}
