package ru.kirka.fluxclient.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;

public class PotionWidget extends Widget implements Interface {
   private final BooleanSetting sideDisplay;
   private final StatusEffectInstance previewEffect;
   private final Map<RegistryEntry<StatusEffect>, AnimationUtil> animMap = new HashMap<>();
   private final Map<RegistryEntry<StatusEffect>, Integer> initialDurationMap = new HashMap<>();

   public PotionWidget() {
      super(new DragInfo("Зелья", 10.0F, 150.0F, 100.0F, 20.0F));
      this.sideDisplay = new BooleanSetting("Боковое отображение", "Отображать с левого края экрана", false);
      this.previewEffect = new StatusEffectInstance(StatusEffects.SPEED, 1200, 0);
      this.j().setWidget(this);
      this.a(this.sideDisplay);
   }

   private AnimationUtil getAnimation(StatusEffectInstance effect) {
      return this.animMap.computeIfAbsent(effect.getEffectType(), k -> new AnimationUtil());
   }

   private int getInitialDuration(StatusEffectInstance effect) {
      return this.initialDurationMap.computeIfAbsent(effect.getEffectType(), k -> Math.max(1, effect.getDuration()));
   }

   @Override
   public void a(DrawEvent event) {
      this.d().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());

      for (StatusEffectInstance effect : this.getEffectsList()) {
         this.getAnimation(effect).a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      }

      if (this.sideDisplay.get()) {
         this.renderSide(event);
      } else {
         this.renderWindow(event);
      }

