package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;
import ru.kirka.fluxclient.gui.screens.ClickGuiScreen;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class InfoBarWindow {
   public static float posX = 16.0F;
   public static float posY = 16.0F;
   private static boolean dragging = false;
   private static float grabDX = 0.0F;
   private static float grabDY = 0.0F;
   private static boolean prevLmb = false;

   public static void render(boolean isInteractive) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         boolean editMode = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
         String name = mc.player.getName().getString();
         int fps = mc.getCurrentFps();
         int ping = getPing(mc);
         int posXInt = (int)Math.floor(mc.player.getX());
         int posYInt = (int)Math.floor(mc.player.getY());
         int posZInt = (int)Math.floor(mc.player.getZ());
         String server = mc.isInSingleplayer() ? "Singleplayer" : (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Server");
         float pill1H = 28.0F;
         float pillRadius = 8.0F;
         float logoW = 32.0F;
         String infoText = name + "   " + fps + " FPS   " + ping + " ms   " + server;
         float textW = ImGui.calcTextSize(infoText).x;
         float infoW = textW + 80.0F;
         String coordsText = posXInt + " X  " + posYInt + " Y  " + posZInt + " Z";
         float coordsW = ImGui.calcTextSize(coordsText).x + 38.0F;
         float totalW = Math.max(logoW + 6.0F + infoW, coordsW);
         float totalH = pill1H + 6.0F + 25.0F;
         float screenW = ImGui.getIO().getDisplaySizeX();
         float screenH = ImGui.getIO().getDisplaySizeY();
         if (editMode) {
            long handle = mc.getWindow().getHandle();
            boolean lmb = GLFW.glfwGetMouseButton(handle, 0) == 1;
            double[] cx = new double[1];
            double[] cy = new double[1];
            GLFW.glfwGetCursorPos(handle, cx, cy);
            float mx = (float)cx[0];
            float my = (float)cy[0];
            boolean inside = mx >= posX - 4.0F && mx <= posX + totalW + 4.0F && my >= posY - 4.0F && my <= posY + totalH + 4.0F;
            if (lmb && !prevLmb && inside) {
               dragging = true;
               grabDX = mx - posX;
               grabDY = my - posY;
            }

            if (!lmb) {
               dragging = false;
            }

            if (dragging && lmb) {
               posX = MathUtil.clamp(mx - grabDX, 0.0F, Math.max(0.0F, screenW - totalW));
               posY = MathUtil.clamp(my - grabDY, 0.0F, Math.max(0.0F, screenH - totalH));
            }

            prevLmb = lmb;
         }

         ImGui.setNextWindowPos(posX, posY, 1);
         ImGui.setNextWindowSize(totalW + 20.0F, totalH + (editMode ? 24.0F : 10.0F), 1);
         ImGui.pushStyleVar(2, 0.0F, 0.0F);
         ImGui.pushStyleVar(4, 0.0F);
         int flags = 795563;
         if (ImGui.begin("##FluxInfoBar", flags)) {
            ImDrawList draw = ImGui.getWindowDrawList();
            float x = posX;
            float y = posY;
            draw.addRectFilled(x, y, x + logoW, y + pill1H, ImColor.rgba(12, 12, 17, 240), pillRadius);
            draw.addRect(x, y, x + logoW, y + pill1H, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.5F), pillRadius, 0, 1.0F);
            drawNeonLogoF(draw, x + 6.0F, y + 4.0F, 20.0F);
            float infoX = x + logoW + 6.0F;
            draw.addRectFilled(infoX, y, infoX + infoW, y + pill1H, ImColor.rgba(12, 12, 17, 240), pillRadius);
            draw.addRect(infoX, y, infoX + infoW, y + pill1H, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F), pillRadius, 0, 1.0F);
            drawVectorUser(draw, infoX + 9.0F, y + 8.0F);
            draw.addText(infoX + 24.0F, y + 6.5F, ImColor.rgba(245, 245, 252, 255), name);
            float monX = infoX + 28.0F + ImGui.calcTextSize(name).x + 12.0F;
            drawVectorScreen(draw, monX, y + 9.0F);
            draw.addText(monX + 17.0F, y + 6.5F, ImColor.rgba(245, 245, 252, 255), fps + " FPS");
            float sigX = monX + 17.0F + ImGui.calcTextSize(fps + " FPS").x + 12.0F;
            drawVectorSignal(draw, sigX, y + 9.0F);
            draw.addText(sigX + 15.0F, y + 6.5F, ImColor.rgba(245, 245, 252, 255), ping + " ms");
            float srvX = sigX + 15.0F + ImGui.calcTextSize(ping + " ms").x + 12.0F;
            draw.addText(srvX, y + 6.5F, ImGuiTheme.ACCENT_COLOR_HOVER, server);
            float pill2Y = y + pill1H + 6.0F;
            draw.addRectFilled(x, pill2Y, x + coordsW, pill2Y + 25.0F, ImColor.rgba(12, 12, 17, 240), pillRadius);
            draw.addRect(x, pill2Y, x + coordsW, pill2Y + 25.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.35F), pillRadius, 0, 1.0F);
            drawVectorCompass(draw, x + 7.0F, pill2Y + 6.0F);
            draw.addText(x + 24.0F, pill2Y + 5.0F, ImColor.rgba(245, 245, 252, 255), coordsText);
            if (editMode) {
               float pulse = RenderWidgets.pulse(2.5F);
               draw.addRect(
                  posX - 4.0F,
                  posY - 4.0F,
                  posX + totalW + 4.0F,
                  posY + totalH + 4.0F,
                  ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.4F + 0.4F * pulse),
                  6.0F,
                  0,
                  1.5F
               );
            }
         }

         ImGui.end();
         ImGui.popStyleVar(2);
      }
   }

   private static void drawNeonLogoF(ImDrawList draw, float x, float y, float size) {
      int neon = ImGuiTheme.ACCENT_COLOR;
      int glow = ImGuiTheme.ACCENT_COLOR_DIM;
      draw.addLine(x + size * 0.2F, y + size * 0.85F, x + size * 0.2F, y + size * 0.2F, glow, 3.2F);
      draw.addLine(x + size * 0.2F, y + size * 0.2F, x + size * 0.85F, y + size * 0.2F, glow, 3.2F);
      draw.addLine(x + size * 0.2F, y + size * 0.85F, x + size * 0.65F, y + size * 0.45F, glow, 3.0F);
      draw.addLine(x + size * 0.2F, y + size * 0.85F, x + size * 0.2F, y + size * 0.2F, neon, 1.8F);
      draw.addLine(x + size * 0.2F, y + size * 0.2F, x + size * 0.85F, y + size * 0.2F, neon, 1.8F);
      draw.addLine(x + size * 0.85F, y + size * 0.2F, x + size * 0.68F, y + size * 0.38F, neon, 1.8F);
      draw.addLine(x + size * 0.68F, y + size * 0.38F, x + size * 0.42F, y + size * 0.38F, neon, 1.8F);
      float ax = x + size * 0.65F;
      float ay = y + size * 0.45F;
      draw.addLine(x + size * 0.22F, y + size * 0.82F, ax, ay, neon, 1.8F);
      draw.addLine(ax, ay, ax - size * 0.22F, ay, neon, 1.8F);
      draw.addLine(ax, ay, ax, ay + size * 0.22F, neon, 1.8F);
      draw.addLine(x + size * 0.45F, y + size * 0.68F, x + size * 0.72F, y + size * 0.68F, neon, 1.6F);
   }

   private static void drawVectorUser(ImDrawList draw, float x, float y) {
      draw.addCircle(x + 5.0F, y + 4.0F, 3.5F, ImColor.rgba(215, 215, 230, 255), 10, 1.2F);
      draw.addRect(x + 1.0F, y + 8.5F, x + 9.0F, y + 12.0F, ImColor.rgba(215, 215, 230, 255), 2.0F, 0, 1.2F);
   }

   private static void drawVectorScreen(ImDrawList draw, float x, float y) {
      draw.addRect(x, y, x + 11.0F, y + 8.0F, ImColor.rgba(215, 215, 230, 255), 2.0F, 0, 1.2F);
      draw.addLine(x + 5.5F, y + 8.0F, x + 5.5F, y + 10.5F, ImColor.rgba(215, 215, 230, 255), 1.2F);
      draw.addLine(x + 3.0F, y + 10.5F, x + 8.0F, y + 10.5F, ImColor.rgba(215, 215, 230, 255), 1.2F);
   }

   private static void drawVectorSignal(ImDrawList draw, float x, float y) {
      draw.addRectFilled(x, y + 6.0F, x + 2.0F, y + 9.0F, ImColor.rgba(215, 215, 230, 255));
      draw.addRectFilled(x + 3.0F, y + 3.5F, x + 5.0F, y + 9.0F, ImColor.rgba(215, 215, 230, 255));
      draw.addRectFilled(x + 6.0F, y + 1.0F, x + 8.0F, y + 9.0F, ImColor.rgba(215, 215, 230, 255));
   }

   private static void drawVectorCompass(ImDrawList draw, float x, float y) {
      draw.addCircle(x + 6.0F, y + 6.0F, 5.5F, ImColor.rgba(215, 215, 230, 255), 12, 1.2F);
      draw.addLine(x + 3.5F, y + 8.5F, x + 8.5F, y + 3.5F, ImGuiTheme.ACCENT_COLOR, 1.6F);
   }

   private static int getPing(MinecraftClient mc) {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         if (entry != null) {
            return entry.getLatency();
         }
      }

      return 0;
   }
}
