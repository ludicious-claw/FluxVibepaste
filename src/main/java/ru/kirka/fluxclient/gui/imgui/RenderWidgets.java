package ru.kirka.fluxclient.gui.imgui;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import java.util.HashMap;
import java.util.Map;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class RenderWidgets {
   private static final Map<String, Animation> GENERIC_ANIMS = new HashMap<>();
   private static final Map<String, Animation> SWITCH_ANIMS = new HashMap<>();

   public static float animValue(String id, float target, float durationMs, Easing easing) {
      Animation anim = GENERIC_ANIMS.computeIfAbsent(id, k -> {
         Animation a = new Animation(durationMs, easing);
         a.reset(target);
         return a;
      });
      anim.setTarget(MathUtil.clamp(target, 0.0F, 1.0F));
      anim.update();
      return anim.getValue();
   }

   public static float hoverAnim(String id, boolean hovered) {
      return animValue(id, hovered ? 1.0F : 0.0F, 150.0F, Easing.EASE_OUT_QUAD);
   }

   public static void resetAnim(String id, float durationMs, Easing easing) {
      Animation anim = GENERIC_ANIMS.computeIfAbsent(id, k -> new Animation(durationMs, easing));
      anim.reset(0.0F);
   }

   public static int fade(int col, float a) {
      return ColorUtil.withAlpha(col, (col >>> 24) / 255.0F * MathUtil.clamp(a, 0.0F, 1.0F));
   }

   public static float pulse(float speed) {
      return 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * 2.0 * speed);
   }

   public static float entrance(long baseStamp, int index, int delayPerIndexMs, int durationMs) {
      float t = (float)(System.currentTimeMillis() - baseStamp - (long)index * delayPerIndexMs) / durationMs;
      t = MathUtil.clamp(t, 0.0F, 1.0F);
      return Easing.EASE_OUT_QUAD.ease(t);
   }

   public static int interpolateColor(int c1, int c2, float t) {
      int r1 = c1 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int b1 = c1 >> 16 & 0xFF;
      int a1 = c1 >> 24 & 0xFF;
      int r2 = c2 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b2 = c2 >> 16 & 0xFF;
      int a2 = c2 >> 24 & 0xFF;
      int r = (int)MathUtil.lerp(r1, r2, t);
      int g = (int)MathUtil.lerp(g1, g2, t);
      int b = (int)MathUtil.lerp(b1, b2, t);
      int a = (int)MathUtil.lerp(a1, a2, t);
      return ImColor.rgba(r, g, b, a);
   }

   public static boolean drawSwitch(ImDrawList draw, String id, boolean state, float x, float y, float width, float height) {
      Animation anim = SWITCH_ANIMS.computeIfAbsent(id, k -> new Animation(170.0F, Easing.EASE_OUT_QUAD));
      anim.setTarget(state ? 1.0F : 0.0F);
      anim.update();
      float progress = anim.getValue();
      float radius = height / 2.0F;
      ImGui.setCursorScreenPos(x, y);
      boolean clicked = ImGui.invisibleButton(id, width, height);
      int offBg = ImColor.rgba(24, 24, 32, 255);
      int onBg = ImGuiTheme.ACCENT_COLOR;
      int bg = interpolateColor(offBg, onBg, progress);
      if (progress > 0.05F) {
         draw.addRectFilled(x, y + 1.0F, x + width, y + height + 2.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.25F * progress), radius);
      }

      draw.addRectFilled(x, y, x + width, y + height, bg, radius);
      draw.addRect(x, y, x + width, y + height, interpolateColor(ImColor.rgba(50, 50, 65, 180), ImGuiTheme.ACCENT_COLOR_HOVER, progress), radius, 0, 1.0F);
      float pad = 2.0F;
      float circleRadius = radius - pad;
      float currentX = MathUtil.lerp(x + radius, x + width - radius, progress);
      draw.addCircleFilled(currentX, y + radius, circleRadius, ImColor.rgba(255, 255, 255, 255));
      return clicked ? !state : state;
   }

   public static void drawColumnCard(ImDrawList draw, float x, float y, float w, float h, float alpha) {
      float r = 16.0F;
      draw.addRectFilled(x, y, x + w, y + h, fade(ImColor.rgba(10, 10, 15, 215), alpha), r);
      draw.addRectFilled(x, y, x + w, y + h, fade(ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, 0.04F), alpha), r);
      draw.addRect(x, y, x + w, y + h, fade(ImColor.rgba(255, 255, 255, 18), alpha), r, 0, 1.0F);
   }

   public static float drawDarkPanel(ImDrawList draw, String id, float x, float y, float w, float h, boolean hovered, boolean pressed, float alpha) {
      float hoverT = animValue(id + "_hov", hovered ? 1.0F : 0.0F, 170.0F, Easing.EASE_OUT_QUAD);
      float pressT = animValue(id + "_prs", pressed ? 1.0F : 0.0F, 110.0F, Easing.EASE_OUT_QUAD);
      float s = 1.0F + 0.035F * hoverT - 0.025F * pressT;
      float cx = x + w / 2.0F;
      float cy = y + h / 2.0F;
      float sx = cx - w * s / 2.0F;
      float sy = cy - h * s / 2.0F;
      float sw = w * s;
      float sh = h * s;
      float r = Math.min(sh / 2.0F - 1.0F, 12.0F);
      draw.addRectFilled(sx - 1.0F, sy + 3.0F, sx + sw + 1.0F, sy + sh + 4.0F, fade(ImColor.rgba(0, 0, 0, 95), alpha), r + 2.0F);
      int base = ImColor.rgba(12, 12, 16, 220);
      int fill = interpolateColor(base, ImGuiTheme.ACCENT_COLOR_DIM, 0.2F + 0.5F * hoverT);
      draw.addRectFilled(sx, sy, sx + sw, sy + sh, fade(fill, alpha), r);
      int frame = interpolateColor(ImColor.rgba(36, 36, 46, 240), ImGuiTheme.ACCENT_COLOR_HOVER, hoverT * hoverT);
      draw.addRect(sx, sy, sx + sw, sy + sh, fade(frame, alpha), r, 0, 1.0F + hoverT * 0.5F);
      return hoverT;
   }
}
