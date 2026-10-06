package ru.kirka.fluxclient.ui.widget;

import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.util.MathUtil;

public class EffectMarker {
   private EffectMarker() {
   }

   public static void a(List<EffectMarker.a> list, float x, float y) {
      if (list != null) {
         list.add(new EffectMarker.a(x, y));
      }
   }

   public static void a(MatrixStack matrices, float partialTicks, List<EffectMarker.a> list) {
      if (list != null && !list.isEmpty()) {
         for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).update(matrices, partialTicks)) {
               list.remove(i);
            }
         }
      }
   }

   public static final class a {
      private final AnimationUtil a = new AnimationUtil();
      private final long b = System.nanoTime() + 300000000L;
      private final float c;
      private final float d;
      private boolean e;

      public a(float x, float y) {
         this.c = x;
         this.d = y;
      }

      boolean update(MatrixStack matrices, float partialTicks) {
         this.a.a(0.0F, 1.0F, 0.2F, EasingList.h, partialTicks);
         if (!this.e && System.nanoTime() >= this.b) {
            this.e = true;
         }

         this.a.a(!this.e);
         float progress = MathUtil.b(this.a.c(), 0.0F, 1.0F);
         float scale = this.e ? progress : this.easeScale(progress);
         float length = 4.0F * Math.max(1.0E-4F, scale);
         int color = ColorUtil.convertToARGB(255, 255, 255, Math.round(250.0F * progress));
         matrices.push();
         matrices.translate(this.c, this.d, 0.0F);

         for (int i = 0; i < 4; i++) {
            this.drawMarker(matrices, length, 45.0F + 90.0F * i, length, color);
         }

         matrices.pop();
         return this.e && progress <= 0.01F;
      }

      private void drawMarker(MatrixStack matrices, float length, float angleDeg, float offset, int color) {
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
         matrices.translate(offset, 0.0F, 0.0F);
         FluxClient.getInstance().getDraw2DProcessor().a(matrices, -length / 2.0F, -0.25F, length, 0.5F, 0.0F, color);
         matrices.pop();
      }

      private float easeScale(float scale) {
         if (scale <= 0.0F) {
            return 0.0F;
         } else if (scale < 0.6F) {
            return scale / 0.6F;
         } else {
            return scale < 0.8F ? 1.0F + (scale - 0.6F) / 0.5F * 0.5F : 1.2F - (scale - 0.8F) / 0.2F * 0.2F;
         }
      }
   }
}
