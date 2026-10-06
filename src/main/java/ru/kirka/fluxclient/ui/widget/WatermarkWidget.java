package ru.kirka.fluxclient.ui.widget;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;
import ru.kirka.fluxclient.util.ServerUtil;

public class WatermarkWidget extends Widget implements Interface {
   private final BooleanSetting sideDisplay = new BooleanSetting("Боковое отображение", "Прикрепить панель к левой стороне", true);
   private final BooleanSetting separatePills = new BooleanSetting("Разделять элементы", "Отображать элементы отдельными плашками", false);
   private final BooleanSetting showFps = new BooleanSetting("Частота кадров", "Показывать FPS", true);
   private final BooleanSetting showPing = new BooleanSetting("Задержка игрока", "Показывать пинг в мс", true);
   private final BooleanSetting showTime = new BooleanSetting("Текущее время", "Показывать часы", true);
   private final BooleanSetting showLogin = new BooleanSetting("Логин в клиенте", "Показывать ник игрока", true);
   private final BooleanSetting showCoords = new BooleanSetting("Координаты", "Показывать X Y Z", true);
   private final BooleanSetting showTps = new BooleanSetting("Задержка сервера", "Показывать TPS", true);
   private final BooleanSetting showBps = new BooleanSetting("Скорость игрока", "Показывать скорость блоков/сек", true);
   private float smoothedFps;

   public WatermarkWidget() {
      super(new DragInfo("Инфо-панель", 0.0F, 0.0F, 0.0F, 0.0F));
      this.j().setWidget(this);
      this.j().setDragStatus(2);
      this.a(this.sideDisplay, this.separatePills, this.showFps, this.showPing, this.showTime, this.showLogin, this.showCoords, this.showTps, this.showBps);
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(true);
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      this.smoothedFps = MathUtil.c(this.smoothedFps, mc.getCurrentFps(), 0.1F);
      float iconSize = 7.0F - 0.5F;
      float logoSize = 7.0F + 1.0F;
      float sectionGap = !this.separatePills.get() ? 5.0F : 2.0F;
      String[][] topSections = this.getTopSections();
      String[][] bottomSections = this.getBottomSections();
      float topWidth = this.calcSectionsWidth(topSections, true, iconSize, logoSize, 5.0F, sectionGap, 3.0F, 4.0F);
      float bottomWidth = this.calcSectionsWidth(bottomSections, false, iconSize, logoSize, 5.0F, sectionGap, 3.0F, 4.0F);
      float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
      float x = this.sideDisplay.get() ? 5.0F : (screenW - topWidth) / 2.0F;
      float y = 5.0F;
      float bottomX = this.sideDisplay.get() ? x : x + (topWidth - bottomWidth) / 2.0F;
      this.j().setX(x);
      this.j().setY(y);
      this.j().setWidth(topWidth);
      this.j().setHeight(bottomSections.length > 0 ? 12.5F + 3.0F + 12.5F : 12.5F);
      int primaryColor = ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor(), 1.0F);
      this.drawBar(event, x, y, topWidth, topSections, true, primaryColor, iconSize, logoSize, 5.0F, sectionGap, 3.0F, 4.0F, -0.5F);
      if (bottomSections.length > 0) {
         this.drawBar(
            event, bottomX, y + 12.5F + 3.0F, bottomWidth, bottomSections, false, primaryColor, iconSize, logoSize, 5.0F, sectionGap, 3.0F, 4.0F, -0.5F
         );
      }

