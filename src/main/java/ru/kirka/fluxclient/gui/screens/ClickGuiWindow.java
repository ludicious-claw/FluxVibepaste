package ru.kirka.fluxclient.gui.screens;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImString;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.config.Setting;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.MultiSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.config.impl.TextSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class ClickGuiWindow {
   private static ClickGuiWindow.NavMode currentTab = ClickGuiWindow.NavMode.MODULES;
   private static Module expandedModule = null;
   private static Module bindingModule = null;
   private static final ImString searchFilter = new ImString(64);
   private static final ImString newConfigName = new ImString(32);
   private static final Map<String, ImString> textBuffers = new HashMap<>();
   private static String activeConfig = "Default";
   private static final Animation openAnimation = new Animation(210.0F, Easing.EASE_OUT_QUAD);
   private static final Animation popAnimation = new Animation(230.0F, Easing.EASE_OUT_BACK);
   private static boolean closing = false;
   private static boolean finishingClose = false;
   private static long openTimestamp = 0L;

   public static void onOpen() {
      closing = false;
      finishingClose = false;
      openAnimation.reset(0.0F);
      openAnimation.setTarget(1.0F);
      popAnimation.reset(0.0F);
      popAnimation.setTarget(1.0F);
      openTimestamp = System.currentTimeMillis();
      bindingModule = null;
   }

   public static void requestClose() {
      if (bindingModule != null) {
         bindingModule = null;
      } else {
         closing = true;
         openAnimation.setTarget(0.0F);
         popAnimation.setTarget(0.0F);
      }
   }

   public static boolean isClosing() {
      return closing;
   }

   public static boolean isAnimating() {
      return openAnimation.getValue() > 0.001F || closing;
   }

   public static boolean isFinishingClose() {
      return finishingClose;
   }

   public static boolean isSettingsOpen() {
      return expandedModule != null || bindingModule != null;
   }

   public static void openSettings(Module m) {
      if (m != null) {
         bindingModule = null;
         expandedModule = expandedModule == m ? null : m;
      }
   }

   public static void closeSettings() {
      expandedModule = null;
      bindingModule = null;
   }

   public static void render() {
      openAnimation.update();
      popAnimation.update();
      float progress = openAnimation.getValue();
      if (progress <= 0.001F) {
         if (closing) {
            closing = false;
            finishingClose = true;
            MinecraftClient client = MinecraftClient.getInstance();
            ImGuiEngine.post(() -> {
               client.setScreen(null);
               Module module = FluxContext.get().getModuleManager().getModule(ClickGuiModule.class);
               if (module != null && module.isEnabled()) {
                  module.setEnabled(false);
               }
            });
            finishingClose = false;
         }
      } else {
         handleKeyBinding();
         float fade = MathUtil.clamp(progress, 0.0F, 1.0F);
         float pop = MathUtil.clamp(popAnimation.getValue(), 0.0F, 1.0F);
         float scale = 0.9F + 0.1F * pop;
         float screenW = ImGui.getIO().getDisplaySizeX();
         float screenH = ImGui.getIO().getDisplaySizeY();
         ImGui.getBackgroundDrawList().addRectFilled(0.0F, 0.0F, screenW, screenH, ImColor.rgba(0, 0, 0, (int)(130.0F * fade)));
         Category[] cats = Category.values();
         float gap = 12.0F;
         float columnW = 235.0F;
         float totalW = cats.length * columnW + (cats.length - 1) * gap;
         float totalH = Math.min(screenH - 120.0F, 630.0F);
         float startX = (screenW - totalW * scale) / 2.0F;
         float startY = (screenH - (totalH + 54.0F) * scale) / 2.0F;
         ImGui.pushStyleVar(0, fade);
         if (currentTab == ClickGuiWindow.NavMode.MODULES) {
            renderColumns(startX, startY, columnW, totalH, gap, scale, fade);
         } else if (currentTab == ClickGuiWindow.NavMode.CONFIGS) {
            renderOverlayView("УПРАВЛЕНИЕ КОНФИГАМИ (.flux)", screenW, screenH, totalW, totalH, scale, fade, ClickGuiWindow::renderConfigsContent);
         } else {
            renderOverlayView("ВЫБОР ЦВЕТОВОЙ ТЕМЫ", screenW, screenH, totalW, totalH, scale, fade, ClickGuiWindow::renderThemesContent);
         }

         renderBottomBar(screenW, screenH, startY + totalH * scale + 14.0F, totalW * scale, fade);
         renderBindingModal(screenW, screenH, fade);
         ImGui.popStyleVar();
      }
   }

   private static void handleKeyBinding() {
      if (bindingModule != null) {
         for (int k = 32; k < 348; k++) {
            if (ImGui.isKeyPressed(k)) {
               if (k != 256 && k != 261) {
                  bindingModule.setKeyBind(k);
               } else {
                  bindingModule.setKeyBind(0);
               }

               bindingModule = null;
               break;
            }
         }
      }
   }

   private static void renderColumns(float startX, float startY, float colW, float colH, float gap, float scale, float fade) {
      Category[] cats = Category.values();

      for (int i = 0; i < cats.length; i++) {
         Category cat = cats[i];
         float cx = startX + i * (colW + gap) * scale;
         float stagger = RenderWidgets.entrance(openTimestamp, i, 35, 200);
         float cy = startY + (1.0F - stagger) * 16.0F;
         float w = colW * scale;
         float h = colH * scale;
         renderSingleColumn(cat, cx, cy, w, h, fade);
      }
   }

   private static void renderSingleColumn(Category cat, float x, float y, float w, float h, float fade) {
      ImDrawList draw = ImGui.getBackgroundDrawList();
      RenderWidgets.drawColumnCard(draw, x, y, w, h, fade);
      float headerH = 46.0F;
      String title = cat.getDisplayName();
      List<Module> all = FluxContext.get().getModuleManager().getModulesByCategory(cat);
      int enabledCount = 0;

      for (Module m : all) {
         if (m.isEnabled()) {
            enabledCount++;
         }
      }

      ImGui.pushFont(ImGuiEngine.getHeaderFont());
      float titleX = x + 18.0F;
      float titleY = y + 14.0F;
      draw.addText(titleX, titleY, ImColor.rgba(245, 245, 252, 255), title);
      ImGui.popFont();
      String badge = enabledCount + "/" + all.size();
      ImVec2 bsz = ImGui.calcTextSize(badge);
      float badgeX = x + w - bsz.x - 18.0F;
      int badgeCol = enabledCount > 0 ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(110, 110, 130, 255);
      draw.addText(badgeX, titleY + 2.0F, badgeCol, badge);
      draw.addLine(x + 14.0F, y + headerH, x + w - 14.0F, y + headerH, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.25F), 1.0F);
      float listY = y + headerH + 6.0F;
      float listH = h - headerH - 12.0F;
      ImGui.setNextWindowPos(x + 6.0F, listY, 1);
      ImGui.setNextWindowSize(w - 12.0F, listH, 1);
      int flags = 419;
      if (ImGui.begin("##scroll_" + cat.name(), flags)) {
         String filter = searchFilter.get().toLowerCase().trim();
         List<Module> visibleModules = all.stream()
            .filter(mx -> filter.isEmpty() || mx.getName().toLowerCase().contains(filter) || mx.getDescription().toLowerCase().contains(filter))
            .toList();
         if (visibleModules.isEmpty()) {
            ImGui.setCursorPos(14.0F, 16.0F);
            ImGui.textColored(ImColor.rgba(120, 120, 140, 255), "Нет совпадений");
         }

         float availW = ImGui.getContentRegionAvailX();
         float rowW = availW - 6.0F;

         for (Module mx : visibleModules) {
            renderModuleRow(mx, rowW);
         }
      }

      ImGui.end();
   }

   private static void renderModuleRow(Module m, float rowW) {
      ImGui.pushID(m.getName());
      float rowH = 32.0F;
      float rx = ImGui.getCursorScreenPosX() + 3.0F;
      float ry = ImGui.getCursorScreenPosY();
      boolean hovered = ImGui.isMouseHoveringRect(rx, ry, rx + rowW, ry + rowH);
      float hovT = RenderWidgets.hoverAnim("row_hov_" + m.getName(), hovered);
      float activeT = RenderWidgets.animValue("row_act_" + m.getName(), m.isEnabled() ? 1.0F : 0.0F, 180.0F, Easing.EASE_OUT_QUAD);
      ImDrawList draw = ImGui.getWindowDrawList();
      int baseCol = ImColor.rgba(0, 0, 0, 0);
      int hoverCol = ImColor.rgba(24, 24, 34, 200);
      int activeCol = ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_DIM, 0.65F);
      int rowBg = RenderWidgets.interpolateColor(RenderWidgets.interpolateColor(baseCol, hoverCol, hovT), activeCol, activeT);
      draw.addRectFilled(rx, ry, rx + rowW, ry + rowH, rowBg, 8.0F);
      if (activeT > 0.02F) {
         draw.addRect(rx, ry, rx + rowW, ry + rowH, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F * activeT), 8.0F, 0, 1.0F);
      }

      float dotX = rx + 14.0F;
      float dotY = ry + rowH / 2.0F;
      float dotRadius = 3.2F;
      if (m.isEnabled()) {
         float pulse = RenderWidgets.pulse(2.0F);
         draw.addCircleFilled(dotX, dotY, dotRadius + 2.5F + pulse * 1.5F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F));
      }

      int dotColor = RenderWidgets.interpolateColor(ImColor.rgba(60, 60, 75, 255), ImGuiTheme.ACCENT_COLOR_HOVER, activeT);
      draw.addCircleFilled(dotX, dotY, dotRadius, dotColor);
      int textColor = RenderWidgets.interpolateColor(
         RenderWidgets.interpolateColor(ImColor.rgba(165, 165, 180, 255), ImColor.rgba(235, 235, 245, 255), hovT), ImColor.rgba(255, 255, 255, 255), activeT
      );
      boolean hasSettings = !m.getSettings().isEmpty();
      float maxTextW = hasSettings ? rowW - 68.0F : rowW - 36.0F;
      String name = truncate(m.getName(), maxTextW);
      ImGui.pushFont(ImGuiEngine.getBoldFont());
      draw.addText(rx + 26.0F, ry + 7.5F, textColor, name);
      ImGui.popFont();
      if (hovered && m.getDescription() != null && !m.getDescription().isEmpty()) {
         ImGui.setTooltip(m.getName() + "\n" + m.getDescription() + "\n\nЛКМ — включить/выключить" + (hasSettings ? "\nПКМ или [...] — настройки" : ""));
      }

      boolean expanded = m == expandedModule;
      float dotsBtnW = 28.0F;
      float dotsBtnH = rowH - 8.0F;
      float dotsBtnX = rx + rowW - dotsBtnW - 4.0F;
      float dotsBtnY = ry + 4.0F;
      if (hasSettings) {
         boolean dotsHov = ImGui.isMouseHoveringRect(dotsBtnX, dotsBtnY, dotsBtnX + dotsBtnW, dotsBtnY + dotsBtnH);
         float dotsHovT = RenderWidgets.hoverAnim("dots_hov_" + m.getName(), dotsHov);
         int dotsBg = expanded
            ? ImGuiTheme.ACCENT_COLOR_DIM
            : RenderWidgets.interpolateColor(ImColor.rgba(18, 18, 24, 200), ImColor.rgba(36, 36, 48, 255), dotsHovT);
         draw.addRectFilled(dotsBtnX, dotsBtnY, dotsBtnX + dotsBtnW, dotsBtnY + dotsBtnH, dotsBg, 6.0F);
         int dotsBorder = expanded
            ? ImGuiTheme.ACCENT_COLOR
            : RenderWidgets.interpolateColor(ImColor.rgba(40, 40, 55, 160), ImGuiTheme.ACCENT_COLOR_HOVER, dotsHovT);
         draw.addRect(dotsBtnX, dotsBtnY, dotsBtnX + dotsBtnW, dotsBtnY + dotsBtnH, dotsBorder, 6.0F, 0, 1.0F);
         float bcx = dotsBtnX + dotsBtnW / 2.0F;
         float bcy = dotsBtnY + dotsBtnH / 2.0F;
         float dotR = 1.8F;
         float dotSpacing = 5.0F;
         int dotCol = expanded ? ImColor.rgba(255, 255, 255, 255) : (dotsHov ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(180, 180, 200, 255));
         draw.addCircleFilled(bcx - dotSpacing, bcy, dotR, dotCol);
         draw.addCircleFilled(bcx, bcy, dotR, dotCol);
         draw.addCircleFilled(bcx + dotSpacing, bcy, dotR, dotCol);
         ImGui.setCursorScreenPos(dotsBtnX, dotsBtnY);
         if (ImGui.invisibleButton("##dots_" + m.getName(), dotsBtnW, dotsBtnH)) {
            openSettings(m);
         }
      }

      ImGui.setCursorScreenPos(rx, ry);
      float clickAreaW = hasSettings ? rowW - dotsBtnW - 8.0F : rowW;
      if (ImGui.invisibleButton("##btn_" + m.getName(), clickAreaW, rowH)) {
         m.toggle();
      }

      if (ImGui.isItemClicked(1) && hasSettings) {
         openSettings(m);
      }

      ImGui.setCursorScreenPos(rx, ry + rowH + 4.0F);
      ImGui.dummy(rowW, 0.0F);
      renderInlineSettings(m, rowW, rx);
      ImGui.popID();
   }

   private static void renderInlineSettings(Module m, float rowW, float rx) {
      boolean isOpen = m == expandedModule;
      float animT = RenderWidgets.animValue("mod_exp_" + m.getName(), isOpen ? 1.0F : 0.0F, 220.0F, Easing.EASE_OUT_QUAD);
      if (!(animT <= 0.005F)) {
         float innerX = rx + 2.0F;
         float innerW = rowW - 4.0F;
         float contentX = innerX + 8.0F;
         float contentW = innerW - 16.0F;
         float startY = ImGui.getCursorScreenPosY();
         float totalH = calculateSettingsHeight(m, contentW);
         float currentH = totalH * animT;
         ImDrawList draw = ImGui.getWindowDrawList();
         draw.addRectFilled(innerX, startY - 2.0F, innerX + innerW, startY + currentH, ImColor.rgba(10, 10, 15, 245), 8.0F);
         draw.addRect(innerX, startY - 2.0F, innerX + innerW, startY + currentH, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.42F * animT), 8.0F, 0, 1.0F);
         ImGui.pushClipRect(innerX - 2.0F, startY - 2.0F, innerX + innerW + 2.0F, startY + currentH, true);
         ImGui.setCursorScreenPos(contentX, startY + 4.0F);
         renderBindControl(m, contentX, contentW);

         for (Setting<?> s : m.getSettings()) {
            if (s.isVisible()) {
               ImGui.pushID(s.getName());
               renderSettingControl(s, contentX, contentW);
               ImGui.popID();
            }
         }

         ImGui.popClipRect();
         ImGui.setCursorScreenPos(rx, startY + currentH + 6.0F);
         ImGui.dummy(rowW, 0.0F);
      }
   }

   private static float getModeSettingHeight(ModeSetting m, float w) {
      float cx = 0.0F;
      float cy = 20.0F;
      float pillH = 22.0F;
      float gap = 5.0F;

      for (String opt : m.getModes()) {
         ImVec2 osz = ImGui.calcTextSize(opt);
         float pillW = Math.min(osz.x + 16.0F, w);
         if (cx > 0.0F && cx + pillW > w) {
            cx = 0.0F;
            cy += pillH + gap;
         }

         cx += pillW + gap;
      }

      return cy + pillH + 6.0F;
   }

   private static float calculateSettingsHeight(Module m, float contentW) {
      float h = 30.0F;

      for (Setting<?> s : m.getSettings()) {
         if (s.isVisible()) {
            if (s instanceof BooleanSetting) {
               h += 26.0F;
            } else if (s instanceof NumberSetting) {
               h += 38.0F;
            } else if (s instanceof ModeSetting mode) {
               h += getModeSettingHeight(mode, contentW);
            } else if (s instanceof ColorSetting) {
               h += 28.0F;
            } else if (s instanceof MultiSetting multi) {
               h += 24.0F + multi.getOptions().size() * 22.0F;
            } else if (s instanceof TextSetting) {
               h += 52.0F;
            }
         }
      }

      return h + 10.0F;
   }

   private static void renderBindControl(Module m, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      float h = 24.0F;
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 4.0F, ImColor.rgba(200, 200, 215, 255), "Бинд клавиши");
      String bindName = bindingModule == m ? "..." : getKeyName(m.getKeyBind());
      ImVec2 bsz = ImGui.calcTextSize(bindName);
      float btnW = Math.max(52.0F, bsz.x + 18.0F);
      float bx = x + w - btnW;
      boolean hovered = ImGui.isMouseHoveringRect(bx, y, bx + btnW, y + h);
      float hovT = RenderWidgets.hoverAnim("bind_btn_hov_" + m.getName(), hovered);
      int bg = bindingModule == m
         ? ImGuiTheme.ACCENT_COLOR_DIM
         : RenderWidgets.interpolateColor(ImColor.rgba(20, 20, 26, 255), ImColor.rgba(36, 36, 48, 255), hovT);
      draw.addRectFilled(bx, y, bx + btnW, y + h, bg, 6.0F);
      int border = bindingModule == m
         ? ImGuiTheme.ACCENT_COLOR
         : RenderWidgets.interpolateColor(ImColor.rgba(40, 40, 55, 180), ImGuiTheme.ACCENT_COLOR_HOVER, hovT);
      draw.addRect(bx, y, bx + btnW, y + h, border, 6.0F, 0, 1.0F);
      draw.addText(
         bx + (btnW - bsz.x) / 2.0F, y + (h - bsz.y) / 2.0F, bindingModule == m ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(235, 235, 245, 255), bindName
      );
      ImGui.setCursorScreenPos(bx, y);
      if (ImGui.invisibleButton("##bind_btn_" + m.getName(), btnW, h)) {
         bindingModule = bindingModule == m ? null : m;
      }

      ImGui.setCursorScreenPos(x, y + h + 6.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderSettingControl(Setting<?> s, float x, float w) {
      if (s instanceof BooleanSetting bool) {
         renderBoolSetting(bool, x, w);
      } else if (s instanceof NumberSetting num) {
         renderNumberSetting(num, x, w);
      } else if (s instanceof ModeSetting mode) {
         renderModeSetting(mode, x, w);
      } else if (s instanceof ColorSetting col) {
         renderColorSetting(col, x, w);
      } else if (s instanceof MultiSetting multi) {
         renderMultiSetting(multi, x, w);
      } else if (s instanceof TextSetting text) {
         renderTextSetting(text, x, w);
      }
   }

   private static void renderBoolSetting(BooleanSetting b, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      float h = 22.0F;
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 3.0F, ImColor.rgba(215, 215, 225, 255), b.getName());
      float swW = 34.0F;
      float swH = 17.0F;
      float swX = x + w - swW;
      float swY = y + 2.5F;
      boolean val = RenderWidgets.drawSwitch(draw, "##sw_" + b.getName(), b.get(), swX, swY, swW, swH);
      if (val != b.get()) {
         b.set(val);
      }

      if (b.getDescription() != null && !b.getDescription().isEmpty() && ImGui.isMouseHoveringRect(x, y, x + w - swW - 4.0F, y + h)) {
         ImGui.setTooltip(b.getDescription());
      }

      ImGui.setCursorScreenPos(x, y + h + 4.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderNumberSetting(NumberSetting n, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 2.0F, ImColor.rgba(215, 215, 225, 255), n.getName());
      String fmt = n.getStep() >= 1.0F ? "%.0f" : (n.getStep() >= 0.1F ? "%.1f" : "%.2f");
      String valStr = String.format(fmt, n.get());
      ImVec2 vsz = ImGui.calcTextSize(valStr);
      draw.addText(x + w - vsz.x, y + 2.0F, ImGuiTheme.ACCENT_COLOR_HOVER, valStr);
      float trackY = y + 22.0F;
      float trackH = 4.0F;
      float hitH = 14.0F;
      ImGui.setCursorScreenPos(x, trackY - (hitH - trackH) / 2.0F);
      ImGui.invisibleButton("##slider_hit_" + n.getName(), w, hitH);
      boolean isHovered = ImGui.isItemHovered();
      boolean isActive = ImGui.isItemActive();
      float min = n.getMin();
      float max = n.getMax();
      float range = Math.max(1.0E-4F, max - min);
      if (isActive) {
         float mouseX = ImGui.getIO().getMousePosX();
         float pct = MathUtil.clamp((mouseX - x) / w, 0.0F, 1.0F);
         float newVal = min + pct * range;
         float step = n.getStep();
         if (step > 0.0F) {
            newVal = Math.round(newVal / step) * step;
         }

         n.set(MathUtil.clamp(newVal, min, max));
      }

      float curPct = MathUtil.clamp((n.get() - min) / range, 0.0F, 1.0F);
      draw.addRectFilled(x, trackY, x + w, trackY + trackH, ImColor.rgba(24, 24, 34, 255), 2.0F);
      float fillW = w * curPct;
      if (fillW > 0.0F) {
         draw.addRectFilled(x, trackY, x + fillW, trackY + trackH, ImGuiTheme.ACCENT_COLOR, 2.0F);
      }

      float knobRadius = !isActive && !isHovered ? 4.5F : 5.5F;
      float knobX = x + fillW;
      float knobY = trackY + trackH / 2.0F;
      if (isActive || isHovered) {
         draw.addCircleFilled(knobX, knobY, knobRadius + 3.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F));
      }

      draw.addCircleFilled(knobX, knobY, knobRadius, ImColor.rgba(255, 255, 255, 255));
      draw.addCircle(knobX, knobY, knobRadius, ImGuiTheme.ACCENT_COLOR, 0, 1.5F);
      if (n.getDescription() != null && !n.getDescription().isEmpty() && isHovered) {
         ImGui.setTooltip(n.getDescription() + "\nДиапазон: " + min + " — " + max);
      }

      ImGui.setCursorScreenPos(x, y + 38.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderModeSetting(ModeSetting m, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 2.0F, ImColor.rgba(215, 215, 225, 255), m.getName());
      if (m.getDescription() != null
         && !m.getDescription().isEmpty()
         && ImGui.isMouseHoveringRect(x, y, x + ImGui.calcTextSize(m.getName()).x + 10.0F, y + 18.0F)) {
         ImGui.setTooltip(m.getDescription());
      }

      float cx = x;
      float cy = y + 20.0F;
      float pillH = 22.0F;
      float gap = 5.0F;

      for (String opt : m.getModes()) {
         boolean active = m.is(opt);
         ImVec2 osz = ImGui.calcTextSize(opt);
         float pillW = Math.min(osz.x + 16.0F, w);
         if (cx > x && cx + pillW > x + w) {
            cx = x;
            cy += pillH + gap;
         }

         boolean hovered = ImGui.isMouseHoveringRect(cx, cy, cx + pillW, cy + pillH);
         float hovT = RenderWidgets.hoverAnim("mode_hov_" + m.getName() + "_" + opt, hovered);
         float actT = RenderWidgets.animValue("mode_act_" + m.getName() + "_" + opt, active ? 1.0F : 0.0F, 160.0F, Easing.EASE_OUT_QUAD);
         int bg = RenderWidgets.interpolateColor(
            RenderWidgets.interpolateColor(ImColor.rgba(18, 18, 25, 255), ImColor.rgba(30, 30, 42, 255), hovT), ImGuiTheme.ACCENT_COLOR, actT
         );
         draw.addRectFilled(cx, cy, cx + pillW, cy + pillH, bg, 6.0F);
         int border = RenderWidgets.interpolateColor(ImColor.rgba(42, 42, 58, 180), ImGuiTheme.ACCENT_COLOR_HOVER, Math.max(actT, hovT));
         draw.addRect(cx, cy, cx + pillW, cy + pillH, border, 6.0F, 0, 1.0F);
         int txtCol = RenderWidgets.interpolateColor(ImColor.rgba(175, 175, 195, 255), ImColor.rgba(255, 255, 255, 255), Math.max(actT, hovT));
         draw.addText(cx + (pillW - osz.x) / 2.0F, cy + (pillH - osz.y) / 2.0F, txtCol, opt);
         ImGui.setCursorScreenPos(cx, cy);
         if (ImGui.invisibleButton("##pill_" + m.getName() + "_" + opt, pillW, pillH)) {
            m.set(opt);
         }

         cx += pillW + gap;
      }

      ImGui.setCursorScreenPos(x, cy + pillH + 6.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderColorSetting(ColorSetting c, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      float h = 24.0F;
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 4.0F, ImColor.rgba(215, 215, 225, 255), c.getName());
      float boxW = 40.0F;
      float boxX = x + w - boxW;
      int col = c.get().getRGB();
      draw.addRectFilled(boxX, y + 2.0F, boxX + boxW, y + h - 2.0F, col, 5.0F);
      draw.addRect(boxX, y + 2.0F, boxX + boxW, y + h - 2.0F, ImColor.rgba(255, 255, 255, 60), 5.0F, 0, 1.0F);
      ImGui.setCursorScreenPos(boxX, y + 2.0F);
      float[] clr = c.getFloatRGBA();
      if (ImGui.colorEdit4("##cp_" + c.getName(), clr, 160)) {
         c.set(new Color(clr[0], clr[1], clr[2], clr[3]));
      }

      ImGui.setCursorScreenPos(x, y + h + 4.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderMultiSetting(MultiSetting m, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 2.0F, ImColor.rgba(215, 215, 225, 255), m.getName());
      float subY = y + 20.0F;

      for (BooleanSetting opt : m.getOptions()) {
         boolean val = opt.get();
         float rowH = 20.0F;
         float boxSize = 14.0F;
         float boxX = x + 2.0F;
         float boxY = subY + 3.0F;
         draw.addRectFilled(boxX, boxY, boxX + boxSize, boxY + boxSize, val ? ImGuiTheme.ACCENT_COLOR : ImColor.rgba(20, 20, 28, 255), 3.0F);
         draw.addRect(boxX, boxY, boxX + boxSize, boxY + boxSize, val ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(50, 50, 65, 180), 3.0F, 0, 1.0F);
         if (val) {
            draw.addText(boxX + 2.5F, boxY - 1.0F, ImColor.rgba(255, 255, 255, 255), "✓");
         }

         draw.addText(boxX + boxSize + 8.0F, subY + 2.0F, ImColor.rgba(200, 200, 215, 255), opt.getName());
         ImGui.setCursorScreenPos(x, subY);
         if (ImGui.invisibleButton("##multi_" + m.getName() + "_" + opt.getName(), w, rowH)) {
            opt.set(!val);
         }

         subY += rowH + 2.0F;
      }

      ImGui.setCursorScreenPos(x, subY + 4.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static void renderTextSetting(TextSetting t, float x, float w) {
      float y = ImGui.getCursorScreenPosY();
      ImDrawList draw = ImGui.getWindowDrawList();
      draw.addText(x, y + 2.0F, ImColor.rgba(215, 215, 225, 255), t.getName());
      String cur = t.get() == null ? "" : t.get();
      ImString buf = textBuffers.computeIfAbsent(t.getName(), k -> new ImString(cur, 256));
      if (!cur.equals(buf.get()) && !ImGui.isItemActive()) {
         buf.set(cur);
      }

      ImGui.setCursorScreenPos(x, y + 20.0F);
      ImGui.pushItemWidth(w);
      ImGui.pushStyleColor(7, ImColor.rgba(16, 16, 22, 255));
      ImGui.pushStyleColor(5, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F));
      ImGui.pushStyleVar(13, 1.0F);
      ImGui.pushStyleVar(12, 6.0F);
      ImGui.inputText("##input_" + t.getName(), buf);
      ImGui.popStyleVar(2);
      ImGui.popStyleColor(2);
      ImGui.popItemWidth();
      if (!buf.get().equals(cur)) {
         t.set(buf.get());
      }

      ImGui.setCursorScreenPos(x, y + 48.0F);
      ImGui.dummy(w, 0.0F);
   }

   private static String getKeyName(int keyCode) {
      if (keyCode == 344) {
         return "RSHIFT";
      } else if (keyCode == 340) {
         return "LSHIFT";
      } else if (keyCode == 32) {
         return "SPACE";
      } else if (keyCode <= 0) {
         return "NONE";
      } else {
         String name = GLFW.glfwGetKeyName(keyCode, 0);
         return name != null ? name.toUpperCase() : "KEY_" + keyCode;
      }
   }

   private static String truncate(String s, float maxW) {
      if (s == null) {
         return "";
      } else if (ImGui.calcTextSize(s).x <= maxW) {
         return s;
      } else {
         int len = s.length();

         while (len > 0 && ImGui.calcTextSize(s.substring(0, len) + "..").x > maxW) {
            len--;
         }

         return len <= 0 ? "" : s.substring(0, len) + "..";
      }
   }

   private static void renderBottomBar(float screenW, float screenH, float y, float totalW, float fade) {
      float barH = 38.0F;
      float barW = Math.min(totalW, 490.0F);
      float bx = (screenW - barW) / 2.0F;
      ImGui.setNextWindowPos(bx, y, 1);
      ImGui.setNextWindowSize(barW, barH, 1);
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(4, 0.0F);
      int barFlags = 431;
      if (ImGui.begin("##FluxBottomBar", barFlags)) {
         ImDrawList draw = ImGui.getWindowDrawList();
         draw.addRectFilled(bx, y, bx + barW, y + barH, ImColor.rgba(11, 11, 16, 242), 19.0F);
         draw.addRect(bx, y, bx + barW, y + barH, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.38F), 19.0F, 0, 1.0F);
         float btnW = 74.0F;
         float btnGap = 5.0F;
         float rightBtnsTotalW = btnW * 3.0F + btnGap * 2.0F;
         float inputW = barW - rightBtnsTotalW - 28.0F;
         ImGui.setCursorScreenPos(bx + 16.0F, y + 6.0F);
         ImGui.pushItemWidth(inputW);
         ImGui.pushStyleColor(7, ImColor.rgba(0, 0, 0, 0));
         ImGui.pushStyleColor(5, ImColor.rgba(0, 0, 0, 0));
         ImGui.pushStyleVar(13, 0.0F);
         if (currentTab == ClickGuiWindow.NavMode.MODULES) {
            ImGui.inputTextWithHint("##SearchInput", "Поиск функций...", searchFilter);
         } else {
            String title = currentTab == ClickGuiWindow.NavMode.CONFIGS ? "Конфигурации" : "Цветовые темы";
            draw.addText(bx + 16.0F, y + 10.0F, ImColor.rgba(240, 240, 250, 255), title);
            ImGui.dummy(inputW, 20.0F);
         }

         ImGui.popStyleVar();
         ImGui.popStyleColor(2);
         ImGui.popItemWidth();
         float btnX = bx + barW - rightBtnsTotalW - 10.0F;
         float btnY = y + 5.0F;
         float bH = 28.0F;
         renderTabButton(draw, "Модули", btnX, btnY, btnW, bH, currentTab == ClickGuiWindow.NavMode.MODULES, () -> currentTab = ClickGuiWindow.NavMode.MODULES);
         renderTabButton(
            draw,
            "Конфиги",
            btnX + btnW + btnGap,
            btnY,
            btnW,
            bH,
            currentTab == ClickGuiWindow.NavMode.CONFIGS,
            () -> currentTab = ClickGuiWindow.NavMode.CONFIGS
         );
         renderTabButton(
            draw,
            "Темы",
            btnX + (btnW + btnGap) * 2.0F,
            btnY,
            btnW,
            bH,
            currentTab == ClickGuiWindow.NavMode.THEMES,
            () -> currentTab = ClickGuiWindow.NavMode.THEMES
         );
      }

      ImGui.end();
      ImGui.popStyleVar(2);
   }

   private static void renderTabButton(ImDrawList draw, String label, float x, float y, float w, float h, boolean active, Runnable onClick) {
      boolean hovered = ImGui.isMouseHoveringRect(x, y, x + w, y + h);
      float hovT = RenderWidgets.hoverAnim("tab_btn_" + label, hovered);
      float actT = RenderWidgets.animValue("tab_act_" + label, active ? 1.0F : 0.0F, 150.0F, Easing.EASE_OUT_QUAD);
      int bg = RenderWidgets.interpolateColor(
         RenderWidgets.interpolateColor(ImColor.rgba(20, 20, 28, 255), ImColor.rgba(36, 36, 50, 255), hovT), ImGuiTheme.ACCENT_COLOR, actT
      );
      draw.addRectFilled(x, y, x + w, y + h, bg, 14.0F);
      int border = RenderWidgets.interpolateColor(ImColor.rgba(40, 40, 55, 180), ImGuiTheme.ACCENT_COLOR_HOVER, Math.max(actT, hovT * 0.5F));
      draw.addRect(x, y, x + w, y + h, border, 14.0F, 0, 1.0F);
      ImVec2 lsz = ImGui.calcTextSize(label);
      draw.addText(
         x + (w - lsz.x) / 2.0F,
         y + (h - lsz.y) / 2.0F,
         RenderWidgets.interpolateColor(ImColor.rgba(180, 180, 195, 255), ImColor.rgba(255, 255, 255, 255), Math.max(actT, hovT)),
         label
      );
      ImGui.setCursorScreenPos(x, y);
      if (ImGui.invisibleButton("##tab_btn_act_" + label, w, h)) {
         onClick.run();
      }
   }

   private static void renderOverlayView(
      String title, float screenW, float screenH, float totalW, float totalH, float scale, float fade, Consumer<Float> contentRenderer
   ) {
      float winW = Math.min(totalW * scale, 680.0F);
      float winH = totalH * scale;
      float px = (screenW - winW) / 2.0F;
      float py = (screenH - (winH + 50.0F)) / 2.0F;
      ImDrawList draw = ImGui.getBackgroundDrawList();
      RenderWidgets.drawColumnCard(draw, px, py, winW, winH, fade);
      ImGui.pushFont(ImGuiEngine.getHeaderFont());
      draw.addText(px + 24.0F, py + 18.0F, ImColor.rgba(245, 245, 252, 255), title);
      ImGui.popFont();
      draw.addLine(px + 20.0F, py + 48.0F, px + winW - 20.0F, py + 48.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F), 1.0F);
      float padX = 20.0F;
      float padTop = 56.0F;
      float padBottom = 20.0F;
      float contentW = winW - padX * 2.0F;
      float contentH = winH - padTop - padBottom;
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(4, 0.0F);
      ImGui.setNextWindowPos(px + padX, py + padTop, 1);
      ImGui.setNextWindowSize(contentW, contentH, 1);
      int flags = 419;
      if (ImGui.begin("##overlay_content", flags)) {
         contentRenderer.accept(contentW);
      }

      ImGui.end();
      ImGui.popStyleVar(2);
   }

   private static void renderConfigsContent(float contentW) {
      float marginX = 2.0F;
      float availW = ImGui.getContentRegionAvailX();
      float rw = availW - marginX * 2.0F;
      ImGui.setCursorPosX(marginX + 2.0F);
      ImGui.textColored(ImColor.rgba(140, 140, 160, 255), "Конфиги сохраняются в папку run/fluxclient/configs/");
      ImGui.setCursorPosX(marginX);
      if (ImGui.button("Открыть папку с конфигами в Проводнике", 320.0F, 28.0F)) {
         FluxContext.get().getConfigManager().openFolder();
      }

      ImGui.dummy(0.0F, 10.0F);

      for (String cfg : FluxContext.get().getConfigManager().getAvailableConfigs()) {
         float rx = ImGui.getCursorScreenPosX() + marginX;
         float ry = ImGui.getCursorScreenPosY();
         float rh = 44.0F;
         boolean active = activeConfig.equals(cfg);
         boolean hov = ImGui.isMouseHoveringRect(rx, ry, rx + rw, ry + rh);
         float ht = RenderWidgets.hoverAnim("cfg_hov_" + cfg, hov);
         ImDrawList draw = ImGui.getWindowDrawList();
         int bg = RenderWidgets.interpolateColor(
            RenderWidgets.interpolateColor(ImColor.rgba(14, 14, 20, 255), ImColor.rgba(24, 24, 34, 255), ht), ImGuiTheme.ACCENT_COLOR_DIM, active ? 0.7F : 0.0F
         );
         draw.addRectFilled(rx, ry, rx + rw, ry + rh, bg, 10.0F);
         draw.addRect(rx, ry, rx + rw, ry + rh, active ? ImGuiTheme.ACCENT_COLOR : ImColor.rgba(36, 36, 48, 200), 10.0F, 0, 1.0F);
         ImVec2 cfgSz = ImGui.calcTextSize(cfg + ".flux");
         float textY = ry + (rh - cfgSz.y) / 2.0F;
         draw.addText(rx + 16.0F, textY, ImColor.rgba(240, 240, 250, 255), cfg + ".flux");
         if (active) {
            draw.addText(rx + 16.0F + cfgSz.x + 10.0F, textY, ImGuiTheme.ACCENT_COLOR_HOVER, "(активен)");
         }

         float bw = 88.0F;
         float btnH = 26.0F;
         float btnY = ry + (rh - btnH) / 2.0F;
         float loadBtnX = rx + rw - bw - 12.0F;
         float saveBtnX = loadBtnX - bw - 8.0F;
         ImGui.setCursorScreenPos(saveBtnX, btnY);
         if (ImGui.button("Сохранить##" + cfg, bw, btnH)) {
            activeConfig = cfg;
            FluxContext.get().getConfigManager().saveConfig(cfg);
         }

         ImGui.setCursorScreenPos(loadBtnX, btnY);
         if (ImGui.button("Загрузить##" + cfg, bw, btnH)) {
            activeConfig = cfg;
            FluxContext.get().getConfigManager().loadConfig(cfg);
         }

         ImGui.setCursorScreenPos(rx - marginX, ry + rh);
         ImGui.dummy(rw, 4.0F);
      }

      ImGui.dummy(0.0F, 8.0F);
      ImGui.setCursorPosX(marginX);
      float btnCreateW = 150.0F;
      float gap = 8.0F;
      float inputW = rw - btnCreateW - gap;
      ImGui.pushItemWidth(inputW);
      ImGui.inputTextWithHint("##NewCfgName", "Имя нового конфига...", newConfigName);
      ImGui.popItemWidth();
      ImGui.sameLine(0.0F, gap);
      if (ImGui.button("Создать .flux", btnCreateW, 26.0F)) {
         String name = newConfigName.get().trim();
         if (!name.isEmpty()) {
            activeConfig = name;
            FluxContext.get().getConfigManager().saveConfig(name);
            newConfigName.set("");
         }
      }
   }

   private static void renderThemesContent(float contentW) {
      float marginX = 2.0F;
      float availW = ImGui.getContentRegionAvailX();
      float rw = availW - marginX * 2.0F;
      ImGui.setCursorPosX(marginX + 2.0F);
      ImGui.textColored(ImColor.rgba(140, 140, 160, 255), "Текущая активная тема: " + ImGuiTheme.currentTheme);
      ImGui.dummy(0.0F, 10.0F);
      String[][] themes = new String[][]{
         {"Crimson Blood", "235", "45", "60"},
         {"Celestial Blue", "59", "130", "246"},
         {"Flux Violet", "147", "51", "234"},
         {"Emerald Green", "16", "185", "129"},
         {"Sakura Pink", "236", "72", "153"},
         {"Sunset Orange", "249", "115", "22"}
      };

      for (String[] th : themes) {
         float rx = ImGui.getCursorScreenPosX() + marginX;
         float ry = ImGui.getCursorScreenPosY();
         float rh = 46.0F;
         boolean active = ImGuiTheme.currentTheme.equals(th[0]);
         boolean hov = ImGui.isMouseHoveringRect(rx, ry, rx + rw, ry + rh);
         float ht = RenderWidgets.hoverAnim("theme_hov_" + th[0], hov);
         int r = Integer.parseInt(th[1]);
         int g = Integer.parseInt(th[2]);
         int b = Integer.parseInt(th[3]);
         ImDrawList draw = ImGui.getWindowDrawList();
         int bg = RenderWidgets.interpolateColor(
            RenderWidgets.interpolateColor(ImColor.rgba(14, 14, 20, 255), ImColor.rgba(24, 24, 34, 255), ht),
            ImColor.rgba(r / 4, g / 4, b / 4, 255),
            active ? 0.85F : 0.0F
         );
         draw.addRectFilled(rx, ry, rx + rw, ry + rh, bg, 12.0F);
         draw.addRect(rx, ry, rx + rw, ry + rh, active ? ImColor.rgba(r, g, b, 255) : ImColor.rgba(36, 36, 48, 200), 12.0F, 0, 1.0F);
         float dotCx = rx + 24.0F;
         float dotCy = ry + rh / 2.0F;
         draw.addCircleFilled(dotCx, dotCy, 9.0F, ImColor.rgba(r, g, b, 255));
         if (active) {
            float p = RenderWidgets.pulse(1.8F);
            draw.addCircle(dotCx, dotCy, 12.0F + p * 2.0F, ColorUtil.withAlpha(ImColor.rgba(r, g, b, 255), 0.5F), 0, 1.5F);
         }

         ImVec2 nameSz = ImGui.calcTextSize(th[0]);
         draw.addText(rx + 44.0F, ry + (rh - nameSz.y) / 2.0F, ImColor.rgba(240, 240, 250, 255), th[0]);
         if (active) {
            String tag = "АКТИВНА";
            ImVec2 tsz = ImGui.calcTextSize(tag);
            float tagX = rx + rw - tsz.x - 20.0F;
            float tagY = ry + (rh - tsz.y) / 2.0F;
            float badgePadX = 8.0F;
            float badgePadY = 4.0F;
            draw.addRectFilled(
               tagX - badgePadX,
               tagY - badgePadY,
               tagX + tsz.x + badgePadX,
               tagY + tsz.y + badgePadY,
               ColorUtil.withAlpha(ImColor.rgba(r, g, b, 255), 0.18F),
               6.0F
            );
            draw.addRect(
               tagX - badgePadX,
               tagY - badgePadY,
               tagX + tsz.x + badgePadX,
               tagY + tsz.y + badgePadY,
               ColorUtil.withAlpha(ImColor.rgba(r, g, b, 255), 0.5F),
               6.0F,
               0,
               1.0F
            );
            draw.addText(tagX, tagY, ImColor.rgba(r, g, b, 255), tag);
         }

         ImGui.setCursorScreenPos(rx, ry);
         if (ImGui.invisibleButton("##th_btn_" + th[0], rw, rh)) {
            ImGuiTheme.setTheme(th[0], r, g, b);
         }

         ImGui.setCursorScreenPos(rx - marginX, ry + rh);
         ImGui.dummy(rw, 4.0F);
      }
   }

   private static void renderBindingModal(float sw, float sh, float fade) {
      if (bindingModule != null) {
         ImDrawList fg = ImGui.getForegroundDrawList();
         fg.addRectFilled(0.0F, 0.0F, sw, sh, ImColor.rgba(0, 0, 0, (int)(140.0F * fade)));
         float modalW = 340.0F;
         float modalH = 110.0F;
         float mx = (sw - modalW) / 2.0F;
         float my = (sh - modalH) / 2.0F;
         float p = RenderWidgets.pulse(2.2F);
         fg.addRectFilled(mx - 2.0F, my - 2.0F, mx + modalW + 2.0F, my + modalH + 2.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F + 0.25F * p), 14.0F);
         fg.addRectFilled(mx, my, mx + modalW, my + modalH, ImColor.rgba(12, 12, 17, 255), 12.0F);
         ImVec2 t1 = ImGui.calcTextSize("Нажмите клавишу для бинда...");
         fg.addText(mx + (modalW - t1.x) / 2.0F, my + 22.0F, ImColor.rgba(255, 255, 255, 255), "Нажмите клавишу для бинда...");
         String sub = bindingModule.getName() + "  |  ESC/DEL — сбросить";
         ImVec2 t2 = ImGui.calcTextSize(sub);
         fg.addText(mx + (modalW - t2.x) / 2.0F, my + 50.0F, ImColor.rgba(160, 160, 180, 255), sub);
         String dots = ".".repeat(1 + (int)(p * 3.0F));
         ImVec2 t3 = ImGui.calcTextSize(dots);
         fg.addText(mx + (modalW - t3.x) / 2.0F, my + 74.0F, ImGuiTheme.ACCENT_COLOR_HOVER, dots);
      }
   }

   private static enum NavMode {
      MODULES,
      CONFIGS,
      THEMES;
   }
}
