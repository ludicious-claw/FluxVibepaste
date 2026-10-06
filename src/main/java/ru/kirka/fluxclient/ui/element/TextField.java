package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.Font;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.theme.ThemeInfo;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.util.CursorUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class TextField {
   private final TextField.type a;
   private final boolean numbersOnly;
   private final StringBuilder textBuffer;
   private Vector2f position = new Vector2f(0.0F, 0.0F);
   private Vector2f size = new Vector2f(0.0F, 0.0F);
   private String placeholder = "";
   private Vector2f selectionRange;
   private int cursorIndex;
   private boolean focused;
   private boolean hovering;
   private float scrollOffset;

   public TextField(TextField.type type) {
      this(type, false);
   }

   public TextField(TextField.type type, boolean numbers) {
      this.textBuffer = new StringBuilder();
      this.selectionRange = new Vector2f(0.0F, 0.0F);
      this.a = type;
      this.numbersOnly = numbers;
   }

   public TextField.type getType() {
      return this.a;
   }

   public boolean isNumbersOnly() {
      return this.numbersOnly;
   }

   public Vector2f getPosition() {
      return this.position;
   }

   public void setPosition(Vector2f position) {
      this.position = position;
   }

   public Vector2f getSize() {
      return this.size;
   }

   public void setSize(Vector2f size) {
      this.size = size;
   }

   public String getPlaceholder() {
      return this.placeholder;
   }

   public void setPlaceholder(String placeholder) {
      this.placeholder = placeholder;
   }

   public StringBuilder getTextBuffer() {
      return this.textBuffer;
   }

   public Vector2f getSelectionRange() {
      return this.selectionRange;
   }

   public int getCursorIndex() {
      return this.cursorIndex;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public boolean isHovering() {
      return this.hovering;
   }

   public float getScrollOffset() {
      return this.scrollOffset;
   }

   public void render(DrawContext context, double mouseX, double mouseY, float delta, float alpha) {
      MatrixStack matrices = context.getMatrices();
      Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
      ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
      float lineHeight = this.a.d.getFontData().lineHeight() * this.a.e;
      float textX = this.position.getX() + this.a.f;
      float textY = this.position.getY() + (this.size.getY() - lineHeight) / 2.0F + this.a.g;
      float visibleWidth = this.size.getX() - this.a.f * 2.0F;
      this.updateScrollOffset(visibleWidth);
      boolean hover = MathUtil.isHovered(mouseX, mouseY, this.position.getX(), this.position.getY(), this.size.getX(), this.size.getY());
      if (hover != this.hovering) {
         this.hovering = hover;
         CursorUtil.setCursor(hover ? CursorUtil.CursorType.TEXT : CursorUtil.CursorType.DEFAULT);
      }

      boolean placeholding = !this.focused && this.textBuffer.isEmpty() && this.a.h;
      String content = placeholding ? this.placeholder : this.textBuffer.toString();
      int color = ColorUtil.applyAlphaToColor(theme.a(placeholding ? ThemeInfo.TEXT_DISABLED : ThemeInfo.TEXT).toIntColor(), alpha);
      this.a.a(draw, matrices, this.position.getX(), this.position.getY(), this.size.getX(), this.size.getY(), alpha);
      this.drawSelectionHighlight(context, textX, textY, lineHeight, visibleWidth, alpha);
      this.drawCaret(context, textX, textY, lineHeight, visibleWidth, alpha);
      this.a.d.c(matrices, this.trimToVisibleContent(content), textX, textY, this.a.e, color, visibleWidth);
   }

   private void drawSelectionHighlight(DrawContext context, float textX, float textY, float lineHeight, float visibleWidth, float alpha) {
      if (this.focused && this.selectionRange.getX() != this.selectionRange.getY()) {
         int from = (int)Math.min(this.selectionRange.getX(), this.selectionRange.getY());
         int to = (int)Math.max(this.selectionRange.getX(), this.selectionRange.getY());
         float start = Math.max(textX + this.getCharWidth(from) - this.scrollOffset, textX);
         float end = Math.min(textX + this.getCharWidth(to) - this.scrollOffset, textX + visibleWidth);
         if (start < end) {
            Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
            ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
            draw.a(context, start, textY, end - start, lineHeight, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.47F * alpha));
         }
      }
   }

   private void drawCaret(DrawContext context, float textX, float textY, float lineHeight, float visibleWidth, float alpha) {
      if (this.focused) {
         float caretX = textX + this.getCharWidth((int)this.selectionRange.getY()) - this.scrollOffset;
         if (caretX >= textX && caretX <= textX + visibleWidth) {
            float blink = (float)(Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5);
            float caretHeight = this.a.e / 1.01F;
            float caretY = textY + (lineHeight - caretHeight) / 2.0F;
            Draw2DProcessor draw = FluxClient.getInstance().getDraw2DProcessor();
            ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
            draw.a(context, caretX, caretY, 0.5F, caretHeight, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), blink * alpha));
         }
      }
   }

   private float getCharWidth(int index) {
      return this.a.d.a(this.textBuffer.substring(0, Math.min(index, this.textBuffer.length())), this.a.e) + 0.5F;
   }

   private String trimToVisibleContent(String content) {
      if (content != null && !content.isEmpty()) {
         for (int i = 0; i < content.length(); i++) {
            if (this.a.d.a(content.substring(0, i), this.a.e) >= this.scrollOffset) {
               return content.substring(i);
            }
         }

         return "";
      } else {
         return content == null ? "" : content;
      }
   }

   private void updateScrollOffset(float visibleWidth) {
      float cursor = this.getCharWidth((int)this.selectionRange.getY());
      if (cursor - this.scrollOffset > visibleWidth) {
         this.scrollOffset = cursor - visibleWidth;
      } else if (cursor < this.scrollOffset) {
         this.scrollOffset = cursor;
      }

      if (this.a.d.a(this.textBuffer.toString(), this.a.e) < visibleWidth) {
         this.scrollOffset = 0.0F;
      }
   }

   public void onMouseClick(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.position.getX(), this.position.getY(), this.size.getX(), this.size.getY())) {
         this.focused = true;
         if (button == 0) {
            int cursor = this.getCharIndexAtPosition((float)mouseX);
            this.cursorIndex = cursor;
            this.selectionRange = new Vector2f(cursor, cursor);
         }
      } else {
         if (this.focused) {
            this.a(false);
         }
      }
   }

   public void onMouseDrag(double mouseX, double mouseY, int button) {
      if (this.focused && button == 0) {
         int cursor = this.getCharIndexAtPosition((float)mouseX);
         this.selectionRange = new Vector2f(Math.min(this.cursorIndex, cursor), Math.max(this.cursorIndex, cursor));
      }
   }

   private int getCharIndexAtPosition(float mouseX) {
      float textX = this.position.getX() + this.a.f;
      float adjusted = mouseX + this.scrollOffset;

      for (int i = 0; i <= this.textBuffer.length(); i++) {
         if (textX + this.getCharWidth(i) > adjusted) {
            return i;
         }
      }

      return this.textBuffer.length();
   }

   public void a(int keyCode, int scanCode, int modifiers) {
      if (this.focused) {
         boolean ctrl = (modifiers & 2) != 0;
         boolean hasSelection = this.selectionRange.getX() != this.selectionRange.getY();
         if (ctrl && keyCode == 65) {
            this.selectionRange = new Vector2f(0.0F, this.textBuffer.length());
            return;
         }

         if (ctrl && keyCode == 67) {
            this.m();
            return;
         }

         if (ctrl && keyCode == 86) {
            this.n();
            return;
         }

         if (keyCode == 259) {
            this.b(hasSelection);
            return;
         }

         if (keyCode == 261) {
            this.c(hasSelection);
            return;
         }

         if (keyCode == 263) {
            this.b(-1);
            return;
         }

         if (keyCode == 262) {
            this.b(1);
         } else if (keyCode == 257 || keyCode == 256) {
            this.a(false);
         }
      }
   }

   public void a(char chr, int modifiers) {
      if (this.focused && (!this.numbersOnly || chr >= '0' && chr <= '9')) {
         this.c(String.valueOf(chr));
      }
   }

   private void c(String string) {
      if (this.selectionRange.getX() != this.selectionRange.getY()) {
         this.o();
      }

      int pos = (int)this.selectionRange.getY();
      this.textBuffer.insert(pos, string);
      this.selectionRange = new Vector2f(pos + string.length(), pos + string.length());
   }

   private void m() {
      int from = (int)Math.min(this.selectionRange.getX(), this.selectionRange.getY());
      int to = (int)Math.max(this.selectionRange.getX(), this.selectionRange.getY());
      if (from < to && Interface.mc.getWindow() != null) {
         GLFW.glfwSetClipboardString(Interface.mc.getWindow().getHandle(), this.textBuffer.substring(from, to));
      }
   }

   private void n() {
      if (Interface.mc.getWindow() != null) {
         String clip = GLFW.glfwGetClipboardString(Interface.mc.getWindow().getHandle());
         if (clip != null && !clip.isEmpty()) {
            this.c(this.numbersOnly ? clip.replaceAll("[^0-9]", "") : clip);
         }
      }
   }

   private void b(boolean hasSelection) {
      if (hasSelection) {
         this.o();
      } else if (this.selectionRange.getY() > 0.0F) {
         int pos = (int)this.selectionRange.getY();
         this.textBuffer.deleteCharAt(pos - 1);
         this.selectionRange = new Vector2f(pos - 1, pos - 1);
      }
   }

   private void c(boolean hasSelection) {
      if (hasSelection) {
         this.o();
      } else if (this.selectionRange.getY() < this.textBuffer.length()) {
         this.textBuffer.deleteCharAt((int)this.selectionRange.getY());
      }
   }

   private void b(int direction) {
      int pos = (int)this.selectionRange.getY() + direction;
      if (pos >= 0 && pos <= this.textBuffer.length()) {
         this.selectionRange = new Vector2f(pos, pos);
      }
   }

   private void o() {
      int from = (int)Math.min(this.selectionRange.getX(), this.selectionRange.getY());
      int to = (int)Math.max(this.selectionRange.getX(), this.selectionRange.getY());
      this.textBuffer.delete(from, to);
      this.selectionRange = new Vector2f(from, from);
   }

   public void a() {
      this.textBuffer.setLength(0);
      this.selectionRange = new Vector2f(0.0F, 0.0F);
      this.focused = false;
   }

   public void a(boolean status) {
      this.focused = status;
      this.selectionRange = new Vector2f(status ? this.textBuffer.length() : 0.0F, status ? this.textBuffer.length() : 0.0F);
   }

   public static enum type {
      ALT_MANAGER(Fonts.b, 7.0F, 6.0F, -0.5F, true) {
         @Override
         public void a(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float width, float height, float alpha) {
            draw.a(matrices, x, y, width + 2.0F, height, new Vector4f(5.0F, 1.0F, 5.0F, 1.0F), ColorUtil.applyAlphaToColor(16777215, 0.039215688F * alpha));
         }
      },
      GUI(Fonts.c, 7.0F, 6.0F, 0.0F, true) {
         @Override
         public void a(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float width, float height, float alpha) {
            ThemeProcessor theme = FluxClient.getInstance().getThemeProcessor();
            int background = ColorUtil.applyAlphaToColor(
               ColorUtil.lerpColor(theme.a(ThemeInfo.BACKGROUND_GUI).toIntColor(), theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.05F), 0.78431374F * alpha
            );
            draw.a(matrices, x, y, width, height, 6.0F, background, alpha, background, 2.0F);
            draw.a(
               matrices,
               x,
               y,
               width,
               height,
               6.0F,
               0.5F,
               ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.OUTLINE_MEDIUM).toIntColor(), theme.a(ThemeInfo.OUTLINE_MEDIUM).getAlphaFloat() * alpha)
            );
         }
      },
      GUI_SETTING(Fonts.c, 6.5F, 4.0F, 0.0F, true) {
         @Override
         public void a(Draw2DProcessor draw, MatrixStack matrices, float x, float y, float width, float height, float alpha) {
            draw.a(matrices, x, y, width, height, 2.5F, ColorUtil.convertToARGB(255, 255, 255, (int)(14.0F * alpha)));
            draw.a(matrices, x, y, width, height, 2.5F, 0.5F, ColorUtil.convertToARGB(255, 255, 255, (int)(28.0F * alpha)));
         }
      };

      final Font d;
      final float e;
      final float f;
      final float g;
      final boolean h;

      private type(final Font font, final float fontSize, final float paddingX, final float textOffset, final boolean placeholder) {
         this.d = font;
         this.e = fontSize;
         this.f = paddingX;
         this.g = textOffset;
         this.h = placeholder;
      }

      public abstract void a(Draw2DProcessor var1, MatrixStack var2, float var3, float var4, float var5, float var6, float var7);

      public Font a() {
         return this.d;
      }

      public float b() {
         return this.e;
      }

      public float c() {
         return this.f;
      }

      public float d() {
         return this.g;
      }

      public boolean e() {
         return this.h;
      }
   }
}
