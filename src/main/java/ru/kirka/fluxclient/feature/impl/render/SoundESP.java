package ru.kirka.fluxclient.feature.impl.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.event.impl.SoundEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.context.CustomDrawContext;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Font;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class SoundESP extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final BooleanSetting tnt = new BooleanSetting("Взрывы", true);
   public final BooleanSetting trident = new BooleanSetting("Трезубец", true);
   public final BooleanSetting fireworks = new BooleanSetting("Фейерверки", true);
   private final Map<String, SoundESP.SoundMarker> markers = new ConcurrentHashMap<>();
   private final EventListener<SoundEvent> onSound = event -> {
      SoundInstance sound = event.getSound();
      if (sound != null) {
         Identifier id = sound.getId();
         if (id != null) {
            String soundPath = id.toString();
            boolean track = false;
            String name = "";
            ItemStack icon = ItemStack.EMPTY;
            if (soundPath.contains("generic.explode") && this.tnt.get()) {
               track = true;
               name = "Взрыв";
               icon = new ItemStack(Items.TNT);
            } else if ((soundPath.contains("trident.throw") || soundPath.contains("trident.return")) && this.trident.get()) {
               track = true;
               name = "Трезубец";
               icon = new ItemStack(Items.TRIDENT);
            } else if (soundPath.contains("firework_rocket.launch") && this.fireworks.get()) {
               track = true;
               name = "Фейерверк";
               icon = new ItemStack(Items.FIREWORK_ROCKET);
            }

            if (track && mc.player != null) {
               Vec3d pos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
               String key = name + "_" + System.currentTimeMillis();
               this.markers.put(key, new SoundESP.SoundMarker(name, pos, icon, System.currentTimeMillis()));
            }
         }
      }
   };
   private final EventListener<PreHudRenderEvent> onRender = event -> {
      if (!this.markers.isEmpty() && mc.player != null) {
         long now = System.currentTimeMillis();
         this.markers.entrySet().removeIf(entry -> now - entry.getValue().time > 4500L);
         CustomDrawContext context = event.getContext();
         Font font = Fonts.MEDIUM.getFont(10.0F);

         for (SoundESP.SoundMarker marker : this.markers.values()) {
            Vec2f screen = Utils.worldToScreen(marker.pos.add(0.0, 0.5, 0.0));
            if (screen != null) {
               float distance = (float)mc.player.getPos().distanceTo(marker.pos);
               float scale = MathHelper.clamp(1.0F - distance / 35.0F, 0.7F, 1.1F);
               String text = marker.name + " (" + String.format("%.0f", distance) + "m)";
               float textW = font.getFont().getWidth(text, font.getSize());
               float w = textW + 20.0F;
               float h = font.getSize() + 6.0F;
               float x = screen.x - w / 2.0F;
               float y = screen.y - h / 2.0F;
               context.getMatrices().push();
               context.getMatrices().translate(screen.x, screen.y, 0.0F);
               context.getMatrices().scale(scale, scale, 1.0F);
               context.getMatrices().translate(-screen.x, -screen.y, 0.0F);
               DrawUtility.drawRoundedRect(context.getMatrices(), x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(15.0F, 15.0F, 15.0F, 180.0F));
               DrawUtility.drawRoundedBorder(context.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(4.0F), new ColorRGBA(255.0F, 170.0F, 0.0F, 180.0F));
               if (!marker.icon.isEmpty()) {
                  context.getMatrices().push();
                  context.getMatrices().translate(x + 2.0F, y + 1.0F, 0.0F);
                  context.getMatrices().scale(0.65F, 0.65F, 1.0F);
                  context.drawItem(marker.icon, 0, 0);
                  context.getMatrices().pop();
               }

               context.drawText(font, text, x + 16.0F, y + 3.0F, ColorRGBA.WHITE);
               context.getMatrices().pop();
            }
         }
      }
   };

   public SoundESP() {
      super("SoundESP", "Визуализация звуков взрывов, трезубцев и фейерверков", Category.RENDER, -1);
      this.registerSetting(this.tnt);
      this.registerSetting(this.trident);
      this.registerSetting(this.fireworks);
   }

   private record SoundMarker(String name, Vec3d pos, ItemStack icon, long time) {
   }
}
