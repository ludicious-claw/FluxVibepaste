package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class NotificationManager {
   private static final List<NotificationManager.Notification> notifications = new ArrayList<>();

   public static void notifyToggle(String moduleName, boolean enabled) {
      String title = enabled ? "Модуль включен" : "Модуль выключен";
      String message = moduleName + (enabled ? " активирован" : " деактивирован");
      int color = enabled ? ImColor.rgba(16, 185, 129, 255) : ImGuiTheme.ACCENT_COLOR;
      notifications.add(new NotificationManager.Notification(title, message, color, enabled ? "✓" : "✕", 2400L));
   }

   public static void notifyInfo(String title, String message) {
      notifications.add(new NotificationManager.Notification(title, message, ImGuiTheme.ACCENT_COLOR_HOVER, "ℹ", 2800L));
   }

   public static void render() {
      if (!notifications.isEmpty()) {
         float screenW = ImGui.getIO().getDisplaySizeX();
         float screenH = ImGui.getIO().getDisplaySizeY();
         long now = System.currentTimeMillis();
         float startY = screenH - 26.0F;
         Iterator<NotificationManager.Notification> it = notifications.iterator();

         while (it.hasNext()) {
            NotificationManager.Notification n = it.next();
            long elapsed = now - n.startTime;
            if (elapsed > n.duration) {
               it.remove();
            } else {
               n.anim.update();
               float progress = n.anim.getValue();
               float fadeProgress = 1.0F;
               if (elapsed > n.duration - 240L) {
                  fadeProgress = (float)(n.duration - elapsed) / 240.0F;
               }

               ImGui.pushFont(ImGuiEngine.getBoldFont());
               float titleW = ImGui.calcTextSize(n.title).x;
               ImGui.popFont();
               float msgW = ImGui.calcTextSize(n.message).x;
               float cardW = Math.max(240.0F, Math.max(titleW, msgW) + 64.0F);
               float cardH = 48.0F;
               startY -= cardH + 8.0F;
               float targetX = screenW - cardW - 16.0F;
               float offscreenX = screenW + 10.0F;
               float currentX = MathUtil.lerp(offscreenX, targetX, MathUtil.clamp(progress, 0.0F, 1.0F));
               ImGui.setNextWindowPos(currentX - 2.0F, startY - 2.0F, 1);
               ImGui.setNextWindowSize(cardW + 12.0F, cardH + 12.0F, 1);
               ImGui.pushStyleVar(2, 0.0F, 0.0F);
               ImGui.pushStyleVar(4, 0.0F);
               ImGui.pushStyleVar(0, MathUtil.clamp(fadeProgress, 0.0F, 1.0F));
               int flags = 787371;
               if (ImGui.begin("##Notif_" + n.id, flags)) {
                  ImDrawList draw = ImGui.getWindowDrawList();
                  float x = ImGui.getWindowPosX() + 2.0F;
                  float y = ImGui.getWindowPosY() + 2.0F;
                  draw.addRectFilled(x, y, x + cardW, y + cardH, ImColor.rgba(11, 11, 16, 246), 10.0F);
                  draw.addRect(x, y, x + cardW, y + cardH, ColorUtil.withAlpha(n.accentColor, 0.45F), 10.0F, 0, 1.0F);
                  float badgeRadius = 12.0F;
                  float badgeCx = x + 20.0F;
                  float badgeCy = y + cardH / 2.0F - 1.0F;
                  draw.addCircleFilled(badgeCx, badgeCy, badgeRadius, ColorUtil.withAlpha(n.accentColor, 0.2F));
                  draw.addCircle(badgeCx, badgeCy, badgeRadius, ColorUtil.withAlpha(n.accentColor, 0.5F), 0, 1.0F);
                  ImVec2 isz = ImGui.calcTextSize(n.icon);
                  draw.addText(badgeCx - isz.x / 2.0F, badgeCy - isz.y / 2.0F, n.accentColor, n.icon);
                  ImGui.pushFont(ImGuiEngine.getBoldFont());
                  draw.addText(x + 40.0F, y + 7.0F, ImColor.rgba(250, 250, 255, 255), n.title);
                  ImGui.popFont();
                  draw.addText(x + 40.0F, y + 25.0F, ImColor.rgba(165, 170, 185, 255), n.message);
                  float timePercent = Math.max(0.0F, 1.0F - (float)elapsed / (float)n.duration);
                  float barY = y + cardH - 3.5F;
                  float barPad = 8.0F;
                  float totalBarW = cardW - barPad * 2.0F;
                  draw.addRectFilled(x + barPad, barY, x + barPad + totalBarW, barY + 2.0F, ImColor.rgba(255, 255, 255, 16), 1.0F);
                  if (timePercent > 0.005F) {
                     draw.addRectFilled(x + barPad, barY, x + barPad + totalBarW * timePercent, barY + 2.0F, n.accentColor, 1.0F);
                  }
               }

               ImGui.end();
               ImGui.popStyleVar(3);
            }
         }
      }
   }

   private static class Notification {
      private static int counter = 0;
      final int id = counter++;
      final String title;
      final String message;
      final int accentColor;
      final String icon;
      final long duration;
      final long startTime = System.currentTimeMillis();
      final Animation anim = new Animation(220.0F, Easing.EASE_OUT_BACK);

      Notification(String title, String message, int accentColor, String icon, long duration) {
         this.title = title;
         this.message = message;
         this.accentColor = accentColor;
         this.icon = icon;
         this.duration = duration;
         this.anim.setTarget(1.0F);
      }
   }
}
