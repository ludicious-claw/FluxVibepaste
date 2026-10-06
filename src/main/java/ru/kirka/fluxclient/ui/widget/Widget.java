package ru.kirka.fluxclient.ui.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.Setting;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.ui.element.Element;

public class Widget implements Interface {
   protected final AnimationUtil a = new AnimationUtil();
   protected final AnimationUtil b = new AnimationUtil();
   protected final AnimationUtil c = new AnimationUtil();
   protected final float d = 12.5F;
   protected final float e = 7.0F;
   private final List<Setting<?>> f = new ArrayList<>();
   private final List<Element<?>> g = new ArrayList<>();
   private final DragInfo i;
   private boolean h = false;

   public Widget(DragInfo dragInfo) {
      this.i = dragInfo;
      dragInfo.setWidget(this);
   }

   public List<Setting<?>> b() {
      return this.f;
   }

   public List<Element<?>> c() {
      return this.g;
   }

   public AnimationUtil d() {
      return this.a;
   }

   public AnimationUtil e() {
      return this.b;
   }

   public AnimationUtil f() {
      return this.c;
   }

   public void a(boolean status) {
      this.h = status;
   }

   public boolean g() {
      return this.h;
   }

   public float h() {
      return 12.5F;
   }

   public float i() {
      return 7.0F;
   }

   public DragInfo j() {
      return this.i;
   }

   protected final void a(Setting<?>... settings) {
      for (Setting<?> setting : settings) {
         this.f.add(setting);
      }
   }

   public void a(DrawEvent event) {
      this.e().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      this.c.a(this.h && Interface.mc.currentScreen instanceof ChatScreen);
      this.c.a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
      if (this.c.c() > 0.0F) {
         this.b(event);
      }
   }

   public void a(GlobalEvent event) {
      if (FluxClient.getInstance() != null && FluxClient.getInstance().getDragProcessor() != null) {
         this.e().a(this.i == FluxClient.getInstance().getDragProcessor().getActiveDragInfo());
      }
   }

   protected void b(DrawEvent event) {
      List<Element<?>> visible = this.g.stream().filter(Element::isEnabled).toList();
      if (!visible.isEmpty()) {
         float panelWidth = visible.stream().map(e2 -> 19.5F + Fonts.e.a(e2.getSetting().getName(), 6.5F) + 25.0F).reduce(0.0F, Math::max);
         float totalHeight = 12.0F * visible.size() + (visible.size() - 1);
         float anim = this.c.c() * this.a();
         float baseX = this.i.getClampedY() - totalHeight - 2.0F >= 0.0F
            ? this.i.getClampedX() + this.i.getWidth() / 2.0F - panelWidth / 2.0F
            : this.i.getClampedX() + this.i.getWidth() + 2.0F;
         float baseY = this.i.getClampedY() - totalHeight - 2.0F >= 0.0F ? this.i.getClampedY() - totalHeight - 2.0F : this.i.getClampedY();
         float screenW = (float)Interface.mc.getWindow().getFramebufferWidth()
            / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont());
         float screenH = (float)Interface.mc.getWindow().getFramebufferHeight()
            / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont());
         float baseX2 = Math.min(Math.max(baseX, 0.0F), screenW - panelWidth - 2.0F);
         float baseY2 = Math.min(Math.max(baseY, 0.0F), screenH - totalHeight);
         this.a(event, baseX2, baseY2, panelWidth, totalHeight, true, anim);
         float y = baseY2;

