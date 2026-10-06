package ru.kirka.fluxclient.feature.impl.render;

import java.util.Locale;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class TNTTimer extends Module {
   public final BooleanSetting showIcon = new BooleanSetting("Иконка", true);
   public final NumberSetting maxDistance = new NumberSetting("Дистанция", "Максимальная дистанция показа таймера", 64.0F, 10.0F, 128.0F, 1.0F);
   private final EventListener<PreHudRenderEvent> onHudRender = event -> {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrices = event.getContext().getMatrices();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntEntity tnt) {
               this.renderTntBadge(event, matrices, tnt);
            }
         }
      }
   };

   public TNTTimer() {
      super("TNTTimer", "Отображает время до детонации динамита над блоком TNT", Category.RENDER, -1);
      this.registerSetting(this.showIcon);
      this.registerSetting(this.maxDistance);
   }

   private void renderTntBadge(PreHudRenderEvent event, MatrixStack matrices, TntEntity entity) {
      int fuse = entity.getFuse();
      float seconds = Math.max(0.0F, fuse / 20.0F);
      String text = String.format(Locale.ROOT, "%.1fs", seconds);
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getPos().distanceTo(renderPos);
         if (!(distance > this.maxDistance.get())) {
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            matrices.push();
            matrices.translate(screenPos.x, screenPos.y, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            float fontHeight = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).height() : 11.0F;
            float textWidth = Fonts.MEDIUM != null ? Fonts.MEDIUM.getFont(11.0F).width(text) : 24.0F;
            boolean hasIcon = this.showIcon.get();
            float badgeWidth = textWidth + (hasIcon ? 28.0F : 12.0F);
            float badgeHeight = fontHeight + 8.0F;
            float x = -badgeWidth / 2.0F;
            float y = -badgeHeight / 2.0F;
            DrawUtility.drawRoundedRect(matrices, x, y, badgeWidth, badgeHeight, BorderRadius.all(4.0F), new ColorRGBA(15.0F, 15.0F, 15.0F, 180.0F));
            ColorRGBA borderColor = fuse < 20 ? new ColorRGBA(255.0F, 60.0F, 60.0F, 220.0F) : new ColorRGBA(255.0F, 170.0F, 0.0F, 200.0F);
            DrawUtility.drawRoundedBorder(matrices, x, y, badgeWidth, badgeHeight, 1.0F, BorderRadius.all(4.0F), borderColor);
            if (hasIcon) {
               matrices.push();
               matrices.translate(x + 2.0F, y + (badgeHeight - 12.0F) / 2.0F, 0.0F);
               matrices.scale(0.75F, 0.75F, 1.0F);
               event.getContext().drawItem(Items.TNT.getDefaultStack(), 0, 0);
               matrices.pop();
            }

            float textX = hasIcon ? x + 16.0F : x + 6.0F;
            float textY = y + (badgeHeight - fontHeight) / 2.0F;
            if (Fonts.MEDIUM != null) {
               event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, textX, textY, ColorRGBA.WHITE);
            }

            matrices.pop();
         }
      }
   }
}
