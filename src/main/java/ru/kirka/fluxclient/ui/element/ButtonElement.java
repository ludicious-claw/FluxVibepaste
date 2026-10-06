package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.ButtonSetting;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class ButtonElement extends Element<ButtonSetting> {
   public ButtonElement(ButtonSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
   }

   @Override
   public float getDefaultHeight() {
      return 14.0F;
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         Vector4f vector4f = this.a;
         if (!MathUtil.isHovered(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
            return false;
         } else {
            if (this.b != null) {
               this.b.run();
            }

            return true;
         }
      }
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      this.getActivationAnimation().a(MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F);
      this.getActivationAnimation().a(0.0F, 1.0F, 0.3F, EasingList.i, delta);
      float hover = this.getActivationAnimation().c();
      int text = ColorUtil.convertToARGB(255, 255, 255, 255);
      draw.a(
         matrices,
         this.a.x,
         this.a.y,
         this.a.z,
         this.a.w,
         4.0F,
         ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), (10.0F + 20.0F * hover) / 255.0F * extend)
      );
      draw.a(
         matrices,
         this.a.x,
         this.a.y,
         this.a.z,
         this.a.w,
         4.0F,
         0.5F,
         ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_MEDIUM).toIntColor(), theme.a(ThemeInfo.OUTLINE_MEDIUM).getAlphaFloat() * extend)
      );
      Fonts.c
         .b(
            matrices,
            this.b.getName(),
            this.a.x + this.a.z / 2.0F,
            this.a.y + (this.a.w - Fonts.c.a(7.0F)) / 2.0F - 0.5F,
            7.0F,
            ColorUtil.applyAlphaToColor(text, extend)
         );
   }
}
