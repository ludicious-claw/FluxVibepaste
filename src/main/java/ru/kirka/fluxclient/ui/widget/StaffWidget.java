package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.staff.StaffConstructor;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;

public class StaffWidget extends Widget implements Interface {
   public StaffWidget() {
      super(new DragInfo("Стафф", 10.0F, 450.0F, 100.0F, 20.0F));
      this.j().setWidget(this);
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      float x = this.j().getClampedX();
      float y = this.j().getClampedY();
      float targetWidth = 14.5F + Fonts.e.a("Staff-list", 7.0F) + 5.0F + 2.0F;
      float contentY = y + 12.5F + 3.0F;
      boolean active = false;
      if (FluxClient.getInstance() != null && FluxClient.getInstance().getStaffProcessor() != null) {
         for (StaffConstructor staff : FluxClient.getInstance().getStaffProcessor().getStaffList()) {
            if (staff.getAnimation().c() > 0.0F) {
               targetWidth = Math.max(
                  targetWidth,
                  19.0F + Fonts.e.a(staff.getName(), 6.5F) + 8.0F + Fonts.e.a(this.isNear(staff.getName()) ? "Near" : "Online", 6.5F) + 5.0F + 2.0F
               );
               active = true;
            }
         }
      }

      float width = MathUtil.c(this.j().getWidth(), targetWidth, 0.5F);
      this.j().setWidth(width);
      if (this.a() > 0.0F) {
         this.a(event, "i", "Staff-list", width, this.a());
      }

      if (FluxClient.getInstance() != null && FluxClient.getInstance().getStaffProcessor() != null) {
         for (StaffConstructor staff2 : FluxClient.getInstance().getStaffProcessor().getStaffList()) {
            AnimationUtil animationUtil = staff2.getAnimation();
            animationUtil.a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
            float animation = animationUtil.c() * this.a();
            if (animation > 0.0F) {
               float offsetX = -8.0F * (1.0F - animation);
               float offsetY = -(1.0F - animation);
               float drawY = contentY + offsetY;
               float textY = drawY + (11.5F - Fonts.e.a(6.5F)) / 2.0F - 0.5F;
               this.a(event, x + offsetX, drawY, width, 11.5F, false, animation);
               this.a(event, x + offsetX + 15.0F, drawY, 11.5F, animation);
               PlayerListEntry entry = mc.getNetworkHandler() == null
                  ? null
                  : mc.getNetworkHandler()
                     .getPlayerList()
                     .stream()
                     .filter(e -> e.getProfile().getName().equalsIgnoreCase(staff2.getName()))
                     .findFirst()
                     .orElse(null);
               if (entry != null) {
                  FluxClient.getInstance()
                     .getDraw2DProcessor()
                     .a(
                        event.getMatrixStack(),
                        x + offsetX + 5.0F,
                        drawY + 2.0F,
                        7.5F,
                        7.5F,
                        2.0F,
                        ColorUtil.applyAlphaToColor(-1, animation),
                        0.125F,
                        0.125F,
                        0.125F,
                        0.125F,
                        mc.getTextureManager().getTexture(entry.getSkinTextures().texture()).getGlId()
                     );
               } else {
                  Fonts.a
                     .a(
                        event.getMatrixStack(),
                        "y",
                        x + offsetX + 5.0F,
                        drawY + (11.5F - Fonts.a.a(8.0F)) / 2.0F,
                        8.0F,
                        ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor(), animation)
                     );
               }

               Fonts.e
                  .a(
                     event.getMatrixStack(),
                     staff2.getName(),
                     x + offsetX + 19.0F,
                     textY,
                     6.5F,
                     ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), animation)
                  );
               boolean near = this.isNear(staff2.getName());
               Fonts.e
                  .a(
                     event.getMatrixStack(),
                     near ? "Near" : "Online",
                     x + offsetX + width - 5.0F - Fonts.e.a(near ? "Near" : "Online", 6.5F) - 1.0F,
                     textY,
                     6.5F,
                     ColorUtil.applyAlphaToColor(near ? -1529792 : -9711765, animation)
                  );
               contentY += 13.5F * animation;
            }
         }
      }

      this.j().setHeight(active ? contentY - y - 2.0F : 12.5F);
      super.a(event);
   }

   @Override
   public void a(GlobalEvent event) {
      boolean visible = mc.currentScreen instanceof ChatScreen;
      if (FluxClient.getInstance() != null && FluxClient.getInstance().getStaffProcessor() != null) {
         for (StaffConstructor staff : FluxClient.getInstance().getStaffProcessor().getStaffList()) {
            staff.getAnimation()
               .a(
                  mc.getNetworkHandler() != null
                        && mc.getNetworkHandler().getPlayerList().stream().anyMatch(e -> e.getProfile().getName().equalsIgnoreCase(staff.getName()))
                     || this.isNear(staff.getName())
               );
            if (staff.getAnimation().c() > 0.0F) {
               visible = true;
            }
         }
      }

      this.d().a(visible);
      super.a(event);
   }

   private boolean isNear(String name) {
      return mc.world != null && mc.world.getPlayers().stream().anyMatch(playerEntity -> playerEntity.getName().getString().equalsIgnoreCase(name));
   }
}
