package ru.kirka.fluxclient.ui.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScaleUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.shader.GradientUtil;
import ru.kirka.fluxclient.ui.widget.EffectMarker;
import ru.kirka.fluxclient.util.MathUtil;

public class MainScreen extends Screen {
   private static final float[] parallax = new float[2];
   private final AnimationUtil openAnim;
   private final List<MainScreen.MenuItem> menuItems;
   private final List<EffectMarker.a> rippleEffects;
   private float quitHoverAnim = 0.0F;

   public MainScreen() {
      super(Text.empty());
      this.openAnim = new AnimationUtil();
      this.rippleEffects = new ArrayList<>();
      if (Interface.mc.currentScreen instanceof MainScreen) {
         this.openAnim.c(1.0F);
         this.openAnim.d(1.0F);
         this.openAnim.e(1.0F);
      }

      this.menuItems = List.of(
         new MainScreen.MenuItem("Одиночная игра", "Локальные миры и выживание", "W", () -> Interface.mc.setScreen(new SelectWorldScreen(this))),
         new MainScreen.MenuItem("Сетевая игра", "Серверы, лобби и PvP дуэли", "I", () -> Interface.mc.setScreen(new MultiplayerScreen(this))),
         new MainScreen.MenuItem("Менеджер аккаунтов", "Быстрая смена ников и профилей", "L", () -> Interface.mc.setScreen(new AltScreen())),
         new MainScreen.MenuItem(
            "Настройки", "Параметры графики, управления и звука", "P", () -> Interface.mc.setScreen(new OptionsScreen(this, Interface.mc.options))
         )
      );
   }

   public static void renderParallaxBackground(DrawContext context, int width, int height, int mouseX, int mouseY, float scale) {
      float marginX = width * 0.025F;
      float marginY = height * 0.025F;
      parallax[0] = parallax[0] + (MathHelper.clamp(((float)mouseX / width - 0.5F) * 2.0F * marginX, -marginX * 0.9F, marginX * 0.9F) - parallax[0]) * 0.03F;
      parallax[1] = parallax[1] + (MathHelper.clamp(((float)mouseY / height - 0.5F) * 2.0F * marginY, -marginY * 0.9F, marginY * 0.9F) - parallax[1]) * 0.03F;
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(width / 2.0F, height / 2.0F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-width / 2.0F, -height / 2.0F, 0.0F);
      FluxClient.getInstance()
         .getDraw2DProcessor()
         .a(
            matrices,
            Identifier.of("fluxclient", "pictures/main.png"),
            -marginX + parallax[0],
            -marginY + parallax[1],
            width + marginX * 2.0F,
            height + marginY * 2.0F,
            0.0F,
            -1
         );
      matrices.pop();
   }

