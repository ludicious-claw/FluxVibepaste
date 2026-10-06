package ru.kirka.fluxclient.gui.hud;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.hud.TargetHUD;
import ru.kirka.fluxclient.gui.imgui.ColorUtil;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.screens.ClickGuiScreen;
import ru.kirka.fluxclient.util.animation.Animation;
import ru.kirka.fluxclient.util.animation.Easing;
import ru.kirka.fluxclient.util.math.MathUtil;

public final class TargetHudWindow {
   private static final Animation showAnim = new Animation(300.0F, Easing.EASE_OUT_BACK);
   private static final Animation fadeAnim = new Animation(220.0F, Easing.EASE_OUT_QUAD);
   private static final Animation switchPop = new Animation(200.0F, Easing.EASE_OUT_BACK);
   private static float hpShown = 20.0F;
   private static float hpFlash = 0.0F;
   private static float distShown = 3.5F;
   private static float ringBurst = 0.0F;
   private static int lastId = Integer.MIN_VALUE;
   private static double lastT = -1.0;
   private static final float PAD = 10.0F;

   public static void render(LivingEntity target) {
      MinecraftClient mc = MinecraftClient.getInstance();
      boolean isInteractive = mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof ChatScreen;
      double now = ImGui.getTime();
      float dt = lastT < 0.0 ? 0.016F : (float)Math.min(0.1, now - lastT);
      lastT = now;
      boolean compact = false;

      try {
         TargetHUD mod = FluxContext.get().getModuleManager().getModule(TargetHUD.class);
         if (mod != null) {
            compact = mod.style.is("Компакт");
         }
      } catch (Exception var52) {
      }

      boolean active = target != null || isInteractive;
      showAnim.setTarget(active ? 1.0F : 0.0F);
      showAnim.update();
      fadeAnim.setTarget(active ? 1.0F : 0.0F);
      fadeAnim.update();
      float fade = MathUtil.clamp(fadeAnim.getValue(), 0.0F, 1.0F);
      float pop = showAnim.getValue();
      if (fade <= 0.01F) {
         lastId = Integer.MIN_VALUE;
      } else {
         int id = target != null ? target.getId() : -1;
         if (active && target != null && id != lastId) {
            lastId = id;
            switchPop.reset(0.0F);
            switchPop.setTarget(1.0F);
            hpFlash = Math.max(hpFlash, 0.6F);
            ringBurst += (float) Math.PI;
            hpShown = target.getHealth();
         }

         if (target == null) {
            lastId = Integer.MIN_VALUE;
         }

         switchPop.update();
         float hp = target != null ? target.getHealth() : 20.0F;
         float maxHp = target != null ? Math.max(1.0F, target.getMaxHealth()) : 20.0F;
         float absorb = target != null ? target.getAbsorptionAmount() : 0.0F;
         float dist = target != null && mc.player != null ? mc.player.distanceTo(target) : 3.5F;
         if (hp < hpShown - 0.4F) {
            hpFlash = 1.0F;
         }

         hpShown = MathUtil.damp(hpShown, hp, 10.0F, dt);
         if (Math.abs(hpShown - hp) < 0.04F) {
            hpShown = hp;
         }

         hpFlash = Math.max(0.0F, hpFlash - dt * 2.4F);
         distShown = MathUtil.damp(distShown, dist, 8.0F, dt);
         ringBurst = Math.max(0.0F, ringBurst - dt * 3.0F);
         float percent = MathUtil.clamp(hpShown / maxHp, 0.0F, 1.0F);
         float contentW = compact ? 220.0F : 260.0F;
         float contentH = compact ? 62.0F : 80.0F;
         float screenW = ImGui.getIO().getDisplaySizeX();
         float screenH = ImGui.getIO().getDisplaySizeY();
         ImGui.setNextWindowPos(screenW / 2.0F + 40.0F, screenH / 2.0F + 40.0F, 4);
         ImGui.setNextWindowSize(contentW + 20.0F, contentH + 20.0F, 1);
         ImGui.pushStyleVar(0, fade);
         ImGui.pushStyleVar(2, 0.0F, 0.0F);
         ImGui.pushStyleVar(4, 0.0F);
         int flags = 171;
         if (!isInteractive) {
            flags |= 786944;
         }

         if (ImGui.begin("##FluxTargetHUD", flags)) {
            ImDrawList draw = ImGui.getWindowDrawList();
            float slideX = (1.0F - pop) * 48.0F;
            float x = ImGui.getWindowPosX() + 10.0F + slideX;
            float y = ImGui.getWindowPosY() + 10.0F;
            float switchDip = (1.0F - MathUtil.clamp(switchPop.getValue(), 0.0F, 1.0F)) * 6.0F;
            y += switchDip;
            boolean lowHp = percent < 0.3F && target != null;
            float lowPulse = lowHp ? 0.5F + 0.5F * (float)Math.sin(now * 7.0) : 0.0F;
            int borderCol = lowHp
               ? ColorUtil.lerpColor(ColorUtil.rgba(40, 40, 52, 220), ImColor.rgba(255, 50, 50, 255), lowPulse)
               : ColorUtil.lerpColor(ColorUtil.rgba(36, 36, 48, 220), ImGuiTheme.ACCENT_COLOR, 0.45F);
            draw.addRectFilled(x, y, x + contentW, y + contentH, ColorUtil.rgba(10, 10, 15, 238), 12.0F);
            draw.addRect(x, y, x + contentW, y + contentH, borderCol, 12.0F, 0, 1.2F);
            String name = target != null ? target.getName().getString() : "Превью цели";
            if (name.length() > 16) {
               name = name.substring(0, 16) + "..";
            }

            float avR = compact ? 13.0F : 17.0F;
            float avPop = 0.75F + 0.25F * MathUtil.clamp(switchPop.getValue(), 0.0F, 1.0F);
            float acx = x + (compact ? 26.0F : 32.0F);
            float acy = y + (compact ? 28.0F : 33.0F);
            draw.addCircleFilled(acx, acy, avR * avPop + 3.5F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_DIM, 0.6F));
            draw.addCircleFilled(acx, acy, avR * avPop, ImGuiTheme.ACCENT_COLOR);
            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            float iw = ImGui.calcTextSize(initial).x;
            draw.addText(acx - iw / 2.0F, acy - 8.0F, ImColor.rgba(255, 255, 255, 255), initial);
            float ringR = avR * avPop + 6.5F;
            float phase = (float)(now * 2.6 + ringBurst * 2.0);
            int segs = 18;
            float arc = (float)Math.toRadians(210.0);

            for (int s = 0; s < segs; s++) {
               float a0 = phase + arc * s / segs;
               float a1 = phase + arc * (s + 1) / segs;
               draw.addLine(
                  acx + (float)Math.cos(a0) * ringR,
                  acy + (float)Math.sin(a0) * ringR,
                  acx + (float)Math.cos(a1) * ringR,
                  acy + (float)Math.sin(a1) * ringR,
                  ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_HOVER, 0.6F + 0.4F * hpFlash),
                  1.8F
               );
            }

            if (hpFlash > 0.01F) {
               draw.addCircle(acx, acy, ringR + 2.0F, ColorUtil.withAlpha(ImColor.rgba(255, 255, 255, 255), hpFlash * 0.85F), 24, 2.0F);
            }

            float tx = x + (compact ? 48.0F : 58.0F);
            draw.addText(tx, y + (compact ? 8.0F : 10.0F), ImColor.rgba(245, 245, 252, 255), name);
            if (!compact) {
               String tag;
               int tagCol;
               if (target instanceof PlayerEntity) {
                  tag = "ИГРОК";
                  tagCol = ImGuiTheme.ACCENT_COLOR_HOVER;
               } else if (target instanceof WitherEntity || target instanceof EnderDragonEntity) {
                  tag = "БОСС";
                  tagCol = ColorUtil.rgba(180, 80, 255, 255);
               } else if (target instanceof HostileEntity) {
                  tag = "МОБ";
                  tagCol = ColorUtil.rgba(255, 90, 90, 255);
               } else if (target instanceof PassiveEntity) {
                  tag = "ЖИВОТНОЕ";
                  tagCol = ColorUtil.rgba(90, 220, 130, 255);
               } else {
                  tag = target == null ? "ПРЕВЬЮ" : "ЦЕЛЬ";
                  tagCol = ColorUtil.rgba(150, 150, 170, 255);
               }

               float tagW = ImGui.calcTextSize(tag).x + 12.0F;
               float tagX = x + contentW - 10.0F - tagW;
               draw.addRectFilled(tagX, y + 10.0F, tagX + tagW, y + 24.0F, ColorUtil.withAlpha(ImGuiTheme.ACCENT_COLOR_DIM, 0.75F), 7.0F);
               draw.addText(tagX + 6.0F, y + 11.5F, tagCol, tag);
            }

            StringBuilder sub = new StringBuilder(String.format("%.1f HP", Math.max(0.0F, hpShown)));
            sub.append("  •  ").append(String.format("%.1f", distShown)).append("m");
            if (target instanceof PlayerEntity p) {
               int ping = getPing(mc, p);
               if (ping > 0) {
                  sub.append("  •  ").append(ping).append("ms");
               }
            }

            draw.addText(tx, y + (compact ? 24.0F : 28.0F), ColorUtil.rgba(145, 145, 165, 255), sub.toString());
            float bx = x + (compact ? 10.0F : 12.0F);
            float bw = contentW - (compact ? 20.0F : 24.0F);
            float by = y + (compact ? 46.0F : 56.0F);
            float bh = compact ? 8.0F : 10.0F;
            float rounding = bh / 2.0F;
            draw.addRectFilled(bx, by, bx + bw, by + bh, ColorUtil.rgba(20, 20, 28, 255), rounding);
            float fill = bw * percent;
            if (fill > 0.5F) {
               int hpCol = percent > 0.5F
                  ? ColorUtil.lerpColor(ImColor.rgba(235, 45, 60, 255), ImColor.rgba(255, 100, 115, 255), (percent - 0.5F) / 0.5F)
                  : ColorUtil.lerpColor(ImColor.rgba(160, 20, 30, 255), ImColor.rgba(235, 45, 60, 255), percent / 0.5F);
               draw.addRectFilled(bx, by, bx + fill, by + bh, hpCol, rounding);
               if (hpFlash > 0.01F) {
                  draw.addRectFilled(bx, by, bx + fill, by + bh, ColorUtil.rgba(255, 255, 255, (int)(150.0F * hpFlash)), rounding);
               }

               float hx = bx + (float)(now * 65.0 % (bw + 30.0)) - 15.0F;
               float sx0 = Math.max(hx - 8.0F, bx);
               float sx1 = Math.min(hx + 8.0F, bx + fill);
               if (sx1 > sx0) {
                  draw.addRectFilled(sx0, by, sx1, by + bh, ColorUtil.rgba(255, 255, 255, 30), rounding * 0.7F);
               }
            }

            float absorbFrac = MathUtil.clamp(absorb / maxHp, 0.0F, 1.0F);
            if (absorbFrac > 0.01F) {
               draw.addRectFilled(bx, by - 5.0F, bx + bw * absorbFrac, by - 2.0F, ColorUtil.rgba(250, 210, 70, 235), 1.5F);
            }
         }

         ImGui.end();
         ImGui.popStyleVar(3);
      }
   }

   private static int getPing(MinecraftClient mc, PlayerEntity player) {
      try {
         if (mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
               return entry.getLatency();
            }
         }
      } catch (Exception var3) {
      }

      return 0;
   }
}
