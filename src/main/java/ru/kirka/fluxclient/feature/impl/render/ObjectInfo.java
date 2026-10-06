package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.event.impl.ReceivePacketEvent;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.Draw3DUtility;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class ObjectInfo extends Module {
   public final BooleanSetting show2D = new BooleanSetting("2D метка", "Отображать плашку на экране", true);
   public final BooleanSetting show3D = new BooleanSetting("3D бокс", "Отображать 3D куб зоны ловушки", true);
   private final Map<BlockPos, ObjectInfo.Info> infos = new ConcurrentHashMap<>();
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (event.getPacket() instanceof PlaySoundS2CPacket sound) {
         String soundName = ((SoundEvent)sound.getSound().value()).id().toString();
         BlockPos pos = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
         this.handleSound(soundName, pos, sound.getVolume(), sound.getPitch());
      }
   };
   private final EventListener<ru.kirka.fluxclient.event.impl.SoundEvent> onSound = event -> {
      SoundInstance sound = event.getSound();
      if (sound != null) {
         Identifier id = sound.getId();
         if (id != null) {
            BlockPos pos = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
            this.handleSound(id.toString(), pos, sound.getVolume(), sound.getPitch());
         }
      }
   };
   private final EventListener<PreHudRenderEvent> onHudRender = event -> {
      if (this.show2D.get() && mc.world != null && mc.player != null) {
         long now = System.currentTimeMillis();
         this.infos.entrySet().removeIf(e -> now - e.getValue().startTime > e.getValue().type.durationMs);
         MatrixStack matrices = event.getContext().getMatrices();

         for (ObjectInfo.Info info : this.infos.values()) {
            Vec3d renderPos = info.pos.toCenterPos().add(0.0, 0.5, 0.0);
            Vec2f screenPos = Utils.worldToScreen(renderPos);
            if (screenPos != null) {
               float distance = (float)mc.player.getPos().distanceTo(renderPos);
               float scale = MathHelper.clamp(1.0F - distance / 25.0F, 0.5F, 1.0F);
               long elapsed = now - info.startTime;
               long remaining = Math.max(0L, info.type.durationMs - elapsed);
               int seconds = (int)(remaining / 1000L);
               float progress = (float)remaining / (float)info.type.durationMs;
               matrices.push();
               matrices.translate(screenPos.x, screenPos.y, 0.0F);
               matrices.scale(scale, scale, 1.0F);
               String text = info.type.displayName + " (" + seconds + "с)";
               float fontHeight = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).height() : 11.0F;
               float textWidth = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).width(text) : 40.0F;
               float badgeWidth = textWidth + 28.0F;
               float badgeHeight = fontHeight + 10.0F;
               float x = -badgeWidth / 2.0F;
               float y = -badgeHeight / 2.0F;
               DrawUtility.drawRoundedRect(matrices, x, y, badgeWidth, badgeHeight, BorderRadius.all(4.0F), new ColorRGBA(15.0F, 15.0F, 15.0F, 190.0F));
               DrawUtility.drawRoundedBorder(matrices, x, y, badgeWidth, badgeHeight, 1.0F, BorderRadius.all(4.0F), new ColorRGBA(255.0F, 60.0F, 60.0F, 200.0F));
               float barW = (badgeWidth - 4.0F) * progress;
               if (barW > 0.0F) {
                  DrawUtility.drawRoundedRect(
                     matrices, x + 2.0F, y + badgeHeight - 3.0F, barW, 2.0F, BorderRadius.all(1.0F), new ColorRGBA(255.0F, 50.0F, 50.0F, 230.0F)
                  );
               }

               matrices.push();
               matrices.translate(x + 2.0F, y + (badgeHeight - 12.0F) / 2.0F, 0.0F);
               matrices.scale(0.75F, 0.75F, 1.0F);
               event.getContext().drawItem(info.type.item.getDefaultStack(), 0, 0);
               matrices.pop();
               if (Fonts.MEDIUM != null) {
                  event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x + 16.0F, y + (badgeHeight - fontHeight - 2.0F) / 2.0F, ColorRGBA.WHITE);
               }

               matrices.pop();
            }
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.show3D.get() && mc.world != null && mc.player != null && mc.gameRenderer != null) {
         long now = System.currentTimeMillis();
         this.infos.entrySet().removeIf(e -> now - e.getValue().startTime > e.getValue().type.durationMs);
         if (!this.infos.isEmpty()) {
            MatrixStack matrices = event.getMatrices();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);

            for (ObjectInfo.Info info : this.infos.values()) {
               if (info.type != ObjectInfo.ObjType.PLAST) {
                  int radius = info.type == ObjectInfo.ObjType.TRAP ? 2 : 4;
                  ColorRGBA boxColor = info.type == ObjectInfo.ObjType.TRAP
                     ? new ColorRGBA(255.0F, 60.0F, 60.0F, 180.0F)
                     : new ColorRGBA(255.0F, 200.0F, 0.0F, 180.0F);
                  Box box = new Box(
                     info.pos.getX() - radius,
                     info.pos.getY() - radius,
                     info.pos.getZ() - radius,
                     info.pos.getX() + radius + 1,
                     info.pos.getY() + radius + 1,
                     info.pos.getZ() + radius + 1
                  );
                  Draw3DUtility.renderOutlinedBox(matrices, buffer, box, boxColor);
               }
            }

            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
               BufferRenderer.drawWithGlobalProgram(built);
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrices.pop();
         }
      }
   };

   private void handleSound(String soundName, BlockPos pos, float volume, float pitch) {
      if (!soundName.contains("piston.extend") && !soundName.contains("piston.contract")) {
         if (soundName.contains("anvil.place")) {
            if ((volume == 0.5F || volume == 0.7F) && (pitch == 1.1F || pitch == 0.5F)) {
               this.infos.put(pos, new ObjectInfo.Info(pos, ObjectInfo.ObjType.PLAST, System.currentTimeMillis()));
            }
         } else if (soundName.contains("evoker_fangs.attack") && (volume == 0.5F || volume == 0.7F) && (pitch == 1.0F || pitch == 0.85F)) {
            this.infos.put(pos, new ObjectInfo.Info(pos, ObjectInfo.ObjType.DRAGON_FT, System.currentTimeMillis()));
         }
      } else if ((volume == 0.5F || volume == 0.7F) && pitch == 0.5F) {
         this.infos.put(pos, new ObjectInfo.Info(pos, ObjectInfo.ObjType.TRAP, System.currentTimeMillis()));
      }
   }

   public ObjectInfo() {
      super("ObjectInfo", "Показывает информацию и время действия ловушек, пластов и драконок в мире", Category.RENDER, -1);
      this.registerSetting(this.show2D);
      this.registerSetting(this.show3D);
   }

   private static class Info {
      final BlockPos pos;
      final ObjectInfo.ObjType type;
      final long startTime;

      Info(BlockPos pos, ObjectInfo.ObjType type, long startTime) {
         this.pos = pos;
         this.type = type;
         this.startTime = startTime;
      }
   }

   private static enum ObjType {
      TRAP("Трапка", Items.COBWEB, 15000L),
      DRAGON_FT("Драконка", Items.RESPAWN_ANCHOR, 30000L),
      PLAST("Пласт", Items.ANVIL, 20000L);

      final String displayName;
      final Item item;
      final long durationMs;

      private ObjType(String displayName, Item item, long durationMs) {
         this.displayName = displayName;
         this.item = item;
         this.durationMs = durationMs;
      }
   }
}
