package ru.kirka.fluxclient.event;

public class ResizeEvent extends ru.kirka.fluxclient.core.Event {
   private final int width;
   private final int height;

   public ResizeEvent(int width, int height) {
      this.width = width;
      this.height = height;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }
}
