package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.hud.ArmorHUD;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class ArmorHUDWindow {
   public static float posX = -1.0F;
   public static float posY = 320.0F;
   private static boolean dragging = false;
   private static boolean prevLmb = false;
   private static float grabDX = 0.0F;
   private static float grabDY = 0.0F;
   private static final String[] PIECES = new String[]{"Шлем", "Нагрудник", "Поножи", "Ботинки"};

   private ArmorHUDWindow() {
   }

   public static void render() {
      render(FluxContext.get().getModuleManager().getModule(ArmorHUD.class));
   }

   public static void render(ArmorHUD hud) {
      if (hud != null && hud.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.player != null) {
            boolean editMode = mc.currentScreen instanceof ChatScreen;
            if (mc.currentScreen == null || editMode) {
               float cw = 38.0F;
               float ch = 52.0F;
               float gap = 6.0F;
               float w = cw * 4.0F + gap * 3.0F;
               float sw = ImGui.getIO().getDisplaySizeX();
               float sh = ImGui.getIO().getDisplaySizeY();
               if (posX < 0.0F) {
                  posX = sw / 2.0F - w / 2.0F;
               }

               if (!editMode) {
                  dragging = false;
                  prevLmb = false;
               }

               if (editMode) {
                  long handle = mc.getWindow().getHandle();
                  boolean lmb = GLFW.glfwGetMouseButton(handle, 0) == 1;
                  double[] cx = new double[1];
                  double[] cy = new double[1];
                  GLFW.glfwGetCursorPos(handle, cx, cy);
                  float mx = (float)cx[0];
                  float my = (float)cy[0];
                  boolean inside = mx >= posX - 4.0F && mx <= posX + w + 4.0F && my >= posY - 4.0F && my <= posY + ch + 4.0F;
                  if (lmb && !prevLmb && inside) {
                     dragging = true;
                     grabDX = mx - posX;
                     grabDY = my - posY;
                  }

                  if (!lmb) {
                     dragging = false;
                  }

                  if (dragging && lmb) {
                     posX = MathUtil.clamp(mx - grabDX, 0.0F, sw - w);
                     posY = MathUtil.clamp(my - grabDY, 0.0F, sh - ch);
                  }

                  prevLmb = lmb;
               }

               ImGui.setNextWindowPos(posX - (editMode ? 6.0F : 0.0F), posY - (editMode ? 20.0F : 0.0F), 1);
               ImGui.setNextWindowSize(w + (editMode ? 12.0F : 0.0F), ch + (editMode ? 26.0F : 0.0F), 1);
               ImGui.pushStyleVar(2, 0.0F, 0.0F);
               int flags = 795563;
               if (ImGui.begin("##flux_armorhud", flags)) {
                  ImDrawList draw = ImGui.getWindowDrawList();

                  for (int i = 0; i < 4; i++) {
                     ItemStack st = (ItemStack)mc.player.getInventory().armor.get(3 - i);
                     float x = posX + i * (cw + gap);
                     float y = posY;
                     if (hud.background.get()) {
                        draw.addRectFilled(x, y, x + cw, y + ch, ImColor.rgba(12, 12, 17, 220), 8.0F);
                     }

                     draw.addRect(x, y, x + cw, y + ch, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F), 8.0F, 0, 1.0F);
                     String letter = PIECES[i].substring(0, 1);
                     ImVec2 lsz = ImGui.calcTextSize(letter);
                     draw.addText(x + (cw - lsz.x) / 2.0F, y + 6.0F, ImColor.rgba(200, 200, 215, 255), letter);
                     if (!st.isEmpty()) {
                        float pct = 1.0F - (float)st.getDamage() / Math.max(1, st.getMaxDamage());
                        int col = pct > 0.6F
                           ? ColorUtil.lerpColor(ImColor.rgba(240, 200, 60, 255), ImColor.rgba(50, 220, 120, 255), (pct - 0.6F) / 0.4F)
                           : ColorUtil.lerpColor(ImColor.rgba(235, 45, 60, 255), ImColor.rgba(240, 200, 60, 255), pct / 0.6F);
                        float barW = cw - 12.0F;
                        float barH = 4.0F;
                        float bx = x + 6.0F;
                        float by = y + 26.0F;
                        draw.addRectFilled(bx, by, bx + barW, by + barH, ImColor.rgba(26, 26, 36, 255), 2.0F);
                        draw.addRectFilled(bx, by, bx + barW * pct, by + barH, col, 2.0F);
                        if (hud.showPercent.get()) {
                           String t = Math.round(pct * 100.0F) + "%";
                           ImVec2 tsz = ImGui.calcTextSize(t);
                           draw.addText(x + (cw - tsz.x) / 2.0F, y + 34.0F, col, t);
                        }
                     } else {
                        ImVec2 esz = ImGui.calcTextSize("-");
                        draw.addText(x + (cw - esz.x) / 2.0F, y + 33.0F, ImColor.rgba(100, 100, 120, 255), "-");
                     }
                  }

                  if (editMode) {
                     float pulse = RenderWidgets.pulse(2.5F);
                     draw.addRect(
                        posX - 4.0F,
                        posY - 4.0F,
                        posX + w + 4.0F,
                        posY + ch + 4.0F,
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
}
