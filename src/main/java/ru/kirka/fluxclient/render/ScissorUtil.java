package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.kirka.fluxclient.common.Interface;

public class ScissorUtil implements Interface {
   private static final Deque<ScissorUtil.ScissorBox> scissorStack = new ArrayDeque<>();

   private ScissorUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void pushScissor(MatrixStack matrixStack, float x, float y, float width, float height) {
      int fbWidth = mc.getWindow().getFramebufferWidth();
      int fbHeight = mc.getWindow().getFramebufferHeight();
      Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(matrixStack.peek().getPositionMatrix());
      Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F);
      Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F);
      mvp.transform(p1);
      mvp.transform(p2);
      float p1w = p1.w != 0.0F ? p1.w : 1.0F;
      float p2w = p2.w != 0.0F ? p2.w : 1.0F;
      float ndcX1 = p1.x / p1w;
      float ndcY1 = p1.y / p1w;
      float ndcX2 = p2.x / p2w;
      float ndcY2 = p2.y / p2w;
      float screenX1 = (ndcX1 + 1.0F) * 0.5F * fbWidth;
      float screenY1 = (ndcY1 + 1.0F) * 0.5F * fbHeight;
      float screenX2 = (ndcX2 + 1.0F) * 0.5F * fbWidth;
      float screenY2 = (ndcY2 + 1.0F) * 0.5F * fbHeight;
      int sx = (int)Math.floor(Math.min(screenX1, screenX2));
      int sy = (int)Math.floor(Math.min(screenY1, screenY2));
      int sw = (int)Math.ceil(Math.abs(screenX2 - screenX1));
      int sh = (int)Math.ceil(Math.abs(screenY2 - screenY1));
      int clampedX = Math.max(0, Math.min(fbWidth, sx));
      int clampedY = Math.max(0, Math.min(fbHeight, sy));
      int clampedW = Math.max(0, Math.min(fbWidth - clampedX, sw));
      int clampedH = Math.max(0, Math.min(fbHeight - clampedY, sh));
      ScissorUtil.ScissorBox scissorBox = new ScissorUtil.ScissorBox(clampedX, clampedY, clampedW, clampedH);
      if (!scissorStack.isEmpty()) {
         scissorBox = scissorBox.intersect(scissorStack.peek());
      }

      scissorStack.push(scissorBox);
      matrixStack.push();
      apply(scissorBox);
   }

   public static void a(MatrixStack matrixStack, float x, float y, float width, float height) {
      pushScissor(matrixStack, x, y, width, height);
   }

   public static void popScissor(MatrixStack matrixStack) {
      if (!scissorStack.isEmpty()) {
         scissorStack.pop();
      }

      if (scissorStack.isEmpty()) {
         RenderSystem.disableScissor();
      } else {
         apply(scissorStack.peek());
      }

      matrixStack.pop();
   }

   public static void a(MatrixStack matrixStack) {
      popScissor(matrixStack);
   }

   private static void apply(ScissorUtil.ScissorBox box) {
      RenderSystem.enableScissor(box.x, box.y, box.w, box.h);
   }

   record ScissorBox(int x, int y, int w, int h) {
      ScissorUtil.ScissorBox intersect(ScissorUtil.ScissorBox other) {
         int nx = Math.max(this.x, other.x);
         int ny = Math.max(this.y, other.y);
         return new ScissorUtil.ScissorBox(
            nx, ny, Math.max(0, Math.min(this.x + this.w, other.x + other.w) - nx), Math.max(0, Math.min(this.y + this.h, other.y + other.h) - ny)
         );
      }
   }
}
