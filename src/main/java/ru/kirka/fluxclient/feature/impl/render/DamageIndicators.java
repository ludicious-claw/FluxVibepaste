package ru.kirka.fluxclient.feature.impl.render;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.render.WorldOverlays;

public class DamageIndicators extends Module {
   public final BooleanSetting showCrits = new BooleanSetting("Критические удары", "Выделять криты золотым цветом", true);
   public final BooleanSetting heartIcon = new BooleanSetting("Иконка сердца", "Добавлять значок сердечка ❤", true);
   public final NumberSetting duration = new NumberSetting("Длительность", "Время показа цифр (сек)", 1.2F, 0.5F, 3.0F, 0.1F);
   public final NumberSetting scale = new NumberSetting("Масштаб текста", "Размер всплывающих цифр", 1.1F, 0.6F, 2.0F, 0.1F);
   public final ColorSetting normalDamageColor = new ColorSetting("Цвет урона", "Цвет стандартного удара", new Color(239, 68, 68, 255));
   public final ColorSetting critDamageColor = new ColorSetting("Цвет крита", "Цвет критического урона", new Color(245, 158, 11, 255));
   private static final List<DamageIndicators.DamageTag> tags = new ArrayList<>();
   private final Map<LivingEntity, Float> lastHealths = new HashMap<>();
   private static final Random random = new Random();

   public DamageIndicators() {
      super("DamageIndicators", "Всплывающие 3D цифры урона над мобами и игроками", Category.RENDER, -1);
      this.registerSetting(this.showCrits);
      this.registerSetting(this.heartIcon);
      this.registerSetting(this.duration);
      this.registerSetting(this.scale);
      this.registerSetting(this.normalDamageColor);
      this.registerSetting(this.critDamageColor);
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && living != mc.player && living.isAlive()) {
               float curHp = living.getHealth();
               Float prevHp = this.lastHealths.get(living);
               this.lastHealths.put(living, curHp);
               if (prevHp != null && curHp < prevHp) {
                  float diff = prevHp - curHp;
                  if (diff >= 0.1F && mc.player.distanceTo(living) <= 12.0F) {
                     boolean isCrit = this.showCrits.get() && mc.player.fallDistance > 0.0F && !mc.player.isOnGround();
                     this.spawnDamage(living, diff, isCrit);
                  }
               }
            }
         }
      } else {
         this.lastHealths.clear();
      }
   }

   private void spawnDamage(LivingEntity target, float amount, boolean isCrit) {
      Vec3d pos = target.getPos()
         .add((random.nextDouble() - 0.5) * 0.45, target.getHeight() * 0.65 + random.nextDouble() * 0.35, (random.nextDouble() - 0.5) * 0.45);
      String text;
      if (isCrit) {
         text = String.format("CRIT -%.1f", amount);
      } else {
         text = String.format("-%.1f", amount);
      }

      if (this.heartIcon.get()) {
         text = text + " HP";
      }

      tags.add(new DamageIndicators.DamageTag(pos, text, isCrit, System.currentTimeMillis()));
   }

   public void render2D(ImDrawList draw) {
      if (!tags.isEmpty() && mc.player != null) {
         long now = System.currentTimeMillis();
         long maxLifetime = (long)(this.duration.get() * 1000.0F);
         tags.removeIf(txx -> now - txx.spawnTime > maxLifetime);
         if (!tags.isEmpty()) {
            float[] sp = new float[2];
            float baseScale = this.scale.get();

            for (DamageIndicators.DamageTag t : tags) {
               float progress = (float)(now - t.spawnTime) / (float)maxLifetime;
               if (!(progress > 1.0F)) {
                  double yOffset = Math.sin(progress * Math.PI * 0.5) * 0.85;
                  Vec3d renderWorldPos = t.worldPos.add(0.0, yOffset, 0.0);
                  if (WorldOverlays.worldToScreen(renderWorldPos, sp)) {
                     float a = 1.0F;
                     if (progress > 0.6F) {
                        a = 1.0F - (progress - 0.6F) / 0.4F;
                     }

                     Color c = t.isCrit ? this.critDamageColor.get() : this.normalDamageColor.get();
                     int textColor = ImColor.rgba(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255.0F));
                     int shadowColor = ImColor.rgba(0, 0, 0, (int)(a * 190.0F));
                     float s = baseScale * (t.isCrit ? 1.25F : 1.0F);
                     float textSize = 15.0F * s;
                     float textW = ImGui.calcTextSize(t.text).x * (textSize / 17.0F);
                     float tx = sp[0] - textW / 2.0F;
                     float ty = sp[1];
                     draw.addText(ImGui.getFont(), (int)textSize, tx + 1.2F, ty + 1.2F, shadowColor, t.text);
                     draw.addText(ImGui.getFont(), (int)textSize, tx, ty, textColor, t.text);
                  }
               }
            }
         }
      }
   }

   private static class DamageTag {
      Vec3d worldPos;
      String text;
      boolean isCrit;
      long spawnTime;

      DamageTag(Vec3d worldPos, String text, boolean isCrit, long spawnTime) {
         this.worldPos = worldPos;
         this.text = text;
         this.isCrit = isCrit;
         this.spawnTime = spawnTime;
      }
   }
}
