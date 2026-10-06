package ru.kirka.fluxclient.ui.screen;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.gui.hud.NotificationManager;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScaleUtil;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.element.TextField;
import ru.kirka.fluxclient.util.AccountManager;
import ru.kirka.fluxclient.util.MathUtil;

public class AltScreen extends Screen {
   private final AnimationUtil openAnim = new AnimationUtil();
   private final TextField nameField;
   private float scroll = 0.0F;
   private float targetScroll = 0.0F;

   public AltScreen() {
      super(Text.empty());
      this.nameField = new TextField(TextField.type.ALT_MANAGER);
      this.nameField.setPlaceholder("Никнейм для входа...");
   }

   protected void init() {
      this.openAnim.c(0.0F);
      this.openAnim.a(true);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.openAnim.a(Interface.mc.currentScreen instanceof AltScreen);
      this.openAnim.a(0.0F, 1.0F, 0.18F, EasingList.g, delta);
      float open = Math.min(1.0F, this.openAnim.c() / 0.9F);
      double scaledX = MathUtil.scale(mouseX, 2);
      double scaledY = MathUtil.scale(mouseY, 2);
      ScaleUtil.a(context, 2);
      int screenWidth = (int)(
         (float)Interface.mc.getWindow().getFramebufferWidth() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      int screenHeight = (int)(
         (float)Interface.mc.getWindow().getFramebufferHeight() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      MainScreen.renderParallaxBackground(context, screenWidth, screenHeight, (int)scaledX, (int)scaledY, EasingList.s.ease(open) * 0.2F + 1.05F);
      FluxClient.getInstance().getDraw2DProcessor().e().a(context.getMatrices());
      float ease = EasingList.s.ease(open);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(screenWidth * 0.5F, screenHeight * 0.5F, 0.0F);
      float scale = ease * 0.15F + 0.85F;
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-screenWidth * 0.5F, -screenHeight * 0.5F, 0.0F);
      float panelW = 280.0F;
      float panelH = 320.0F;
      float panelX = (screenWidth - panelW) * 0.5F;
      float panelY = (screenHeight - panelH) * 0.5F;
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      int glassTint = ColorUtil.convertToARGB(10, 10, 14, (int)(185.0F * open));
      draw.drawThemedBlurredPanel(
         matrices, panelX, panelY, panelW, panelH, 12.0F, themeColor, 0.1F, ColorUtil.applyAlphaToColor(themeColor, 0.35F * open), 20.0F, glassTint
      );
      draw.a(matrices, panelX, panelY, panelW, panelH, 12.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, 0.35F * open));
      float backW = 46.0F;
      float backH = 16.0F;
      float backX = panelX + 12.0F;
      float backY = panelY + 12.0F;
      boolean backHovered = MathUtil.isHovered(scaledX, scaledY, backX, backY, backW, backH);
      draw.a(
         matrices,
         backX,
         backY,
         backW,
         backH,
         3.5F,
         ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), (backHovered ? 0.12F : 0.05F) * open)
      );
      Fonts.c
         .b(
            matrices,
            "Назад",
            backX + backW * 0.5F,
            Fonts.c.a("Назад", 6.0F, backY + backH * 0.5F),
            6.0F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(220, 220, 230, 255), open)
         );
      Fonts.c
         .b(
            matrices,
            "Менеджер Аккаунтов",
            panelX + panelW * 0.5F,
            Fonts.c.a("Менеджер Аккаунтов", 9.0F, panelY + 18.0F),
            9.0F,
            ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), open)
         );
      draw.a(
         matrices,
         panelX + 12.0F,
         panelY + 36.0F,
         panelW - 24.0F,
         0.5F,
         0.0F,
         ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.1F * open)
      );
      float fieldW = 186.0F;
      float fieldH = 22.0F;
      float fieldX = panelX + 14.0F;
      float fieldY = panelY + 44.0F;
      this.nameField.setPosition(new Vector2f(fieldX, fieldY));
      this.nameField.setSize(new Vector2f(fieldW, fieldH));
      this.nameField.render(context, scaledX, scaledY, delta, open);
      float addBtnX = fieldX + fieldW + 8.0F;
      float addBtnW = panelW - 28.0F - fieldW - 8.0F;
      boolean addHovered = MathUtil.isHovered(scaledX, scaledY, addBtnX, fieldY, addBtnW, fieldH);
      draw.a(matrices, addBtnX, fieldY, addBtnW, fieldH, 4.0F, ColorUtil.applyAlphaToColor(themeColor, (addHovered ? 0.95F : 0.8F) * open));
      Fonts.c
         .b(
            matrices,
            "+ Войти",
            addBtnX + addBtnW * 0.5F,
            Fonts.c.a("+ Войти", 7.0F, fieldY + fieldH * 0.5F),
            7.0F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), open)
         );
      float listX = panelX + 14.0F;
      float listY = panelY + 74.0F;
      float listW = panelW - 28.0F;
      float listH = panelH - 88.0F;
      draw.a(matrices, listX, listY, listW, listH, 6.0F, ColorUtil.convertToARGB(14, 14, 20, (int)(120.0F * open)));
      draw.a(matrices, listX, listY, listW, listH, 6.0F, 0.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.05F * open));
      float innerPad = 8.0F;
      float innerListX = listX + innerPad;
      float innerListY = listY + innerPad;
      float innerListW = listW - innerPad * 2.0F;
      float innerListH = listH - innerPad * 2.0F;
      this.scroll = this.scroll + (this.targetScroll - this.scroll) * 0.25F;
      List<AccountManager.Entry> accounts = AccountManager.getAccounts();
      String currentNick = AccountManager.currentName();
      float cardH = 26.0F;
      float cardStep = 32.0F;
      if (accounts.isEmpty()) {
         float var10000 = 0.0F;
      } else {
         float var84 = accounts.size() * cardStep - 6.0F;
      }

      float minScroll = Math.min(0.0F, innerListH - accounts.size() * cardStep);
      if (this.targetScroll < minScroll) {
         this.targetScroll = minScroll;
      }

      if (this.targetScroll > 0.0F) {
         this.targetScroll = 0.0F;
      }

      float scrollbarSpace = minScroll < 0.0F ? 8.0F : 0.0F;
      float cardW = innerListW - scrollbarSpace;
      ScissorUtil.a(matrices, innerListX - 2.0F, innerListY - 1.0F, innerListW + 4.0F, innerListH + 2.0F);
      matrices.push();
      matrices.translate(0.0F, this.scroll, 0.0F);
      boolean mouseInList = MathUtil.isHovered(scaledX, scaledY, innerListX, innerListY, innerListW, innerListH);
      float curCardY = innerListY;

      for (AccountManager.Entry acc : accounts) {
         boolean isCurrent = currentNick != null && currentNick.equalsIgnoreCase(acc.name);
         boolean cardHovered = mouseInList && MathUtil.isHovered(scaledX, scaledY - this.scroll, innerListX, curCardY, cardW, cardH);
         int cardBg = ColorUtil.applyAlphaToColor(
            isCurrent
               ? ColorUtil.lerpColor(ColorUtil.convertToARGB(20, 20, 25, 255), themeColor, 0.18F)
               : ColorUtil.convertToARGB(25, 25, 30, (int)(180.0F * open)),
            open
         );
         if (cardHovered && !isCurrent) {
            cardBg = ColorUtil.lerpColor(cardBg, ColorUtil.convertToARGB(255, 255, 255, 255), 0.06F);
         }

         draw.a(matrices, innerListX, curCardY, cardW, cardH, 4.5F, cardBg);
         int borderCol = isCurrent ? themeColor : ColorUtil.convertToARGB(255, 255, 255, (int)(32.0F * open));
         draw.a(matrices, innerListX, curCardY, cardW, cardH, 4.5F, 0.5F, ColorUtil.applyAlphaToColor(borderCol, open));
         float headSize = 18.0F;
         float headX = innerListX + 4.5F;
         float headY = curCardY + (cardH - headSize) * 0.5F;
         UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc.name).getBytes(StandardCharsets.UTF_8));
         Identifier skinTex = DefaultSkinHelper.getSkinTextures(uuid).texture();
         draw.drawPlayerHead(matrices, skinTex, null, headX, headY, headSize, headSize, 3.5F, open);
         float delBtnW = 16.0F;
         float delBtnX = innerListX + cardW - delBtnW - 5.0F;
         float delBtnY = curCardY + (cardH - delBtnW) * 0.5F;
         boolean delHover = mouseInList && MathUtil.isHovered(scaledX, scaledY - this.scroll, delBtnX, delBtnY, delBtnW, delBtnW);
         draw.a(
            matrices,
            delBtnX,
            delBtnY,
            delBtnW,
            delBtnW,
            3.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(220, 50, 50, 255), (delHover ? 0.45F : 0.15F) * open)
         );
         draw.a(
            matrices,
            delBtnX,
            delBtnY,
            delBtnW,
            delBtnW,
            3.5F,
            0.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 80, 80, 255), (delHover ? 0.7F : 0.3F) * open)
         );
         Fonts.a
            .b(
               matrices,
               "Y",
               delBtnX + delBtnW * 0.5F,
               Fonts.a.a("Y", 6.0F, delBtnY + delBtnW * 0.5F),
               6.0F,
               ColorUtil.applyAlphaToColor(delHover ? ColorUtil.convertToARGB(255, 255, 255, 255) : ColorUtil.convertToARGB(255, 140, 140, 255), open)
            );
         float actionBtnH = 16.0F;
         float actionBtnY = curCardY + (cardH - actionBtnH) * 0.5F;
         float rightLimit;
         if (isCurrent) {
            float badgeW = 50.0F;
            float badgeX = delBtnX - badgeW - 6.0F;
            rightLimit = badgeX;
            draw.a(matrices, badgeX, actionBtnY, badgeW, actionBtnH, 3.5F, ColorUtil.applyAlphaToColor(themeColor, 0.22F * open));
            draw.a(matrices, badgeX, actionBtnY, badgeW, actionBtnH, 3.5F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, 0.65F * open));
            Fonts.a.a(matrices, "b", badgeX + 5.0F, Fonts.a.a("b", 5.0F, actionBtnY + actionBtnH * 0.5F), 5.0F, ColorUtil.applyAlphaToColor(themeColor, open));
            Fonts.c
               .a(
                  matrices,
                  "Активен",
                  badgeX + 15.0F,
                  Fonts.c.a("Активен", 5.5F, actionBtnY + actionBtnH * 0.5F),
                  5.5F,
                  ColorUtil.applyAlphaToColor(themeColor, open)
               );
         } else {
            float selectW = 44.0F;
            float selectX = delBtnX - selectW - 6.0F;
            rightLimit = selectX;
            boolean selHover = mouseInList && MathUtil.isHovered(scaledX, scaledY - this.scroll, selectX, actionBtnY, selectW, actionBtnH);
            draw.a(matrices, selectX, actionBtnY, selectW, actionBtnH, 3.5F, ColorUtil.applyAlphaToColor(themeColor, (selHover ? 0.9F : 0.45F) * open));
            draw.a(matrices, selectX, actionBtnY, selectW, actionBtnH, 3.5F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, (selHover ? 1.0F : 0.7F) * open));
            Fonts.c
               .b(
                  matrices,
                  "Выбрать",
                  selectX + selectW * 0.5F,
                  Fonts.c.a("Выбрать", 5.5F, actionBtnY + actionBtnH * 0.5F),
                  5.5F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), open)
               );
         }

         float nickX = headX + headSize + 7.0F;
         float nickMaxW = rightLimit - 6.0F - nickX;
         Fonts.c
            .a(
               matrices,
               acc.name,
               nickX,
               Fonts.c.a(acc.name, 7.0F, curCardY + cardH * 0.5F),
               7.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(245, 245, 255, 255), open),
               nickMaxW,
               false,
               0.0F,
               0.0F
            );
         curCardY += cardStep;
      }

      matrices.pop();
      ScissorUtil.a(matrices);
      if (accounts.isEmpty()) {
         Fonts.c
            .b(
               matrices,
               "Список аккаунтов пуст",
               panelX + panelW * 0.5F,
               listY + listH * 0.5F - 6.0F,
               7.5F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(180, 180, 190, 255), open)
            );
         Fonts.c
            .b(
               matrices,
               "Введите ник выше для входа",
               panelX + panelW * 0.5F,
               listY + listH * 0.5F + 6.0F,
               6.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(130, 130, 140, 255), open)
            );
      }

      if (minScroll < 0.0F) {
         float scrollRatio = innerListH / (accounts.size() * cardStep);
         float barHeight = Math.max(16.0F, innerListH * scrollRatio);
         float maxScrollProgress = this.scroll / minScroll;
         float barY = innerListY + (innerListH - barHeight) * maxScrollProgress;
         draw.a(matrices, innerListX + innerListW - 3.0F, barY, 2.5F, barHeight, 1.25F, ColorUtil.applyAlphaToColor(themeColor, 0.55F * open));
      }

      matrices.pop();
      ScaleUtil.a(context);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double scaledX = MathUtil.scale(mouseX, 2);
      double scaledY = MathUtil.scale(mouseY, 2);
      int screenWidth = (int)(
         (float)Interface.mc.getWindow().getFramebufferWidth() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      int screenHeight = (int)(
         (float)Interface.mc.getWindow().getFramebufferHeight() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
      );
      float panelW = 280.0F;
      float panelH = 320.0F;
      float panelX = (screenWidth - panelW) * 0.5F;
      float panelY = (screenHeight - panelH) * 0.5F;
      float backW = 46.0F;
      float backH = 16.0F;
      float backX = panelX + 12.0F;
      float backY = panelY + 12.0F;
      if (button == 0 && MathUtil.isHovered(scaledX, scaledY, backX, backY, backW, backH)) {
         this.close();
         return true;
      } else {
         this.nameField.onMouseClick(scaledX, scaledY, button);
         if (this.nameField.isFocused()) {
            return true;
         } else {
            float fieldW = 186.0F;
            float fieldH = 22.0F;
            float fieldX = panelX + 14.0F;
            float fieldY = panelY + 44.0F;
            float addBtnX = fieldX + fieldW + 8.0F;
            float addBtnW = panelW - 28.0F - fieldW - 8.0F;
            if (button == 0 && MathUtil.isHovered(scaledX, scaledY, addBtnX, fieldY, addBtnW, fieldH)) {
               this.submitNewAccount();
               return true;
            } else {
               float listX = panelX + 14.0F;
               float listY = panelY + 74.0F;
               float listW = panelW - 28.0F;
               float listH = panelH - 88.0F;
               float innerPad = 8.0F;
               float innerListX = listX + innerPad;
               float innerListY = listY + innerPad;
               float innerListW = listW - innerPad * 2.0F;
               float innerListH = listH - innerPad * 2.0F;
               if (MathUtil.isHovered(scaledX, scaledY, innerListX, innerListY, innerListW, innerListH)) {
                  double cardMouseY = scaledY - this.scroll;
                  float curCardY = innerListY;
                  List<AccountManager.Entry> accounts = AccountManager.getAccounts();
                  float cardH = 26.0F;
                  float cardStep = 32.0F;
                  float minScroll = Math.min(0.0F, innerListH - accounts.size() * cardStep);
                  float scrollbarSpace = minScroll < 0.0F ? 8.0F : 0.0F;
                  float cardW = innerListW - scrollbarSpace;

                  for (AccountManager.Entry acc : accounts) {
                     if (MathUtil.isHovered(scaledX, cardMouseY, innerListX, curCardY, cardW, cardH)) {
                        float delBtnW = 16.0F;
                        float delBtnX = innerListX + cardW - delBtnW - 5.0F;
                        float delBtnY = curCardY + (cardH - delBtnW) * 0.5F;
                        if (button == 0 && MathUtil.isHovered(scaledX, cardMouseY, delBtnX, delBtnY, delBtnW, delBtnW)) {
                           AccountManager.removeAccount(acc.name);
                           NotificationManager.notifyInfo("AltManager", "Удален аккаунт: " + acc.name);
                           return true;
                        }

                        if (button == 0) {
                           this.selectAccount(acc.name);
                           return true;
                        }
                     }

                     curCardY += cardStep;
                  }
               }

               return super.mouseClicked(mouseX, mouseY, button);
            }
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.targetScroll += (float)verticalAmount * 24.0F;
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.nameField.isFocused()) {
         if (keyCode == 256) {
            this.close();
            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      } else {
         this.nameField.a(keyCode, scanCode, modifiers);
         if (keyCode == 257 || keyCode == 335) {
            this.submitNewAccount();
         }

         return true;
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.nameField.isFocused()) {
         this.nameField.a(chr, modifiers);
         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }

   private void submitNewAccount() {
      String nick = this.nameField.getTextBuffer().toString().trim();
      if (!nick.isEmpty()) {
         String error = AccountManager.addAccount(nick);
         if (error != null) {
            NotificationManager.notifyInfo("AltManager", error);
         } else {
            this.selectAccount(nick);
            this.nameField.a();
         }
      }
   }

   private void selectAccount(String name) {
      boolean ok = AccountManager.switchTo(name);
      if (ok) {
         NotificationManager.notifyInfo("AltManager", "Вход выполнен: " + name);
      } else {
         NotificationManager.notifyInfo("AltManager", "Не удалось сменить ник");
      }
   }

   public void close() {
      Interface.mc.setScreen(new MainScreen());
   }

   public boolean shouldPause() {
      return false;
   }
}
