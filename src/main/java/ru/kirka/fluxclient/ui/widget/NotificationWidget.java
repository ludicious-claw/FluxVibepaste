package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.notification.Notification;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;

public class NotificationWidget extends Widget implements Interface {
   private final BooleanSetting notifyDrops = new BooleanSetting("Оповещать о поднятии донат-предметов", "Уведомления о редких предметах", true);
   private final BooleanSetting notifyFriends = new BooleanSetting("Обновления и уведомления друзей", "Уведомления о действиях друзей", true);

   public NotificationWidget() {
      super(new DragInfo("Уведомления", 0.0F, 25.0F, 0.0F, 0.0F));
      this.j().setWidget(this);
      this.j().setDragStatus(1);
      this.a(this.notifyFriends, this.notifyDrops);
   }

   @Override
   public void a(GlobalEvent event) {
      boolean hasNotifications = FluxClient.getInstance() != null && !FluxClient.getInstance().getNotificationProcessor().getNotifications().isEmpty();
      this.d().a(mc.currentScreen instanceof ChatScreen || hasNotifications);
      super.a(event);
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      float contentY = this.j().getClampedY();
      if (FluxClient.getInstance() != null) {
         float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());

         for (Notification notification : FluxClient.getInstance().getNotificationProcessor().getNotifications()) {
            float animation = notification.a().c() * this.a();
            if (animation > 0.0F) {
               Object message = notification.c();
               float textWidth;
               if (message instanceof Text value) {
                  textWidth = Fonts.e.a(value, 7.0F);
               } else {
                  textWidth = Fonts.e.a(String.valueOf(message), 7.0F);
               }

               float width = 17.5F + textWidth + 4.0F;
               float x = (screenW - width) / 2.0F;
               int color = notification.e() == -1 ? FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor() : notification.e();
               Object iconObj = notification.d();
               if (iconObj instanceof ItemStack stack) {
                  this.a(event, x, contentY, stack, message, width, animation, color);
               } else {
                  this.a(event, x, contentY, String.valueOf(iconObj != null ? iconObj : "Q"), message, width, animation, color);
               }

               this.j().setX(x);
               this.j().setWidth(width);
               this.j().setHeight(12.5F);
               contentY += (12.5F + 3.5F) * animation;
            }
         }

         super.a(event);
      }
   }
}
