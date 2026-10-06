package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.render.esp.EspAnimationState;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.color.Colors;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;

public class ESP extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final ModeSetting boxMode = new ModeSetting("Тип бокса", "Стиль 2D обводки", "Box", "Box", "Corners", "Off");
   public final BooleanSetting boxFill = new BooleanSetting("Заливка", "Полупрозрачная заливка внутри бокса", true);
   public final BooleanSetting healthBar = new BooleanSetting("Здоровье", "Вертикальная полоска HP слева", true);
   public final BooleanSetting armorBar = new BooleanSetting("Броня", "Отображение экипированных предметов", true);
   public final BooleanSetting offscreenArrows = new BooleanSetting("Стрелочки", "Указатели на игроков вне экрана", true);
   public final NumberSetting arrowRadius = new NumberSetting("Радиус стрелок", "Расстояние от центра экрана", 80.0F, 40.0F, 180.0F, 5.0F);
   public final BooleanSetting targetPlayers = new BooleanSetting("Игроки", true);
   public final BooleanSetting targetMobs = new BooleanSetting("Мобы", false);
   public final BooleanSetting targetAnimals = new BooleanSetting("Животные", false);
   public final BooleanSetting targetItems = new BooleanSetting("Предметы", true);
   public final BooleanSetting themeSync = new BooleanSetting("Цвет темы", true);
   public final ColorSetting playerColor = new ColorSetting("Цвет игроков", "Цвет боксов игроков", new Color(168, 85, 247, 255));
   public final ColorSetting mobColor = new ColorSetting("Цвет мобов", "Цвет враждебных мобов", new Color(255, 80, 80, 255));
   public final ColorSetting itemColor = new ColorSetting("Цвет предметов", "Цвет дропа", new Color(255, 215, 0, 255));
   private final Map<Integer, EspAnimationState> animations = new HashMap<>();
   private final EventListener<PreHudRenderEvent> onHudRender = event -> {
      if (mc.world != null && mc.player != null) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
         DrawContext context = event.getContext();

         for (Entity entity : mc.world.getEntities()) {
            if (this.isValidTarget(entity)) {
               this.renderEntityEsp(context, entity, tickDelta);
            }
         }

         if (this.offscreenArrows.get()) {
            this.renderOffscreenArrows(context, tickDelta);
         }
      }
   };

   public ESP() {
      super("ESP", "Подсветка игроков, мобов и предметов (2D боксы, стрелочки, HP)", Category.RENDER, -1);
      this.registerSetting(this.boxMode);
      this.registerSetting(this.boxFill);
      this.registerSetting(this.healthBar);
      this.registerSetting(this.armorBar);
      this.registerSetting(this.offscreenArrows);
      this.registerSetting(this.arrowRadius);
      this.registerSetting(this.targetPlayers);
      this.registerSetting(this.targetMobs);
      this.registerSetting(this.targetAnimals);
      this.registerSetting(this.targetItems);
      this.registerSetting(this.themeSync);
      this.registerSetting(this.playerColor);
      this.registerSetting(this.mobColor);
      this.registerSetting(this.itemColor);
   }

   private boolean isValidTarget(Entity entity) {
      if (entity == null || !entity.isAlive()) {
         return false;
      } else if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) {
         return false;
      } else if (entity instanceof PlayerEntity) {
         return this.targetPlayers.get();
      } else if (entity instanceof Monster) {
         return this.targetMobs.get();
      } else if (entity instanceof AnimalEntity) {
         return this.targetAnimals.get();
      } else {
         return entity instanceof ItemEntity ? this.targetItems.get() : false;
      }
   }

   private ColorRGBA getEntityColor(Entity entity) {
      if (this.themeSync.get()) {
         return Colors.getAccent();
      } else if (entity instanceof PlayerEntity) {
         Color c = this.playerColor.get();
         return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
      } else if (entity instanceof Monster) {
         Color c = this.mobColor.get();
         return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
      } else if (entity instanceof ItemEntity) {
         Color c = this.itemColor.get();
         return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
      } else {
         return Colors.getAccent();
      }
   }

   private void renderEntityEsp(DrawContext context, Entity entity, float tickDelta) {
      Box box = entity.getBoundingBox();
      Vec3d min = new Vec3d(
         MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - box.getLengthX() / 2.0,
         MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()),
         MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - box.getLengthZ() / 2.0
      );
      Vec3d max = min.add(box.getLengthX(), box.getLengthY(), box.getLengthZ());
      Vec3d[] corners = new Vec3d[]{
         new Vec3d(min.x, min.y, min.z),
         new Vec3d(min.x, max.y, min.z),
         new Vec3d(max.x, min.y, min.z),
         new Vec3d(max.x, max.y, min.z),
         new Vec3d(min.x, min.y, max.z),
         new Vec3d(min.x, max.y, max.z),
         new Vec3d(max.x, min.y, max.z),
         new Vec3d(max.x, max.y, max.z)
      };
      float left = Float.MAX_VALUE;
      float top = Float.MAX_VALUE;
      float right = Float.MIN_VALUE;
      float bottom = Float.MIN_VALUE;

      for (Vec3d corner : corners) {
         Vec2f screen = Utils.worldToScreen(corner);
         if (screen == null) {
            return;
         }

         left = Math.min(left, screen.x);
         top = Math.min(top, screen.y);
         right = Math.max(right, screen.x);
         bottom = Math.max(bottom, screen.y);
      }

      float width = right - left;
      float height = bottom - top;
      ColorRGBA color = this.getEntityColor(entity);
      if (!this.boxMode.is("Off")) {
         if (this.boxFill.get()) {
            DrawUtility.drawRect(context.getMatrices(), left, top, width, height, color.withAlpha(35.0F));
         }

         if (this.boxMode.is("Box")) {
            DrawUtility.drawRoundedBorder(context.getMatrices(), left, top, width, height, 1.0F, BorderRadius.ZERO, color);
         } else if (this.boxMode.is("Corners")) {
            float cornerLen = Math.min(width, height) * 0.25F;
            DrawUtility.drawRect(context.getMatrices(), left, top, cornerLen, 1.0F, color);
            DrawUtility.drawRect(context.getMatrices(), left, top, 1.0F, cornerLen, color);
            DrawUtility.drawRect(context.getMatrices(), right - cornerLen, top, cornerLen, 1.0F, color);
            DrawUtility.drawRect(context.getMatrices(), right - 1.0F, top, 1.0F, cornerLen, color);
            DrawUtility.drawRect(context.getMatrices(), left, bottom - 1.0F, cornerLen, 1.0F, color);
            DrawUtility.drawRect(context.getMatrices(), left, bottom - cornerLen, 1.0F, cornerLen, color);
            DrawUtility.drawRect(context.getMatrices(), right - cornerLen, bottom - 1.0F, cornerLen, 1.0F, color);
            DrawUtility.drawRect(context.getMatrices(), right - 1.0F, bottom - cornerLen, 1.0F, cornerLen, color);
         }
      }

      if (this.healthBar.get() && entity instanceof LivingEntity living) {
         float hp = living.getHealth();
         float maxHp = living.getMaxHealth();
         float hpPercent = MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);
         float barX = left - 4.0F;
         float barW = 2.0F;
         DrawUtility.drawRect(context.getMatrices(), barX, top, barW, height, new ColorRGBA(20.0F, 20.0F, 20.0F, 180.0F));
         float fillH = height * hpPercent;
         ColorRGBA hpColor = hpPercent > 0.6F
            ? new ColorRGBA(94.0F, 252.0F, 132.0F, 255.0F)
            : (hpPercent > 0.3F ? new ColorRGBA(255.0F, 214.0F, 92.0F, 255.0F) : new ColorRGBA(255.0F, 85.0F, 85.0F, 255.0F));
         DrawUtility.drawRect(context.getMatrices(), barX, bottom - fillH, barW, fillH, hpColor);
      }

      if (this.armorBar.get() && entity instanceof LivingEntity living) {
         int i = 0;
         float iconSize = 10.0F;

         for (ItemStack armor : living.getArmorItems()) {
            if (!armor.isEmpty()) {
               context.getMatrices().push();
               context.getMatrices().translate(right + 2.0F, top + i * (iconSize + 2.0F), 0.0F);
               context.getMatrices().scale(0.65F, 0.65F, 1.0F);
               context.drawItem(armor, 0, 0);
               context.getMatrices().pop();
               i++;
            }
         }
      }
   }

   private void renderOffscreenArrows(DrawContext context, float tickDelta) {
      float centerX = mc.getWindow().getScaledWidth() / 2.0F;
      float centerY = mc.getWindow().getScaledHeight() / 2.0F;
      float radius = this.arrowRadius.get();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && player.isAlive()) {
            Vec3d targetPos = Utils.getInterpolatedPos(player, tickDelta).add(0.0, player.getStandingEyeHeight(), 0.0);
            Vec2f screen = Utils.worldToScreen(targetPos);
            if (screen == null
               || !(screen.x >= 0.0F)
               || !(screen.x <= mc.getWindow().getScaledWidth())
               || !(screen.y >= 0.0F)
               || !(screen.y <= mc.getWindow().getScaledHeight())) {
               double dx = targetPos.x - mc.player.getX();
               double dz = targetPos.z - mc.player.getZ();
               double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
               double angle = Math.toRadians(yaw - mc.player.getYaw());
               float arrowX = (float)(centerX + Math.cos(angle) * radius);
               float arrowY = (float)(centerY + Math.sin(angle) * radius);
               ColorRGBA col = this.getEntityColor(player);
               context.getMatrices().push();
               context.getMatrices().translate(arrowX, arrowY, 0.0F);
               context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)angle + (float) (Math.PI / 2)));
               DrawUtility.drawRoundedRect(context.getMatrices(), -3.0F, -3.0F, 6.0F, 6.0F, BorderRadius.all(2.0F), col);
               context.getMatrices().pop();
            }
         }
      }
   }
}
