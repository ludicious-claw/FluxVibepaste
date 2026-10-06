package ru.kirka.fluxclient.ui.element;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class ColorElement extends Element<ColorSetting> {
   private final Vector4f pickerBackground = new Vector4f();
   private final Vector4f satArea = new Vector4f();
   private final Vector4f hueBar = new Vector4f();
   private final Vector4f g = new Vector4f();
   private float h;
   private float i;
   private float j;
   private float k;
   private ColorElement.DragMode l = ColorElement.DragMode.NONE;
   private boolean m;

   public ColorElement(ColorSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
      this.initFromSetting();
   }

   @Override
   public float getDefaultHeight() {
      return 11.0F;
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      Vector4f vector4f = this.a;
      Vector4f vector4f2 = this.satArea;
      Vector4f vector4f3 = this.hueBar;
      Vector4f vector4f4 = this.g;
      if (MathUtil.isHovered(mouseX, mouseY, vector4f.x + vector4f.z - 11.0F, vector4f.y + vector4f.w / 2.0F - 5.0F, 11.0F, 11.0F)) {
         this.m = !this.m;
         if (this.m) {
            this.initFromSetting();
         }

         return true;
      } else if (!this.m || button != 0) {
         return false;
      } else if (MathUtil.isHovered(mouseX, mouseY, vector4f2.x, vector4f2.y, vector4f2.z, vector4f2.w)) {
         this.l = ColorElement.DragMode.AREA;
         this.updateColorFromMouse(mouseX, mouseY);
         return true;
      } else if (MathUtil.isHovered(mouseX, mouseY, vector4f3.x - 2.0F, vector4f3.y, vector4f3.z + 4.0F, vector4f3.w)) {
         this.l = ColorElement.DragMode.HUE;
         this.updateColorFromMouse(mouseX, mouseY);
         return true;
      } else if (MathUtil.isHovered(mouseX, mouseY, vector4f4.x - 2.0F, vector4f4.y, vector4f4.z + 4.0F, vector4f4.w)) {
         this.l = ColorElement.DragMode.ALPHA;
         this.updateColorFromMouse(mouseX, mouseY);
         return true;
      } else {
         return MathUtil.isHovered(mouseX, mouseY, this.pickerBackground.x, this.pickerBackground.y, this.pickerBackground.z, this.pickerBackground.w);
      }
   }

   @Override
   public boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.m && this.l != ColorElement.DragMode.NONE) {
         this.updateColorFromMouse(mouseX, mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onMouseRelease(double mouseX, double mouseY, int button) {
      if (this.l != ColorElement.DragMode.NONE) {
         this.l = ColorElement.DragMode.NONE;
         return true;
      } else {
         return false;
      }
   }

   public boolean isDragging() {
      return this.l != ColorElement.DragMode.NONE;
   }

   public boolean isPickerOpen() {
      return this.m;
   }

   public void setPickerOpen(boolean open) {
      this.m = open;
      if (open) {
         this.initFromSetting();
      } else {
         this.l = ColorElement.DragMode.NONE;
      }
   }

   public Vector4f getPickerBackground() {
      return this.pickerBackground;
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      float centerY = this.a.y + this.a.w / 2.0F + 0.5F;
      float boxX = this.a.x + this.a.z - 11.0F;
      float boxY = centerY - 5.5F;
      float pickerW = 84.0F;
      float pickerH = 66.0F;
      float pickerX = boxX + 16.0F + pickerW > this.a.x + this.a.z + 20.0F ? boxX - pickerW - 4.0F : boxX + 16.0F;
      float pickerY = boxY - 5.0F;
      this.pickerBackground.set(pickerX, pickerY, pickerW, pickerH);
      this.satArea.set(pickerX + 5.0F, pickerY + 5.0F, 56.0F, 56.0F);
      this.hueBar.set(this.satArea.x + 56.0F + 5.0F, this.satArea.y, 4.0F, 56.0F);
      this.g.set(this.hueBar.x + 4.0F + 5.0F, this.satArea.y, 4.0F, 56.0F);
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
      if (extend < 1.0F) {
         this.m = false;
      }

      this.drawLabel(
         matrices,
         Fonts.c,
         this.b.getName(),
         this.a.x,
         this.a.y,
         this.a.w,
         6.5F,
         theme.a(ThemeInfo.TEXT).toIntColor(),
         boxX - this.a.x - 4.0F,
         hovered,
         extend,
         delta
      );
      draw.a(matrices, boxX, boxY, 11.0F, 11.0F, 2.0F, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.039215688F * extend));
      draw.a(
         matrices,
         boxX,
         boxY,
         11.0F,
         11.0F,
         2.0F,
         0.5F,
         ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_MEDIUM).toIntColor(), theme.a(ThemeInfo.OUTLINE_MEDIUM).getAlphaFloat() * extend)
      );
      Fonts.a
         .a(
            matrices,
            "J",
            boxX + (11.0F - Fonts.a.b("J", 6.5F)) / 2.0F,
            Fonts.a.a("J", 6.5F, centerY),
            6.5F,
            ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), extend)
         );
      int currentColor = this.b.get() != null ? this.b.get().getRGB() : -1;
      draw.a(matrices, boxX + 11.0F - 3.0F - 1.25F, boxY + 11.0F - 3.0F - 1.25F, 3.0F, 3.0F, 0.5F, ColorUtil.applyAlphaToColor(currentColor, extend));
   }

   @Override
   public void renderColorPicker(DrawContext context, double mouseX, double mouseY, float delta) {
      this.getActivationAnimation().a(this.m);
      this.getActivationAnimation().a(0.0F, 1.0F, 0.25F, EasingList.p, delta);
      float anim = EasingList.p.ease(this.getActivationAnimation().c());
      if (anim > 0.0F) {
         MatrixStack matrices = context.getMatrices();
         Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
         ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
         if (this.l != ColorElement.DragMode.NONE) {
            this.updateColorFromMouse(mouseX, mouseY);
         }

         int hueColor = Color.HSBtoRGB(this.h, 1.0F, 1.0F);
         int currentColor = this.b.get() != null ? this.b.get().getRGB() : -1;
         int rgb = currentColor & 16777215;
         int handle = ColorUtil.applyAlphaToColor(16777215, anim);
         int background = ColorUtil.applyAlphaToColor(
            ColorUtil.lerpColor(theme.a(ThemeInfo.BACKGROUND_GUI).toIntColor(), theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.05F), 0.92F * anim
         );
         float scale = 0.85F + 0.15F * EasingList.s.ease(this.getActivationAnimation().c());
         float centerX = this.pickerBackground.x + this.pickerBackground.z / 2.0F;
         float centerY = this.pickerBackground.y + this.pickerBackground.w / 2.0F;
         matrices.push();
         matrices.translate(centerX, centerY + (1.0F - anim) * 6.0F, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         matrices.translate(-centerX, -centerY, 0.0F);
         draw.b(matrices, this.pickerBackground.x, this.pickerBackground.y, this.pickerBackground.z, this.pickerBackground.w, 4.0F, background, anim);
         draw.a(
            matrices,
            this.pickerBackground.x,
            this.pickerBackground.y,
            this.pickerBackground.z,
            this.pickerBackground.w,
            4.0F,
            0.5F,
            ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_MEDIUM).toIntColor(), theme.a(ThemeInfo.OUTLINE_MEDIUM).getAlphaFloat() * anim)
         );
         draw.a(
            matrices,
            this.satArea.x,
            this.satArea.y,
            this.satArea.z,
            this.satArea.w,
            2.0F,
            ColorUtil.applyAlphaToColor(16777215, anim),
            ColorUtil.applyAlphaToColor(hueColor, anim),
            ColorUtil.applyAlphaToColor(0, anim),
            ColorUtil.applyAlphaToColor(0, anim)
         );
         draw.a(
            matrices,
            this.satArea.x,
            this.satArea.y,
            this.satArea.z,
            this.satArea.w,
            2.0F,
            0.5F,
            ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_SMALL).toIntColor(), theme.a(ThemeInfo.OUTLINE_SMALL).getAlphaFloat() * anim)
         );
         float cursorX = MathUtil.b(this.satArea.x + this.i * this.satArea.z, this.satArea.x + 2.0F, this.satArea.x + this.satArea.z - 2.0F);
         float cursorY = MathUtil.b(this.satArea.y + (1.0F - this.j) * this.satArea.w, this.satArea.y + 2.0F, this.satArea.y + this.satArea.w - 2.0F);
         draw.a(matrices, cursorX - 2.0F, cursorY - 2.0F, 4.0F, 4.0F, 1.0F, 0.5F, handle);
         float knob = this.hueBar.z + 2.0F;
         draw.a(
            matrices,
            Identifier.of("fluxclient", "pictures/color.png"),
            this.hueBar.x,
            this.hueBar.y,
            this.hueBar.z,
            this.hueBar.w,
            this.hueBar.z / 4.0F,
            ColorUtil.applyAlphaToColor(16777215, anim)
         );
         draw.a(context, this.hueBar.x - 1.0F, this.hueBar.y + this.h * this.hueBar.w - 0.5F, knob, 1.0F, handle);
         draw.a(
            matrices,
            Identifier.of("fluxclient", "pictures/opacity.png"),
            this.g.x,
            this.g.y,
            this.g.z,
            this.g.w,
            this.g.z / 4.0F,
            ColorUtil.applyAlphaToColor(16777215, 0.019607844F * anim)
         );
         draw.a(
            matrices,
            this.g.x,
            this.g.y,
            this.g.z,
            this.g.w,
            this.g.z / 4.0F,
            ColorUtil.applyAlphaToColor(rgb, anim),
            ColorUtil.applyAlphaToColor(rgb, anim),
            ColorUtil.applyAlphaToColor(rgb, 0.0F),
            ColorUtil.applyAlphaToColor(rgb, 0.0F)
         );
         draw.a(context, this.g.x - 1.0F, this.g.y + (1.0F - this.k) * this.g.w - 0.5F, knob, 1.0F, handle);
         matrices.pop();
      }
   }

   private void updateColorFromMouse(double mouseX, double mouseY) {
      switch (this.l) {
         case NONE:
            return;
         case AREA:
            this.i = MathUtil.b((float)(mouseX - this.satArea.x) / this.satArea.z, 0.0F, 1.0F);
            this.j = 1.0F - MathUtil.b((float)(mouseY - this.satArea.y) / this.satArea.w, 0.0F, 1.0F);
            break;
         case HUE:
            this.h = MathUtil.b((float)(mouseY - this.hueBar.y) / this.hueBar.w, 0.0F, 1.0F);
            break;
         case ALPHA:
            this.k = 1.0F - MathUtil.b((float)(mouseY - this.g.y) / this.g.w, 0.0F, 1.0F);
      }

      int argb = ColorUtil.applyAlphaToColor(Color.HSBtoRGB(this.h, this.i, this.j), this.k);
      this.b.set(new Color(argb, true));
   }

   private void initFromSetting() {
      int color = this.b.get() != null ? this.b.get().getRGB() : -1;
      float[] hsb = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null);
      this.h = hsb[0];
      this.i = hsb[1];
      this.j = hsb[2];
      this.k = (color >> 24 & 0xFF) / 255.0F;
   }

   static enum DragMode {
      NONE,
      AREA,
      HUE,
      ALPHA;
   }
}
