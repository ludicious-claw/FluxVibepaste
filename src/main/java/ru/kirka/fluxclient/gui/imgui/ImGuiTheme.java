package ru.kirka.fluxclient.gui.imgui;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiStyle;
import java.awt.Color;

public final class ImGuiTheme {
   public static int ACCENT_COLOR = ImColor.rgba(235, 45, 60, 255);
   public static int ACCENT_COLOR_HOVER = ImColor.rgba(255, 80, 95, 255);
   public static int ACCENT_COLOR_DIM = ImColor.rgba(62, 14, 20, 255);
   public static int ACCENT_GLOW = ImColor.rgba(235, 45, 60, 75);
   public static int ACCENT_HOVER = ACCENT_COLOR_HOVER;
   public static int ACCENT_BG = ACCENT_COLOR_DIM;
   public static String currentTheme = "Crimson Blood";

   private ImGuiTheme() {
   }

   public static Color primaryColor() {
      return new Color(ACCENT_COLOR & 0xFF, ACCENT_COLOR >> 8 & 0xFF, ACCENT_COLOR >> 16 & 0xFF, 255);
   }

   public static void setTheme(String name, int r, int g, int b) {
      currentTheme = name;
      ACCENT_COLOR = ImColor.rgba(r, g, b, 255);
      ACCENT_COLOR_HOVER = ImColor.rgba(Math.min(255, r + 35), Math.min(255, g + 35), Math.min(255, b + 35), 255);
      ACCENT_COLOR_DIM = ImColor.rgba(r / 4, g / 4, b / 4, 255);
      ACCENT_GLOW = ImColor.rgba(r, g, b, 75);
      ACCENT_HOVER = ACCENT_COLOR_HOVER;
      ACCENT_BG = ACCENT_COLOR_DIM;
      apply();
   }

   public static void apply() {
      ImGuiStyle style = ImGui.getStyle();
      style.setWindowRounding(14.0F);
      style.setChildRounding(12.0F);
      style.setFrameRounding(8.0F);
      style.setPopupRounding(10.0F);
      style.setScrollbarRounding(6.0F);
      style.setGrabRounding(7.0F);
      style.setWindowBorderSize(0.0F);
      style.setChildBorderSize(1.0F);
      style.setFrameBorderSize(0.0F);
      style.setWindowPadding(14.0F, 14.0F);
      style.setFramePadding(8.0F, 5.0F);
      style.setItemSpacing(8.0F, 6.0F);
      style.setScrollbarSize(4.0F);
      style.setAntiAliasedLines(true);
      style.setAntiAliasedLinesUseTex(true);
      style.setAntiAliasedFill(true);
      style.setCurveTessellationTol(0.5F);
      style.setColor(2, 0.04F, 0.04F, 0.06F, 0.94F);
      style.setColor(3, 0.06F, 0.06F, 0.08F, 0.88F);
      style.setColor(5, 0.18F, 0.18F, 0.24F, 0.65F);
      style.setColor(4, 0.06F, 0.06F, 0.08F, 0.98F);
      style.setColor(7, 0.08F, 0.08F, 0.11F, 0.9F);
      style.setColor(8, 0.14F, 0.14F, 0.18F, 1.0F);
      style.setColor(9, 0.18F, 0.18F, 0.24F, 1.0F);
      style.setColor(21, 0.08F, 0.08F, 0.12F, 0.85F);
      style.setColor(22, 0.16F, 0.16F, 0.24F, 1.0F);
      style.setColor(23, 0.22F, 0.22F, 0.32F, 1.0F);
      style.setColor(0, 0.95F, 0.95F, 0.98F, 1.0F);
      style.setColor(1, 0.45F, 0.47F, 0.55F, 1.0F);
      float r = (ACCENT_COLOR & 0xFF) / 255.0F;
      float g = (ACCENT_COLOR >> 8 & 0xFF) / 255.0F;
      float b = (ACCENT_COLOR >> 16 & 0xFF) / 255.0F;
      style.setColor(18, r, g, b, 1.0F);
      style.setColor(19, r, g, b, 1.0F);
      style.setColor(20, Math.min(1.0F, r + 0.15F), Math.min(1.0F, g + 0.15F), Math.min(1.0F, b + 0.15F), 1.0F);
      style.setColor(24, 0.12F, 0.12F, 0.16F, 0.85F);
      style.setColor(25, 0.18F, 0.18F, 0.24F, 1.0F);
      style.setColor(26, 0.24F, 0.24F, 0.32F, 1.0F);
      style.setColor(27, 0.16F, 0.16F, 0.22F, 0.8F);
      style.setColor(28, r, g, b, 0.6F);
      style.setColor(29, r, g, b, 1.0F);
      style.setColor(14, 0.02F, 0.02F, 0.03F, 0.0F);
      style.setColor(15, 0.22F, 0.22F, 0.28F, 0.75F);
      style.setColor(16, r, g, b, 0.8F);
      style.setColor(17, r, g, b, 1.0F);
      style.setColor(54, 0.0F, 0.0F, 0.0F, 0.65F);
   }
}
