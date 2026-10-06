package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.config.Setting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.hud.ArrayListHUD;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class ArrayListWindow {
   private static final Map<String, Float> ENTRY_ANIM = new HashMap<>();
   private static float smoothWidth = -1.0F;
   private static double lastTime = -1.0;
   private static float dt = 0.016F;
   public static float posX = -1.0F;
   public static float posY = 40.0F;
   private static boolean dragging = false;
   private static boolean prevLmb = false;
   private static float grabDX = 0.0F;
   private static float grabDY = 0.0F;
   private static final float ROW_H = 18.0F;
   private static final float ROW_GAP = 2.0F;

   private ArrayListWindow() {
   }

   public static void render() {
      render(FluxContext.get().getModuleManager().getModule(ArrayListHUD.class));
   }

   public static void render(ArrayListHUD hud) {
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
               float screenW = ImGui.getIO().getDisplaySizeX();
               float screenH = ImGui.getIO().getDisplaySizeY();
               if (posX < 0.0F) {
                  posX = screenW - 170.0F;
               }

               if (!editMode) {
                  dragging = false;
                  prevLmb = false;
               }

               List<Module> all = new ArrayList<>();

               for (Category c : Category.values()) {
                  all.addAll(FluxContext.get().getModuleManager().getModulesByCategory(c));
               }

               float speed = hud.animation.get() ? 12.0F : 1000.0F;
               List<ArrayListWindow.RowItem> displayed = new ArrayList<>();

               for (Module m : all) {
                  String key = m.getName();
                  float target = m.isEnabled() ? 1.0F : 0.0F;
                  float cur = ENTRY_ANIM.getOrDefault(key, 0.0F);
                  float next = cur + (target - cur) * MathUtil.clamp(speed * dt, 0.0F, 1.0F);
                  if (next > 0.01F) {
                     ENTRY_ANIM.put(key, next);
                     String suffix = getModuleSuffix(m);
                     displayed.add(new ArrayListWindow.RowItem(m, m.getName(), suffix));
                  } else if (target > 0.0F) {
                     ENTRY_ANIM.put(key, next);
                  } else {
                     ENTRY_ANIM.remove(key);
                  }
               }

               if (hud.sorting.is("По алфавиту")) {
                  displayed.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));
               } else {
                  displayed.sort((a, b) -> Float.compare(calcRowWidth(b), calcRowWidth(a)));
               }

               float maxTextW = 50.0F;

               for (ArrayListWindow.RowItem r : displayed) {
                  maxTextW = Math.max(maxTextW, calcRowWidth(r));
               }

               float targetW = maxTextW + 16.0F;
               smoothWidth = smoothWidth < 0.0F ? targetW : smoothWidth + (targetW - smoothWidth) * MathUtil.clamp(10.0F * dt, 0.0F, 1.0F);
               float listW = smoothWidth;
               float listH = displayed.isEmpty() ? 0.0F : displayed.size() * 20.0F - 2.0F;
               if (editMode) {
                  long handle = mc.getWindow().getHandle();
                  boolean lmb = GLFW.glfwGetMouseButton(handle, 0) == 1;
                  double[] cx = new double[1];
                  double[] cy = new double[1];
                  GLFW.glfwGetCursorPos(handle, cx, cy);
                  float mx = (float)cx[0];
                  float my = (float)cy[0];
                  boolean inside = mx >= posX - 4.0F && mx <= posX + listW + 4.0F && my >= posY - 4.0F && my <= posY + listH + 4.0F;
                  if (lmb && !prevLmb && inside) {
                     dragging = true;
                     grabDX = mx - posX;
                     grabDY = my - posY;
                  }

                  if (!lmb) {
                     dragging = false;
                  }

                  if (dragging && lmb) {
                     posX = MathUtil.clamp(mx - grabDX, 0.0F, Math.max(0.0F, screenW - listW));
                     posY = MathUtil.clamp(my - grabDY, 0.0F, Math.max(0.0F, screenH - listH));
                  }

                  prevLmb = lmb;
               }

               float pad = editMode ? 6.0F : 0.0F;
               float winX = posX - pad;
               float winY = posY - pad;
               float winW = Math.max(2.0F, listW + pad * 2.0F);
               float winH = Math.max(2.0F, listH + pad * 2.0F + 16.0F);
               ImGui.setNextWindowPos(winX, winY, 1);
               ImGui.setNextWindowSize(winW, winH, 1);
               ImGui.pushStyleVar(2, 0.0F, 0.0F);
               int flags = 795579;
               if (ImGui.begin("##flux_arraylist_hud", flags)) {
                  ImDrawList draw = ImGui.getWindowDrawList();
                  int n = displayed.size();

                  for (int i = 0; i < n; i++) {
                     ArrayListWindow.RowItem item = displayed.get(i);
                     float v = ENTRY_ANIM.getOrDefault(item.module.getName(), 0.0F);
                     float ey = posY + i * 20.0F;
                     float ex = posX + (1.0F - v) * (listW + 20.0F);
                     float t = n <= 1 ? 0.0F : (float)i / (n - 1);
                     int accent = RenderWidgets.interpolateColor(ImColor.rgba(255, 65, 80, 255), ImColor.rgba(185, 20, 35, 255), t);
                     if (hud.background.get()) {
                        draw.addRectFilled(ex, ey, posX + listW, ey + 18.0F, ColorUtil.withAlpha(ImColor.rgba(10, 10, 14, 255), 0.65F * v), 3.0F);
                     }

                     draw.addRectFilled(posX + listW - 2.5F, ey, posX + listW, ey + 18.0F, ColorUtil.withAlpha(accent, v), 1.5F);
                     ImGui.pushFont(ImGuiEngine.getBoldFont());
                     draw.addText(ex + 6.0F, ey + 1.0F, ColorUtil.withAlpha(ImColor.rgba(245, 245, 252, 255), v), item.name);
                     if (item.suffix != null && !item.suffix.isEmpty()) {
                        float nameW = ImGui.calcTextSize(item.name).x;
                        draw.addText(ex + 8.0F + nameW, ey + 1.0F, ColorUtil.withAlpha(ImColor.rgba(160, 160, 175, 255), v), item.suffix);
                     }

                     ImGui.popFont();
                  }

                  if (editMode) {
                     float pulse = RenderWidgets.pulse(2.0F);
                     draw.addRect(
                        posX - 4.0F,
                        posY - 4.0F,
                        posX + listW + 4.0F,
                        posY + listH + 4.0F,
                        ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F + 0.4F * pulse),
                        5.0F,
                        0,
                        1.5F
                     );
                     draw.addText(posX, winY + listH + 6.0F, ImColor.rgba(240, 240, 250, 255), "ArrayList: зажмите ЛКМ для перемещения");
                  }
               }

               ImGui.end();
               ImGui.popStyleVar();
            }
         }
      }
   }

   private static String getModuleSuffix(Module m) {
      for (Setting<?> s : m.getSettings()) {
         if (s instanceof ModeSetting mode && s.isVisible()) {
            return " [" + mode.get() + "]";
         }
      }

      return "";
   }

   private static float calcRowWidth(ArrayListWindow.RowItem item) {
      float nw = ImGui.calcTextSize(item.name).x;
      float sw = item.suffix != null && !item.suffix.isEmpty() ? ImGui.calcTextSize(item.suffix).x + 4.0F : 0.0F;
      return nw + sw;
   }

   private static class RowItem {
      final Module module;
      final String name;
      final String suffix;

      RowItem(Module module, String name, String suffix) {
         this.module = module;
         this.name = name;
         this.suffix = suffix;
      }
   }
}
