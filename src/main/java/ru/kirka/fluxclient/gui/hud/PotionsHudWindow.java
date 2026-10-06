package ru.kirka.fluxclient.gui.hud;

import imgui.ImDrawList;
import imgui.ImGui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.hud.PotionsHUD;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class PotionsHudWindow {
   private static final Animation showAnim = new Animation(300.0F, Easing.EASE_OUT_BACK);
   private static final Animation fadeAnim = new Animation(240.0F, Easing.EASE_OUT_QUAD);
   private static final Map<String, PotionsHudWindow.RowFx> rows = new LinkedHashMap<>();
   private static float curH = 0.0F;
   private static double lastT = -1.0;
   private static final float PAD = 12.0F;

   public static void render(boolean isInteractive) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         double now = ImGui.getTime();
         float dt = lastT < 0.0 ? 0.016F : (float)Math.min(0.1, now - lastT);
         lastT = now;
         boolean compact = false;
         boolean bg = true;
         boolean preview = true;

         try {
            PotionsHUD mod = FluxContext.get().getModuleManager().getModule(PotionsHUD.class);
            if (mod != null) {
               compact = mod.style.is("Компакт");
               bg = mod.background.get();
               preview = mod.preview.get();
            }
         } catch (Exception var57) {
         }

         List<PotionsHudWindow.Item> items = new ArrayList<>();

         for (StatusEffectInstance eff : mc.player.getStatusEffects()) {
            PotionsHudWindow.Item it = new PotionsHudWindow.Item();
            int rgb = ((StatusEffect)eff.getEffectType().value()).getColor();
            it.color = ColorUtil.rgba(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255);
            int amp = eff.getAmplifier();
            it.name = ((StatusEffect)eff.getEffectType().value()).getName().getString() + (amp > 0 ? " " + roman(amp + 1) : "");
            it.key = it.name;
            it.dur = eff.getDuration();
            it.infinite = it.dur < 0;
            it.maxDur = Math.max(1, it.dur);
            items.add(it);
         }

         if (items.isEmpty() && preview && isInteractive) {
            items.add(fake("Скорость II", 8171462, 90, 150));
            items.add(fake("Сила I", 9643043, 45, 120));
         }

         boolean active = !items.isEmpty();
         showAnim.setTarget(active ? 1.0F : 0.0F);
         showAnim.update();
         fadeAnim.setTarget(active ? 1.0F : 0.0F);
         fadeAnim.update();
         float fade = MathUtil.clamp(fadeAnim.getValue(), 0.0F, 1.0F);
         float pop = showAnim.getValue();
         if (fade <= 0.01F) {
            rows.clear();
            curH = 0.0F;
         } else {
            Map<String, PotionsHudWindow.Item> alive = new LinkedHashMap<>();

            for (PotionsHudWindow.Item it : items) {
               alive.put(it.key, it);
               PotionsHudWindow.RowFx fx = rows.get(it.key);
               if (fx == null) {
                  fx = new PotionsHudWindow.RowFx();
                  fx.spawnT = now;
                  fx.bar = it.infinite ? 1.0F : MathUtil.clamp((float)it.dur / it.maxDur, 0.0F, 1.0F);
                  fx.maxDur = it.maxDur;
                  rows.put(it.key, fx);
               }

               fx.maxDur = Math.max(fx.maxDur, it.maxDur);
               float target = it.infinite ? 1.0F : MathUtil.clamp((float)it.dur / fx.maxDur, 0.0F, 1.0F);
               fx.bar = MathUtil.damp(fx.bar, target, 10.0F, dt);
            }

            rows.keySet().retainAll(alive.keySet());
            float cardW = compact ? 178.0F : 200.0F;
            float rowH = compact ? 24.0F : 34.0F;
            float headerH = compact ? 26.0F : 32.0F;
            float bottomPad = compact ? 8.0F : 10.0F;
            float targetH = headerH + items.size() * rowH + bottomPad + 24.0F;
            curH = curH <= 0.0F ? targetH : MathUtil.damp(curH, targetH, 12.0F, dt);
            float screenH = ImGui.getIO().getDisplaySizeY();
            ImGui.setNextWindowPos(16.0F, screenH / 2.0F - 70.0F, 4);
            ImGui.setNextWindowSize(cardW + 24.0F, curH, 1);
            ImGui.pushStyleVar(0, fade);
            ImGui.pushStyleVar(2, 0.0F, 0.0F);
            ImGui.pushStyleVar(4, 0.0F);
            int flags = 171;
            if (!isInteractive) {
               flags |= 786944;
            }

            if (ImGui.begin("##FluxPotionsHUD", flags)) {
               ImDrawList draw = ImGui.getWindowDrawList();
               float slideX = (1.0F - pop) * 44.0F;
               float x = ImGui.getWindowPosX() + 12.0F + slideX;
               float y = ImGui.getWindowPosY() + 12.0F;
               float radius = 10.0F;
               if (bg) {
                  draw.addRectFilled(x, y, x + cardW, y + headerH + items.size() * rowH + bottomPad, ColorUtil.rgba(10, 10, 15, 235), radius);
                  draw.addRect(
                     x,
                     y,
                     x + cardW,
                     y + headerH + items.size() * rowH + bottomPad,
                     ColorUtil.lerpColor(ColorUtil.rgba(32, 32, 44, 200), ImGuiTheme.ACCENT_COLOR, 0.35F),
                     radius,
                     0,
                     1.0F
                  );
               }

               float dotPulse = 0.55F + 0.45F * (float)Math.sin(now * 3.0);
               draw.addCircleFilled(x + 14.0F, y + (compact ? 13.0F : 16.0F), 3.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR, dotPulse));
               draw.addText(x + 22.0F, y + (compact ? 6.0F : 8.0F), ColorUtil.rgba(205, 205, 218, 255), compact ? "Зелья" : "Активные зелья");
               String count = String.valueOf(items.size());
               float countW = ImGui.calcTextSize(count).x + 14.0F;
               float badgeX = x + cardW - 10.0F - countW;
               draw.addRectFilled(
                  badgeX,
                  y + (compact ? 5.0F : 8.0F),
                  badgeX + countW,
                  y + (compact ? 19.0F : 22.0F),
                  ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_DIM, 0.85F),
                  9.0F
               );
               draw.addText(badgeX + 7.0F, y + (compact ? 5.5F : 8.5F), ImGuiTheme.ACCENT_COLOR_HOVER, count);
               float divY = y + headerH - 6.0F;
               draw.addLine(x + 10.0F, divY, x + cardW - 10.0F, divY, ColorUtil.rgba(42, 42, 56, 160), 1.0F);
               float sweepX = x + 10.0F + (float)(now * 55.0 % (cardW - 20.0));
               draw.addCircleFilled(sweepX, divY, 1.6F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_HOVER, 0.7F));

               for (int i = 0; i < items.size(); i++) {
                  PotionsHudWindow.Item it = items.get(i);
                  PotionsHudWindow.RowFx fx = rows.get(it.key);
                  if (fx != null) {
                     float rowEase = Easing.EASE_OUT_QUAD.ease(MathUtil.clamp(fade * 1.45F - i * 0.16F, 0.0F, 1.0F));
                     if (!(rowEase <= 0.01F)) {
                        float ry = y + headerH + i * rowH + (1.0F - rowEase) * 26.0F;
                        float st = MathUtil.clamp((float)((now - fx.spawnT) / 0.35), 0.0F, 1.0F);
                        float bounce = Easing.EASE_OUT_BACK.ease(st);
                        boolean expiring = !it.infinite && it.dur > 0 && it.dur < 200;
                        float warnPulse = expiring ? 0.5F + 0.5F * (float)Math.sin(now * 7.0) : 0.0F;
                        float chipR = (compact ? 3.5F : 4.0F) * (0.6F + 0.4F * Math.min(bounce, 1.2F));
                        float chipCx = x + (compact ? 13.0F : 15.0F);
                        float chipCy = ry + (compact ? 12.0F : 11.0F);
                        draw.addCircleFilled(chipCx, chipCy, chipR + 3.0F, ColorUtil.withAlpha(it.color, 0.22F * rowEase));
                        draw.addCircleFilled(chipCx, chipCy, chipR, ColorUtil.withAlpha(it.color, rowEase));
                        float textY = ry + (compact ? 5.0F : 3.0F);
                        draw.addText(x + (compact ? 24.0F : 28.0F), textY, ColorUtil.withAlpha(ColorUtil.rgba(240, 240, 250, 255), rowEase), it.name);
                        String timeStr = it.infinite ? "∞" : fmtTime(it.dur);
                        int timeCol = expiring
                           ? ColorUtil.lerpColor(ImGuiTheme.ACCENT_COLOR_HOVER, ColorUtil.rgba(255, 70, 70, 255), warnPulse)
                           : ImGuiTheme.ACCENT_COLOR_HOVER;
                        float timeW = ImGui.calcTextSize(timeStr).x;
                        draw.addText(x + cardW - 10.0F - timeW, textY, ColorUtil.withAlpha(timeCol, rowEase), timeStr);
                        if (!compact) {
                           float bx = x + 10.0F;
                           float bw = cardW - 20.0F;
                           float by = ry + 21.0F;
                           float bh = 5.0F;
                           draw.addRectFilled(bx, by, bx + bw, by + bh, ColorUtil.withAlpha(ColorUtil.rgba(26, 26, 36, 255), rowEase), 2.5F);
                           float fill = bw * MathUtil.clamp(fx.bar, 0.0F, 1.0F);
                           if (fill > 0.5F) {
                              int barCol = expiring ? ColorUtil.lerpColor(it.color, ColorUtil.rgba(255, 70, 70, 255), warnPulse) : it.color;
                              draw.addRectFilled(bx, by, bx + fill, by + bh, ColorUtil.withAlpha(barCol, rowEase), 2.5F);
                              float hx = bx + (float)(now * 45.0 % (bw + 30.0)) - 15.0F;
                              float sx0 = Math.max(hx - 7.0F, bx);
                              float sx1 = Math.min(hx + 7.0F, bx + fill);
                              if (sx1 > sx0) {
                                 draw.addRectFilled(sx0, by, sx1, by + bh, ColorUtil.rgba(255, 255, 255, (int)(34.0F * rowEase)), 2.0F);
                              }
                           }
                        }
                     }
                  }
               }
            }

            ImGui.end();
            ImGui.popStyleVar(3);
         }
      }
   }

   private static PotionsHudWindow.Item fake(String name, int rgb, int dur, int max) {
      PotionsHudWindow.Item it = new PotionsHudWindow.Item();
      it.key = "preview:" + name;
      it.name = name;
      it.color = ColorUtil.rgba(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255);
      it.dur = dur * 20;
      it.maxDur = max * 20;
      it.infinite = false;
      return it;
   }

   private static String fmtTime(int ticks) {
      int s = Math.max(0, ticks / 20);
      return String.format("%02d:%02d", s / 60, s % 60);
   }

   private static String roman(int n) {
      return switch (n) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(n);
      };
   }

   private static final class Item {
      String key;
      String name;
      int color;
      int dur;
      int maxDur;
      boolean infinite;
   }

   private static final class RowFx {
      float bar = 0.0F;
      int maxDur = 1;
      double spawnT = 0.0;
   }
}
