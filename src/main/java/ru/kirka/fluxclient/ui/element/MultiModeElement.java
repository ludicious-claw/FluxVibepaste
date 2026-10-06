package ru.kirka.fluxclient.ui.element;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.Setting;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.MultiSetting;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class MultiModeElement extends Element<MultiSetting> {
   private final AnimationUtil[] modeAnimations;

   public MultiModeElement(MultiSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
      List<BooleanSetting> list = setting.getOptions();
      this.modeAnimations = new AnimationUtil[list != null ? list.size() : 0];

      for (int i = 0; i < this.modeAnimations.length; i++) {
         this.modeAnimations[i] = new AnimationUtil();
      }
   }

   public float calculateHeight(float width) {
      List<BooleanSetting> list = this.b != null ? this.b.getOptions() : null;
      if (list == null || list.isEmpty()) {
         return 22.0F;
      } else if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
         try {
            float cursorX = 0.0F;
            float cursorY = Fonts.c.a(6.5F) + 5.0F;

            for (BooleanSetting mode : list) {
               float btnWidth = Fonts.c.a(mode.getName(), 6.25F) + 6.0F;
               if (cursorX + btnWidth > width && cursorX > 0.0F) {
                  cursorX = 0.0F;
                  cursorY += 12.0F;
               }

               cursorX += btnWidth + 3.0F;
            }

            return cursorY + 12.0F;
         } catch (Throwable var8) {
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
      MultiSetting setting = this.b;
      List<BooleanSetting> list = setting.getOptions();
      if (list == null) {
         return false;
      } else if (button == 0) {
         float f = vector4f.x;
         float fA = vector4f.y + Fonts.c.a(6.5F) + 5.0F;

         for (BooleanSetting booleanSetting : list) {
            float fA2 = Fonts.c.a(booleanSetting.getName(), 6.25F) + 6.0F;
            if (f + fA2 > vector4f.x + vector4f.z) {
               f = vector4f.x;
               fA += 12.0F;
            }

            if (MathUtil.isHovered(mouseX, mouseY, f - 1.0F, fA - 1.5F, fA2 + 2.0F, 12.0F)) {
               booleanSetting.set(!booleanSetting.get());
               return true;
            }

            f += fA2 + 3.0F;
         }

         return false;
      } else if (button == 2 && MathUtil.isHovered(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
         for (BooleanSetting b : list) {
            b.set(false);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      List<BooleanSetting> list = this.b.getOptions();
      if (list != null) {
         int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
         long selectedCount = list.stream().filter(Setting::get).count();
         String counter = selectedCount + " из " + list.size();
         float counterWidth = Fonts.c.a(counter, 6.5F);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
         this.drawLabel(
            matrices,
            Fonts.c,
            this.b.getName(),
            this.a.x,
            this.a.y,
            Fonts.c.a(6.5F) + 1.0F,
            6.5F,
            ColorUtil.convertToARGB(245, 245, 255, 255),
            this.a.z - counterWidth - 4.0F,
            hovered,
            extend,
            delta
         );
         Fonts.c
            .a(
               matrices,
               counter,
               this.a.x + this.a.z - counterWidth,
               this.a.y + 0.25F,
               6.5F,
               ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(180, 180, 200, 255), extend)
            );
         float x = this.a.x;
         float y = this.a.y + Fonts.c.a(6.5F) + 5.0F;
         int i = 0;

         for (BooleanSetting mode : list) {
            float width = Fonts.c.a(mode.getName(), 6.25F) + 6.0F;
            if (x + width > this.a.x + this.a.z) {
               x = this.a.x;
               y += 12.0F;
            }

            if (i < this.modeAnimations.length) {
               this.modeAnimations[i].a(mode.get());
               this.modeAnimations[i].a(0.0F, 1.0F, 0.3F, EasingList.i, delta);
               float value = this.modeAnimations[i].c();
               int unselectedBg = ColorUtil.convertToARGB(255, 255, 255, 14);
               int unselectedBorder = ColorUtil.convertToARGB(255, 255, 255, 26);
               int activeBg = ColorUtil.applyAlphaToColor(themeColor, 0.85F * extend);
               int activeBorder = ColorUtil.applyAlphaToColor(themeColor, extend);
               int pillBg = ColorUtil.lerpColor(unselectedBg, activeBg, value);
               int pillBorder = ColorUtil.lerpColor(unselectedBorder, activeBorder, value);
               draw.a(matrices, x, y, width, 9.0F, 2.5F, pillBg);
               draw.a(matrices, x, y, width, 9.0F, 2.5F, 0.5F, pillBorder);
               int color = ColorUtil.lerpColor(ColorUtil.convertToARGB(185, 185, 205, 255), -1, value);
               Fonts.c
                  .b(
                     matrices,
                     mode.getName(),
                     x + width / 2.0F,
                     y + (9.0F - Fonts.c.a(6.25F)) / 2.0F - 0.75F,
                     6.25F,
                     ColorUtil.applyAlphaToColor(color, extend)
                  );
            }

            x += width + 3.0F;
            i++;
         }

         this.a.w = y + 12.0F - this.a.y;
      }
   }
}
