package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.BindSetting;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.KeyUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class BindElement extends Element<BindSetting> {
   private boolean isListening;

   public BindElement(BindSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
   }

   @Override
   public float getDefaultHeight() {
      return 11.0F;
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      Vector4f vector4f = this.a;
      if (this.isListening) {
         this.b.set(-100 + button);
         this.isListening = false;
         return true;
      } else if (!MathUtil.isHovered(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
         return false;
      } else if (button == 0) {
         this.isListening = true;
         return true;
      } else if (button == 2) {
         this.b.set(-1);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
      if (!this.isListening) {
         return false;
      } else {
         if (keyCode == 256) {
            this.b.set(-1);
         } else {
            this.b.set(keyCode);
         }

         this.isListening = false;
         return true;
      }
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      this.getActivationAnimation().a(this.isListening);
      this.getActivationAnimation().a(0.0F, 1.0F, 0.4F, EasingList.p, delta);
      float centerY = this.a.y + this.a.w / 2.0F + 0.5F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
      float anim = this.getActivationAnimation().c();
      float reverse = 1.0F - anim;
      int key = this.b.get() != null ? this.b.get() : -1;
      String value = key == -1 ? "None" : KeyUtil.b(key);
      float total = Fonts.c.a(value, 6.5F) * reverse + Fonts.c.a("...", 6.5F) * anim;
      float boxWidth = total + 8.0F;
      float boxHeight = Fonts.c.a(6.5F) + 3.0F;
      float boxX = this.a.x + this.a.z - boxWidth;
      float boxY = centerY - boxHeight / 2.0F;
      float textY = boxY + (boxHeight - Fonts.c.a(6.5F)) / 2.0F - 0.75F;
      this.drawLabel(
         matrices,
         Fonts.c,
         this.b.getName(),
         this.a.x,
         this.a.y,
         this.a.w,
         6.5F,
         ColorUtil.convertToARGB(245, 245, 255, 255),
         boxX - this.a.x - 4.0F,
         hovered,
         extend,
         delta
      );
      int bindPillBg = this.isListening
         ? ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.35F * extend)
         : ColorUtil.convertToARGB(255, 255, 255, 14);
      int bindPillBorder = this.isListening ? theme.a(ThemeInfo.PRIMARY).toIntColor() : ColorUtil.convertToARGB(255, 255, 255, 28);
      draw.a(matrices, boxX, boxY, boxWidth, boxHeight, 2.5F, bindPillBg);
      draw.a(matrices, boxX, boxY, boxWidth, boxHeight, 2.5F, 0.5F, bindPillBorder);
      ScissorUtil.a(matrices, boxX, boxY, boxWidth, boxHeight);
      if (reverse > 0.0F) {
         Fonts.c.a(matrices, value, boxX + 4.0F, textY, 6.5F, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(235, 235, 245, 255), extend * reverse));
      }

      if (anim > 0.0F) {
         Fonts.c.a(matrices, "...", boxX + 4.0F, textY, 6.5F, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), extend * anim));
      }

      ScissorUtil.a(matrices);
   }
}