         for (Element<?> element : visible) {
            element.getBounds().set(baseX2, y, panelWidth, 12.0F);
            element.onDrawEvent(event, baseX2, y, panelWidth, anim);
            y += 13.0F;
            if (element != visible.getLast()) {
               event.getDraw2DProcessor()
                  .a(
                     event.getMatrixStack(),
                     baseX2,
                     y - 1.0F,
                     panelWidth,
                     0.75F,
                     0.0F,
                     ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(200, 200, 200, 255), 0.2F * anim)
                  );
            }
         }
      }
   }

   protected void a(DrawEvent event, String icon, Object title, float width, float animation) {
      this.a(event, icon, title, width, animation, FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.PRIMARY).toIntColor());
   }

   protected void a(DrawEvent event, String icon, Object title, float width, float animation, int iconColor) {
      this.a(event, this.i.getClampedX(), this.i.getClampedY(), icon, title, width, animation, iconColor);
   }

   protected void a(DrawEvent event, float x, float y, String icon, Object title, float width, float animation, int iconColor) {
      this.a(event, x, y, icon, null, title, width, animation, iconColor);
   }

   protected void a(DrawEvent event, float x, float y, ItemStack icon, Object title, float width, float animation, int iconColor) {
      this.a(event, x, y, null, icon, title, width, animation, iconColor);
   }

   private void a(DrawEvent event, float x, float y, String icon, ItemStack stack, Object title, float width, float animation, int iconColor) {
      if (animation > 0.0F) {
         float iconSize = 7.0F + 1.0F;
         this.a(event, x, y, width, 12.5F, true, animation);
         if (stack != null) {
            FluxClient.getInstance()
               .getDraw3DProcessor()
               .a(event.getDrawContext(), stack, x + 3.0F, y + (12.5F - 8.0F) / 2.0F - 0.25F, 0, animation, 0.5F, false);
         } else {
            Fonts.a
               .a(event.getMatrixStack(), icon, x + 3.0F, y + (12.5F - Fonts.a.a(iconSize)) / 2.0F, iconSize, ColorUtil.applyAlphaToColor(iconColor, animation));
         }

         this.a(event, x + 13.5F, y, 12.5F, animation);
         if (title instanceof Text text) {
            Fonts.e.a(event.getMatrixStack(), text, x + 17.5F, y + (12.5F - Fonts.e.a(7.0F)) / 2.0F - 0.5F, 7.0F, (double)animation);
         } else {
            Fonts.e
               .a(
                  event.getMatrixStack(),
                  String.valueOf(title),
                  x + 17.5F,
                  y + (12.5F - Fonts.e.a(7.0F)) / 2.0F - 0.5F,
                  7.0F,
                  ColorUtil.applyAlphaToColor(FluxClient.getInstance().getThemeProcessor().a(ThemeInfo.TEXT).toIntColor(), animation)
               );
         }
      }
   }

   protected void a(DrawEvent event, float x, float y, float width, float height, boolean glow, float animation) {
      if (animation > 0.0F) {
         ThemeProcessor themeProcessor = FluxClient.getInstance().getThemeProcessor();
         int themeColor = themeProcessor.a(ThemeInfo.PRIMARY).toIntColor();
         float radius = 5.0F + this.b.c();
         Draw2DProcessor draw = event.getDraw2DProcessor();
         if (draw.e() != null && !draw.e().e().isEmpty()) {
            int glowCol = glow ? ColorUtil.applyAlphaToColor(themeColor, 0.36F * animation) : 0;
            float glowRad = glow ? 7.0F + 2.0F * this.b.c() : 0.0F;
            int glassTint = ColorUtil.convertToARGB(11, 11, 15, (int)(140.0F * animation));
            draw.drawThemedBlurredPanel(event.getMatrixStack(), x, y, width, height, radius, themeColor, 0.36F, glowCol, glowRad, glassTint);
         } else {
            float alpha = themeProcessor.a(ThemeInfo.BACKGROUND_HUD).getAlphaFloat() * animation;
            int background = ColorUtil.lerpColor(themeProcessor.a(ThemeInfo.BACKGROUND_HUD).toIntColor(), themeColor, 0.1F);
            if (glow) {
               draw.a(
                  event.getMatrixStack(),
                  x,
                  y,
                  width,
                  height,
                  radius,
                  ColorUtil.applyAlphaToColor(background, alpha),
                  animation,
                  ColorUtil.applyAlphaToColor(background, alpha),
                  8.0F + 2.0F * this.b.c()
               );
            } else {
               draw.b(event.getMatrixStack(), x, y, width, height, radius, ColorUtil.applyAlphaToColor(background, alpha), animation);
            }
         }

         draw.a(event.getMatrixStack(), x, y, width, height, radius, 0.5F, ColorUtil.applyAlphaToColor(themeColor, (glow ? 0.3F : 0.16F) * animation));
      }
   }

   protected void a(DrawEvent event, float x, float y, float height, float animation) {
      float separatorHeight = height / 2.0F;
      event.getDraw2DProcessor()
         .a(
            event.getMatrixStack(),
            x,
            y + (height - separatorHeight) / 2.0F,
            0.75F,
            separatorHeight,
            0.0F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(200, 200, 200, 255), 0.5F * animation)
         );
   }

   public float a() {
      return this.a.c() * (1.0F - 0.1F * this.b.c());
   }
}
