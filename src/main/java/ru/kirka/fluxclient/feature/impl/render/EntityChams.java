package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.Render3DUtil;

public class EntityChams extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Стиль подсветки", "Уголки", "Уголки", "Неон", "Заливка", "Сетка", "Призрак");
   public final ModeSetting targets = new ModeSetting("Цели", "Кого подсвечивать", "Все", "Все", "Только игроки", "Только мобы");
   public final ColorSetting playerColor = new ColorSetting("Цвет игроков", "Цвет подсветки игроков", new Color(244, 63, 94, 220));
   public final ColorSetting mobColor = new ColorSetting("Цвет мобов", "Цвет подсветки мобов", new Color(234, 179, 8, 220));
   public final BooleanSetting healthColor = new BooleanSetting("По здоровью", "Цвет зависит от остатка HP", false);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Видно сквозь препятствия", true);
   public final BooleanSetting rainbow = new BooleanSetting("Радужный градиент", "Плавная смена цветов", false);
   public final NumberSetting lineWidth = new NumberSetting("Толщина сетки", "Толщина линий каркаса", 2.0F, 0.5F, 5.0F, 0.5F);

   public EntityChams() {
      super("EntityChams", "Неоновая 3D подсветка и каркас игроков и мобов сквозь стены", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.targets);
      this.registerSetting(this.playerColor);
      this.registerSetting(this.mobColor);
      this.registerSetting(this.healthColor);
      this.registerSetting(this.throughWalls);
      this.registerSetting(this.rainbow);
      this.registerSetting(this.lineWidth);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.world != null && mc.player != null) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
         String targetMode = this.targets.get();
         String renderMode = this.mode.get();
         boolean wall = this.throughWalls.get();
         float width = this.lineWidth.get();

         for (Entity entity : mc.world.getEntities()) {
            if (entity != mc.player && entity instanceof LivingEntity living && living.isAlive()) {
               boolean isPlayer = entity instanceof PlayerEntity;
               boolean isMob = entity instanceof HostileEntity || entity instanceof PassiveEntity;
               if ((!targetMode.equals("Только игроки") || isPlayer) && (!targetMode.equals("Только мобы") || isMob)) {
                  double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
                  double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
                  double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
                  Box bb = entity.getBoundingBox();
                  double halfW = (bb.maxX - bb.minX) * 0.5 + 0.05;
                  double h = bb.maxY - bb.minY + 0.08;
                  Box renderBox = new Box(
                     x - halfW - cameraPos.x, y - cameraPos.y, z - halfW - cameraPos.z, x + halfW - cameraPos.x, y + h - cameraPos.y, z + halfW - cameraPos.z
                  );
                  Color col;
                  if (this.healthColor.get()) {
                     float hpPct = Math.max(0.0F, Math.min(1.0F, living.getHealth() / living.getMaxHealth()));
                     col = new Color(
                        (int)((1.0F - hpPct) * 239.0F + hpPct * 16.0F),
                        (int)((1.0F - hpPct) * 68.0F + hpPct * 185.0F),
                        (int)((1.0F - hpPct) * 68.0F + hpPct * 129.0F),
                        230
                     );
                  } else if (this.rainbow.get()) {
                     float hue = ((float)(System.currentTimeMillis() % 4000L) / 4000.0F + entity.getId() % 10 * 0.1F) % 1.0F;
                     col = new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
                  } else {
                     col = isPlayer ? this.playerColor.get() : this.mobColor.get();
                  }

                  Color fill = new Color(col.getRed(), col.getGreen(), col.getBlue(), 60);
                  switch (renderMode) {
                     case "Уголки":
                        Render3DUtil.drawCornerBox(matrices, renderBox, col, width, 0.25F, wall);
                        break;
                     case "Неон":
                        Render3DUtil.drawBox(matrices, renderBox, col, fill, width, wall);
                        break;
                     case "Сетка":
                        Render3DUtil.drawBoxOutline(matrices, renderBox, col, width, wall);
                        break;
                     case "Заливка":
                        Render3DUtil.drawBoxFill(matrices, renderBox, col, wall);
                        break;
                     case "Призрак":
                        float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.005 + entity.getId()) * 0.3 + 0.7);
                        Color pCol = new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(col.getAlpha() * pulse * 0.6F));
                        Render3DUtil.drawBox(matrices, renderBox, col, pCol, width, wall);
                  }
               }
            }
         }
      }
   }
}
