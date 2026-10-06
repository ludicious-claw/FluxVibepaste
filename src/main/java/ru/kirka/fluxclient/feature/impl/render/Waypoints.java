package ru.kirka.fluxclient.feature.impl.render;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.KeyEvent;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class Waypoints extends Module {
   public final BooleanSetting showDistance = new BooleanSetting("Дистанция", "Показывать дистанцию до метки", true);
   public final BooleanSetting sound = new BooleanSetting("Звук", "Звуковой сигнал при создании метки", true);
   private final Map<String, Waypoints.Waypoint> waypoints = new ConcurrentHashMap<>();
   private final EventListener<KeyEvent> onKey = event -> {
      if (event.getKey() == 86 && event.getAction() == 1 && mc.currentScreen == null && mc.player != null && mc.world != null) {
         Vec3d start = mc.player.getEyePos();
         Vec3d direction = mc.player.getRotationVec(1.0F);
         Vec3d end = start.add(direction.multiply(200.0));
         PlayerEntity targetPlayer = null;
         double closestDistance = Double.MAX_VALUE;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player) {
               Box hitbox = player.getBoundingBox().expand(0.3);
               Vec3d hit = (Vec3d)hitbox.raycast(start, end).orElse(null);
               if (hit != null) {
                  double dist = start.distanceTo(hit);
                  if (dist < closestDistance && dist <= 200.0) {
                     closestDistance = dist;
                     targetPlayer = player;
                  }
               }
            }
         }

         if (targetPlayer != null) {
            UUID playerUUID = targetPlayer.getUuid();
            if (this.waypoints.values().stream().anyMatch(w -> playerUUID.equals(w.playerUUID))) {
               return;
            }

            String name = targetPlayer.getName().getString();
            Vec3d pos = targetPlayer.getPos();
            this.addWaypoint(name, pos, true, playerUUID);
            return;
         }

         HitResult raycastResult = mc.player.raycast(200.0, 1.0F, false);
         if (raycastResult.getType() == Type.BLOCK && raycastResult instanceof BlockHitResult blockHit) {
            Vec3d pos = blockHit.getPos();
            String baseName = "Метка";
            String name = baseName;
            int counter = 1;

            while (this.waypoints.containsKey(name)) {
               name = baseName + " " + counter++;
            }

            this.addWaypoint(name, pos, true, null);
         }
      }
   };
   private final EventListener<PreHudRenderEvent> onHudRender = event -> {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrices = event.getContext().getMatrices();
         float tickDelta = event.getTickDelta();
         long currentTime = System.currentTimeMillis();
         this.waypoints.entrySet().removeIf(entry -> {
            Waypoints.Waypoint waypointx = entry.getValue();
            if (waypointx.temp && currentTime - waypointx.creationTime > 15000L) {
               return true;
            } else {
               if (waypointx.playerUUID != null) {
                  PlayerEntity player = mc.world.getPlayerByUuid(waypointx.playerUUID);
                  if (player == null) {
                     return true;
                  }

                  Vec3d targetPos = Utils.getInterpolatedPos(player, tickDelta);
                  waypointx.pos = waypointx.pos.lerp(targetPos, MathHelper.clamp(0.2F * tickDelta, 0.0F, 1.0F));
               }

               return false;
            }
         });

         for (Waypoints.Waypoint waypoint : this.waypoints.values()) {
            Vec3d renderPos = waypoint.pos.add(0.0, 0.5, 0.0);
            Vec2f screenPos = Utils.worldToScreen(renderPos);
            if (screenPos != null) {
               float distance = (float)mc.player.getPos().distanceTo(renderPos);
               float scale = MathHelper.clamp(1.0F - distance / 30.0F, 0.5F, 1.0F);
               matrices.push();
               matrices.translate(screenPos.x, screenPos.y, 0.0F);
               matrices.scale(scale, scale, 1.0F);
               String text = waypoint.name + (this.showDistance.get() ? String.format(Locale.ROOT, " (%.1fm)", distance) : "");
               float fontHeight = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).height() : 11.0F;
               float textWidth = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).width(text) : 40.0F;
               float badgeWidth = textWidth + 24.0F;
               float badgeHeight = fontHeight + 8.0F;
               float x = -badgeWidth / 2.0F;
               float y = -badgeHeight / 2.0F;
               DrawUtility.drawRoundedRect(matrices, x, y, badgeWidth, badgeHeight, BorderRadius.all(4.0F), new ColorRGBA(15.0F, 15.0F, 15.0F, 180.0F));
               ColorRGBA borderColor = waypoint.playerUUID != null ? new ColorRGBA(255.0F, 60.0F, 60.0F, 200.0F) : new ColorRGBA(0.0F, 190.0F, 255.0F, 200.0F);
               DrawUtility.drawRoundedBorder(matrices, x, y, badgeWidth, badgeHeight, 1.0F, BorderRadius.all(4.0F), borderColor);
               matrices.push();
               matrices.translate(x + 2.0F, y + (badgeHeight - 12.0F) / 2.0F, 0.0F);
               matrices.scale(0.75F, 0.75F, 1.0F);
               event.getContext().drawItem(waypoint.playerUUID != null ? Items.PLAYER_HEAD.getDefaultStack() : Items.COMPASS.getDefaultStack(), 0, 0);
               matrices.pop();
               if (Fonts.MEDIUM != null) {
                  event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x + 16.0F, y + (badgeHeight - fontHeight) / 2.0F, ColorRGBA.WHITE);
               }

               matrices.pop();
            }
         }
      }
   };

   public Waypoints() {
      super("Waypoints", "Создает путевые метки на местности (клавиша V) с отображением дистанции", Category.RENDER, -1);
      this.registerSetting(this.showDistance);
      this.registerSetting(this.sound);
   }

   public void addWaypoint(String name, Vec3d pos, boolean isTemp, UUID playerUUID) {
      this.waypoints.put(name, new Waypoints.Waypoint(name, pos, isTemp, System.currentTimeMillis(), playerUUID));
      if (this.sound.get() && mc.player != null) {
         mc.player.playSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
         mc.player.sendMessage(Text.literal("§b[Flux] §fМетка §e" + name + " §fустановлена."), false);
      }
   }

   public void clearWaypoints() {
      this.waypoints.clear();
   }

   private static class Waypoint {
      public final String name;
      public Vec3d pos;
      public final boolean temp;
      public final long creationTime;
      public final UUID playerUUID;

      public Waypoint(String name, Vec3d pos, boolean temp, long creationTime, UUID playerUUID) {
         this.name = name;
         this.pos = pos;
         this.temp = temp;
         this.creationTime = creationTime;
         this.playerUUID = playerUUID;
      }
   }
}