   public static void a(DrawContext context, int width, int height, int mouseX, int mouseY, float scale) {
      renderParallaxBackground(context, width, height, mouseX, mouseY, scale);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.openAnim.a(Interface.mc.currentScreen instanceof MainScreen);
      this.openAnim.a(0.0F, 1.0F, 0.18F, EasingList.g, delta);
      float open = Math.min(1.0F, this.openAnim.c() / 0.9F);
      double scaledMouseX = MathUtil.scale(mouseX, 2);
      double scaledMouseY = MathUtil.scale(mouseY, 2);
      ScaleUtil.a(context, 2);
      int screenWidth = (int)(
         (float)Interface.mc.getWindow().getFramebufferWidth() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      int screenHeight = (int)(
         (float)Interface.mc.getWindow().getFramebufferHeight() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      renderParallaxBackground(context, screenWidth, screenHeight, (int)scaledMouseX, (int)scaledMouseY, 1.25F - EasingList.s.ease(open) * 0.2F);
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      MatrixStack matrices = context.getMatrices();
      draw.a(matrices, 0.0F, 0.0F, screenWidth, screenHeight, 0.0F, ColorUtil.convertToARGB(5, 5, 8, (int)(130.0F * open)));
      draw.e().a(matrices);
      float plateW = 340.0F;
      float plateH = 260.0F;
      float plateX = (screenWidth - plateW) * 0.5F;
      float plateY = (screenHeight - plateH) * 0.5F;
      float animScale = 0.88F + 0.12F * EasingList.g.ease(open);
      matrices.push();
      matrices.translate(plateX + plateW * 0.5F, plateY + plateH * 0.5F, 0.0F);
      matrices.scale(animScale, animScale, 1.0F);
      matrices.translate(-(plateX + plateW * 0.5F), -(plateY + plateH * 0.5F), 0.0F);
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      int glowColor = ColorUtil.applyAlphaToColor(themeColor, 0.38F * open);
      int glassTint = ColorUtil.convertToARGB(9, 9, 13, (int)(165.0F * open));
      draw.drawThemedBlurredPanel(matrices, plateX, plateY, plateW, plateH, 12.0F, themeColor, 0.38F, glowColor, 20.0F, glassTint);
      draw.a(matrices, plateX, plateY, plateW, plateH, 12.0F, 0.75F, ColorUtil.applyAlphaToColor(themeColor, 0.32F * open));
      this.renderHeader(context, plateX, plateY, plateW, open, themeColor);
      this.renderCards(context, plateX, plateY + 54.0F, plateW, (float)scaledMouseX, (float)scaledMouseY, delta, open, themeColor);
      this.renderFooter(context, plateX, plateY + plateH - 38.0F, plateW, (float)scaledMouseX, (float)scaledMouseY, delta, open, themeColor);
      EffectMarker.a(matrices, delta, this.rippleEffects);
      matrices.pop();
      ScaleUtil.a(context);
   }

   private void renderHeader(DrawContext context, float plateX, float plateY, float plateW, float open, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      float headerH = 50.0F;
      float centerX = plateX + plateW * 0.5F;
      String logoText = "FLUX CLIENT";
      float logoW = Fonts.c.a(logoText, 14.0F);
      Fonts.c.a(matrices, GradientUtil.a(logoText, themeColor, 5.0F, 0.5F), centerX - logoW * 0.5F, plateY + 12.0F, 14.0F, 0.0F, open);
      String sub = "NEXT-GENERATION FABRIC CLIENT • 1.21.4";
      float subW = Fonts.e.a(sub, 6.25F);
      Fonts.e.a(matrices, sub, centerX - subW * 0.5F, plateY + 28.0F, 6.25F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(160, 160, 175, 255), open));
      draw.a(matrices, plateX + 16.0F, plateY + headerH, plateW - 32.0F, 0.75F, 0.0F, ColorUtil.applyAlphaToColor(themeColor, 0.22F * open));
   }

   private void renderCards(DrawContext context, float plateX, float startY, float plateW, float mouseX, float mouseY, float delta, float open, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      float itemW = plateW - 28.0F;
      float itemH = 32.0F;
      float gap = 6.0F;
      float itemX = plateX + 14.0F;

      for (int i = 0; i < this.menuItems.size(); i++) {
         MainScreen.MenuItem item = this.menuItems.get(i);
         float curY = startY + i * (itemH + gap);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, itemX, curY, itemW, itemH);
         item.hoverAnim = item.hoverAnim + ((hovered ? 1.0F : 0.0F) - item.hoverAnim) * Math.min(1.0F, delta * 12.0F);
         int cardBase = ColorUtil.convertToARGB(16, 16, 22, (int)(180.0F * open));
         int cardHover = ColorUtil.applyAlphaToColor(themeColor, 0.14F * item.hoverAnim * open);
         int cardBg = ColorUtil.lerpColor(cardBase, cardHover, item.hoverAnim);
         draw.a(matrices, itemX, curY, itemW, itemH, 6.5F, cardBg);
         int borderCol = ColorUtil.applyAlphaToColor(themeColor, (0.14F + 0.5F * item.hoverAnim) * open);
         draw.a(matrices, itemX, curY, itemW, itemH, 6.5F, 0.6F, borderCol);
         float iconBoxX = itemX + 5.0F;
         float iconBoxY = curY + 4.0F;
         float iconBoxS = 24.0F;
         int iconBoxBg = ColorUtil.applyAlphaToColor(themeColor, (0.12F + 0.24F * item.hoverAnim) * open);
         draw.a(matrices, iconBoxX, iconBoxY, iconBoxS, iconBoxS, 5.0F, iconBoxBg);
         draw.a(matrices, iconBoxX, iconBoxY, iconBoxS, iconBoxS, 5.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, (0.25F + 0.35F * item.hoverAnim) * open));
         float iconW = Fonts.a.a(item.icon, 8.5F);
         float iconX = iconBoxX + (iconBoxS - iconW) * 0.5F;
         float iconY = iconBoxY + (iconBoxS - Fonts.a.a(8.5F)) * 0.5F;
         Fonts.a.a(matrices, item.icon, iconX, iconY, 8.5F, ColorUtil.applyAlphaToColor(themeColor, open));
         float textX = iconBoxX + iconBoxS + 9.0F;
         int titleColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(235, 235, 245, (int)(255.0F * open)), themeColor, item.hoverAnim * 0.3F);
         Fonts.c.a(matrices, item.title, textX, curY + 6.0F, 7.5F, titleColor);
         int subColor = ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(140, 140, 155, 255), open);
         Fonts.e.a(matrices, item.subtitle, textX, curY + 17.5F, 5.75F, subColor);
      }
   }

   private void renderFooter(
      DrawContext context, float plateX, float footerY, float plateW, float mouseX, float mouseY, float delta, float open, int themeColor
   ) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      draw.a(matrices, plateX + 16.0F, footerY - 4.0F, plateW - 32.0F, 0.75F, 0.0F, ColorUtil.applyAlphaToColor(themeColor, 0.22F * open));
      float userX = plateX + 14.0F;
      float userY = footerY + 4.0F;
      float userW = 165.0F;
      float userH = 22.0F;
      draw.a(matrices, userX, userY, userW, userH, 5.5F, ColorUtil.convertToARGB(16, 16, 22, (int)(160.0F * open)));
      draw.a(matrices, userX, userY, userW, userH, 5.5F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, 0.2F * open));
      Fonts.a.a(matrices, "L", userX + 6.0F, userY + 5.5F, 7.5F, ColorUtil.applyAlphaToColor(themeColor, open));
      String username = Interface.mc.getSession() != null ? Interface.mc.getSession().getUsername() : "Kirka_int";
      Fonts.c.a(matrices, username, userX + 18.0F, userY + 6.0F, 6.75F, ColorUtil.applyAlphaToColor(-1, open));
      draw.a(matrices, userX + userW - 12.0F, userY + 8.5F, 5.0F, 5.0F, 2.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(46, 204, 113, 255), open));
      float quitW = 74.0F;
      float quitH = 22.0F;
      float quitX = plateX + plateW - 14.0F - quitW;
      float quitY = footerY + 4.0F;
      boolean quitHovered = MathUtil.isHovered(mouseX, mouseY, quitX, quitY, quitW, quitH);
      this.quitHoverAnim = this.quitHoverAnim + ((quitHovered ? 1.0F : 0.0F) - this.quitHoverAnim) * Math.min(1.0F, delta * 12.0F);
      int quitBg = ColorUtil.lerpColor(
         ColorUtil.convertToARGB(20, 16, 18, (int)(160.0F * open)), ColorUtil.convertToARGB(210, 45, 60, (int)(180.0F * open)), this.quitHoverAnim
      );
      draw.a(matrices, quitX, quitY, quitW, quitH, 5.5F, quitBg);
      int quitBorder = ColorUtil.lerpColor(
         ColorUtil.applyAlphaToColor(themeColor, 0.2F * open), ColorUtil.convertToARGB(255, 75, 90, (int)(230.0F * open)), this.quitHoverAnim
      );
      draw.a(matrices, quitX, quitY, quitW, quitH, 5.5F, 0.5F, quitBorder);
      float quitTextW = Fonts.c.a("Выход", 7.25F);
      float quitStartX = quitX + (quitW - quitTextW) * 0.5F;
      int quitCol = ColorUtil.lerpColor(
         ColorUtil.convertToARGB(210, 210, 220, (int)(220.0F * open)), ColorUtil.convertToARGB(255, 255, 255, (int)(255.0F * open)), this.quitHoverAnim
      );
      Fonts.c.a(matrices, "Выход", quitStartX, quitY + 7.0F, 7.25F, quitCol);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double scaledX = MathUtil.scale(mouseX, 2);
      double scaledY = MathUtil.scale(mouseY, 2);
      EffectMarker.a(this.rippleEffects, (float)scaledX, (float)scaledY);
      int screenWidth = (int)(
         (float)Interface.mc.getWindow().getFramebufferWidth() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      int screenHeight = (int)(
         (float)Interface.mc.getWindow().getFramebufferHeight() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      float plateW = 340.0F;
      float plateH = 260.0F;
      float plateX = (screenWidth - plateW) * 0.5F;
      float plateY = (screenHeight - plateH) * 0.5F;
      float itemW = plateW - 28.0F;
      float itemH = 32.0F;
      float gap = 6.0F;
      float itemX = plateX + 14.0F;
      float startY = plateY + 54.0F;

      for (int i = 0; i < this.menuItems.size(); i++) {
         float curY = startY + i * (itemH + gap);
         if (MathUtil.isHovered(scaledX, scaledY, itemX, curY, itemW, itemH)) {
            this.menuItems.get(i).action.run();
            return true;
         }
      }

      float quitW = 74.0F;
      float quitH = 22.0F;
      float quitX = plateX + plateW - 14.0F - quitW;
      float quitY = plateY + plateH - 38.0F + 4.0F;
      if (MathUtil.isHovered(scaledX, scaledY, quitX, quitY, quitW, quitH)) {
         Interface.mc.scheduleStop();
         return true;
      } else {
         return super.mouseClicked(scaledX, scaledY, button);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return keyCode == 256 ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void close() {
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public static class MenuItem {
      public final String title;
      public final String subtitle;
      public final String icon;
      public final Runnable action;
      public float hoverAnim = 0.0F;

      public MenuItem(String title, String subtitle, String icon, Runnable action) {
         this.title = title;
         this.subtitle = subtitle;
         this.icon = icon;
         this.action = action;
      }
   }
}
