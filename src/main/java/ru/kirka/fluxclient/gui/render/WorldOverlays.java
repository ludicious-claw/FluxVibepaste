package ru.kirka.fluxclient.gui.render;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.feature.impl.render.CustomCrosshair;
import ru.kirka.fluxclient.feature.impl.render.DamageIndicators;
import ru.kirka.fluxclient.feature.impl.render.NameTagsPlus;
import ru.kirka.fluxclient.feature.impl.render.Tracers;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.gui.imgui.RenderWidgets;

public final class WorldOverlays {
   private static final float FONT = 12.0F;
   private static final float FONT_BASE = 17.0F;

   private WorldOverlays() {
   }

   public static void render() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && mc.player != null) {
         if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
            ImDrawList draw = ImGui.getForegroundDrawList();
            Tracers tr = FluxContext.get().getModuleManager().getModule(Tracers.class);
            NameTagsPlus nt = FluxContext.get().getModuleManager().getModule(NameTagsPlus.class);
            CustomCrosshair cc = FluxContext.get().getModuleManager().getModule(CustomCrosshair.class);
            DamageIndicators di = FluxContext.get().getModuleManager().getModule(DamageIndicators.class);
            if (tr != null && tr.isEnabled()) {
               drawTracers(draw, tr);
            }

            if (nt != null && nt.isEnabled()) {
               drawNameTags(draw, nt);
            }

            if (cc != null && cc.isEnabled()) {
               drawCrosshair(draw, cc);
            }

            if (di != null && di.isEnabled()) {
               di.render2D(draw);
            }
         }
      }
   }

   public static boolean worldToScreen(Vec3d pos, float[] out) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Camera cam = mc.gameRenderer.getCamera();
      Vec3d cp = cam.getPos();
      double yr = Math.toRadians(cam.getYaw());
      double pr = Math.toRadians(cam.getPitch());
      double fx = -Math.sin(yr) * Math.cos(pr);
      double fy = -Math.sin(pr);
      double fz = Math.cos(yr) * Math.cos(pr);
      double rx = -Math.cos(yr);
      double rz = -Math.sin(yr);
      double ux = -rz * fy;
      double uy = rz * fx - rx * fz;
      double uz = rx * fy;
      double dx = pos.x - cp.x;
      double dy = pos.y - cp.y;
      double dz = pos.z - cp.z;
      double zc = dx * fx + dy * fy + dz * fz;
      if (zc <= 0.08) {
         return false;
      } else {
         double xc = dx * rx + dz * rz;
         double yc = dx * ux + dy * uy + dz * uz;
         double sw = ImGui.getIO().getDisplaySizeX();
         double sh = ImGui.getIO().getDisplaySizeY();
         double scale = sh / 2.0 / Math.tan(Math.toRadians(((Integer)mc.options.getFov().getValue()).intValue()) / 2.0);
         out[0] = (float)(sw / 2.0 + xc / zc * scale);
         out[1] = (float)(sh / 2.0 - yc / zc * scale);
         return true;
      }
   }

   private static boolean wants(Entity e, Tracers tr) {
      if (!(e instanceof PlayerEntity p)) {
         if (e instanceof HostileEntity) {
            return tr.monsters.get();
         } else {
            return e instanceof PassiveEntity ? tr.animals.get() : false;
         }
      } else {
         return tr.players.get() && p != MinecraftClient.getInstance().player;
      }
   }

   private static void drawTracers(ImDrawList draw, Tracers tr) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float sw = ImGui.getIO().getDisplaySizeX();
      float sh = ImGui.getIO().getDisplaySizeY();
      float ox = sw / 2.0F;
      float oy = tr.origin.is("Низ экрана") ? sh : sh / 2.0F;
      float[] sp = new float[2];

      for (Entity e : mc.world.getEntities()) {
         if (e instanceof LivingEntity && wants(e, tr)) {
            Vec3d p = e.getPos().add(0.0, e.getHeight() / 2.0F, 0.0);
            if (worldToScreen(p, sp)) {
               float dist = mc.player.distanceTo(e);
               float a = tr.opacity.get();
               if (tr.distanceFade.get()) {
                  a *= Math.max(0.18F, Math.min(1.0F, 1.0F - (dist - 5.0F) / 70.0F));
               }

               boolean isCurrentTarget = Aura.target == e;
               int col;
               if (isCurrentTarget) {
                  col = ImColor.rgba(245, 158, 11, 255);
               } else if (e instanceof PlayerEntity) {
                  col = ImGuiTheme.ACCENT_COLOR_HOVER;
               } else if (e instanceof HostileEntity) {
                  col = ImColor.rgba(239, 68, 68, 255);
               } else {
                  col = ImColor.rgba(16, 185, 129, 255);
               }

               draw.addLine(ox, oy, sp[0], sp[1], withAlpha(col, a), tr.thickness.get());
               if (tr.targetDot.get()) {
                  draw.addCircleFilled(sp[0], sp[1], tr.thickness.get() * 2.2F, withAlpha(col, a * 0.45F));
                  draw.addCircleFilled(sp[0], sp[1], tr.thickness.get() * 1.1F, withAlpha(ImColor.rgba(255, 255, 255, 255), a));
               }
            }
         }
      }
   }

   private static void drawNameTags(ImDrawList draw, NameTagsPlus nt) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Camera cam = mc.gameRenderer.getCamera();
      Vec3d cp = cam.getPos();
      float[] sp = new float[2];

      for (Entity e : mc.world.getEntities()) {
         boolean isPlayer = e instanceof PlayerEntity;
         if (e instanceof LivingEntity liv && (isPlayer || nt.monsters.get()) && liv != mc.player) {
            Vec3d head = e.getPos().add(0.0, e.getHeight() + 0.45, 0.0);
            if (worldToScreen(head, sp)) {
               float dist = (float)cp.distanceTo(head);
               float distScale = Math.max(0.68F, Math.min(1.22F, 9.5F / Math.max(4.5F, (float)Math.sqrt(dist * 5.0F))));
               float s = nt.scale.get() * distScale;
               String name = liv.getName().getString();
               float nameW = ImGui.calcTextSize(name).x * 0.7058824F * s;
               boolean hasArmor = nt.armor.get() && isPlayer;
               boolean hasHeld = nt.heldItem.get() && !liv.getMainHandStack().isEmpty();
               float minW = 76.0F * s;
               if (hasArmor) {
                  minW = Math.max(minW, 82.0F * s);
               }

               float w = Math.max(nameW + 24.0F * s, minW);
               float h = 18.0F * s;
               if (hasArmor) {
                  h += 11.0F * s;
               }

               if (hasHeld) {
                  h += 13.0F * s;
               }

               if (nt.hpBar.get()) {
                  h += 7.0F * s;
               }

               if (nt.info.get()) {
                  h += 13.0F * s;
               }

               float x = sp[0] - w / 2.0F;
               float y = sp[1] - h;
               draw.addRectFilled(x, y, x + w, y + h, ImColor.rgba(10, 10, 16, 230), 6.0F * s);
               if (nt.borderGlow.get()) {
                  boolean isTarget = Aura.target == liv;
                  int borderCol = isTarget ? ImColor.rgba(245, 158, 11, 240) : withAlpha(ImGuiTheme.ACCENT_COLOR, 0.85F);
                  draw.addRect(x, y, x + w, y + h, borderCol, 6.0F * s, 0, 1.2F * s);
               }

               draw.addText(ImGui.getFont(), (int)(12.0F * s), x + w / 2.0F - nameW / 2.0F, y + 4.0F * s, ImColor.rgba(245, 245, 252, 255), name);
               float cy = y + 17.0F * s;
               if (hasArmor) {
                  PlayerEntity p = (PlayerEntity)liv;
                  float startArmorX = x + w / 2.0F - 36.0F * s;

                  for (int i = 3; i >= 0; i--) {
                     ItemStack st = (ItemStack)p.getInventory().armor.get(i);
                     float cx = startArmorX + (3 - i) * 19 * s;
                     if (st.isEmpty()) {
                        draw.addRect(cx, cy, cx + 15.0F * s, cy + 6.0F * s, ImColor.rgba(50, 50, 65, 160), 2.0F * s, 0, 1.0F);
                     } else {
                        float pct = 1.0F - (float)st.getDamage() / Math.max(1, st.getMaxDamage());
                        int dc = RenderWidgets.interpolateColor(ImColor.rgba(239, 68, 68, 255), ImColor.rgba(16, 185, 129, 255), pct);
                        draw.addRectFilled(cx, cy, cx + 15.0F * s, cy + 6.0F * s, ImColor.rgba(25, 25, 35, 255), 2.0F * s);
                        draw.addRectFilled(cx, cy, cx + 15.0F * s * pct, cy + 6.0F * s, dc, 2.0F * s);
                        draw.addRect(cx, cy, cx + 15.0F * s, cy + 6.0F * s, withAlpha(dc, 0.7F), 2.0F * s, 0, 1.0F);
                     }
                  }

                  cy += 10.0F * s;
               }

               if (hasHeld) {
                  ItemStack mainHand = liv.getMainHandStack();
                  String itemTitle = mainHand.getName().getString();
                  if (mainHand.getCount() > 1) {
                     itemTitle = itemTitle + " x" + mainHand.getCount();
                  }

                  float itemW = ImGui.calcTextSize(itemTitle).x * 0.7058824F * 0.78F * s;
                  draw.addText(ImGui.getFont(), (int)(10.0F * s), x + w / 2.0F - itemW / 2.0F, cy, ImColor.rgba(216, 180, 254, 255), itemTitle);
                  cy += 12.0F * s;
               }

               if (nt.hpBar.get()) {
                  float maxHp = Math.max(1.0F, liv.getMaxHealth());
                  float curHp = Math.max(0.0F, Math.min(maxHp, liv.getHealth()));
                  float pct = curHp / maxHp;
                  float barX = x + 5.0F * s;
                  float barW = w - 10.0F * s;
                  draw.addRectFilled(barX, cy, barX + barW, cy + 4.5F * s, ImColor.rgba(25, 25, 35, 255), 2.0F * s);
                  int hpColor = pct > 0.6F
                     ? RenderWidgets.interpolateColor(ImColor.rgba(234, 179, 8, 255), ImColor.rgba(16, 185, 129, 255), (pct - 0.6F) / 0.4F)
                     : RenderWidgets.interpolateColor(ImColor.rgba(239, 68, 68, 255), ImColor.rgba(234, 179, 8, 255), pct / 0.6F);
                  draw.addRectFilled(barX, cy, barX + barW * pct, cy + 4.5F * s, hpColor, 2.0F * s);
                  cy += 7.0F * s;
               }

               if (nt.info.get()) {
                  float hpVal = liv.getHealth();
                  String hpStr = String.format("%.1f HP", hpVal);
                  String infoStr = hpStr;
                  if (isPlayer) {
                     PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(liv.getUuid());
                     if (entry != null) {
                        int latency = entry.getLatency();
                        infoStr = hpStr + "  •  " + latency + "ms";
                     }
                  }

                  infoStr = infoStr + "  •  " + String.format("%.0fm", dist);
                  float iw = ImGui.calcTextSize(infoStr).x * 0.7058824F * 0.82F * s;
                  draw.addText(ImGui.getFont(), (int)(10.0F * s), x + w / 2.0F - iw / 2.0F, cy, ImColor.rgba(180, 180, 205, 255), infoStr);
               }
            }
         }
      }
   }

   private static void drawCrosshair(ImDrawList draw, CustomCrosshair cc) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float cx = ImGui.getIO().getDisplaySizeX() / 2.0F;
      float cy = ImGui.getIO().getDisplaySizeY() / 2.0F;
      boolean onTarget = cc.targetGlow.get() && Aura.target != null;
      float t = (float)ImGui.getTime();
      float pulse = onTarget ? 1.5F + (float)Math.sin(t * 8.0F) : 0.0F;
      int col = onTarget ? ImGuiTheme.ACCENT_COLOR_HOVER : ImColor.rgba(245, 245, 250, 255);
      float gap = cc.gap.get() + pulse;
      float len = cc.len.get();
      float th = cc.thick.get();
      String var12 = cc.style.get();
      switch (var12) {
         case "Точка":
            draw.addCircleFilled(cx, cy, th, col);
            break;
         case "Круг":
            draw.addCircle(cx, cy, gap + 3.0F, col, 0, th);
            break;
         case "Уголки":
            float r = gap + len;
            draw.addLine(cx - r, cy - r, cx - r + len * 0.6F, cy - r, col, th);
            draw.addLine(cx - r, cy - r, cx - r, cy - r + len * 0.6F, col, th);
            draw.addLine(cx + r, cy - r, cx + r - len * 0.6F, cy - r, col, th);
            draw.addLine(cx + r, cy - r, cx + r, cy - r + len * 0.6F, col, th);
            draw.addLine(cx - r, cy + r, cx - r + len * 0.6F, cy + r, col, th);
            draw.addLine(cx - r, cy + r, cx - r, cy + r - len * 0.6F, col, th);
            draw.addLine(cx + r, cy + r, cx + r - len * 0.6F, cy + r, col, th);
            draw.addLine(cx + r, cy + r, cx + r, cy + r - len * 0.6F, col, th);
            break;
         default:
            draw.addLine(cx, cy - gap - len, cx, cy - gap, col, th);
            draw.addLine(cx, cy + gap, cx, cy + gap + len, col, th);
            draw.addLine(cx - gap - len, cy, cx - gap, cy, col, th);
            draw.addLine(cx + gap, cy, cx + gap + len, cy, col, th);
      }
   }

   private static int withAlpha(int col, float a) {
      int r = col & 0xFF;
      int g = col >> 8 & 0xFF;
      int b = col >> 16 & 0xFF;
      int alpha = (int)(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return ImColor.rgba(r, g, b, alpha);
   }
}
