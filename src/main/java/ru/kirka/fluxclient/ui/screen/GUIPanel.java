package ru.kirka.fluxclient.ui.screen;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.element.Element;
import ru.kirka.fluxclient.util.KeyUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class GUIPanel {
   private final Vector4f a = new Vector4f(0.0F, 0.0F, 125.0F, 270.0F);
   private final AnimationUtil b = new AnimationUtil();
   private final AnimationUtil c = new AnimationUtil();
   private final Category d;
   private List<Module> e;
   private Module f;

   public GUIPanel(Category category) {
      this.d = category;
   }

   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      if (this.e == null) {
         return false;
      } else {
         for (Module module : this.e) {
            if (module.isBound()) {
               module.setKeyBind(-100 + button);
               module.setBound(false);
               return true;
            }
         }

         if (this.f != null) {
            if (button == 0) {
               this.f.toggle();
               return true;
            }

            if (button == 1) {
               this.f.setExtended(!this.f.isExtended());
               return true;
            }

            if (button == 2) {
               this.e.forEach(this::toggleBindMode);
               return true;
            }
         }

         return this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(modulex -> modulex.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onMouseClick(mouseX, mouseY, button));
      }
   }

   public boolean a(double mouseX, double mouseY, int button) {
      return this.onMouseClick(mouseX, mouseY, button);
   }

   public boolean onMouseRelease(double mouseX, double mouseY, int button) {
      return this.e == null
         ? false
         : this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(module -> module.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onMouseRelease(mouseX, mouseY, button));
   }

   public boolean b(double mouseX, double mouseY, int button) {
      return this.onMouseRelease(mouseX, mouseY, button);
   }

   public boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return this.e == null
         ? false
         : this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(module -> module.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY));
   }

   public boolean a(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return this.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY);
   }

   public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
      if (this.e == null) {
         return false;
      } else {
         for (Module module : this.e) {
            if (module.isBound()) {
               if (keyCode == 256) {
                  module.setKeyBind(-1);
               } else {
                  module.setKeyBind(keyCode);
               }

               module.setBound(false);
               return true;
            }
         }

         return this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(modulex -> modulex.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onKeyPress(keyCode, scanCode, modifiers));
      }
   }

   public boolean a(int keyCode, int scanCode, int modifiers) {
      return this.onKeyPress(keyCode, scanCode, modifiers);
   }

   public boolean onCharTyped(char chr, int modifiers) {
      return this.e == null
         ? false
         : this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(module -> module.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onCharTyped(chr, modifiers));
   }

   public boolean a(char chr, int modifiers) {
      return this.onCharTyped(chr, modifiers);
   }

   public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
      if (!MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w)) {
         return false;
      } else if (this.e != null
         && this.e
            .stream()
            .filter(Module::isExtended)
            .flatMap(module -> module.getElements().stream())
            .filter(Element::isEnabled)
            .anyMatch(element -> element.onMouseScroll(mouseX, mouseY, amount))) {
         return true;
      } else {
         this.b.a((float)amount * 15.0F);
         return true;
      }
   }

   public boolean a(double mouseX, double mouseY, double amount) {
      return this.onMouseScroll(mouseX, mouseY, amount);
   }

   public void setModules(List<Module> modules) {
      this.e = modules;
   }

   public void a(List<Module> modules) {
      this.setModules(modules);
   }

   public void setHovered(Module hovered) {
      this.f = hovered;
   }

   public void a(Module hovered) {
      this.setHovered(hovered);
   }

   public Vector4f getBounds() {
      return this.a;
   }

   public Vector4f f() {
      return this.a;
   }

   public AnimationUtil getScrollAnimation() {
      return this.b;
   }

   public AnimationUtil a() {
      return this.b;
   }

   public AnimationUtil getOpenAnimation() {
      return this.c;
   }

   public AnimationUtil b() {
      return this.c;
   }

   public Category getCategory() {
      return this.d;
   }

   public Category c() {
      return this.d;
   }

   public List<Module> getModules() {
      return this.e;
   }

   public List<Module> d() {
      return this.e;
   }

   public Module getHovered() {
      return this.f;
   }

   public Module e() {
      return this.f;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      float scale = 0.8F + 0.2F * EasingList.s.ease(this.c.c());
      matrices.push();
      matrices.translate(this.a.x + this.a.z / 2.0F, this.a.y + this.a.w / 2.0F + (1.0F - EasingList.p.ease(this.c.c())) * 14.0F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-(this.a.x + this.a.z / 2.0F), -(this.a.y + this.a.w / 2.0F), 0.0F);
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      int glowColor = ColorUtil.applyAlphaToColor(themeColor, 0.35F);
      int glassTint = ColorUtil.convertToARGB(10, 8, 12, 110);
      draw.drawThemedBlurredPanel(matrices, this.a.x, this.a.y, this.a.z, this.a.w, 8.0F, themeColor, 0.36F, glowColor, 16.0F, glassTint);
      draw.a(matrices, this.a.x, this.a.y, this.a.z, this.a.w, 8.0F, 0.75F, ColorUtil.applyAlphaToColor(themeColor, 0.45F));
      this.renderHeader(matrices, theme, 24.0F);
      this.renderModules(context, mouseX, mouseY, this.a.y + 24.0F + 4.0F, delta);
      matrices.pop();
   }

   public void a(DrawContext context, int mouseX, int mouseY, float delta) {
      this.render(context, mouseX, mouseY, delta);
   }

   private void renderHeader(MatrixStack matrices, ThemeProcessor theme, float header) {
      float headerCenter = this.a.y + header / 2.0F;
      float titleX = this.a.x + 8.0F;
      String catName = this.d.getDisplayName();
      int whiteColor = ColorUtil.convertToARGB(245, 245, 250, 255);
      Fonts.c.a(matrices, catName, titleX, Fonts.c.a(catName, 8.5F, headerCenter), 8.5F, whiteColor);
      if (this.e != null) {
         long enabledCount = this.e.stream().filter(Module::isEnabled).count();
         int totalCount = this.e.size();
         String enabledStr = String.valueOf(enabledCount);
         String slashTotalStr = "/" + totalCount;
         float totalW = Fonts.c.a(slashTotalStr, 7.5F);
         float enabledW = Fonts.c.a(enabledStr, 7.5F);
         float rightX = this.a.x + this.a.z - 8.0F;
         float textY = Fonts.c.a(slashTotalStr, 7.5F, headerCenter);
         Fonts.c.a(matrices, slashTotalStr, rightX - totalW, textY, 7.5F, ColorUtil.convertToARGB(120, 120, 130, 255));
         int enabledColor = enabledCount > 0L ? theme.a(ThemeInfo.PRIMARY).toIntColor() : ColorUtil.convertToARGB(120, 120, 130, 255);
         Fonts.c.a(matrices, enabledStr, rightX - totalW - enabledW, textY, 7.5F, enabledColor);
      }
   }

   private void renderModules(DrawContext context, int mouseX, int mouseY, float y, float delta) {
      if (this.e != null) {
         MatrixStack matrices = context.getMatrices();
         Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
         ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
         float view = this.a.y + this.a.w - 6.0F - y + 4.0F;
         float content = 0.0F;

         for (Module module : this.e) {
            content += this.getModuleHeight(module) + 4.0F;
         }

         float y2 = y + this.b.a(Math.min(0.0F, view - content), 0.0F, 1.0F);
         this.f = null;
         ScissorUtil.a(matrices, this.a.x, y, this.a.z, view);
         float bottom = y + view;

         for (Module module : this.e) {
            float center = y2 + 8.0F;
            float activation = module.getEnableAnimation().c();
            float fade = (float)Math.pow(MathUtil.a(MathUtil.b((bottom - y2) / 16.0F, 0.0F, 1.0F)), 1.0);
            boolean hover = mouseY >= y && mouseY <= bottom && MathUtil.isHovered(mouseX, mouseY, this.a.x + 6.0F, y2, this.a.z - 12.0F, 16.0F);
            if (hover) {
               this.f = module;
            }

            module.getBindAnimation().a(0.0F, 1.0F, 0.5F, EasingList.i, delta);
            module.getExtendAnimation().a(0.0F, 1.0F, 0.25F, EasingList.i, delta);
            module.getBindAnimation().a(module.isExtended());
            module.getExtendAnimation().a(module == this.f);
            float total = this.getModuleHeight(module);
            if (y2 + total > y && y2 < bottom) {
               draw.a(
                  matrices,
                  this.a.x + 6.0F,
                  y2,
                  this.a.z - 12.0F,
                  total,
                  4.0F,
                  ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.039215688F * activation * fade)
               );
               draw.a(
                  matrices,
                  this.a.x + 6.0F,
                  y2,
                  this.a.z - 12.0F,
                  total,
                  4.0F,
                  ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.023529412F * module.getExtendAnimation().c() * fade)
               );
               draw.a(
                  matrices,
                  this.a.x + 6.0F,
                  y2,
                  this.a.z - 12.0F,
                  total,
                  4.0F,
                  0.5F,
                  ColorUtil.applyAlphaToColor(
                     theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), theme.a(ThemeInfo.OUTLINE_SMALL).getAlphaFloat() * activation * fade
                  )
               );
               Fonts.c
                  .a(
                     matrices,
                     module.getName(),
                     this.a.x + 6.0F + 4.0F,
                     center - Fonts.c.a(7.25F) / 2.0F - 0.5F,
                     7.25F,
                     ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), fade)
                  );
               if (module.getDisableAnimation().c() > 0.0F) {
                  float bind = module.getDisableAnimation().c();
                  String bindText = module.isBound() ? "?" : KeyUtil.b(module.getKeyBind());
                  float iconW = Fonts.a.b("C", 6.0F);
                  float boxWidth = 4.0F + iconW + 2.5F + Fonts.c.a(bindText, 6.0F) + 4.0F;
                  float boxX = this.a.x + 6.0F + 4.0F + Fonts.c.a(module.getName(), 7.25F) + 4.0F;
                  float boxY = center - 4.5F;
                  draw.a(matrices, boxX, boxY, boxWidth, 9.0F, 2.0F, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.15686275F * bind));
                  draw.a(
                     matrices,
                     boxX,
                     boxY,
                     boxWidth,
                     9.0F,
                     2.0F,
                     0.5F,
                     ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_MEDIUM).toIntColor(), theme.a(ThemeInfo.OUTLINE_MEDIUM).getAlphaFloat() * bind)
                  );
                  Fonts.a
                     .a(matrices, "C", boxX + 4.0F, Fonts.a.a("C", 6.0F, center), 6.0F, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), bind));
                  Fonts.c
                     .a(
                        matrices,
                        bindText,
                        boxX + 4.0F + iconW + 2.5F,
                        Fonts.c.a(bindText, 6.0F, center),
                        6.0F,
                        ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), bind)
                     );
               }

               if (module.getElements().stream().anyMatch(Element::isEnabled)) {
                  Fonts.c
                     .a(
                        matrices,
                        "...",
                        this.a.x + this.a.z - 6.0F - 4.0F - Fonts.c.a("...", 10.0F) - (activation > 0.0F ? 18.0F : 0.0F),
                        Fonts.c.a("...", 10.0F, center),
                        10.0F,
                        ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT_DISABLED).toIntColor(), fade)
                     );
               }

               if (activation > 0.0F) {
                  float toggleX = this.a.x + this.a.z - 6.0F - 4.0F - 14.0F;
                  float toggleY = center - 4.25F;
                  draw.a(
                     matrices,
                     toggleX,
                     toggleY,
                     14.0F,
                     8.5F,
                     3.25F,
                     ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.49019608F * activation * fade)
                  );
                  draw.a(
                     matrices,
                     toggleX,
                     toggleY,
                     14.0F,
                     8.5F,
                     3.25F,
                     0.3F,
                     ColorUtil.applyAlphaToColor(
                        theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), theme.a(ThemeInfo.OUTLINE_SMALL).getAlphaFloat() * activation * fade
                     )
                  );
                  draw.a(
                     matrices,
                     toggleX + 1.5F + 5.5F * activation,
                     toggleY + 1.5F,
                     5.5F,
                     5.5F,
                     1.75F,
                     ColorUtil.applyAlphaToColor(
                        ColorUtil.lerpColor(ColorUtil.convertToARGB(150, 150, 155, 255), ColorUtil.convertToARGB(255, 255, 255, 255), activation),
                        activation * fade
                     )
                  );
               }

               float extend = module.getBindAnimation().c();
               if (extend > 0.0F) {
                  ScissorUtil.a(matrices, this.a.x + 6.0F, y2, this.a.z - 12.0F, total);
                  float baseY = y2 + 16.0F - 4.0F * (1.0F - extend);
                  float offset = 0.0F;

                  for (Element<?> element : module.getElements()) {
                     element.getVisibilityAnimation().a(element.isEnabled());
                     element.getVisibilityAnimation().a(0.0F, 1.0F, 0.4F, EasingList.i, delta);
                     float visible = element.getVisibilityAnimation().c();
                     if (visible > 0.0F) {
                        float targetY = baseY + offset - 4.0F * (1.0F - visible);
                        float currentY = baseY + (targetY - baseY) * extend;
                        element.getBounds().set(this.a.x + 6.0F + 4.5F, currentY, this.a.z - 12.0F - 8.0F, element.getBounds().w);
                        element.render(context, mouseX, mouseY, delta, extend * visible);
                        offset += (element.getBounds().w + 4.0F) * visible;
                     }
                  }

                  ScissorUtil.a(matrices);
               }
            }

            y2 += total + 4.0F;
         }

         ScissorUtil.a(matrices);
      }
   }

   public void renderColorPickers(DrawContext context, double mouseX, double mouseY, float delta) {
      if (this.e != null) {
         for (Module module : this.e) {
            for (Element<?> element : module.getElements()) {
               element.renderColorPicker(context, mouseX, mouseY, delta);
            }
         }
      }
   }

   public void a(DrawContext context, double mouseX, double mouseY, float delta) {
      this.renderColorPickers(context, mouseX, mouseY, delta);
   }

   private float getModuleHeight(Module module) {
      return 16.0F
         + (
            module.getElements().isEmpty()
               ? 0.0F
               : (float)module.getElements().stream().mapToDouble(e -> (e.getBounds().w + 4.0F) * e.getVisibilityAnimation().c()).sum()
                  * module.getBindAnimation().c()
         );
   }

   public void toggleBindMode(Module module) {
      module.setBound(module == this.f && !module.isBound());
   }

   public void i(Module module) {
      this.toggleBindMode(module);
   }
}
