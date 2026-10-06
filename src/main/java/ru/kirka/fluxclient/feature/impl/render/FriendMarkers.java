package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.CrystalRenderer;
import ru.kirka.fluxclient.render.draw.RenderUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.util.FriendUtil;

public class FriendMarkers extends Module {
   public final ColorSetting color = new ColorSetting("Цвет", "Цвет кристалла над головой друга", new Color(52, 199, 89, 255));
   public final NumberSetting size = new NumberSetting("Размер", "Размер кристалла", 0.12F, 0.05F, 0.3F, 0.01F);
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world != null && mc.player != null) {
         RenderUtility.setupRender3D(true);
         MatrixStack ms = event.getMatrices();
         Color c = this.color.get();
         ColorRGBA colorRGBA = new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
         BufferBuilder builder = CrystalRenderer.createBuffer();
         float sz = this.size.get();
         boolean renderedAny = false;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && FriendUtil.isFriend(player.getName().getString())) {
               Vec3d pos = Utils.getInterpolatedPos(player, event.getTickDelta());
               ms.push();
               RenderUtility.prepareMatrices(ms, pos);
               CrystalRenderer.render(ms, builder, 0.0F, player.getHeight() + 0.4F, 0.0F, sz, colorRGBA);
               ms.pop();
               renderedAny = true;
            }
         }

         if (renderedAny) {
            BuiltBuffer built = builder.endNullable();
            if (built != null) {
               BufferRenderer.drawWithGlobalProgram(built);
            }
         }

         RenderUtility.endRender3D();
      }
   };

   public FriendMarkers() {
      super("FriendMarkers", "Отображает кристаллы (Sims) над головами добавленных друзей", Category.RENDER, -1);
      this.registerSetting(this.color);
      this.registerSetting(this.size);
   }
}
