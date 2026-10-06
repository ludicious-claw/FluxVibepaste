package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class ModeElement extends Element<ModeSetting> {
   private final AnimationUtil[] modeAnimations;

   public ModeElement(ModeSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
      this.modeAnimations = new AnimationUtil[setting.getModes().size()];

      for (int i = 0; i < this.modeAnimations.length; i++) {
         this.modeAnimations[i] = new AnimationUtil();
      }
   }

   public float calculateHeight(float width) {
      if (this.b == null || this.b.getModes() == null || this.b.getModes().isEmpty()) {
         return 22.0F;
      } else if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
         try {
            float cursorX = 0.0F;
            float cursorY = Fonts.c.a(6.5F) + 5.0F;

            for (String mode : this.b.getModes()) {
               float btnWidth = Fonts.c.a(mode, 6.25F) + 6.0F;
               if (cursorX + btnWidth > width && cursorX > 0.0F) {
                  cursorX = 0.0F;
                  cursorY += 12.0F;
               }

               cursorX += btnWidth + 3.0F;
            }

            return cursorY + 12.0F;
         } catch (Throwable var7) {
            return 22.0F;
         }
      } else {
         return 22.0F;
      }
   }

   @Override
   public float getDefaultHeight() {
      return 22.0F;
   }

   @Override
   public float getHeight() {
      return this.calculateHeight(this.a.z > 0.0F ? this.a.z : 171.0F);
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      Vector4f vector4f = this.a;
      if (button != 0) {
         return false;
      } else {
         float f = vector4f.x;
         float fA = vector4f.y + Fonts.c.a(6.5F) + 5.0F;

         for (String mode : this.b.getModes()) {
            float width = Fonts.c.a(mode, 6.25F) + 6.0F;
            if (f + width > vector4f.x + vector4f.z) {
               f = vector4f.x;
               fA += 12.0F;
            }

            if (MathUtil.isHovered(mouseX, mouseY, f - 1.0F, fA - 1.5F, width + 2.0F, 12.0F)) {
               this.b.set(mode);
               return true;
            }

            f += width + 3.0F;
         }

         return false;
      }
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
      this.drawLabel(
         matrices, Fonts.c, this.b.getName(), this.a.x, this.a.y, 8.0F, 6.5F, ColorUtil.convertToARGB(245, 245, 255, 255), this.a.z, hovered, extend, delta
      );
      float cursorX = this.a.x;
      float cursorY = this.a.y + Fonts.c.a(6.5F) + 5.0F;

      for (int i = 0; i < this.b.getModes().size(); i++) {
         String mode = this.b.getModes().get(i);
         boolean isSelected = this.b.get().equalsIgnoreCase(mode);
         this.modeAnimations[i].a(isSelected);
         this.modeAnimations[i].a(0.0F, 1.0F, 0.4F, EasingList.i, delta);
         float active = this.modeAnimations[i].c();
         float btnWidth = Fonts.c.a(mode, 6.25F) + 6.0F;
         if (cursorX + btnWidth > this.a.x + this.a.z) {
            cursorX = this.a.x;
            cursorY += 12.0F;
         }

         int unselectedBg = ColorUtil.convertToARGB(255, 255, 255, 14);
         int unselectedBorder = ColorUtil.convertToARGB(255, 255, 255, 26);
         int activeBg = ColorUtil.applyAlphaToColor(themeColor, 0.85F * extend);
         int activeBorder = ColorUtil.applyAlphaToColor(themeColor, extend);
         int pillBg = ColorUtil.lerpColor(unselectedBg, activeBg, active);
         int pillBorder = ColorUtil.lerpColor(unselectedBorder, activeBorder, active);
         draw.a(matrices, cursorX, cursorY, btnWidth, 9.0F, 2.5F, pillBg);
         draw.a(matrices, cursorX, cursorY, btnWidth, 9.0F, 2.5F, 0.5F, pillBorder);
         int textColor = ColorUtil.lerpColor(ColorUtil.convertToARGB(185, 185, 205, 255), -1, active);
         Fonts.c.a(matrices, mode, cursorX + 3.0F, cursorY + (9.0F - Fonts.c.a(6.25F)) / 2.0F - 0.5F, 6.25F, ColorUtil.applyAlphaToColor(textColor, extend));
         cursorX += btnWidth + 3.0F;
      }

      this.a.w = cursorY + 12.0F - this.a.y;
   }
}
