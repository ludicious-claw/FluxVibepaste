package ru.kirka.fluxclient.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.EventTarget;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.render.EasingList;

public class NotificationProcessor implements Interface {
   private final Notification previewNotification = new Notification("o", "FluxClient: Пример уведомления", 0);
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public NotificationProcessor() {
      EventManager.a(this);
   }

   public Notification getPreviewNotification() {
      return this.previewNotification;
   }

   public Notification a() {
      return this.getPreviewNotification();
   }

   public List<Notification> getNotifications() {
      return this.notifications;
   }

   public List<Notification> b() {
      return this.getNotifications();
   }

   public void add(Notification notification) {
      this.notifications.add(notification);
   }

   public void a(Notification notification) {
      this.add(notification);
   }

   @EventTarget
   public void onDraw(DrawEvent event) {
      if (event.b() && !this.notifications.isEmpty()) {
         for (Notification notification : this.notifications) {
            notification.a().a(0.0F, 1.0F, 0.3F, EasingList.g, event.g());
         }
      }
   }

   @EventTarget
   public void onGlobal(GlobalEvent event) {
      this.onTick();
   }

   public void post(String icon, String message, int durationMs) {
      this.add(new Notification(icon, message, durationMs));
   }

   public void post(String message) {
      this.post("i", message, 3000);
   }

   public void onTick() {
      List<Notification> list = new ArrayList<>(this.notifications);
      list.remove(this.previewNotification);
      boolean preview = mc.currentScreen instanceof ChatScreen && list.isEmpty();
      if (preview && !this.notifications.contains(this.previewNotification)) {
         this.notifications.add(this.previewNotification);
      }

      for (Notification notification : new ArrayList<>(this.notifications)) {
         if (notification == this.previewNotification) {
            notification.a().a(preview);
            if (!preview && notification.a().c() == 0.0F) {
               this.notifications.remove(this.previewNotification);
            }
         } else {
            boolean finished = notification.b().a(notification.f() - 100);
            notification.a().a(!finished);
            if (finished && notification.a().c() == 0.0F) {
               this.notifications.remove(notification);
            }
         }
      }
   }
}
