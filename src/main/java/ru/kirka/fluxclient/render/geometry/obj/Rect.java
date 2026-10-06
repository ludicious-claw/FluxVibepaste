package ru.kirka.fluxclient.render.geometry.obj;

public class Rect {
   public static final Rect EMPTY = new Rect(0.0F, 0.0F, 0.0F, 0.0F);
   protected float x;
   protected float y;
   protected float width;
   protected float height;

   public Rect() {
   }

   public Rect(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public void set(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public Rect x(float x) {
      return new Rect(x, this.y, this.width, this.height);
   }

   public Rect y(float y) {
      return new Rect(this.x, y, this.width, this.height);
   }

   public Rect width(float width) {
      return new Rect(this.x, this.y, width, this.height);
   }

   public Rect height(float height) {
      return new Rect(this.x, this.y, this.width, height);
   }

   public Rect size(float off) {
      return new Rect(this.x + off, this.y + off, this.width - off * 2.0F, this.height - off * 2.0F);
   }

   public static Rect interpolate(Rect oldValue, Rect newValue, double t) {
      float ix = (float)(oldValue.x + (newValue.x - oldValue.x) * t);
      float iy = (float)(oldValue.y + (newValue.y - oldValue.y) * t);
      float iw = (float)(oldValue.width + (newValue.width - oldValue.width) * t);
      float ih = (float)(oldValue.height + (newValue.height - oldValue.height) * t);
      return new Rect(ix, iy, iw, ih);
   }

   public boolean contains(float x, float y, float width, float height) {
      return this.x + this.width > x && this.x < x + width && this.y + this.height > y && this.y < y + height;
   }

   public boolean inside(float x, float y, float width, float height) {
      return this.x > x && this.x + this.width < x + width && this.y > y && this.y + this.height < y + height;
   }

   public boolean hovered(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }
}
