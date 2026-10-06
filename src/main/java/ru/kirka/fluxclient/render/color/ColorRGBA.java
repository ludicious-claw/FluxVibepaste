package ru.kirka.fluxclient.render.color;

import java.awt.Color;
import net.minecraft.util.math.MathHelper;

public class ColorRGBA {
   public static final ColorRGBA WHITE = new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F);
   public static final ColorRGBA BLACK = new ColorRGBA(0.0F, 0.0F, 0.0F, 255.0F);
   public static final ColorRGBA RED = new ColorRGBA(255.0F, 0.0F, 0.0F, 255.0F);
   public static final ColorRGBA GREEN = new ColorRGBA(0.0F, 255.0F, 0.0F, 255.0F);
   public static final ColorRGBA BLUE = new ColorRGBA(0.0F, 0.0F, 255.0F, 255.0F);
   private float red;
   private float green;
   private float blue;
   private float alpha;

   public ColorRGBA(float red, float green, float blue, float alpha) {
      this.red = MathHelper.clamp(red, 0.0F, 255.0F);
      this.green = MathHelper.clamp(green, 0.0F, 255.0F);
      this.blue = MathHelper.clamp(blue, 0.0F, 255.0F);
      this.alpha = MathHelper.clamp(alpha, 0.0F, 255.0F);
   }

   public ColorRGBA(float red, float green, float blue) {
      this(red, green, blue, 255.0F);
   }

   public ColorRGBA(int argb) {
      this.alpha = argb >> 24 & 0xFF;
      this.red = argb >> 16 & 0xFF;
      this.green = argb >> 8 & 0xFF;
      this.blue = argb & 0xFF;
   }

   public ColorRGBA(Color color) {
      this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
   }

   public static ColorRGBA of(int argb) {
      return new ColorRGBA(argb);
   }

   public float getRed() {
      return this.red;
   }

   public float getGreen() {
      return this.green;
   }

   public float getBlue() {
      return this.blue;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public float getRedFloat() {
      return this.red / 255.0F;
   }

   public float getGreenFloat() {
      return this.green / 255.0F;
   }

   public float getBlueFloat() {
      return this.blue / 255.0F;
   }

   public float getAlphaFloat() {
      return this.alpha / 255.0F;
   }

   public int getRGB() {
      return ((int)this.alpha & 0xFF) << 24 | ((int)this.red & 0xFF) << 16 | ((int)this.green & 0xFF) << 8 | (int)this.blue & 0xFF;
   }

   public ColorRGBA withAlpha(float newAlpha) {
      return new ColorRGBA(this.red, this.green, this.blue, newAlpha);
   }

   public ColorRGBA multAlpha(float factor) {
      return new ColorRGBA(this.red, this.green, this.blue, this.alpha * factor);
   }

   public static ColorRGBA lerp(ColorRGBA a, ColorRGBA b, float t) {
      float r = a.red + (b.red - a.red) * t;
      float g = a.green + (b.green - a.green) * t;
      float bl = a.blue + (b.blue - a.blue) * t;
      float al = a.alpha + (b.alpha - a.alpha) * t;
      return new ColorRGBA(r, g, bl, al);
   }

   public Color toAwt() {
      return new Color((int)this.red, (int)this.green, (int)this.blue, (int)this.alpha);
   }
}
