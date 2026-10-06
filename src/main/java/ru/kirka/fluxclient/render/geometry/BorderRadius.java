package ru.kirka.fluxclient.render.geometry;

public class BorderRadius {
   public static final BorderRadius ZERO = new BorderRadius(0.0F, 0.0F, 0.0F, 0.0F);
   private final float topLeft;
   private final float topRight;
   private final float bottomRight;
   private final float bottomLeft;

   public BorderRadius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
      this.topLeft = topLeft;
      this.topRight = topRight;
      this.bottomRight = bottomRight;
      this.bottomLeft = bottomLeft;
   }

   public static BorderRadius all(float radius) {
      return new BorderRadius(radius, radius, radius, radius);
   }

   public static BorderRadius of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
      return new BorderRadius(topLeft, topRight, bottomRight, bottomLeft);
   }

   public float getTopLeft() {
      return this.topLeft;
   }

   public float getTopRight() {
      return this.topRight;
   }

   public float getBottomRight() {
      return this.bottomRight;
   }

   public float getBottomLeft() {
      return this.bottomLeft;
   }

   public float topLeftRadius() {
      return this.topLeft;
   }

   public float topRightRadius() {
      return this.topRight;
   }

   public float bottomLeftRadius() {
      return this.bottomLeft;
   }

   public float bottomRightRadius() {
      return this.bottomRight;
   }
}
