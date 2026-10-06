package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;

public class TargetWidget extends Widget {
   private final BooleanSetting showItems = new BooleanSetting("Визуализация предметов", "Отображать броню и оружие цели", true);
   private final BooleanSetting targetCrosshair = new BooleanSetting("Отображать при наводке", "Показывать цель под прицелом", false);
   private final AnimationUtil hpAnimation = new AnimationUtil();
   private final AnimationUtil textAnimation = new AnimationUtil();
   private LivingEntity targetEntity;
   private String prevHp = "";

   public TargetWidget() {
      super(new DragInfo("Таргет-худ", 100.0F, 100.0F, 100.0F, 24.0F));
      this.j().setWidget(this);
      this.a(this.targetCrosshair, this.showItems);
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      if (this.a() > 0.0F && this.targetEntity != null) {
         this.j().setWidth(100.0F);
         this.j().setHeight(24.0F);
         float x = this.j().getClampedX();
         float y = this.j().getClampedY();
         this.a(event, x, y, this.j().getWidth(), this.j().getHeight(), true, this.a());
         float headSize = this.j().getHeight() / 1.35F;
         float headY = y + (this.j().getHeight() - headSize) / 2.0F;
         if (this.targetEntity instanceof AbstractClientPlayerEntity player) {
            event.getDraw2DProcessor()
               .a(
                  event.getMatrixStack(),
                  x + 5.0F,
                  headY,
                  headSize,
                  headSize,
                  2.0F,
                  ColorUtil.applyAlphaToColor(-1, this.a()),
                  0.125F,
                  0.125F,
                  0.125F,
                  0.125F,
                  Interface.mc.getTextureManager().getTexture(player.getSkinTextures().texture()).getGlId()
               );
         } else {
            float iconS = headSize * 0.75F;
            Fonts.a
               .a(
                  event.getMatrixStack(),
                  "B",
                  x + 5.0F + (headSize - iconS) / 2.0F,
                  headY + (headSize - iconS) / 2.0F,
                  iconS,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), this.a())
               );
         }

         float textX = x + 5.0F + headSize + 5.0F;
         String name = this.targetEntity.getName().getString();
         if (name.length() > 12) {
            Fonts.e
               .c(
                  event.getMatrixStack(),
                  name,
                  textX,
                  headY,
                  7.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), this.a()),
                  Fonts.e.a(name.substring(0, 12), 7.5F)
               );
         } else {
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  name,
                  textX,
                  headY,
                  7.5F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), this.a())
               );
         }

         if (this.showItems.get()) {
            int i = 0;

            for (ItemStack stack : new ItemStack[]{
               this.targetEntity.getEquippedStack(EquipmentSlot.FEET),
               this.targetEntity.getEquippedStack(EquipmentSlot.LEGS),
               this.targetEntity.getEquippedStack(EquipmentSlot.CHEST),
               this.targetEntity.getEquippedStack(EquipmentSlot.HEAD),
               this.targetEntity.getOffHandStack(),
               this.targetEntity.getMainHandStack()
            }) {
               if (!stack.isEmpty()) {
                  FluxClient.getInstance()
                     .getDraw3DProcessor()
                     .a(event.getDrawContext(), stack, x + this.j().getWidth() - 10.0F - i * 9, y + this.j().getHeight(), 0, this.a(), 0.55F, true);
                  i++;
               }
            }
         }

         int primary = FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor();
         float health = Math.max(0.0F, this.targetEntity.getHealth());
         String hpValue = String.valueOf((int)health);
         if (this.prevHp.isEmpty()) {
            this.prevHp = hpValue;
         }

         float progress = hpValue.equals(this.prevHp) ? 1.0F : this.textAnimation.a(0.0F, 1.0F, 0.75F);
         this.drawHpValue(event, hpValue, this.prevHp, x + this.j().getWidth() - 5.0F - Fonts.e.a(hpValue, 7.0F), headY + 0.5F, 7.0F, primary, progress);
         if (progress >= 0.99F) {
            this.prevHp = hpValue;
            this.textAnimation.c(0.0F);
         }

         float maxHp = Math.max(1.0F, this.targetEntity.getMaxHealth());
         float targetHP = MathUtil.b(health / maxHp, 0.0F, 1.0F);
         float lineHP = this.hpAnimation.a(targetHP, targetHP, 0.5F);
         event.getDraw2DProcessor()
            .a(event.getMatrixStack(), textX, headY + 12.5F, 54.0F, 3.0F, 1.5F, ColorUtil.applyAlphaToColor(ColorUtil.b(primary, 0.25F), this.a()));
         event.getDraw2DProcessor().a(event.getMatrixStack(), textX, headY + 12.5F, 54.0F * lineHP, 3.0F, 1.5F, ColorUtil.applyAlphaToColor(primary, this.a()));
      }

      super.a(event);
   }

   private void drawHpValue(DrawEvent event, String current, String previous, float right, float y, float size, int color, float progress) {
      float height = Fonts.e.a(size);
      float cursor = right + Fonts.e.a(current, size);

      for (int i = 0; i < current.length(); i++) {
         char digit = current.charAt(current.length() - 1 - i);
         char old = i < previous.length() ? previous.charAt(previous.length() - 1 - i) : 32;
         String value = String.valueOf(digit);
         cursor -= Fonts.e.a(value, size);
         if (digit != old && !(progress >= 1.0F)) {
            ScissorUtil.a(event.getMatrixStack(), cursor - 0.5F, y, Fonts.e.a(value, size) + 1.0F, height + 1.0F);
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  String.valueOf(old),
                  cursor,
                  y - height * progress,
                  size,
                  ColorUtil.applyAlphaToColor(color, (1.0F - progress) * this.a())
               );
            Fonts.e.a(event.getMatrixStack(), value, cursor, y + height * (1.0F - progress), size, ColorUtil.applyAlphaToColor(color, progress * this.a()));
            ScissorUtil.a(event.getMatrixStack());
         } else {
            Fonts.e.a(event.getMatrixStack(), value, cursor, y, size, ColorUtil.applyAlphaToColor(color, this.a()));
         }
      }
   }

   @Override
   public void a(GlobalEvent event) {
      LivingEntity target = null;
      if (Aura.target != null && Aura.target.isAlive()) {
         target = Aura.target;
      } else if (this.targetCrosshair.get()
         && mc.crosshairTarget instanceof EntityHitResult hit
         && hit.getEntity() instanceof LivingEntity living
         && living != mc.player) {
         target = living;
      } else if (mc.currentScreen instanceof ChatScreen) {
         target = mc.player;
      }

      boolean visible = target != null;
      if (target != null) {
         this.targetEntity = target;
      }

      this.d().a(visible);
      if (!visible && this.d().a() <= 0.0F) {
         this.targetEntity = null;
      }

      super.a(event);
   }
}
