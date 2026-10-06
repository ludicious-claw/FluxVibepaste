package ru.kirka.fluxclient.render.geometry.gradient;

import ru.kirka.fluxclient.render.color.ColorRGBA;

public class Gradient {
   protected final ColorRGBA topLeftColor;
   protected final ColorRGBA bottomLeftColor;
   protected final ColorRGBA topRightColor;
   protected final ColorRGBA bottomRightColor;

   protected Gradient(ColorRGBA topLeftColor, ColorRGBA bottomLeftColor, ColorRGBA topRightColor, ColorRGBA bottomRightColor) {
      this.topLeftColor = topLeftColor;
      this.bottomLeftColor = bottomLeftColor;
      this.topRightColor = topRightColor;
      this.bottomRightColor = bottomRightColor;
   }

   public static Gradient of(ColorRGBA topLeftColor, ColorRGBA bottomLeftColor, ColorRGBA topRightColor, ColorRGBA bottomRightColor) {
      return new Gradient(topLeftColor, bottomLeftColor, topRightColor, bottomRightColor);
   }

   public static Gradient horizontal(ColorRGBA start, ColorRGBA end) {
      return new Gradient(start, start, end, end);
   }

   public static Gradient vertical(ColorRGBA top, ColorRGBA bottom) {
      return new Gradient(top, bottom, top, bottom);
   }

   public static Gradient diagonal(ColorRGBA start, ColorRGBA end) {
      return new Gradient(start, end, end, start);
   }

   public Gradient rotate() {
      return this;
   }

   public ColorRGBA getTopLeftColor() {
      return this.topLeftColor;
   }

   public ColorRGBA getBottomLeftColor() {
      return this.bottomLeftColor;
   }

   public ColorRGBA getTopRightColor() {
      return this.topRightColor;
   }

   public ColorRGBA getBottomRightColor() {
      return this.bottomRightColor;
   }
}