      this.j().setDragStatus(this.sideDisplay.get() ? 2 : 0);
   }

   private void renderWindow(DrawEvent event) {
      float x = this.j().getClampedX();
      float y = this.j().getClampedY();
      float targetWidth = 14.5F + Fonts.e.a("Potion-list", 7.0F) + 5.0F + 2.0F;
      float contentY = y + 12.5F + 3.0F;
      boolean active = false;

      for (StatusEffectInstance effect : this.getEffectsList()) {
         AnimationUtil anim = this.getAnimation(effect);
         if (anim.c() > 0.0F) {
            active = true;
            String name = Text.translatable(((StatusEffect)effect.getEffectType().value()).getTranslationKey()).getString() + " " + (effect.getAmplifier() + 1);
            int sec = effect.getDuration() / 20;
            String dur = effect.getDuration() > 1000000 ? "∞" : sec / 60 + ":" + String.format("%02d", sec % 60);
            targetWidth = Math.max(targetWidth, 19.0F + Fonts.e.a(name, 6.5F) + 8.0F + Fonts.e.a(dur, 6.5F) + 5.0F + 2.0F);
         }
      }

      float width = MathUtil.c(this.j().getWidth(), targetWidth, 0.5F);
      this.j().setWidth(width);
      if (this.a() > 0.0F) {
         this.a(event, "E", "Potion-list", width, this.a());
      }

      for (StatusEffectInstance effectx : this.getEffectsList()) {
         AnimationUtil anim = this.getAnimation(effectx);
         float animation = anim.c() * this.a();
         if (animation > 0.0F) {
            String name = Text.translatable(((StatusEffect)effectx.getEffectType().value()).getTranslationKey()).getString()
               + " "
               + (effectx.getAmplifier() + 1);
            int seconds = effectx.getDuration() / 20;
            String duration = effectx.getDuration() > 1000000 ? "∞" : seconds / 60 + ":" + String.format("%02d", seconds % 60);
            float offsetX = -8.0F * (1.0F - animation);
            float offsetY = -(1.0F - animation);
            float drawY = contentY + offsetY;
            float durationWidth = Fonts.e.a(duration, 6.5F);
            float textY = drawY + (11.5F - Fonts.e.a(6.5F)) / 2.0F - 0.5F;
            this.a(event, x + offsetX, drawY, width, 11.5F, false, animation);
            this.a(event, x + offsetX + 15.0F, drawY, 11.5F, animation);
            FluxClient.getInstance()
               .getDraw3DProcessor()
               .a(
                  event.getDrawContext(),
                  mc.getStatusEffectSpriteManager().getSprite(effectx.getEffectType()),
                  x + offsetX + 5.0F,
                  drawY + 2.0F,
                  0.0F,
                  0.4F,
                  animation
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  name,
                  x + offsetX + 19.0F,
                  textY,
                  6.5F,
                  ColorUtil.applyAlphaToColor(
                     ((StatusEffect)effectx.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL
                        ? ColorUtil.convertToARGB(255, 76, 76, 255)
                        : FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(),
                     animation
                  )
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  duration,
                  x + offsetX + width - 5.0F - durationWidth - 1.0F,
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

   private void renderSide(DrawEvent event) {
      int primary = FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor();
      int visibleCount = 0;

      for (StatusEffectInstance effect : this.getEffectsList()) {
         if (this.getAnimation(effect).c() > 0.0F) {
            visibleCount++;
         }
      }

      float screenH = (float)mc.getWindow().getFramebufferHeight() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
      float posY = (screenH - (visibleCount * 26.0F + (visibleCount - 1) * 2.0F)) / 2.0F;
      float contentY = posY;
      float maxWidth = 0.0F;

      for (StatusEffectInstance effectx : this.getEffectsList()) {
         AnimationUtil anim = this.getAnimation(effectx);
         float animation = anim.c() * this.a();
         if (animation > 0.0F) {
            boolean harmful = ((StatusEffect)effectx.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL;
            String name = Text.translatable(((StatusEffect)effectx.getEffectType().value()).getTranslationKey()).getString()
               + " "
               + (effectx.getAmplifier() + 1);
            int seconds = effectx.getDuration() / 20;
            String duration = effectx.getDuration() > 1000000 ? "∞" : seconds / 60 + ":" + String.format("%02d", seconds % 60);
            float textWidth = Math.max(Fonts.e.a(name, 7.0F), Fonts.e.a(duration, 6.0F));
            float width = 18.5F + textWidth + 8.0F;
            float drawX = 3.0F - width * (1.0F - animation);
            float textX = drawX + 13.5F + 6.0F;
            this.a(event, drawX, contentY, width, 24.0F, true, animation);
            FluxClient.getInstance()
               .getDraw3DProcessor()
               .a(
                  event.getDrawContext(),
                  mc.getStatusEffectSpriteManager().getSprite(effectx.getEffectType()),
                  drawX + 3.5F,
                  contentY + 5.75F,
                  0.0F,
                  0.6944444F,
                  animation
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  name,
                  textX,
                  contentY + 3.5F,
                  7.0F,
                  ColorUtil.applyAlphaToColor(
                     harmful ? ColorUtil.convertToARGB(215, 76, 76, 255) : FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(),
                     animation
                  )
               );
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  duration,
                  textX,
                  contentY + 13.0F,
                  6.0F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), 0.55F * animation)
               );
            int initial = this.getInitialDuration(effectx);
            float progress = Math.min(1.0F, (float)effectx.getDuration() / initial);
            int accent = harmful ? ColorUtil.convertToARGB(215, 76, 76, 255) : primary;
            event.getDraw2DProcessor()
               .a(
                  event.getMatrixStack(),
                  drawX + 2.0F,
                  contentY + 24.0F - 1.5F,
                  width - 4.0F,
                  1.5F,
                  new Vector4f(0.0F, 0.0F, 1.0F, 1.0F),
                  ColorUtil.applyAlphaToColor(accent, 0.15F * animation)
               );
            event.getDraw2DProcessor()
               .a(
                  event.getMatrixStack(),
                  drawX + 2.0F,
                  contentY + 24.0F - 1.5F,
                  (width - 4.0F) * progress,
                  1.5F,
                  new Vector4f(0.0F, 0.0F, 1.0F, 1.0F),
                  ColorUtil.applyAlphaToColor(accent, animation)
               );
            maxWidth = Math.max(maxWidth, width);
            contentY += 28.0F * animation;
         }
      }

      this.j().setX(3.0F);
      this.j().setY(posY);
      this.j().setWidth(maxWidth);
      this.j().setHeight(contentY - posY - 2.0F);
      super.a(event);
   }

   @Override
   public void a(GlobalEvent event) {
      boolean visible = mc.currentScreen instanceof ChatScreen;

      for (StatusEffectInstance effect : this.getEffectsList()) {
         if (!effect.getEffectType().equals(StatusEffects.NIGHT_VISION)) {
            AnimationUtil anim = this.getAnimation(effect);
            anim.a(effect == this.previewEffect ? mc.currentScreen instanceof ChatScreen : effect.getDuration() > 20);
            if (anim.c() > 0.0) {
               visible = true;
            }
         }
      }

      this.d().a(visible);
      super.a(event);
   }

   private List<StatusEffectInstance> getEffectsList() {
      List<StatusEffectInstance> effects = new ArrayList<>();
      if (mc.player != null) {
         effects.addAll(mc.player.getStatusEffects());
      }

      if (effects.isEmpty() && (mc.currentScreen instanceof ChatScreen || this.getAnimation(this.previewEffect).c() > 0.0F)) {
         effects.add(this.previewEffect);
      }

      return effects;
   }
}
