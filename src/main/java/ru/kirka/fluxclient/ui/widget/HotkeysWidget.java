package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.gui.screen.ChatScreen;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.KeyUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class HotkeysWidget extends Widget implements Interface {
   public HotkeysWidget() {
      super(new DragInfo("Клавиши", 10.0F, 250.0F, 100.0F, 20.0F));
      this.j().setWidget(this);
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      float x = this.j().getClampedX();
      float y = this.j().getClampedY();
      float targetWidth = 14.5F + Fonts.e.a("Hot-keys", 7.0F) + 5.0F + 2.0F;
      float contentY = y + 12.5F + 3.0F;
      float rightWidth = Fonts.a.a("Q", 6.5F);
      boolean active = false;

      for (Module module : FluxClient.getInstance().getModuleManager().getModules()) {
         if (module.getKeyBind() != -1 && module.getEnableAnimation().c() > 0.0F) {
            active = true;
            targetWidth = Math.max(
               targetWidth,
               19.0F + Fonts.e.a(module.getName(), 6.5F) + 8.0F + Fonts.e.a(KeyUtil.b(module.getKeyBind()), 6.5F) + 4.0F + rightWidth + 5.0F + 2.0F
            );
         }
      }

      float width = MathUtil.c(this.j().getWidth(), targetWidth, 0.5F);
      this.j().setWidth(width);
      this.a(event, "Q", "Hot-keys", width, this.a());

      for (Module module2 : FluxClient.getInstance().getModuleManager().getModules()) {
         module2.getEnableAnimation().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
         float animation = module2.getKeyBind() != -1 ? module2.getEnableAnimation().c() * this.a() : 0.0F;
         if (animation > 0.0F) {
            float offsetX = -8.0F * (1.0F - animation);
            float offsetY = -(1.0F - animation);
            float drawY = contentY + offsetY;
            float bindWidth = Fonts.e.a(KeyUtil.b(module2.getKeyBind()), 6.5F);
            float rightIconX = x + offsetX + width - 5.0F - rightWidth - 1.0F;
            float textY = drawY + (11.5F - Fonts.e.a(6.5F)) / 2.0F - 0.5F;
            this.a(event, x + offsetX, drawY, width, 11.5F, false, animation);
            this.a(event, x + offsetX + 15.0F, drawY, 11.5F, animation);
            Fonts.a
               .a(
                  event.getMatrixStack(),
                  module2.getCategory().getIcon(),
                  x + offsetX + 5.0F,
                  drawY + (11.5F - Fonts.a.a(6.5F)) / 2.0F - 0.15F,
                  6.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor(), animation)
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  module2.getName(),
                  x + offsetX + 19.0F,
                  textY,
                  6.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), animation)
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  KeyUtil.b(module2.getKeyBind()),
                  rightIconX - 4.0F - bindWidth,
                  textY,
                  6.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), 0.55F * animation)
               );
            Fonts.a
               .a(
                  event.getMatrixStack(),
                  "C",
                  rightIconX,
                  drawY + (11.5F - Fonts.a.a(6.5F)) / 2.0F + 0.15F,
                  6.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor(), animation)
               );
            contentY += 13.5F * animation;
         }
      }

      this.j().setHeight(active ? contentY - y - 2.0F : 12.5F);
      super.a(event);
   }

   @Override
   public void a(GlobalEvent event) {
      boolean visible = mc.currentScreen instanceof ChatScreen;

      for (Module module : FluxClient.getInstance().getModuleManager().getModules()) {
         if (module.getKeyBind() != -1 && module.getEnableAnimation().c() > 0.0F) {
            visible = true;
            break;
         }
      }

      this.d().a(visible);
      super.a(event);
   }
}
