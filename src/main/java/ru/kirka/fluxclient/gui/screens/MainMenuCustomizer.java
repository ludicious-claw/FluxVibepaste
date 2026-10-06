package ru.kirka.fluxclient.gui.screens;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImString;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.util.AccountManager;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class MainMenuCustomizer {
   private static TitleScreen lastParent;
   private static long enterStamp;
   private static long lastFrameMs;
   private static MainMenuCustomizer.MenuView view = MainMenuCustomizer.MenuView.MAIN;
   private static long viewStamp;
   private static boolean popupOpen;
   private static boolean popupClosing;
   private static long popupStamp;
   private static long popupCloseStamp;
   private static final ImString nickBuf = new ImString(16);
   private static String popupError;
   private static boolean popupFocus;
   private static float accScroll;
   private static boolean inputBlocked;
   private static int bgTexId;
   private static int bgW;
   private static int bgH;
   private static long bgLoadedStamp = -1L;
   private static String bgLoadedName;
   private static boolean bgEmbedded;
   private static boolean embeddedTried;
   private static long bgNextCheck;
   private static final List<float[]> particles = new ArrayList<>();
   private static final Random RNG = new Random();

   private MainMenuCustomizer() {
   }

   public static void render(TitleScreen parent) {
      MinecraftClient mc = MinecraftClient.getInstance();
      long now = System.currentTimeMillis();
      if (parent != lastParent) {
         lastParent = parent;
         enterStamp = now;
         viewStamp = now;
         view = MainMenuCustomizer.MenuView.MAIN;
         popupOpen = false;
         popupClosing = false;
         accScroll = 0.0F;
      }

      float dt = lastFrameMs == 0L ? 0.016F : Math.min((float)(now - lastFrameMs) / 1000.0F, 0.05F);
      lastFrameMs = now;
      float t = (float)now / 1000.0F;
      float sw = ImGui.getIO().getDisplaySizeX();
      float sh = ImGui.getIO().getDisplaySizeY();
      if (!(sw <= 0.0F) && !(sh <= 0.0F)) {
         ImDrawList bg = ImGui.getBackgroundDrawList();
         ImDrawList fg = ImGui.getForegroundDrawList();
         updateBackgroundTexture(mc, now);
         if (bgTexId != 0) {
            drawPhotoBackground(bg, sw, sh, t);
         } else {
            drawAuroraBackground(bg, sw, sh, t);
         }

         bg.addRectFilled(0.0F, 0.0F, sw, sh, ImColor.rgba(0, 0, 0, 70));
         bg.addRectFilledMultiColor(
            0.0F, 0.0F, sw, sh * 0.3F, ImColor.rgba(0, 0, 0, 90), ImColor.rgba(0, 0, 0, 90), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 0)
         );
         bg.addRectFilledMultiColor(
            0.0F, sh * 0.55F, sw, sh, ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 150), ImColor.rgba(0, 0, 0, 150)
         );
         drawParticles(bg, sw, sh, t, dt);
         drawVignette(bg, sw, sh);
         inputBlocked = popupOpen;
         if (ImGui.isKeyPressed(256)) {
            if (popupOpen && !popupClosing) {
               closePopup();
            } else if (view == MainMenuCustomizer.MenuView.ACCOUNTS) {
               view = MainMenuCustomizer.MenuView.MAIN;
               viewStamp = now;
            }
         }

         float cx = sw / 2.0F;
         if (view == MainMenuCustomizer.MenuView.MAIN) {
            drawLogo(fg, cx, sh * 0.2F);
            float bw = Math.min(340.0F, sw - 90.0F);
            float bh = 54.0F;
            float gap = 12.0F;
            float totalH = bh * 4.0F + gap * 3.0F;
            float y0 = sh * 0.6F - totalH / 2.0F;
            float bx = cx - bw / 2.0F;
            menuButton(
               fg,
               "single",
               bx,
               y0,
               bw,
               bh,
               0,
               "Одиночная игра",
               "Выживание, творческий, хардкор",
               enterStamp,
               1,
               () -> mc.setScreen(new SelectWorldScreen(parent))
            );
            menuButton(
               fg, "multi", bx, y0 + bh + gap, bw, bh, 1, "Сетевая игра", "Серверы и FunTime", enterStamp, 2, () -> mc.setScreen(new MultiplayerScreen(parent))
            );
            menuButton(
               fg,
               "accounts",
               bx,
               y0 + (bh + gap) * 2.0F,
               bw,
               bh,
               3,
               "Аккаунты",
               "Быстрая смена ника (оффлайн)",
               enterStamp,
               3,
               MainMenuCustomizer::openAccounts
            );
            menuButton(
               fg,
               "options",
               bx,
               y0 + (bh + gap) * 3.0F,
               bw,
               bh,
               2,
               "Настройки",
               "Графика, звук, управление",
               enterStamp,
               4,
               () -> mc.setScreen(new OptionsScreen(parent, mc.options))
            );
         } else {
            drawAccountsView(fg, sw, sh, mc, now);
         }

         drawCreatePopup(fg, sw, sh, now);
         drawFooter(fg, sw, sh, mc, popupOpen);
      }
   }

   private static void openAccounts() {
      view = MainMenuCustomizer.MenuView.ACCOUNTS;
      viewStamp = System.currentTimeMillis();
      accScroll = 0.0F;
   }

   private static void drawLogo(ImDrawList fg, float cx, float topY) {
      float t0 = RenderWidgets.entrance(enterStamp, 0, 90, 480);
      float rise = (1.0F - t0) * 26.0F;
      float a = 0.1F + 0.9F * t0;
      ImFont title = ImGuiEngine.getTitleFont();
      boolean big = title != null;
      if (big) {
         ImGui.pushFont(title);
      }

      ImVec2 s1 = ImGui.calcTextSize("FLUX");
      ImVec2 s2 = ImGui.calcTextSize("CLIENT");
      float totalW = s1.x + s2.x;
      float lx = cx - totalW / 2.0F;
      float ly = topY + rise;
      fg.addRectFilled(lx - 14.0F, ly - 8.0F, lx + totalW + 14.0F, ly + s1.y + 10.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_DIM, 0.35F * a), 14.0F);
      fg.addText(lx, ly, RenderWidgets.fade(ImColor.rgba(245, 245, 250, 255), a), "FLUX");
      fg.addText(lx + s1.x, ly, RenderWidgets.fade(ImGuiTheme.ACCENT_COLOR_HOVER, a), "CLIENT");
      if (big) {
         ImGui.popFont();
      }
   }

   private static void menuButton(
      ImDrawList fg, String id, float x, float y, float w, float h, int iconKind, String label, String sub, long baseStamp, int order, Runnable onClick
   ) {
      float t = RenderWidgets.entrance(baseStamp, order, 90, 450);
      float yy = y + (1.0F - t) * 24.0F;
      float a = 0.1F + 0.9F * t;
      ImVec2 mp = ImGui.getMousePos();
      boolean hovered = !inputBlocked && mp.x >= x && mp.x <= x + w && mp.y >= yy && mp.y <= yy + h;
      boolean pressed = hovered && ImGui.isMouseDown(0);
      RenderWidgets.drawDarkPanel(fg, "mmenu_" + id, x, yy, w, h, hovered, pressed, a);
      float hoverT = RenderWidgets.animValue("mmenu_" + id + "_hov", hovered ? 1.0F : 0.0F, 170.0F, Easing.EASE_OUT_QUAD);
      float pressT = RenderWidgets.animValue("mmenu_" + id + "_prs", pressed ? 1.0F : 0.0F, 110.0F, Easing.EASE_OUT_QUAD);
      float s = 1.0F + 0.035F * hoverT - 0.028F * pressT;
      float ox = x + w / 2.0F - w * s / 2.0F;
      float oy = yy + h / 2.0F - h * s / 2.0F;
      float iconCx = ox + 34.0F;
      float iconCy = oy + h * s / 2.0F;
      int iconCol = RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(200, 200, 215, 255), ImColor.rgba(255, 255, 255, 255), hoverT), a);
      drawButtonIcon(fg, iconKind, iconCx, iconCy, iconCol);
      int labelCol = RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(225, 225, 235, 255), ImColor.rgba(255, 255, 255, 255), hoverT), a);
      int subCol = RenderWidgets.fade(ImColor.rgba(140, 140, 160, 255), a);
      fg.addText(ox + 58.0F, oy + 8.0F, labelCol, label);
      fg.addText(ox + 58.0F, oy + 29.0F, subCol, sub);
      ImVec2 chz = ImGui.calcTextSize(">");
      fg.addText(
         ox + w * s - 26.0F - chz.x + (1.0F - hoverT) * 5.0F,
         iconCy - chz.y / 2.0F,
         RenderWidgets.fade(ImColor.rgba(150, 150, 170, 255), a * (0.35F + 0.65F * hoverT)),
         ">"
      );
      if (hovered && ImGui.isMouseClicked(0)) {
         ImGuiEngine.post(onClick);
      }
   }

   private static void drawButtonIcon(ImDrawList fg, int kind, float cx, float cy, int col) {
      switch (kind) {
         case 0:
            fg.addRect(cx - 5.0F, cy - 8.0F, cx + 5.0F, cy + 0.5F, col, 3.0F, 0, 1.8F);
            fg.addLine(cx - 8.0F, cy + 8.0F, cx - 5.5F, cy + 3.0F, col, 1.8F);
            fg.addLine(cx - 5.5F, cy + 3.0F, cx + 5.5F, cy + 3.0F, col, 1.8F);
            fg.addLine(cx + 5.5F, cy + 3.0F, cx + 8.0F, cy + 8.0F, col, 1.8F);
            break;
         case 1:
            fg.addCircle(cx, cy, 7.5F, col, 0, 1.8F);
            fg.addLine(cx - 7.5F, cy, cx + 7.5F, cy, col, 1.4F);
            fg.addLine(cx, cy - 7.5F, cx, cy + 7.5F, col, 1.4F);
            fg.addCircle(cx, cy, 3.4F, col, 0, 1.2F);
            break;
         case 2:
         default:
            fg.addCircle(cx, cy, 4.2F, col, 0, 1.8F);
            fg.addCircleFilled(cx, cy, 1.4F, col);

            for (int i = 0; i < 8; i++) {
               double ang = (Math.PI / 4) * i;
               float c = (float)Math.cos(ang);
               float sn = (float)Math.sin(ang);
               fg.addLine(cx + c * 5.6F, cy + sn * 5.6F, cx + c * 8.4F, cy + sn * 8.4F, col, 1.8F);
            }
            break;
         case 3:
            fg.addRect(cx - 8.0F, cy - 6.0F, cx + 8.0F, cy + 6.0F, col, 3.0F, 0, 1.6F);
            fg.addCircleFilled(cx - 3.5F, cy - 1.5F, 2.0F, col);
            fg.addLine(cx - 0.5F, cy - 2.0F, cx + 5.5F, cy - 2.0F, col, 1.4F);
            fg.addLine(cx - 0.5F, cy + 1.0F, cx + 5.5F, cy + 1.0F, col, 1.4F);
            fg.addLine(cx - 5.5F, cy + 4.5F, cx + 5.5F, cy + 4.5F, col, 1.4F);
            break;
         case 4:
            fg.addLine(cx - 6.0F, cy, cx + 6.0F, cy, col, 2.0F);
            fg.addLine(cx, cy - 6.0F, cx, cy + 6.0F, col, 2.0F);
            break;
         case 5:
            fg.addLine(cx - 6.0F, cy + 0.5F, cx - 1.5F, cy + 5.0F, col, 2.2F);
            fg.addLine(cx - 1.5F, cy + 5.0F, cx + 6.5F, cy - 5.0F, col, 2.2F);
            break;
         case 6:
            fg.addLine(cx - 4.5F, cy - 4.5F, cx + 4.5F, cy + 4.5F, col, 2.0F);
            fg.addLine(cx - 4.5F, cy + 4.5F, cx + 4.5F, cy - 4.5F, col, 2.0F);
      }
   }

   private static void drawAccountsView(ImDrawList fg, float sw, float sh, MinecraftClient mc, long now) {
      float t = RenderWidgets.entrance(viewStamp, 0, 70, 380);
      float a = (0.1F + 0.9F * t) * (popupOpen ? 0.22F : 1.0F);
      fg.addRectFilled(0.0F, 0.0F, sw, sh, RenderWidgets.fade(ImColor.rgba(0, 0, 0, 150), a));
      float pw = Math.min(470.0F, sw - 60.0F);
      float ph = Math.min(500.0F, sh - 90.0F);
      float px = (sw - pw) / 2.0F;
      float py = (sh - ph) / 2.0F + (1.0F - t) * 20.0F;
      RenderWidgets.drawDarkPanel(fg, "acc_panel", px, py, pw, ph, false, false, a);
      float innerX = px + 22.0F;
      float innerW = pw - 44.0F;
      ImVec2 mp = ImGui.getMousePos();
      String back = "< Назад";
      ImVec2 bsz = ImGui.calcTextSize(back);
      boolean bh = !popupOpen && mp.x >= innerX && mp.x <= innerX + bsz.x + 8.0F && mp.y >= py + 14.0F && mp.y <= py + 34.0F;
      float bht = RenderWidgets.hoverAnim("acc_back", bh);
      fg.addText(
         innerX,
         py + 16.0F,
         RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(140, 140, 160, 255), ImColor.rgba(255, 255, 255, 255), bht), a),
         back
      );
      if (bh && ImGui.isMouseClicked(0)) {
         view = MainMenuCustomizer.MenuView.MAIN;
         viewStamp = now;
      }

      String title = "Аккаунты";
      ImVec2 tsz = ImGui.calcTextSize(title);
      fg.addText(px + (pw - tsz.x) / 2.0F, py + 14.0F, RenderWidgets.fade(ImColor.rgba(245, 245, 250, 255), a), title);
      List<AccountManager.Entry> accounts = AccountManager.getAccounts();
      String sub = "Оффлайн-ники | клик - выбрать | " + accounts.size();
      ImVec2 ssz = ImGui.calcTextSize(sub);
      fg.addText(px + (pw - ssz.x) / 2.0F, py + 36.0F, RenderWidgets.fade(ImColor.rgba(140, 140, 160, 255), a), sub);
      fg.addLine(innerX, py + 62.0F, innerX + innerW, py + 62.0F, RenderWidgets.fade(ImColor.rgba(255, 255, 255, 255), 0.1F * a), 1.0F);
      float listY = py + 72.0F;
      float createH = 52.0F;
      float createY = py + ph - 22.0F - createH;
      float listH = createY - 12.0F - listY;
      float rowH = 46.0F;
      float step = rowH + 8.0F;
      if (!popupOpen && mp.x >= px && mp.x <= px + pw && mp.y >= listY && mp.y <= listY + listH) {
         float wheel = ImGui.getIO().getMouseWheel();
         if (wheel != 0.0F) {
            float max = Math.max(0.0F, accounts.size() * step - listH);
            accScroll = MathUtil.clamp(accScroll - wheel * step, 0.0F, max);
         }
      }

      String current = AccountManager.currentName();
      if (accounts.isEmpty()) {
         String empty = "Пока пусто — создай первый ник ниже";
         ImVec2 esz = ImGui.calcTextSize(empty);
         fg.addText(px + (pw - esz.x) / 2.0F, listY + 24.0F, RenderWidgets.fade(ImColor.rgba(120, 120, 140, 255), a), empty);
      }

      int idx = 0;

      for (AccountManager.Entry e : accounts) {
         float ry = listY - accScroll + idx * step;
         idx++;
         if (!(ry + rowH < listY) && !(ry > listY + listH)) {
            float rowT = RenderWidgets.entrance(viewStamp, 1 + idx, 50, 300);
            float ra = a * (0.2F + 0.8F * rowT);
            boolean isCurrent = e.name.equalsIgnoreCase(current);
            boolean h = !popupOpen && mp.x >= innerX && mp.x <= innerX + innerW && mp.y >= ry && mp.y <= ry + rowH;
            float ht = RenderWidgets.hoverAnim("acc_row_" + e.name.toLowerCase(Locale.ROOT), h);
            int rowBg = RenderWidgets.interpolateColor(
               RenderWidgets.interpolateColor(ImColor.rgba(18, 20, 28, 255), ImColor.rgba(30, 32, 46, 255), ht),
               ImGuiTheme.ACCENT_COLOR_DIM,
               isCurrent ? 0.55F : 0.0F
            );
            fg.addRectFilled(innerX, ry, innerX + innerW, ry + rowH, RenderWidgets.fade(rowBg, ra), 10.0F);
            fg.addRect(
               innerX,
               ry,
               innerX + innerW,
               ry + rowH,
               RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(40, 40, 55, 200), ImGuiTheme.ACCENT_COLOR, isCurrent ? 0.9F : ht * 0.5F), ra),
               10.0F,
               0,
               1.0F
            );
            fg.addCircleFilled(
               innerX + 18.0F, ry + rowH / 2.0F, 3.5F, RenderWidgets.fade(isCurrent ? ImGuiTheme.ACCENT_COLOR : ImColor.rgba(80, 80, 95, 255), ra)
            );
            fg.addText(innerX + 32.0F, ry + 6.0F, RenderWidgets.fade(ImColor.rgba(240, 240, 250, 255), ra), truncateToWidth(e.name, innerW - 150.0F));
            fg.addText(
               innerX + 32.0F,
               ry + 25.0F,
               RenderWidgets.fade(isCurrent ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(120, 120, 140, 255), ra),
               isCurrent ? "выбран" : "оффлайн"
            );
            float xx = innerX + innerW - 30.0F;
            boolean xh = !popupOpen && mp.x >= xx - 4.0F && mp.x <= xx + 16.0F && mp.y >= ry + 8.0F && mp.y <= ry + rowH - 8.0F;
            float xht = RenderWidgets.hoverAnim("acc_del_" + e.name.toLowerCase(Locale.ROOT), xh);
            fg.addText(
               xx,
               ry + 13.0F,
               RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(110, 110, 130, 255), ImColor.rgba(255, 110, 110, 255), xht), ra),
               "x"
            );
            if (xh && ImGui.isMouseClicked(0)) {
               AccountManager.removeAccount(e.name);
            } else if (h && !xh && ImGui.isMouseClicked(0) && !isCurrent) {
               AccountManager.switchTo(e.name);
            }
         }
      }

      menuButton(fg, "acc_create", innerX, createY, innerW, createH, 4, "Создать аккаунт", "Новый оффлайн-ник", viewStamp, 6, MainMenuCustomizer::openPopup);
   }

   private static String truncateToWidth(String s, float maxW) {
      if (s == null) {
         return "";
      } else if (ImGui.calcTextSize(s).x <= maxW) {
         return s;
      } else {
         int len = s.length();

         while (len > 0 && ImGui.calcTextSize(s.substring(0, len) + "...").x > maxW) {
            len--;
         }

         return len <= 0 ? "" : s.substring(0, len) + "...";
      }
   }

   private static void openPopup() {
      popupOpen = true;
      popupClosing = false;
      popupStamp = System.currentTimeMillis();
      nickBuf.set("");
      popupError = null;
      popupFocus = true;
   }

   private static void closePopup() {
      if (popupOpen && !popupClosing) {
         popupClosing = true;
         popupCloseStamp = System.currentTimeMillis();
      }
   }

   private static void submitPopup() {
      String nick = nickBuf.get().trim();
      String err = AccountManager.addAccount(nick);
      if (err != null) {
         popupError = err;
      } else {
         AccountManager.switchTo(nick);
         closePopup();
      }
   }

   private static void drawCreatePopup(ImDrawList fg, float sw, float sh, long now) {
      if (popupOpen) {
         float pt;
         if (popupClosing) {
            pt = 1.0F - Math.min((float)(now - popupCloseStamp) / 180.0F, 1.0F);
            if (pt <= 0.0F) {
               popupOpen = false;
               popupClosing = false;
               return;
            }
         } else {
            pt = RenderWidgets.entrance(popupStamp, 0, 0, 220);
         }

         float a = MathUtil.clamp(pt, 0.0F, 1.0F);
         ImDrawList bg = ImGui.getBackgroundDrawList();
         bg.addRectFilled(0.0F, 0.0F, sw, sh, RenderWidgets.fade(ImColor.rgba(0, 0, 0, 140), a));
         float pw = Math.min(380.0F, sw - 60.0F);
         float ph = 252.0F;
         float px = (sw - pw) / 2.0F;
         float py = (sh - ph) / 2.0F + (1.0F - a) * 18.0F;
         RenderWidgets.drawDarkPanel(bg, "acc_popup", px, py, pw, ph, false, false, a);
         String title = "Новый аккаунт";
         ImVec2 tsz = ImGui.calcTextSize(title);
         fg.addText(px + (pw - tsz.x) / 2.0F, py + 16.0F, RenderWidgets.fade(ImColor.rgba(245, 245, 250, 255), a), title);
         ImVec2 mp = ImGui.getMousePos();
         float cbx = px + pw - 34.0F;
         boolean ch = mp.x >= cbx && mp.x <= cbx + 22.0F && mp.y >= py + 12.0F && mp.y <= py + 34.0F;
         float cht = RenderWidgets.hoverAnim("acc_popup_x", ch);
         fg.addText(
            cbx + 6.0F,
            py + 14.0F,
            RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(140, 140, 160, 255), ImColor.rgba(255, 120, 120, 255), cht), a),
            "x"
         );
         if (ch && ImGui.isMouseClicked(0)) {
            closePopup();
         }

         if (popupClosing || !ImGui.isMouseClicked(0) || now - popupStamp <= 250L || !(mp.x < px) && !(mp.x > px + pw) && !(mp.y < py) && !(mp.y > py + ph)) {
            ImGui.setNextWindowPos(px, py, 1);
            ImGui.setNextWindowSize(pw, ph, 1);
            ImGui.pushStyleColor(2, ImColor.rgba(0, 0, 0, 0));
            ImGui.pushStyleVar(4, 0.0F);
            ImGui.pushStyleVar(2, 0.0F, 0.0F);
            int winFlags = 303;
            if (ImGui.begin("##AccPopup", winFlags)) {
               ImGui.setCursorPos(24.0F, 52.0F);
               ImGui.textColored(ImColor.rgba(150, 150, 170, 255), "Ник:");
               ImGui.setCursorPos(24.0F, 74.0F);
               ImGui.pushItemWidth(pw - 48.0F);
               ImGui.pushStyleColor(7, ImColor.rgba(10, 12, 20, 255));
               ImGui.pushStyleColor(5, ImGuiTheme.ACCENT_COLOR_DIM);
               ImGui.pushStyleColor(0, ImColor.rgba(255, 255, 255, 255));
               ImGui.pushStyleVar(13, 1.0F);
               ImGui.pushStyleVar(12, 8.0F);
               ImGui.pushStyleVar(11, 10.0F, 8.0F);
               if (popupFocus) {
                  ImGui.setKeyboardFocusHere();
                  popupFocus = false;
               }

               ImGui.inputTextWithHint("##acc_nick", "3-16: латиница, цифры, _", nickBuf);
               ImGui.popStyleVar(3);
               ImGui.popStyleColor(3);
               ImGui.popItemWidth();
               if (popupError != null) {
                  ImGui.setCursorPos(24.0F, 116.0F);
                  ImGui.textColored(ImColor.rgba(255, 110, 110, 255), popupError);
               }
            }

            ImGui.end();
            ImGui.popStyleVar(2);
            ImGui.popStyleColor(1);
            if (!popupClosing && (ImGui.isKeyPressed(257) || ImGui.isKeyPressed(335))) {
               submitPopup();
            }

            float btnY = py + ph - 24.0F - 44.0F;
            float btnW = (pw - 48.0F - 10.0F) / 2.0F;
            popupButton(fg, "acc_ok", px + 24.0F, btnY, btnW, 44.0F, "Готово", true, a, MainMenuCustomizer::submitPopup);
            popupButton(fg, "acc_cancel", px + 24.0F + btnW + 10.0F, btnY, btnW, 44.0F, "Отмена", false, a, MainMenuCustomizer::closePopup);
         } else {
            closePopup();
         }
      }
   }

   private static void popupButton(ImDrawList fg, String id, float x, float y, float w, float h, String label, boolean primary, float alpha, Runnable onClick) {
      ImVec2 mp = ImGui.getMousePos();
      boolean hovered = mp.x >= x && mp.x <= x + w && mp.y >= y && mp.y <= y + h;
      boolean pressed = hovered && ImGui.isMouseDown(0);
      float ht = RenderWidgets.hoverAnim("mmenu_" + id, hovered);
      float prt = RenderWidgets.animValue("mmenu_" + id + "_p", pressed ? 1.0F : 0.0F, 110.0F, Easing.EASE_OUT_QUAD);
      float s = 1.0F + 0.03F * ht - 0.025F * prt;
      float sx = x + w / 2.0F - w * s / 2.0F;
      float sy = y + h / 2.0F - h * s / 2.0F;
      float swd = w * s;
      float shd = h * s;
      int base = primary ? ImGuiTheme.ACCENT_COLOR_DIM : ImColor.rgba(13, 13, 17, 215);
      int bg = RenderWidgets.interpolateColor(base, primary ? ImGuiTheme.ACCENT_COLOR : ImColor.rgba(26, 26, 34, 255), ht * ht);
      fg.addRectFilled(sx, sy, sx + swd, sy + shd, RenderWidgets.fade(bg, alpha), 10.0F);
      fg.addRect(
         sx,
         sy,
         sx + swd,
         sy + shd,
         RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(38, 38, 48, 255), ImGuiTheme.ACCENT_COLOR_HOVER, ht), alpha),
         10.0F,
         0,
         1.0F + ht * 0.5F
      );
      ImVec2 tsz = ImGui.calcTextSize(label);
      fg.addText(
         sx + (swd - tsz.x) / 2.0F,
         sy + (shd - tsz.y) / 2.0F,
         RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(210, 210, 222, 255), ImColor.rgba(255, 255, 255, 255), ht), alpha),
         label
      );
      if (hovered && ImGui.isMouseClicked(0)) {
         onClick.run();
      }
   }

   private static void drawFooter(ImDrawList fg, float sw, float sh, MinecraftClient mc, boolean blocked) {
      float t = RenderWidgets.entrance(enterStamp, 4, 90, 450);
      float a = 0.1F + 0.9F * t;
      float fy = sh - 30.0F + (1.0F - t) * 12.0F;
      fg.addText(18.0F, fy, RenderWidgets.fade(ImColor.rgba(130, 130, 150, 255), a), "FluxClient v1.0.0");
      String name = AccountManager.currentName();
      ImVec2 nsz = ImGui.calcTextSize(name);
      fg.addText(sw - 18.0F - nsz.x, fy - 20.0F, RenderWidgets.fade(ImColor.rgba(130, 130, 150, 255), a), name);
      String quit = "Покинуть игру";
      ImVec2 qsz = ImGui.calcTextSize(quit);
      float qx = sw - 18.0F - qsz.x;
      ImVec2 mp = ImGui.getMousePos();
      boolean h = !blocked && mp.x >= qx - 4.0F && mp.x <= sw - 14.0F && mp.y >= fy - 2.0F && mp.y <= fy + 18.0F;
      float ht = RenderWidgets.hoverAnim("mmenu_quit", h);
      fg.addText(qx, fy, RenderWidgets.fade(RenderWidgets.interpolateColor(ImColor.rgba(130, 130, 150, 255), ImColor.rgba(255, 120, 120, 255), ht), a), quit);
      if (h && ImGui.isMouseClicked(0)) {
         ImGuiEngine.post(mc::scheduleStop);
      }
   }

   private static void updateBackgroundTexture(MinecraftClient mc, long now) {
      if (now >= bgNextCheck) {
         bgNextCheck = now + 4000L;

         try {
            File dir = new File(mc.runDirectory, "fluxclient");
            String[] names = new String[]{"menu_bg.png", "menu_bg.jpg", "menu_bg.jpeg"};
            File found = null;

            for (String n : names) {
               File f = new File(dir, n);
               if (f.isFile()) {
                  found = f;
                  break;
               }
            }

            if (found != null) {
               long stamp = found.lastModified();
               if (!found.getAbsolutePath().equals(bgLoadedName) || stamp != bgLoadedStamp || bgTexId == 0) {
                  byte[] bytes = Files.readAllBytes(found.toPath());
                  int tex = uploadTexture(bytes);
                  if (tex != 0) {
                     if (bgTexId != 0) {
                        GL11.glDeleteTextures(bgTexId);
                     }

                     bgTexId = tex;
                     bgLoadedStamp = stamp;
                     bgLoadedName = found.getAbsolutePath();
                     bgEmbedded = false;
                  }
               }

               return;
            }

            if (bgTexId != 0 && !bgEmbedded) {
               GL11.glDeleteTextures(bgTexId);
               bgTexId = 0;
               bgLoadedStamp = -1L;
               bgLoadedName = null;
            }

            if (bgTexId == 0 && !embeddedTried) {
               embeddedTried = true;
               byte[] bytes = readEmbeddedBackground();
               if (bytes != null) {
                  int tex = uploadTexture(bytes);
                  if (tex != 0) {
                     bgTexId = tex;
                     bgLoadedStamp = 0L;
                     bgLoadedName = "jar:assets/fluxclient/textures/menu_bg";
                     bgEmbedded = true;
                  }
               }
            }
         } catch (Exception var11) {
         }
      }
   }

   private static byte[] readEmbeddedBackground() {
      String[] paths = new String[]{"/assets/fluxclient/textures/menu_bg.png", "/assets/fluxclient/textures/menu_bg.jpg"};

      for (String p : paths) {
         try (InputStream in = MainMenuCustomizer.class.getResourceAsStream(p)) {
            if (in != null) {
               byte[] bytes = in.readAllBytes();
               if (bytes.length > 0) {
                  return bytes;
               }
            }
         } catch (Exception var10) {
         }
      }

      return null;
   }

   private static int loadTexture(File file) {
      try {
         byte[] bytes = Files.readAllBytes(file.toPath());
         return bytes.length == 0 ? 0 : uploadTexture(bytes);
      } catch (Exception var2) {
         return 0;
      }
   }

   private static int uploadTexture(byte[] bytes) {
      ByteBuffer fileBuf = null;
      ByteBuffer img = null;

      int var8;
      try {
         if (bytes == null || bytes.length == 0) {
            return 0;
         }

         fileBuf = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
         MemoryStack stack = MemoryStack.stackPush();

         label184: {
            int tex;
            try {
               IntBuffer w = stack.mallocInt(1);
               IntBuffer h = stack.mallocInt(1);
               IntBuffer ch = stack.mallocInt(1);
               img = STBImage.stbi_load_from_memory(fileBuf, w, h, ch, 4);
               if (img != null) {
                  tex = GL11.glGenTextures();
                  GL11.glBindTexture(3553, tex);
                  GL11.glTexParameteri(3553, 10241, 9729);
                  GL11.glTexParameteri(3553, 10240, 9729);
                  GL11.glTexParameteri(3553, 10242, 33071);
                  GL11.glTexParameteri(3553, 10243, 33071);
                  GL11.glPixelStorei(3317, 1);
                  GL11.glTexImage2D(3553, 0, 32856, w.get(0), h.get(0), 0, 6408, 5121, img);
                  GL11.glPixelStorei(3317, 4);
                  GL11.glBindTexture(3553, 0);
                  bgW = w.get(0);
                  bgH = h.get(0);
                  var8 = tex;
                  break label184;
               }

               tex = 0;
            } catch (Throwable var15) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (stack != null) {
               stack.close();
            }

            return tex;
         }

         if (stack != null) {
            stack.close();
         }
      } catch (Exception var16) {
         return 0;
      } finally {
         if (img != null) {
            STBImage.stbi_image_free(img);
         }

         if (fileBuf != null) {
            MemoryUtil.memFree(fileBuf);
         }
      }

      return var8;
   }

   private static void drawPhotoBackground(ImDrawList bg, float sw, float sh, float t) {
      float zoom = 1.07F + 0.025F * (float)Math.sin(t * 0.11F);
      float s = Math.max(sw / bgW, sh / bgH) * zoom;
      float uvW = sw / (bgW * s);
      float uvH = sh / (bgH * s);
      float u0 = (1.0F - uvW) / 2.0F + 0.02F * (float)Math.sin(t * 0.073F);
      float v0 = (1.0F - uvH) / 2.0F + 0.015F * (float)Math.cos(t * 0.061F);
      bg.addImage(bgTexId, 0.0F, 0.0F, sw, sh, u0, v0, u0 + uvW, v0 + uvH);
   }

   private static void drawAuroraBackground(ImDrawList bg, float sw, float sh, float t) {
      bg.addRectFilledMultiColor(
         0.0F, 0.0F, sw, sh, ImColor.rgba(12, 8, 10, 255), ImColor.rgba(12, 8, 10, 255), ImColor.rgba(4, 4, 6, 255), ImColor.rgba(4, 4, 6, 255)
      );
      int accent = ImGuiTheme.ACCENT_COLOR;
      auroraBlob(bg, sw * (0.28F + 0.05F * (float)Math.sin(t * 0.13F)), sh * 0.3F, Math.min(sw, sh) * 0.3F, ColorUtil.withAlpha(accent, 0.16F));
      auroraBlob(
         bg, sw * (0.74F + 0.04F * (float)Math.cos(t * 0.1F)), sh * 0.62F, Math.min(sw, sh) * 0.36F, ColorUtil.withAlpha(ImColor.rgba(120, 24, 24, 255), 0.14F)
      );
      auroraBlob(
         bg, sw * 0.55F, sh * (0.85F + 0.03F * (float)Math.sin(t * 0.08F)), Math.min(sw, sh) * 0.28F, ColorUtil.withAlpha(ImColor.rgba(70, 70, 80, 255), 0.1F)
      );
   }

   private static void auroraBlob(ImDrawList bg, float x, float y, float r, int col) {
      for (int i = 5; i >= 1; i--) {
         float f = i / 5.0F;
         bg.addCircleFilled(x, y, r * f, ColorUtil.withAlpha(col, (col >>> 24) / 255.0F * (1.0F - f) * 0.9F + 0.02F));
      }
   }

   private static void drawParticles(ImDrawList bg, float sw, float sh, float t, float dt) {
      if (particles.isEmpty()) {
         for (int i = 0; i < 60; i++) {
            particles.add(
               new float[]{
                  RNG.nextFloat(),
                  RNG.nextFloat(),
                  0.008F + RNG.nextFloat() * 0.03F,
                  1.0F + RNG.nextFloat() * 2.2F,
                  0.1F + RNG.nextFloat() * 0.3F,
                  RNG.nextFloat() * 6.283F,
                  RNG.nextFloat()
               }
            );
         }
      }

      int accent = ImGuiTheme.ACCENT_COLOR;

      for (float[] p : particles) {
         p[1] -= p[2] * dt;
         if (p[1] < -0.02F) {
            p[1] = 1.02F;
            p[0] = RNG.nextFloat();
         }

         float x = (p[0] + 0.012F * (float)Math.sin(t * 0.5F + p[5])) * sw;
         float y = p[1] * sh;
         float tw = 0.6F + 0.4F * (float)Math.sin(t * 1.7F + p[5] * 3.0F);
         int col = p[6] < 0.22F ? accent : ImColor.rgba(255, 255, 255, 255);
         bg.addCircleFilled(x, y, p[3], ColorUtil.withAlpha(col, p[4] * tw));
      }
   }

   private static void drawVignette(ImDrawList bg, float sw, float sh) {
      float v = Math.min(sw, sh) * 0.22F;
      bg.addRectFilledMultiColor(0.0F, 0.0F, sw, v, ImColor.rgba(0, 0, 0, 110), ImColor.rgba(0, 0, 0, 110), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 0));
      bg.addRectFilledMultiColor(
         0.0F, sh - v, sw, sh, ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 110), ImColor.rgba(0, 0, 0, 110)
      );
      bg.addRectFilledMultiColor(
         0.0F, 0.0F, v * 1.4F, sh, ImColor.rgba(0, 0, 0, 90), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 90)
      );
      bg.addRectFilledMultiColor(
         sw - v * 1.4F, 0.0F, sw, sh, ImColor.rgba(0, 0, 0, 0), ImColor.rgba(0, 0, 0, 90), ImColor.rgba(0, 0, 0, 90), ImColor.rgba(0, 0, 0, 0)
      );
   }

   private static enum MenuView {
      MAIN,
      ACCOUNTS;
   }
}