      super.a(event);
   }

   private void drawBar(
      DrawEvent event,
      float x,
      float y,
      float width,
      String[][] sections,
      boolean logo,
      int primaryColor,
      float iconSize,
      float logoSize,
      float startPadding,
      float sectionGap,
      float iconTextGap,
      float logoGap,
      float textYOffset
   ) {
      if (this.separatePills.get()) {
         this.drawSeparated(event, x, y, sections, logo, primaryColor, iconSize, logoSize, startPadding, sectionGap, iconTextGap, textYOffset);
      } else {
         this.a(event, x, y, width, 12.5F, true, 1.0F);
         float cursor = x + startPadding;
         float textY = y + (12.5F - Fonts.e.a(7.0F)) / 2.0F + textYOffset;
         if (logo) {
            float logoW = Fonts.c.a("FLUX", 7.5F);
            float logoTextY = y + (12.5F - Fonts.c.a(7.5F)) / 2.0F - 0.5F;
            Fonts.c.a(event.getMatrixStack(), "FLUX", cursor, logoTextY, 7.5F, primaryColor);
            float cursor2 = cursor + logoW + logoGap;
            this.a(event, cursor2, y, 12.5F, 1.0F);
            cursor = cursor2 + 1.0F + sectionGap;
         }

         for (int i = 0; i < sections.length; i++) {
            if (i > 0) {
               this.a(event, cursor, y, 12.5F, 1.0F);
               cursor += 1.0F + sectionGap;
            }

            Fonts.a.a(event.getMatrixStack(), sections[i][0], cursor, y + (12.5F - Fonts.a.a(iconSize)) / 2.0F, iconSize, primaryColor);
            float cursor3 = cursor + Fonts.a.a(sections[i][0], iconSize) + iconTextGap;
            Fonts.e
               .a(event.getMatrixStack(), sections[i][1], cursor3, textY, 7.0F, FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor());
            cursor = cursor3 + Fonts.e.a(sections[i][1], 7.0F) + sectionGap;
         }
      }
   }

   private void drawSeparated(
      DrawEvent event,
      float x,
      float y,
      String[][] sections,
      boolean logo,
      int primaryColor,
      float iconSize,
      float logoSize,
      float startPadding,
      float sectionGap,
      float iconTextGap,
      float textYOffset
   ) {
      float cursor = x;
      float textY = y + (12.5F - Fonts.e.a(7.0F)) / 2.0F + textYOffset;
      if (logo) {
         float logoW = Fonts.c.a("FLUX", 7.5F);
         float logoWidth = startPadding * 2.0F + logoW;
         this.a(event, x, y, logoWidth, 12.5F, true, 1.0F);
         float logoTextY = y + (12.5F - Fonts.c.a(7.5F)) / 2.0F - 0.5F;
         Fonts.c.a(event.getMatrixStack(), "FLUX", x + startPadding, logoTextY, 7.5F, primaryColor);
         cursor = x + (logoWidth + sectionGap);
      }

      for (String[] section : sections) {
         float sectionWidth = startPadding * 2.0F + Fonts.a.a(section[0], iconSize) + iconTextGap + Fonts.e.a(section[1], 7.0F);
         this.a(event, cursor, y, sectionWidth, 12.5F, true, 1.0F);
         float inner = cursor + startPadding;
         Fonts.a.a(event.getMatrixStack(), section[0], inner, y + (12.5F - Fonts.a.a(iconSize)) / 2.0F, iconSize, primaryColor);
         Fonts.e
            .a(
               event.getMatrixStack(),
               section[1],
               inner + Fonts.a.a(section[0], iconSize) + iconTextGap,
               textY,
               7.0F,
               FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor()
            );
         cursor += sectionWidth + sectionGap;
      }
   }

   private float calcSectionsWidth(
      String[][] sections, boolean logo, float iconSize, float logoSize, float startPadding, float sectionGap, float iconTextGap, float logoGap
   ) {
      if (!this.separatePills.get()) {
         float width2 = startPadding;
         if (logo) {
            float logoW = Fonts.c.a("FLUX", 7.5F);
            width2 = startPadding + logoW + logoGap + 1.0F + sectionGap;
         }

         for (int i = 0; i < sections.length; i++) {
            if (i > 0) {
               width2 += 1.0F + sectionGap;
            }

            width2 = width2 + Fonts.a.a(sections[i][0], iconSize) + iconTextGap + Fonts.e.a(sections[i][1], 7.0F) + sectionGap;
         }

         return width2;
      } else {
         float width = 0.0F;
         if (logo) {
            float logoW = Fonts.c.a("FLUX", 7.5F);
            width = startPadding * 2.0F + logoW + sectionGap;
         }

         for (String[] section : sections) {
            width += startPadding * 2.0F + Fonts.a.a(section[0], iconSize) + iconTextGap + Fonts.e.a(section[1], 7.0F) + sectionGap;
         }

         return Math.max(0.0F, width - sectionGap);
      }
   }

   private String[][] getTopSections() {
      List<String[]> sections = new ArrayList<>();
      if (this.showLogin.get()) {
         String user = mc.getSession() != null ? mc.getSession().getUsername() : "Kirka_int";
         sections.add(new String[]{"L", user});
      }

      if (this.showFps.get()) {
         sections.add(new String[]{"q", (int)this.smoothedFps + " FPS"});
      }

      if (this.showPing.get()) {
         sections.add(new String[]{"P", ServerUtil.d() + " ms"});
      }

      if (this.showTime.get()) {
         sections.add(new String[]{"T", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))});
      }

      return sections.toArray(new String[0][]);
   }

   private String[][] getBottomSections() {
      List<String[]> sections = new ArrayList<>();
      if (mc.player != null) {
         if (this.showCoords.get()) {
            sections.add(new String[]{"b", "x " + (int)mc.player.getX() + " y " + (int)mc.player.getY() + " z " + (int)mc.player.getZ()});
         }

         if (this.showTps.get()) {
            sections.add(new String[]{"g", "20.0 TPS"});
         }

         if (this.showBps.get()) {
            sections.add(new String[]{"e", String.format("%.2f BPS", ServerUtil.c())});
         }
      }

      return sections.toArray(new String[0][]);
   }
}
