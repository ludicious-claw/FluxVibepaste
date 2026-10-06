package ru.kirka.fluxclient.event;

public class ScrollEvent extends ru.kirka.fluxclient.core.Event {
   private final double horizontal;
   private final double vertical;

   public ScrollEvent(double horizontal, double vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
   }

   public double getHorizontal() {
      return this.horizontal;
   }

   public double getVertical() {
      return this.vertical;
   }
}
