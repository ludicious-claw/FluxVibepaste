package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class SliderElement extends Element<NumberSetting> {
   private boolean isDragging;

   public SliderElement(NumberSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
   }

   @Override
   public float getDefaultHeight() {
      return 22.0F;
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      Vector4f vector4f = this.a;
      if (!MathUtil.isHovered(mouseX, mouseY, vector4f.x - 2.0F, vector4f.y, vector4f.z + 4.0F, vector4f.w)) {
         return false;
      } else if (button == 0) {
         this.isDragging = true;
         this.updateValue(mouseX);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onMouseRelease(double mouseX, double mouseY, int button) {
      this.isDragging = false;
      return false;
   }

   @Override
   public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
      Vector4f vector4f = this.a;
      if (MathUtil.isHovered(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
         float step = this.b.getStep();
         float newVal = MathUtil.clamp(this.b.get() + (float)amount * step, this.b.getMin(), this.b.getMax());
         float stepped = Math.round(newVal / step) * step;
         this.b.set(Math.round(stepped * 100.0F) / 100.0F);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.isDragging) {
         this.updateValue(mouseX);
         return true;
      } else {
         return false;
      }
   }

   private void updateValue(double mouseX) {
      float min = this.b.getMin();
      float max = this.b.getMax();
      float step = this.b.getStep();
      float pct = MathUtil.clamp((float)((mouseX - this.a.x) / this.a.z), 0.0F, 1.0F);
      float rawVal = min + pct * (max - min);
      float stepped = Math.round(rawVal / step) * step;
      float cleanVal = Math.round(stepped * 100.0F) / 100.0F;
      this.b.set(MathUtil.clamp(cleanVal, min, max));
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      float min = this.b.getMin();
      float max = this.b.getMax();
      float progress = MathUtil.clamp((this.b.get() - min) / (max - min), 0.0F, 1.0F);
      if (this.isDragging) {
         this.getActivationAnimation().setValue(progress);
      } else {
         this.getActivationAnimation().a(progress, 15.0F);
      }

      float animProgress = this.isDragging ? progress : this.getActivationAnimation().c();
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
      String valStr = String.format(this.b.getStep() >= 1.0F ? "%.0f" : "%.2f", this.b.get());
      float valWidth = Fonts.c.a(valStr, 6.75F);
      this.drawLabel(
         matrices,
         Fonts.c,
         this.b.getName(),
         this.a.x,
         this.a.y,
         8.0F,
         6.75F,
         ColorUtil.convertToARGB(245, 245, 255, 255),
         this.a.z - valWidth - 4.0F,
         hovered,
         extend,
         delta
      );
      Fonts.c
         .a(
            matrices,
            valStr,
            this.a.x + this.a.z - valWidth,
            this.a.y + (8.0F - Fonts.c.a(6.75F)) / 2.0F - 0.5F,
            6.75F,
            ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(210, 210, 230, 255), extend)
         );
      float barY = this.a.y + 13.0F;
      float barHeight = 4.0F;
      float trackW = this.a.z;
      draw.a(matrices, this.a.x, barY, trackW, barHeight, 2.0F, ColorUtil.convertToARGB(255, 255, 255, 28));
      draw.a(matrices, this.a.x, barY, trackW, barHeight, 2.0F, 0.5F, ColorUtil.convertToARGB(255, 255, 255, 45));
      float knobRadius = 4.5F;
      float knobDiameter = knobRadius * 2.0F;
      float knobCenterX = this.a.x + trackW * animProgress;
      float knobX = MathHelper.clamp(knobCenterX - knobRadius, this.a.x, this.a.x + trackW - knobDiameter);
      float knobY = barY + barHeight * 0.5F - knobRadius;
      float fillWidth = MathUtil.clamp(knobX + knobRadius - this.a.x, 0.0F, trackW);
      if (animProgress > 0.0F && fillWidth > 0.0F) {
         draw.a(matrices, this.a.x, barY, fillWidth, barHeight, 2.0F, ColorUtil.applyAlphaToColor(themeColor, 0.95F * extend));
      }

      draw.a(
         matrices,
         knobX - 1.5F,
         knobY - 1.5F,
         knobDiameter + 3.0F,
         knobDiameter + 3.0F,
         (knobDiameter + 3.0F) * 0.5F,
         ColorUtil.applyAlphaToColor(themeColor, 0.5F * extend)
      );
      draw.a(matrices, knobX, knobY, knobDiameter, knobDiameter, knobRadius, ColorUtil.applyAlphaToColor(-1, extend));
      draw.a(matrices, knobX, knobY, knobDiameter, knobDiameter, knobRadius, 0.75F, ColorUtil.applyAlphaToColor(themeColor, extend));
      float dotRadius = 1.75F;
      draw.a(
         matrices,
         knobX + knobRadius - dotRadius,
         knobY + knobRadius - dotRadius,
         dotRadius * 2.0F,
         dotRadius * 2.0F,
         dotRadius,
         ColorUtil.applyAlphaToColor(themeColor, 0.9F * extend)
      );
   }
}
