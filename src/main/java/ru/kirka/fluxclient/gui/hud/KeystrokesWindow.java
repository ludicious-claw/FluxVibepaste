package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.hud.Keystrokes;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class KeystrokesWindow {
   public static float posX = -1.0F;
   public static float posY = 200.0F;
   private static boolean dragging = false;
   private static boolean prevLmbDrag = false;
   private static float grabDX = 0.0F;
   private static float grabDY = 0.0F;
   private static final Map<String, Float> PRESS = new HashMap<>();
   private static final List<Long> L_CLICKS = new ArrayList<>();
   private static final List<Long> R_CLICKS = new ArrayList<>();
   private static boolean prevLmb = false;
   private static boolean prevRmb = false;
   private static double lastTime = -1.0;
   private static float dt = 0.016F;

   private KeystrokesWindow() {
   }

   public static void render() {
      render(MinecraftClient.getInstance().getNetworkHandler() != null ? FluxContext.get().getModuleManager().getModule(Keystrokes.class) : null);
   }

   public static void render(Keystrokes hud) {
      if (hud != null && hud.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.player != null) {
            boolean editMode = mc.currentScreen instanceof ChatScreen;
            if (mc.currentScreen == null || editMode) {
               double now = ImGui.getTime();
               if (lastTime < 0.0) {
                  lastTime = now;
               }

               dt = (float)Math.min(0.05, Math.max(0.001, now - lastTime));
               lastTime = now;
               float sw = ImGui.getIO().getDisplaySizeX();
               float sh = ImGui.getIO().getDisplaySizeY();
               if (posX < 0.0F) {
                  posX = 24.0F;
               }

               float cell = 30.0F;
               float gap = 4.0F;
               float w = cell * 3.0F + gap * 2.0F;
               float rows = hud.showMouse.get() ? 3.0F : 2.0F;
               float cpsH = hud.showCPS.get() && hud.showMouse.get() ? 20.0F : 0.0F;
               float h = cell * rows + gap * (rows - 1.0F) + cpsH;
               if (!editMode) {
                  dragging = false;
                  prevLmbDrag = false;
               }

               if (editMode) {
                  long handle = mc.getWindow().getHandle();
                  boolean lmb = GLFW.glfwGetMouseButton(handle, 0) == 1;
                  double[] cx = new double[1];
                  double[] cy = new double[1];
                  GLFW.glfwGetCursorPos(handle, cx, cy);
                  float mx = (float)cx[0];
                  float my = (float)cy[0];
                  boolean inside = mx >= posX - 4.0F && mx <= posX + w + 4.0F && my >= posY - 4.0F && my <= posY + h + 4.0F;
                  if (lmb && !prevLmbDrag && inside) {
                     dragging = true;
                     grabDX = mx - posX;
                     grabDY = my - posY;
                  }

                  if (!lmb) {
                     dragging = false;
                  }

                  if (dragging && lmb) {
                     posX = MathUtil.clamp(mx - grabDX, 0.0F, sw - w);
                     posY = MathUtil.clamp(my - grabDY, 0.0F, sh - h);
                  }

                  prevLmbDrag = lmb;
               }

               ImGui.setNextWindowPos(posX - (editMode ? 6.0F : 0.0F), posY - (editMode ? 20.0F : 0.0F), 1);
               ImGui.setNextWindowSize(w + (editMode ? 12.0F : 0.0F), h + (editMode ? 26.0F : 6.0F), 1);
               ImGui.pushStyleVar(2, 0.0F, 0.0F);
               int flags = 795563;
               if (ImGui.begin("##flux_keystrokes", flags)) {
                  ImDrawList draw = ImGui.getWindowDrawList();
                  long handlex = mc.getWindow().getHandle();
                  boolean lmbNow = GLFW.glfwGetMouseButton(handlex, 0) == 1;
                  if (lmbNow && !prevLmb) {
                     L_CLICKS.add(System.currentTimeMillis());
                  }

                  prevLmb = lmbNow;
                  L_CLICKS.removeIf(t -> System.currentTimeMillis() - t > 1000L);
                  boolean rmbNow = GLFW.glfwGetMouseButton(handlex, 1) == 1;
                  if (rmbNow && !prevRmb) {
                     R_CLICKS.add(System.currentTimeMillis());
                  }

                  prevRmb = rmbNow;
                  R_CLICKS.removeIf(t -> System.currentTimeMillis() - t > 1000L);
                  float x0 = posX;
                  float y0 = posY;
                  drawKey(draw, hud, "W", x0 + cell + gap, y0, cell, cell, GLFW.glfwGetKey(handlex, 87) == 1);
                  drawKey(draw, hud, "A", x0, y0 + cell + gap, cell, cell, GLFW.glfwGetKey(handlex, 65) == 1);
                  drawKey(draw, hud, "S", x0 + cell + gap, y0 + cell + gap, cell, cell, GLFW.glfwGetKey(handlex, 83) == 1);
                  drawKey(draw, hud, "D", x0 + (cell + gap) * 2.0F, y0 + cell + gap, cell, cell, GLFW.glfwGetKey(handlex, 68) == 1);
                  if (hud.showMouse.get()) {
                     float mw = (w - gap) / 2.0F;
                     float myx = y0 + (cell + gap) * 2.0F;
                     drawKey(draw, hud, "ЛКМ", x0, myx, mw, cell, lmbNow);
                     drawKey(draw, hud, "ПКМ", x0 + mw + gap, myx, mw, cell, rmbNow);
                     if (hud.showCPS.get()) {
                        String cpsText = L_CLICKS.size() + " | " + R_CLICKS.size() + " CPS";
                        ImVec2 csz = ImGui.calcTextSize(cpsText);
                        float cxx = x0 + (w - csz.x) / 2.0F;
                        float cyx = myx + cell + 5.0F;
                        draw.addText(cxx, cyx, ImColor.rgba(180, 180, 205, 255), cpsText);
                     }
                  }

                  if (editMode) {
                     float pulse = RenderWidgets.pulse(2.5F);
                     draw.addRect(
                        posX - 4.0F,
                        posY - 4.0F,
                        posX + w + 4.0F,
                        posY + h + 4.0F,
                        ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F + 0.4F * pulse),
                        6.0F,
                        0,
                        1.5F
                     );
                  }
               }

               ImGui.end();
               ImGui.popStyleVar();
            }
         }
      }
   }

   private static void drawKey(ImDrawList draw, Keystrokes hud, String label, float x, float y, float w, float h, boolean pressed) {
      float a = PRESS.computeIfAbsent(label, k -> 0.0F);
      a += ((pressed ? 1.0F : 0.0F) - a) * MathUtil.clamp(18.0F * dt, 0.0F, 1.0F);
      PRESS.put(label, a);
      int bg = hud.background.get() ? ImColor.rgba(12, 12, 17, 220) : ImColor.rgba(12, 12, 17, 100);
      int fill = RenderWidgets.interpolateColor(bg, ImGuiTheme.ACCENT_COLOR_DIM, a);
      draw.addRectFilled(x, y, x + w, y + h, fill, 6.0F);
      int border = RenderWidgets.interpolateColor(ImColor.rgba(35, 35, 48, 180), ImGuiTheme.ACCENT_COLOR_HOVER, a);
      draw.addRect(x, y, x + w, y + h, border, 6.0F, 0, 1.0F);
      ImGui.pushFont(ImGuiEngine.getBoldFont());
      ImVec2 tsz = ImGui.calcTextSize(label);
      int textCol = RenderWidgets.interpolateColor(ImColor.rgba(215, 215, 225, 255), ImColor.rgba(255, 255, 255, 255), a);
      draw.addText(x + (w - tsz.x) / 2.0F, y + (h - tsz.y) / 2.0F, textCol, label);
      ImGui.popFont();
   }
}
