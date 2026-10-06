package ru.kirka.fluxclient.util;

import org.lwjgl.glfw.GLFW;
import ru.kirka.fluxclient.common.Interface;

public class CursorUtil implements Interface {
   private CursorUtil() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static void setCursor(CursorUtil.CursorType type) {
      if (mc != null && mc.getWindow() != null) {
         long cursor = GLFW.glfwCreateStandardCursor(type.getGlfwType());
         if (cursor != 0L) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), cursor);
         }
      }
   }

   public static void a(CursorUtil.CursorType type) {
      setCursor(type);
   }

   public static enum CursorType {
      DEFAULT(221185),
      HAND(221188),
      ARROW_HORIZONTAL(221189),
      ARROW_VERTICAL(221190),
      TEXT(221186),
      CROSSHAIR(221187),
      BLOCK(221194),
      RESIZE_ALL(221193);

      private final int glfwType;

      private CursorType(int glfwType) {
         this.glfwType = glfwType;
      }

      public int getGlfwType() {
         return this.glfwType;
      }
   }

   public static class a {
      public static final CursorUtil.CursorType DEFAULT = CursorUtil.CursorType.DEFAULT;
      public static final CursorUtil.CursorType HAND = CursorUtil.CursorType.HAND;
      public static final CursorUtil.CursorType ARROW_HORIZONTAL = CursorUtil.CursorType.ARROW_HORIZONTAL;
      public static final CursorUtil.CursorType ARROW_VERTICAL = CursorUtil.CursorType.ARROW_VERTICAL;
      public static final CursorUtil.CursorType TEXT = CursorUtil.CursorType.TEXT;
      public static final CursorUtil.CursorType CROSSHAIR = CursorUtil.CursorType.CROSSHAIR;
      public static final CursorUtil.CursorType BLOCK = CursorUtil.CursorType.BLOCK;
      public static final CursorUtil.CursorType RESIZE_ALL = CursorUtil.CursorType.RESIZE_ALL;
   }
}
