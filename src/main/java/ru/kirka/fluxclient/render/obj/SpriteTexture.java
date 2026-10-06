package ru.kirka.fluxclient.render.obj;

public enum SpriteTexture {
   MENU("icons/batched/menu.png", 96.0F, 16.0F, 16.0F),
   BIG_MENU("icons/batched/bigmenu.png", 120.0F, 20.0F, 20.0F);

   private final String texture;
   private final float width;
   private final float height;
   private final float step;
   public float x;

   private SpriteTexture(String texture, float width, float height, float step) {
      this.texture = texture;
      this.width = width;
      this.height = height;
      this.step = step;
   }

   public String getTexture() {
      return this.texture;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public float getStep() {
      return this.step;
   }

   public float getX() {
      return this.x;
   }
}
