package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.MathUtil;

public class BooleanElement extends Element<BooleanSetting> {
   public BooleanElement(BooleanSetting setting) {
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
      if (!MathUtil.isHovered(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, vector4f.w)) {
         return false;
      } else if (button == 0) {
         this.b.set(!this.b.get());
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
      this.getActivationAnimation().a(this.b.get());
      this.getActivationAnimation().a(0.0F, 1.0F, 0.5F, EasingList.i, delta);
      float enabled = this.getActivationAnimation().c();
      int themeColor = theme.a(ThemeInfo.PRIMARY).toIntColor();
      float centerY = this.a.y + this.a.w / 2.0F + 0.5F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0F;
      this.drawLabel(
         matrices,
         Fonts.c,
         this.b.getName(),
         this.a.x,
         this.a.y,
         this.a.w,
         6.5F,
         ColorUtil.convertToARGB(245, 245, 255, 255),
         this.a.z - 11.0F - 4.0F,
         hovered,
         extend,
         delta
      );
      float boxX = this.a.x + this.a.z - 11.0F;
      float boxY = centerY - 5.5F;
      draw.a(matrices, boxX, boxY, 11.0F, 11.0F, 3.0F, ColorUtil.convertToARGB(255, 255, 255, 14));
      draw.a(matrices, boxX, boxY, 11.0F, 11.0F, 3.0F, 0.5F, ColorUtil.convertToARGB(255, 255, 255, 30));
      if (enabled > 0.0F) {
         draw.a(matrices, boxX, boxY, 11.0F, 11.0F, 3.0F, ColorUtil.applyAlphaToColor(themeColor, 0.85F * enabled * extend));
         draw.a(matrices, boxX, boxY, 11.0F, 11.0F, 3.0F, 0.5F, ColorUtil.applyAlphaToColor(themeColor, enabled * extend));
         Fonts.a
            .a(matrices, "b", boxX + 5.5F - Fonts.a.a("b", 6.5F) / 2.0F, Fonts.a.a("b", 6.5F, centerY), 6.5F, ColorUtil.applyAlphaToColor(-1, enabled * extend));
      }
   }
}
