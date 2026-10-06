package ru.kirka.fluxclient.ui.widget;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;

public class CooldownsWidget extends Widget implements Interface {
   private final Map<Item, AnimationUtil> cooldownAnims = new HashMap<>();

   public CooldownsWidget() {
      super(new DragInfo("Задержки", 10.0F, 350.0F, 100.0F, 20.0F));
      this.j().setWidget(this);
   }

   private AnimationUtil getAnim(Item item) {
      return this.cooldownAnims.computeIfAbsent(item, k -> new AnimationUtil());
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      if (mc.player != null) {
         float x = this.j().getClampedX();
         float y = this.j().getClampedY();
         float targetWidth = 14.5F + Fonts.e.a("Cooldowns", 7.0F) + 5.0F + 2.0F;
         float contentY = y + 12.5F + 3.0F;
         Item[] trackedItems = new Item[]{
            Items.ENDER_PEARL, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.SHIELD, Items.CHORUS_FRUIT, Items.WIND_CHARGE
         };
         boolean active = false;

         for (Item item : trackedItems) {
            float cooldownProgress = mc.player.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0.0F);
            AnimationUtil anim = this.getAnim(item);
            anim.a(cooldownProgress > 0.0F);
            anim.a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
            if (anim.c() > 0.0F) {
               active = true;
               targetWidth = Math.max(targetWidth, 19.0F + Fonts.e.a(item.getName().getString(), 6.5F) + 8.0F + Fonts.e.a("3.0с", 6.5F) + 5.0F + 2.0F);
            }
         }

         float width = MathUtil.c(this.j().getWidth(), targetWidth, 0.5F);
         this.j().setWidth(width);
         if (this.a() > 0.0F) {
            this.a(event, "d", "Cooldowns", width, this.a());
         }

         for (Item item2 : trackedItems) {
            AnimationUtil anim2 = this.getAnim(item2);
            float animation = anim2.c() * this.a();
            if (animation > 0.0F) {
               float cooldownProgress2 = mc.player.getItemCooldownManager().getCooldownProgress(item2.getDefaultStack(), 0.0F);
               String time = String.format(Locale.US, "%.1fс", cooldownProgress2 * 5.0F);
               float offsetX = -8.0F * (1.0F - animation);
               float offsetY = -(1.0F - animation);
               float drawY = contentY + offsetY;
               float timeWidth = Fonts.e.a(time, 6.5F);
               float textY = drawY + (11.5F - Fonts.e.a(6.5F)) / 2.0F - 0.5F;
               this.a(event, x + offsetX, drawY, width, 11.5F, false, animation);
               this.a(event, x + offsetX + 15.0F, drawY, 11.5F, animation);
               FluxClient.getInstance()
                  .getDraw3DProcessor()
                  .a(event.getDrawContext(), item2.getDefaultStack(), x + offsetX + 5.0F, drawY + 2.0F, 0, animation, 0.45F, false);
               Fonts.e
                  .a(
                     event.getMatrixStack(),
                     item2.getName().getString(),
                     x + offsetX + 19.0F,
                     textY,
                     6.5F,
                     ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), animation)
                  );
               Fonts.e
                  .a(
                     event.getMatrixStack(),
                     time,
                     x + offsetX + width - 5.0F - timeWidth - 1.0F,
                     textY,
                     6.5F,
                     ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), 0.55F * animation)
                  );
               contentY += 13.5F * animation;
            }
         }

         this.j().setHeight(active ? contentY - y - 2.0F : 12.5F);
         super.a(event);
      }
   }

   @Override
   public void a(GlobalEvent event) {
      boolean visible = mc.currentScreen instanceof ChatScreen;
      if (mc.player != null) {
         Item[] trackedItems = new Item[]{
            Items.ENDER_PEARL, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.SHIELD, Items.CHORUS_FRUIT, Items.WIND_CHARGE
         };

         for (Item item : trackedItems) {
            if (mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
               visible = true;
               break;
            }
         }
      }

      this.d().a(visible);
      super.a(event);
   }
}
