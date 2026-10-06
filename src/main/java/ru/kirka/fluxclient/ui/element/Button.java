package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.util.MathUtil;

public class Button {
   private final AnimationUtil a = new AnimationUtil();
   private final float width;
   private final float height;
   private final String label;
   private final Runnable action;
   private float x;
   private float y;

   public Button(float width, float height, String label, Runnable action) {
      this.width = width;
      this.height = height;
      this.label = label;
      this.action = action;
   }

   public AnimationUtil getAnimation() {
      return this.a;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public String getLabel() {
      return this.label;
   }

   public Runnable getAction() {
      return this.action;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public void setPosition(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta, float open) {
      this.a.a(this.action != null && MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height));
      this.a.a(0.0F, 1.0F, 0.35F, EasingList.i, delta);
      float hover = Math.min(1.0F, this.a.c() / 0.9F);
      float scale = (0.85F + 0.15F * EasingList.s.ease(open)) * (1.0F + 0.03F * hover);
      MatrixStack matrices = context.getMatrices();
      float cx = this.x + this.width / 2.0F;
      float cy = this.y + this.height / 2.0F;
      matrices.push();
      matrices.translate(cx, cy, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-cx, -cy, 0.0F);
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      draw.b(matrices, this.x, this.y, this.width, this.height, 8.0F, ColorUtil.convertToARGB(11, 11, 13, 120), open);
      draw.a(matrices, this.x, this.y, this.width, this.height, 8.0F, 0.5F, ColorUtil.convertToARGB(255, 255, 255, (int)(hover * 20.0F * open)));
      if (this.label != null) {
         float time = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
         MutableText text = Text.literal("");

         for (int i = 0; i < this.label.length(); i++) {
            float wave = (float)(Math.sin((time + i * 0.5F / this.label.length()) * Math.PI * 2.0) * 0.5 + 0.5);
            int c = (int)(180.0F + 65.0F * wave * hover);
            text.append(Text.literal(String.valueOf(this.label.charAt(i))).setStyle(Style.EMPTY.withColor(c << 16 | c << 8 | c)));
         }

         float labelW = Fonts.e.a(this.label, 8.0F);
         Fonts.e.a(matrices, text, this.x + (this.width - labelW) / 2.0F, this.y + (this.height - 9.0F) / 2.0F, 8.0F, 0.0F, open);
      }

      matrices.pop();
   }
}
