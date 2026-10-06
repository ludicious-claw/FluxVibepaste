package ru.kirka.fluxclient.ui.screen;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.ConfigManager;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;
import ru.kirka.fluxclient.notification.Notification;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScaleUtil;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.element.ColorElement;
import ru.kirka.fluxclient.ui.element.Element;
import ru.kirka.fluxclient.ui.element.TextField;
import ru.kirka.fluxclient.util.KeyUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class GUIScreen extends Screen {
   public static final List<GUIScreen.ThemePreset> THEME_PRESETS = List.of(
      new GUIScreen.ThemePreset("Flux Crimson", 235, 45, 60),
      new GUIScreen.ThemePreset("Amethyst Violet", 157, 78, 221),
      new GUIScreen.ThemePreset("Sapphire Blue", 58, 134, 255),
      new GUIScreen.ThemePreset("Emerald Mint", 46, 196, 182),
      new GUIScreen.ThemePreset("Sunset Orange", 255, 107, 107),
      new GUIScreen.ThemePreset("Monochrome White", 240, 240, 245)
   );
   private final AnimationUtil openAnimation = new AnimationUtil();
   private boolean closing = false;
   private GUIScreen.NavTab currentTab = GUIScreen.NavTab.COMBAT;
   private float tabSlideOffset = 0.0F;
   private float indicatorY = 42.0F;
   private float[] tabScrolls = new float[GUIScreen.NavTab.values().length];
   private float[] tabScrollTargets = new float[GUIScreen.NavTab.values().length];
   private TextField searchField;
   private TextField newConfigField;
   private Module hoveredModule = null;
   private float[] hoverTints = new float[GUIScreen.NavTab.values().length];
   private float[] moduleHoverTints = new float[1000];
   private final float PLATE_WIDTH = 520.0F;
   private final float PLATE_HEIGHT = 340.0F;
   private final float SIDEBAR_WIDTH = 125.0F;
   private final float CONTENT_WIDTH = 395.0F;
   private final float HEADER_HEIGHT = 38.0F;
   private final float CONTENT_HEIGHT = 302.0F;

   public GUIScreen() {
      this(Text.literal("Flux ClickGUI"));
   }

   public GUIScreen(Text title) {
      super(title);
   }

   protected void init() {
      this.openAnimation.setValue(0.0F);
      this.openAnimation.setPreviousValue(0.0F);
      this.openAnimation.setAnimationValue(0.0F);
      this.openAnimation.resetTimer();
      this.closing = false;
      this.searchField = new TextField(TextField.type.GUI);
      this.searchField.setPlaceholder("Поиск...");
      this.searchField.setSize(new Vector2f(140.0F, 16.0F));
      this.newConfigField = new TextField(TextField.type.GUI);
      this.newConfigField.setPlaceholder("Имя конфига");
      this.newConfigField.setSize(new Vector2f(120.0F, 18.0F));
   }

   public void startClosing() {
      if (!this.closing) {
         this.closing = true;
         this.openAnimation.a(false);
      }
   }

   public void close() {
      if (!this.closing) {
         this.startClosing();
      } else {
         super.close();
         ClickGuiModule clickGui = FluxClient.getInstance().getModuleManager().getModule(ClickGuiModule.class);
         if (clickGui != null) {
            clickGui.setEnabled(false);
         }
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      double mx = MathUtil.scale(mouseX, 2);
      double my = MathUtil.scale(mouseY, 2);
      this.openAnimation.a(!this.closing);
      if (this.closing) {
         this.openAnimation.a(0.0F, 1.0F, 0.32F, EasingList.i, delta);
         if (this.openAnimation.getValue() <= 0.02F) {
            this.close();
            return;
         }
      } else {
         this.openAnimation.a(0.0F, 1.0F, 0.2F, EasingList.s, delta);
      }

      float anim = this.openAnimation.c();
      float rawProgress = MathUtil.clamp(this.openAnimation.getValue(), 0.0F, 1.0F);
      float alpha = MathUtil.clamp(rawProgress, 0.0F, 1.0F);
      float scale = this.closing ? 0.9F + 0.1F * rawProgress : 0.86F + 0.14F * anim;
      float translateY = this.closing ? (1.0F - rawProgress) * 12.0F : (1.0F - MathUtil.clamp(anim, 0.0F, 1.0F)) * 14.0F;
      ScaleUtil.a(context, 2);
      float screenWidth = Interface.mc.getWindow().getScaledWidth() / 2.0F;
      float screenHeight = Interface.mc.getWindow().getScaledHeight() / 2.0F;
      float startX = screenWidth - 260.0F;
      float startY = screenHeight - 170.0F;
      matrices.push();
      matrices.translate(screenWidth, screenHeight + translateY, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-screenWidth, -screenHeight, 0.0F);
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor themeProcessor = FluxClient.getInstance().getThemeProcessor();
      int themeColor = themeProcessor.a(ThemeInfo.PRIMARY).toIntColor();
      draw.e().a(matrices);
      matrices.push();
      matrices.translate(screenWidth, screenHeight + translateY, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-screenWidth, -screenHeight, 0.0F);
      int glassTint = ColorUtil.convertToARGB(8, 8, 12, (int)(90.0F * alpha));
      draw.drawThemedBlurredPanel(matrices, startX, startY, 520.0F, 340.0F, 12.0F, themeColor, 0.35F, 0, 0.0F, glassTint);
      draw.a(matrices, startX, startY, 520.0F, 340.0F, 12.0F, 0.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.08F * alpha));
      this.renderSidebar(matrices, draw, startX, startY, alpha, mx, my, delta, themeColor);
      this.renderContent(context, draw, startX + 125.0F, startY, alpha, mx, my, delta, themeColor);
      this.hoveredModule = null;
      matrices.pop();
      ScaleUtil.a(context);
   }

   private void renderSidebar(MatrixStack matrices, Draw2DProcessor draw, float x, float y, float alpha, double mx, double my, float delta, int themeColor) {
      draw.a(matrices, x + 125.0F, y, 0.5F, 340.0F, 0.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.08F * alpha));
      float logoX = x + 16.0F;
      float logoY = y + 16.0F;
      Fonts.c.a(matrices, "FLUX", logoX, logoY, 12.0F, ColorUtil.applyAlphaToColor(themeColor, alpha));
      Fonts.c
         .a(
            matrices,
            "CLIENT",
            logoX + Fonts.c.a("FLUX", 12.0F) + 4.0F,
            logoY + 2.0F,
            7.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(240, 240, 245, 255), 0.85F * alpha)
         );
      Fonts.e.a(matrices, "v2.1 • 1.21.4", logoX, logoY + 13.5F, 5.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(130, 135, 145, 255), alpha));
      draw.a(matrices, x + 12.0F, y + 38.0F, 101.0F, 0.5F, 0.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.07F * alpha));
      float tabY = y + 44.0F;
      int activeIndex = this.currentTab.ordinal();
      float targetIndicatorY = tabY + activeIndex * 25.0F;
      if (activeIndex >= GUIScreen.NavTab.CONFIGS.ordinal()) {
         targetIndicatorY += 5.0F;
      }

      this.indicatorY = this.indicatorY + (targetIndicatorY - this.indicatorY) * 0.25F;
      draw.a(matrices, x + 7.0F, this.indicatorY, 111.0F, 22.0F, 5.0F, ColorUtil.applyAlphaToColor(themeColor, 0.18F * alpha));
      draw.a(matrices, x + 7.0F, this.indicatorY + 3.5F, 2.0F, 15.0F, 1.0F, ColorUtil.applyAlphaToColor(themeColor, 0.95F * alpha));

      for (int i = 0; i < GUIScreen.NavTab.values().length; i++) {
         GUIScreen.NavTab tab = GUIScreen.NavTab.values()[i];
         float currentTabY = tabY + i * 25.0F;
         if (i >= GUIScreen.NavTab.CONFIGS.ordinal()) {
            currentTabY += 5.0F;
            if (i == GUIScreen.NavTab.CONFIGS.ordinal()) {
               draw.a(
                  matrices,
                  x + 16.0F,
                  currentTabY - 3.0F,
                  93.0F,
                  0.5F,
                  0.0F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.06F * alpha)
               );
            }
         }

         boolean hovered = MathUtil.isHovered(mx, my, x + 7.0F, currentTabY, 111.0F, 22.0F);
         this.hoverTints[i] = this.hoverTints[i] + ((hovered ? 0.06F : 0.0F) - this.hoverTints[i]) * 0.2F;
         if (this.hoverTints[i] > 0.005F && i != activeIndex) {
            draw.a(
               matrices,
               x + 7.0F,
               currentTabY,
               111.0F,
               22.0F,
               5.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), this.hoverTints[i] * alpha)
            );
         }

         int iconColor = i == activeIndex ? themeColor : ColorUtil.convertToARGB(160, 165, 175, 255);
         int textColor = i == activeIndex ? ColorUtil.convertToARGB(255, 255, 255, 255) : ColorUtil.convertToARGB(190, 195, 205, 255);
         iconColor = ColorUtil.applyAlphaToColor(iconColor, alpha);
         textColor = ColorUtil.applyAlphaToColor(textColor, alpha);
         Fonts.a.a(matrices, tab.icon, x + 16.0F, Fonts.a.a(tab.icon, 7.5F, currentTabY + 11.0F), 7.5F, iconColor);
         Fonts.c.a(matrices, tab.name, x + 31.0F, Fonts.c.a(tab.name, 6.75F, currentTabY + 11.0F), 6.75F, textColor);
         if (tab.category != null) {
            List<Module> mods = FluxClient.getInstance().getModuleManager().getModulesByCategory(tab.category);
            long activeMods = mods.stream().filter(Module::isEnabled).count();
            String badge = activeMods + "/" + mods.size();
            float badgeW = Fonts.e.a(badge, 5.5F) + 6.0F;
            float badgeX = x + 125.0F - 10.0F - badgeW;
            draw.a(
               matrices,
               badgeX,
               currentTabY + 5.0F,
               badgeW,
               12.0F,
               3.0F,
               ColorUtil.applyAlphaToColor(
                  activeMods > 0L ? themeColor : ColorUtil.convertToARGB(255, 255, 255, 255), (i == activeIndex ? 0.15F : 0.06F) * alpha
               )
            );
            Fonts.e
               .b(
                  matrices,
                  badge,
                  badgeX + badgeW / 2.0F,
                  Fonts.e.a(badge, 5.5F, currentTabY + 11.0F),
                  5.5F,
                  ColorUtil.applyAlphaToColor(activeMods > 0L ? themeColor : ColorUtil.convertToARGB(130, 135, 145, 255), alpha)
               );
         }
      }

      float profileY = y + 340.0F - 36.0F;
      draw.a(matrices, x + 8.0F, profileY, 109.0F, 26.0F, 5.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.04F * alpha));
      draw.a(matrices, x + 8.0F, profileY, 109.0F, 26.0F, 5.0F, 0.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.06F * alpha));
      if (Interface.mc.player != null) {
         draw.drawPlayerHead(
            matrices, Interface.mc.player.getSkinTextures().texture(), Interface.mc.player, x + 12.0F, profileY + 4.0F, 18.0F, 18.0F, 3.0F, alpha
         );
         String name = Interface.mc.player.getName().getString();
         if (name.length() > 9) {
            name = name.substring(0, 8) + "..";
         }

         Fonts.c.a(matrices, name, x + 35.0F, profileY + 7.0F, 6.25F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha));
         Fonts.e.a(matrices, "v2.1", x + 35.0F, profileY + 16.5F, 5.25F, ColorUtil.applyAlphaToColor(themeColor, alpha));
      } else {
         Fonts.c.a(matrices, "Flux User", x + 35.0F, profileY + 7.0F, 6.25F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha));
         Fonts.e.a(matrices, "v2.1", x + 35.0F, profileY + 16.5F, 5.25F, ColorUtil.applyAlphaToColor(themeColor, alpha));
      }
   }

   private void renderContent(DrawContext context, Draw2DProcessor draw, float x, float y, float alpha, double mx, double my, float delta, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      this.tabSlideOffset = this.tabSlideOffset + (0.0F - this.tabSlideOffset) * 0.2F;
      float contentAlpha = alpha * (1.0F - Math.abs(this.tabSlideOffset) / 18.0F);
      contentAlpha = MathHelper.clamp(contentAlpha, 0.0F, 1.0F);
      float contentX = x + this.tabSlideOffset;
      Fonts.c
         .a(
            matrices,
            this.currentTab.name,
            contentX + 16.0F,
            y + 13.0F,
            10.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), contentAlpha)
         );
      String subText;
      if (this.hoveredModule != null && this.hoveredModule.getDescription() != null && !this.hoveredModule.getDescription().isEmpty()) {
         subText = this.hoveredModule.getDescription();
      } else if (this.currentTab.category != null) {
         subText = "Настройка функционала клиента";
      } else if (this.currentTab == GUIScreen.NavTab.CONFIGS) {
         subText = "Управление файлами конфигурации";
      } else {
         subText = "Выбор визуальной темы";
      }

      int subColor = this.hoveredModule != null
         ? ColorUtil.applyAlphaToColor(themeColor, contentAlpha)
         : ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(140, 145, 155, 255), contentAlpha);
      Fonts.c.a(matrices, subText, contentX + 16.0F, y + 25.0F, 6.25F, subColor);
      this.searchField.setPosition(new Vector2f(contentX + 395.0F - 150.0F, y + 10.0F));
      this.searchField.render(context, (int)mx, (int)my, delta, contentAlpha);
      if (!this.searchField.getTextBuffer().isEmpty()) {
         float clearX = contentX + 395.0F - 22.0F;
         float clearY = y + 18.0F;
         Fonts.e
            .b(
               matrices,
               "x",
               clearX,
               Fonts.e.a("x", 6.0F, clearY),
               6.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(180, 185, 195, 255), contentAlpha)
            );
      }

      draw.a(
         matrices,
         contentX + 12.0F,
         y + 38.0F,
         371.0F,
         0.5F,
         0.0F,
         ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.07F * contentAlpha)
      );
      float bodyY = y + 38.0F;
      ScissorUtil.a(matrices, contentX, bodyY + 1.0F, 395.0F, 301.0F);
      int tabIdx = this.currentTab.ordinal();
      this.tabScrolls[tabIdx] = this.tabScrolls[tabIdx] + (this.tabScrollTargets[tabIdx] - this.tabScrolls[tabIdx]) * 0.25F;
      float scroll = this.tabScrolls[tabIdx];
      matrices.push();
      matrices.translate(0.0F, scroll, 0.0F);
      float maxScroll = 0.0F;
      if (this.currentTab.category != null) {
         maxScroll = this.renderModules(context, draw, contentX, bodyY, contentAlpha, mx, my - scroll, delta, themeColor);
      } else if (this.currentTab == GUIScreen.NavTab.CONFIGS) {
         maxScroll = this.renderConfigs(context, draw, contentX, bodyY, contentAlpha, mx, my - scroll, delta, themeColor);
      } else if (this.currentTab == GUIScreen.NavTab.THEMES) {
         maxScroll = this.renderThemes(context, draw, contentX, bodyY, contentAlpha, mx, my - scroll, delta, themeColor);
      }

      matrices.pop();
      ScissorUtil.a(matrices);
      float contentMinScroll = Math.min(0.0F, 302.0F - maxScroll);
      if (this.tabScrollTargets[tabIdx] < contentMinScroll) {
         this.tabScrollTargets[tabIdx] = contentMinScroll;
      }

      if (this.tabScrollTargets[tabIdx] > 0.0F) {
         this.tabScrollTargets[tabIdx] = 0.0F;
      }

      if (contentMinScroll < 0.0F) {
         float scrollRatio = 302.0F / maxScroll;
         float barHeight = Math.max(20.0F, 302.0F * scrollRatio);
         float maxScrollProgress = scroll / contentMinScroll;
         float barY = bodyY + (302.0F - barHeight) * maxScrollProgress;
         draw.a(matrices, contentX + 395.0F - 5.0F, barY, 2.0F, barHeight, 1.0F, ColorUtil.applyAlphaToColor(themeColor, 0.6F * contentAlpha));
      }

      if (this.currentTab.category != null) {
         matrices.push();
         matrices.translate(0.0F, scroll, 0.0F);

         for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
            if (m.isExtended()) {
               for (Element<?> e : m.getElements()) {
                  e.renderColorPicker(context, (int)mx, (int)(my - scroll), delta);
               }
            }
         }

         matrices.pop();
      }
   }

   private float renderModules(DrawContext context, Draw2DProcessor draw, float x, float y, float alpha, double mx, double my, float delta, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      float padding = 10.0F;
      float gap = 9.0F;
      float colWidth = 183.0F;
      float[] colY = new float[]{y + padding, y + padding};
      List<Module> mods = FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category);
      String search = this.searchField.getTextBuffer().toString().toLowerCase();
      int i = 0;

      for (Module m : mods) {
         if (search.isEmpty() || m.getName().toLowerCase().contains(search)) {
            m.getExtendAnimation().a(m.isExtended());
            m.getExtendAnimation().a(0.0F, 1.0F, 0.28F, EasingList.g, delta);
            m.getEnableAnimation().a(m.isEnabled());
            m.getEnableAnimation().a(0.0F, 1.0F, 0.28F, EasingList.i, delta);
            int col = colY[0] <= colY[1] ? 0 : 1;
            float cx = x + padding + col * (colWidth + gap);
            float cy = colY[col];
            float headerHeight = 24.0F;
            float extAnim = m.getExtendAnimation().c();
            float elementsHeight = 0.0F;
            if (extAnim > 0.001F) {
               for (Element<?> e : m.getElements()) {
                  if (e.isEnabled()) {
                     elementsHeight += e.getHeight() + 4.0F;
                  }
               }
            }

            float cardHeight = headerHeight + elementsHeight * extAnim + (extAnim > 0.001F ? 6.0F * extAnim : 0.0F);
            boolean hovered = MathUtil.isHovered(mx, my, cx, cy, colWidth, headerHeight);
            if (hovered) {
               this.hoveredModule = m;
            }

            int idx = Math.min(i++, this.moduleHoverTints.length - 1);
            this.moduleHoverTints[idx] = this.moduleHoverTints[idx] + ((hovered ? 0.04F : 0.0F) - this.moduleHoverTints[idx]) * 0.2F;
            int bgBase = ColorUtil.convertToARGB(18, 18, 25, (int)(110.0F * alpha));
            int bgActive = ColorUtil.applyAlphaToColor(themeColor, 0.14F * alpha);
            int bgColor = ColorUtil.lerpColor(bgBase, bgActive, m.getEnableAnimation().c());
            if (this.moduleHoverTints[idx] > 0.005F) {
               bgColor = ColorUtil.lerpColor(bgColor, ColorUtil.convertToARGB(255, 255, 255, 255), this.moduleHoverTints[idx]);
            }

            draw.a(matrices, cx, cy, colWidth, cardHeight, 6.0F, bgColor);
            int borderBase = ColorUtil.convertToARGB(255, 255, 255, (int)(16.0F * alpha));
            int borderActive = ColorUtil.applyAlphaToColor(themeColor, 0.4F * alpha);
            int borderColor = ColorUtil.lerpColor(borderBase, borderActive, m.getEnableAnimation().c());
            draw.a(matrices, cx, cy, colWidth, cardHeight, 6.0F, 0.5F, borderColor);
            if (m.getEnableAnimation().c() > 0.01F) {
               draw.a(
                  matrices,
                  cx + 2.0F,
                  cy + 4.0F,
                  2.0F,
                  headerHeight - 8.0F,
                  1.0F,
                  ColorUtil.applyAlphaToColor(themeColor, 0.9F * alpha * m.getEnableAnimation().c())
               );
            }

            Fonts.c
               .a(
                  matrices,
                  m.getName(),
                  cx + 9.0F,
                  Fonts.c.a(m.getName(), 7.25F, cy + headerHeight / 2.0F),
                  7.25F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(250, 250, 255, 255), alpha)
               );
            float rightX = cx + colWidth - 7.0F;
            if (!m.getElements().isEmpty()) {
               String arrow = m.isExtended() ? "v" : ">";
               boolean arrowHover = MathUtil.isHovered(mx, my, rightX - 14.0F, cy + 3.0F, 14.0F, headerHeight - 6.0F);
               if (arrowHover) {
                  draw.a(
                     matrices,
                     rightX - 14.0F,
                     cy + 3.0F,
                     14.0F,
                     headerHeight - 6.0F,
                     3.0F,
                     ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.08F * alpha)
                  );
               }

               Fonts.c
                  .a(
                     matrices,
                     arrow,
                     rightX - 10.0F,
                     Fonts.c.a(arrow, 6.5F, cy + headerHeight / 2.0F),
                     6.5F,
                     ColorUtil.applyAlphaToColor(m.isExtended() ? themeColor : ColorUtil.convertToARGB(160, 160, 170, 255), alpha)
                  );
               rightX -= 16.0F;
            }

            float swW = 15.0F;
            float swH = 9.0F;
            float swX = rightX - swW;
            float swY = cy + (headerHeight - swH) / 2.0F;
            int swBg = ColorUtil.lerpColor(ColorUtil.convertToARGB(50, 50, 56, (int)(255.0F * alpha)), themeColor, m.getEnableAnimation().c());
            draw.a(matrices, swX, swY, swW, swH, 4.5F, swBg);
            float knobX = swX + 1.5F + m.getEnableAnimation().c() * (swW - 9.0F);
            draw.a(matrices, knobX, swY + 1.5F, 6.0F, 6.0F, 3.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha));
            rightX -= swW + 6.0F;
            String bindText = m.isBound() ? "..." : (m.getKeyBind() == -1 ? "None" : KeyUtil.getKeyName(m.getKeyBind()));
            float bindW = Math.max(18.0F, Fonts.e.a(bindText, 5.5F) + 6.0F);
            float bindX = rightX - bindW;
            float bindY = cy + (headerHeight - 12.0F) / 2.0F;
            draw.a(matrices, bindX, bindY, bindW, 12.0F, 3.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(32, 32, 40, 255), 0.9F * alpha));
            draw.a(matrices, bindX, bindY, bindW, 12.0F, 3.0F, 0.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.06F * alpha));
            Fonts.e
               .b(
                  matrices,
                  bindText,
                  bindX + bindW / 2.0F,
                  Fonts.e.a(bindText, 5.5F, bindY + 6.0F),
                  5.5F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(200, 205, 215, 255), alpha)
               );
            if (!(extAnim > 0.001F)) {
               for (Element<?> ex : m.getElements()) {
                  ex.getBounds().x = -9999.0F;
                  ex.getBounds().y = -9999.0F;
               }
            } else {
               draw.a(
                  matrices,
                  cx + 6.0F,
                  cy + headerHeight - 0.5F,
                  colWidth - 12.0F,
                  0.5F,
                  0.0F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.06F * alpha * extAnim)
               );
               float plateInnerH = cardHeight - headerHeight - 6.0F;
               if (plateInnerH > 0.0F) {
                  draw.a(
                     matrices,
                     cx + 4.0F,
                     cy + headerHeight + 2.0F,
                     colWidth - 8.0F,
                     plateInnerH,
                     5.0F,
                     ColorUtil.convertToARGB(13, 13, 17, (int)(180.0F * alpha * extAnim))
                  );
                  draw.a(
                     matrices,
                     cx + 4.0F,
                     cy + headerHeight + 2.0F,
                     colWidth - 8.0F,
                     plateInnerH,
                     5.0F,
                     0.5F,
                     ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.04F * alpha * extAnim)
                  );
               }

               boolean needCardScissor = extAnim < 0.999F;
               if (needCardScissor) {
                  ScissorUtil.a(matrices, cx, cy + headerHeight, colWidth, cardHeight - headerHeight);
               }

               float elemY = cy + headerHeight + 5.0F;

               for (Element<?> ex : m.getElements()) {
                  if (ex.isEnabled()) {
                     float elemH = ex.getHeight();
                     ex.getBounds().set(cx + 6.0F, elemY, colWidth - 12.0F, elemH);
                     ex.render(context, (int)mx, (int)my, delta, extAnim);
                     elemY += elemH + 4.0F;
                  } else {
                     ex.getBounds().x = -9999.0F;
                     ex.getBounds().y = -9999.0F;
                  }
               }

               if (needCardScissor) {
                  ScissorUtil.a(matrices);
               }
            }

            colY[col] += cardHeight + gap;
         }
      }

      return Math.max(colY[0], colY[1]) - y;
   }

   private float renderConfigs(DrawContext context, Draw2DProcessor draw, float x, float y, float alpha, double mx, double my, float delta, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      float padding = 10.0F;
      float curY = y + padding;
      this.newConfigField.setPosition(new Vector2f(x + padding, curY));
      this.newConfigField.render(context, (int)mx, (int)my, delta, alpha);
      float createX = x + padding + 130.0F;
      boolean createHover = MathUtil.isHovered(mx, my, createX, curY, 76.0F, 18.0F);
      draw.a(matrices, createX, curY, 76.0F, 18.0F, 4.0F, ColorUtil.applyAlphaToColor(themeColor, (createHover ? 1.0F : 0.8F) * alpha));
      Fonts.c
         .b(
            matrices,
            "+ Создать",
            createX + 38.0F,
            Fonts.c.a("+ Создать", 6.5F, curY + 9.0F),
            6.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha)
         );
      float folderX = createX + 80.0F;
      boolean folderHover = MathUtil.isHovered(mx, my, folderX, curY, 64.0F, 18.0F);
      draw.a(
         matrices,
         folderX,
         curY,
         64.0F,
         18.0F,
         4.0F,
         ColorUtil.applyAlphaToColor(folderHover ? ColorUtil.convertToARGB(90, 90, 100, 255) : ColorUtil.convertToARGB(60, 60, 60, 255), alpha)
      );
      Fonts.c
         .b(
            matrices,
            "Папка",
            folderX + 32.0F,
            Fonts.c.a("Папка", 6.5F, curY + 9.0F),
            6.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha)
         );
      curY += 28.0F;
      ConfigManager cm = FluxClient.getInstance().getConfigManager();
      List<String> configs = cm != null ? cm.getAvailableConfigs() : List.of("Default");
      String currentCfg = cm != null ? cm.getCurrentConfigName() : "Default";

      for (String cfg : configs) {
         boolean isCurrent = cfg.equalsIgnoreCase(currentCfg);
         int rowBg = isCurrent
            ? ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(ColorUtil.convertToARGB(25, 25, 32, 255), themeColor, 0.15F), alpha)
            : ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.05F * alpha);
         draw.a(matrices, x + padding, curY, 375.0F, 24.0F, 4.0F, rowBg);
         if (isCurrent) {
            draw.a(matrices, x + padding + 1.0F, curY + 3.0F, 2.5F, 18.0F, 1.25F, ColorUtil.applyAlphaToColor(themeColor, 0.95F * alpha));
            draw.a(matrices, x + padding, curY, 375.0F, 24.0F, 4.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, 0.5F * alpha));
         } else {
            draw.a(
               matrices, x + padding, curY, 375.0F, 24.0F, 4.0F, 0.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.04F * alpha)
            );
         }

         int textCol = isCurrent ? ColorUtil.convertToARGB(255, 255, 255, 255) : ColorUtil.convertToARGB(225, 225, 235, 255);
         Fonts.c
            .a(matrices, cfg, x + padding + (isCurrent ? 12.0F : 8.0F), Fonts.c.a(cfg, 7.5F, curY + 12.0F), 7.5F, ColorUtil.applyAlphaToColor(textCol, alpha));
         float btnX = x + 395.0F - padding - 20.0F;
         boolean delHover = MathUtil.isHovered(mx, my, btnX, curY + 4.0F, 16.0F, 16.0F);
         draw.a(
            matrices,
            btnX,
            curY + 4.0F,
            16.0F,
            16.0F,
            3.0F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(220, 50, 50, 255), (delHover ? 0.65F : 0.3F) * alpha)
         );
         draw.a(
            matrices,
            btnX,
            curY + 4.0F,
            16.0F,
            16.0F,
            3.0F,
            0.5F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 80, 80, 255), (delHover ? 0.85F : 0.4F) * alpha)
         );
         Fonts.a
            .b(
               matrices,
               "Y",
               btnX + 8.0F,
               Fonts.a.a("Y", 6.0F, curY + 12.0F),
               6.0F,
               ColorUtil.applyAlphaToColor(delHover ? ColorUtil.convertToARGB(255, 255, 255, 255) : ColorUtil.convertToARGB(255, 120, 120, 255), alpha)
            );
         btnX -= 60.0F;
         boolean saveHover = MathUtil.isHovered(mx, my, btnX, curY + 4.0F, 56.0F, 16.0F);
         draw.a(matrices, btnX, curY + 4.0F, 56.0F, 16.0F, 3.0F, ColorUtil.applyAlphaToColor(themeColor, (saveHover ? 0.7F : 0.35F) * alpha));
         draw.a(matrices, btnX, curY + 4.0F, 56.0F, 16.0F, 3.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, (saveHover ? 0.9F : 0.55F) * alpha));
         Fonts.c
            .b(
               matrices,
               "Сохранить",
               btnX + 28.0F,
               Fonts.c.a("Сохранить", 6.0F, curY + 12.0F),
               6.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha)
            );
         btnX -= 60.0F;
         boolean loadHover = MathUtil.isHovered(mx, my, btnX, curY + 4.0F, 56.0F, 16.0F);
         draw.a(matrices, btnX, curY + 4.0F, 56.0F, 16.0F, 3.0F, ColorUtil.applyAlphaToColor(themeColor, (loadHover ? 1.0F : 0.75F) * alpha));
         draw.a(matrices, btnX, curY + 4.0F, 56.0F, 16.0F, 3.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, (loadHover ? 1.0F : 0.9F) * alpha));
         Fonts.c
            .b(
               matrices,
               isCurrent ? "Активен" : "Загрузить",
               btnX + 28.0F,
               Fonts.c.a("Загрузить", 6.0F, curY + 12.0F),
               6.0F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha)
            );
         curY += 28.0F;
      }

      return curY - y;
   }

   private float renderThemes(DrawContext context, Draw2DProcessor draw, float x, float y, float alpha, double mx, double my, float delta, int themeColor) {
      MatrixStack matrices = context.getMatrices();
      float padding = 10.0F;
      float gap = 9.0F;
      float w = 183.0F;
      float h = 42.0F;
      float curX = x + padding;
      float curY = y + padding;
      ThemeProcessor tp = FluxClient.getInstance().getThemeProcessor();
      int currentR = ColorUtil.b(themeColor)[0];
      int currentG = ColorUtil.b(themeColor)[1];
      int currentB = ColorUtil.b(themeColor)[2];

      for (int i = 0; i < THEME_PRESETS.size(); i++) {
         GUIScreen.ThemePreset p = THEME_PRESETS.get(i);
         boolean active = Math.abs(p.r - currentR) < 5 && Math.abs(p.g - currentG) < 5 && Math.abs(p.b - currentB) < 5;
         draw.a(matrices, curX, curY, w, h, 6.0F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.05F * alpha));
         if (active) {
            draw.a(matrices, curX, curY, w, h, 6.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, 0.6F * alpha));
         }

         int pColor = ColorUtil.convertToARGB(p.r, p.g, p.b, 255);
         draw.a(matrices, curX + 10.0F, curY + 12.0F, 18.0F, 18.0F, 9.0F, ColorUtil.applyAlphaToColor(pColor, alpha));
         Fonts.c.a(matrices, p.name, curX + 36.0F, curY + 16.0F, 7.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), alpha));
         Fonts.e
            .a(
               matrices,
               active ? "• Активно" : "Выбрать",
               curX + 36.0F,
               curY + 28.0F,
               6.0F,
               ColorUtil.applyAlphaToColor(active ? themeColor : ColorUtil.convertToARGB(150, 150, 150, 255), alpha)
            );
         curX += w + gap;
         if (i % 2 == 1) {
            curX = x + padding;
            curY += h + gap;
         }
      }

      if (THEME_PRESETS.size() % 2 != 0) {
         curY += h + gap;
      }

      return curY - y;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.closing) {
         return false;
      } else {
         double mx = MathUtil.scale(mouseX, 2);
         double my = MathUtil.scale(mouseY, 2);
         float screenWidth = Interface.mc.getWindow().getScaledWidth() / 2.0F;
         float screenHeight = Interface.mc.getWindow().getScaledHeight() / 2.0F;
         float startX = screenWidth - 260.0F;
         float startY = screenHeight - 170.0F;
         float contentX = startX + 125.0F;
         float bodyY = startY + 38.0F;
         float scroll = this.tabScrolls[this.currentTab.ordinal()];
         double scrollMy = my - scroll;
         if (this.currentTab.category != null) {
            for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
               if (m.isExtended()) {
                  for (Element<?> e : m.getElements()) {
                     if (e instanceof ColorElement ce && ce.isPickerOpen()) {
                        if (ce.onMouseClick(mx, scrollMy, button)) {
                           return true;
                        }

                        ce.setPickerOpen(false);
                     }
                  }
               }
            }
         }

         for (int i = 0; i < GUIScreen.NavTab.values().length; i++) {
            float tabY = startY + 44.0F + i * 25.0F;
            if (i >= GUIScreen.NavTab.CONFIGS.ordinal()) {
               tabY += 5.0F;
            }

            if (MathUtil.isHovered(mx, my, startX + 7.0F, tabY, 111.0F, 22.0F)) {
               if (this.currentTab != GUIScreen.NavTab.values()[i]) {
                  GUIScreen.NavTab prev = this.currentTab;
                  this.currentTab = GUIScreen.NavTab.values()[i];
                  this.tabSlideOffset = this.currentTab.ordinal() > prev.ordinal() ? 18.0F : -18.0F;
               }

               return true;
            }
         }

         if (MathUtil.isHovered(mx, my, contentX + 395.0F - 30.0F, startY + 10.0F, 16.0F, 16.0F) && !this.searchField.getTextBuffer().isEmpty()) {
            this.searchField.a();
            return true;
         } else {
            this.searchField.onMouseClick((int)mx, (int)my, button);
            if (this.searchField.isFocused()) {
               return true;
            } else {
               if (this.currentTab.category != null && MathUtil.isHovered(mx, my, contentX, bodyY, 395.0F, 302.0F)) {
                  for (Module mxx : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
                     if (mxx.isExtended()) {
                        for (Element<?> ex : mxx.getElements()) {
                           if (ex.isEnabled() && ex.onMouseClick(mx, scrollMy, button)) {
                              return true;
                           }
                        }
                     }
                  }
               }

               if (this.currentTab.category != null && MathUtil.isHovered(mx, my, contentX, bodyY, 395.0F, 302.0F)) {
                  float padding = 10.0F;
                  float gap = 9.0F;
                  float colWidth = 183.0F;
                  float[] colY = new float[]{bodyY + padding, bodyY + padding};
                  List<Module> mods = FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category);
                  String search = this.searchField.getTextBuffer().toString().toLowerCase();

                  for (Module mxxx : mods) {
                     if (search.isEmpty() || mxxx.getName().toLowerCase().contains(search)) {
                        int col = colY[0] <= colY[1] ? 0 : 1;
                        float cx = contentX + padding + col * (colWidth + gap);
                        float cy = colY[col];
                        float headerHeight = 24.0F;
                        if (MathUtil.isHovered(mx, scrollMy, cx, cy, colWidth, headerHeight)) {
                           if (button == 1 || button == 0 && MathUtil.isHovered(mx, scrollMy, cx + colWidth - 22.0F, cy, 22.0F, headerHeight)) {
                              mxxx.setExtended(!mxxx.isExtended());
                              return true;
                           }

                           String bindText = mxxx.isBound() ? "..." : (mxxx.getKeyBind() == -1 ? "None" : KeyUtil.getKeyName(mxxx.getKeyBind()));
                           float bindW = Math.max(18.0F, Fonts.e.a(bindText, 5.5F) + 6.0F);
                           float swW = 15.0F;
                           float bindX = cx + colWidth - 22.0F - swW - 6.0F - bindW;
                           if (button == 0 && MathUtil.isHovered(mx, scrollMy, bindX, cy + (headerHeight - 12.0F) / 2.0F, bindW, 12.0F)) {
                              mxxx.setBound(!mxxx.isBound());
                              return true;
                           }

                           if (button == 2) {
                              mxxx.setBound(!mxxx.isBound());
                              return true;
                           }

                           if (button == 0) {
                              mxxx.toggle();
                              return true;
                           }
                        }

                        float extAnim = mxxx.getExtendAnimation().c();
                        float elementsHeight = 0.0F;
                        if (extAnim > 0.001F) {
                           for (Element<?> exx : mxxx.getElements()) {
                              if (exx.isEnabled()) {
                                 elementsHeight += exx.getHeight() + 4.0F;
                              }
                           }
                        }

                        colY[col] += headerHeight + elementsHeight * extAnim + (extAnim > 0.001F ? 6.0F * extAnim : 0.0F) + gap;
                     }
                  }
               }

               if (this.currentTab == GUIScreen.NavTab.CONFIGS && MathUtil.isHovered(mx, my, contentX, bodyY, 395.0F, 302.0F)) {
                  float padding = 10.0F;
                  float curY = bodyY + padding;
                  this.newConfigField.onMouseClick((int)mx, (int)scrollMy, button);
                  if (this.newConfigField.isFocused()) {
                     return true;
                  }

                  if (button == 0 && MathUtil.isHovered(mx, scrollMy, contentX + padding + 130.0F, curY, 76.0F, 18.0F)) {
                     String name = this.newConfigField.getTextBuffer().toString().trim();
                     if (!name.isEmpty()) {
                        FluxClient.getInstance().getConfigManager().saveConfig(name);
                        this.newConfigField.a();
                        if (FluxClient.getInstance().getNotificationProcessor() != null) {
                           FluxClient.getInstance()
                              .getNotificationProcessor()
                              .a(new Notification("b", ColorUtil.convertToARGB(100, 220, 100, 255), "Конфиг создан: " + name, 2000));
                        }
                     }

                     return true;
                  }

                  if (button == 0 && MathUtil.isHovered(mx, scrollMy, contentX + padding + 210.0F, curY, 64.0F, 18.0F)) {
                     FluxClient.getInstance().getConfigManager().openFolder();
                     return true;
                  }

                  curY += 28.0F;
                  ConfigManager cm = FluxClient.getInstance().getConfigManager();

                  for (String cfg : cm != null ? cm.getAvailableConfigs() : List.of("Default")) {
                     float btnX = contentX + 395.0F - padding - 20.0F;
                     if (button == 0 && MathUtil.isHovered(mx, scrollMy, btnX - 2.0F, curY + 2.0F, 20.0F, 20.0F)) {
                        if (cm != null) {
                           cm.deleteConfig(cfg);
                        }

                        if (FluxClient.getInstance().getNotificationProcessor() != null) {
                           FluxClient.getInstance()
                              .getNotificationProcessor()
                              .a(new Notification("Y", ColorUtil.convertToARGB(220, 80, 80, 255), "Конфиг удалён: " + cfg, 2000));
                        }

                        return true;
                     }

                     btnX -= 60.0F;
                     if (button == 0 && MathUtil.isHovered(mx, scrollMy, btnX - 2.0F, curY + 2.0F, 60.0F, 20.0F)) {
                        if (cm != null) {
                           cm.saveConfig(cfg);
                        }

                        if (FluxClient.getInstance().getNotificationProcessor() != null) {
                           FluxClient.getInstance()
                              .getNotificationProcessor()
                              .a(new Notification("b", ColorUtil.convertToARGB(100, 220, 100, 255), "Конфиг сохранён: " + cfg, 2000));
                        }

                        return true;
                     }

                     btnX -= 60.0F;
                     if (button == 0 && MathUtil.isHovered(mx, scrollMy, btnX - 2.0F, curY + 2.0F, 60.0F, 20.0F)) {
                        if (cm != null) {
                           cm.loadConfig(cfg);
                        }

                        if (FluxClient.getInstance().getNotificationProcessor() != null) {
                           FluxClient.getInstance()
                              .getNotificationProcessor()
                              .a(new Notification("b", ColorUtil.convertToARGB(100, 220, 100, 255), "Конфиг загружен: " + cfg, 2000));
                        }

                        return true;
                     }

                     curY += 28.0F;
                  }
               }

               if (this.currentTab == GUIScreen.NavTab.THEMES && MathUtil.isHovered(mx, my, contentX, bodyY, 395.0F, 302.0F)) {
                  float paddingx = 10.0F;
                  float gap = 9.0F;
                  float w = 183.0F;
                  float h = 42.0F;
                  float curX = contentX + paddingx;
                  float curYx = bodyY + paddingx;

                  for (int i = 0; i < THEME_PRESETS.size(); i++) {
                     if (MathUtil.isHovered(mx, scrollMy, curX, curYx, w, h)) {
                        GUIScreen.ThemePreset p = THEME_PRESETS.get(i);
                        FluxClient.getInstance().getThemeProcessor().setPrimaryColor(p.r, p.g, p.b);
                        return true;
                     }

                     curX += w + gap;
                     if (i % 2 == 1) {
                        curX = contentX + paddingx;
                        curYx += h + gap;
                     }
                  }
               }

               return super.mouseClicked(mouseX, mouseY, button);
            }
         }
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.closing) {
         return false;
      } else {
         double mx = MathUtil.scale(mouseX, 2);
         double my = MathUtil.scale(mouseY, 2);
         float scroll = this.tabScrolls[this.currentTab.ordinal()];
         if (this.currentTab.category != null) {
            for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
               if (m.isExtended()) {
                  for (Element<?> e : m.getElements()) {
                     if (e.isEnabled()) {
                        e.onMouseRelease(mx, my - scroll, button);
                     }
                  }
               }
            }
         }

         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.closing) {
         return false;
      } else {
         double mx = MathUtil.scale(mouseX, 2);
         double my = MathUtil.scale(mouseY, 2);
         float scroll = this.tabScrolls[this.currentTab.ordinal()];
         double scrollMy = my - scroll;
         this.searchField.onMouseDrag((int)mx, (int)my, button);
         if (this.currentTab == GUIScreen.NavTab.CONFIGS) {
            this.newConfigField.onMouseDrag((int)mx, (int)scrollMy, button);
         }

         if (this.currentTab.category != null) {
            for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
               if (m.isExtended()) {
                  for (Element<?> e : m.getElements()) {
                     if (e.isEnabled()) {
                        if (e instanceof ColorElement ce && ce.isPickerOpen() && ce.isDragging()) {
                           ce.onMouseDrag(mx, scrollMy, button, deltaX, deltaY);
                           return true;
                        }

                        if (e.onMouseDrag(mx, scrollMy, button, deltaX, deltaY)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.closing) {
         return false;
      } else {
         float[] var10000 = this.tabScrollTargets;
         int var10001 = this.currentTab.ordinal();
         var10000[var10001] = (float)(var10000[var10001] + verticalAmount * 25.0);
         double mx = MathUtil.scale(mouseX, 2);
         double my = MathUtil.scale(mouseY, 2);
         float scroll = this.tabScrolls[this.currentTab.ordinal()];
         if (this.currentTab.category != null) {
            for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
               if (m.isExtended()) {
                  for (Element<?> e : m.getElements()) {
                     if (e.isEnabled()) {
                        e.onMouseScroll(mx, my - scroll, verticalAmount);
                     }
                  }
               }
            }
         }

         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.closing) {
         return false;
      } else {
         ClickGuiModule clickGui = FluxClient.getInstance().getModuleManager().getModule(ClickGuiModule.class);
         if (keyCode != 256 && (clickGui == null || keyCode != clickGui.getKeyBind())) {
            if (this.currentTab.category != null) {
               for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
                  if (m.isBound()) {
                     if (keyCode != 261 && keyCode != 259) {
                        m.setKeyBind(keyCode);
                     } else {
                        m.setKeyBind(-1);
                     }

                     m.setBound(false);
                     return true;
                  }

                  if (m.isExtended()) {
                     for (Element<?> e : m.getElements()) {
                        if (e.onKeyPress(keyCode, scanCode, modifiers)) {
                           return true;
                        }
                     }
                  }
               }
            }

            if (this.searchField.isFocused()) {
               this.searchField.a(keyCode, scanCode, modifiers);
               return true;
            } else if (this.currentTab == GUIScreen.NavTab.CONFIGS && this.newConfigField.isFocused()) {
               this.newConfigField.a(keyCode, scanCode, modifiers);
               return true;
            } else {
               return super.keyPressed(keyCode, scanCode, modifiers);
            }
         } else {
            this.startClosing();
            return true;
         }
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.closing) {
         return false;
      } else if (this.searchField.isFocused()) {
         this.searchField.a(chr, modifiers);
         return true;
      } else if (this.currentTab == GUIScreen.NavTab.CONFIGS && this.newConfigField.isFocused()) {
         this.newConfigField.a(chr, modifiers);
         return true;
      } else {
         if (this.currentTab.category != null) {
            for (Module m : FluxClient.getInstance().getModuleManager().getModulesByCategory(this.currentTab.category)) {
               if (m.isExtended()) {
                  for (Element<?> e : m.getElements()) {
                     if (e.onCharTyped(chr, modifiers)) {
                        return true;
                     }
                  }
               }
            }
         }

         return super.charTyped(chr, modifiers);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public List<GUIPanel> getPanels() {
      return Collections.emptyList();
   }

   public TextField getSearchField() {
      return this.searchField;
   }

   public TextField getNewConfigField() {
      return this.newConfigField;
   }

   public static enum NavTab {
      COMBAT("Бой", "A", Category.COMBAT),
      MOVEMENT("Движение", "B", Category.MOVEMENT),
      RENDER("Рендер", "D", Category.RENDER),
      HUD("Интерфейс", "H", Category.HUD),
      MISC("Разное", "M", Category.MISC),
      CONFIGS("Конфиги", "C", null),
      THEMES("Темы", "P", null);

      public final String name;
      public final String icon;
      public final Category category;

      private NavTab(String name, String icon, Category category) {
         this.name = name;
         this.icon = icon;
         this.category = category;
      }
   }

   public static class ThemePreset {
      public final String name;
      public final int r;
      public final int g;
      public final int b;

      public ThemePreset(String name, int r, int g, int b) {
         this.name = name;
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }
}
