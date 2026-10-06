package ru.kirka.fluxclient.event;

public class ClickEvent extends ru.kirka.fluxclient.core.Event {
   private final double x;
   private final double y;
   private final int button;
   private final ClickEvent.Action action;
   private boolean cancelled;

   public ClickEvent(double x, double y, int button, ClickEvent.Action action) {
      this.x = x;
      this.y = y;
      this.button = button;
      this.action = action;
   }

   public double getX() {
      return this.x;
   }

   public double getMouseX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getMouseY() {
      return this.y;
   }

   public int getButton() {
      return this.button;
   }

   public int h() {
      return this.button;
   }

   public ClickEvent.Action getAction() {
      return this.action;
   }

   public boolean isPress() {
      return this.action == ClickEvent.Action.PRESS;
   }

   public boolean b() {
      return this.isPress();
   }

   public boolean isRelease() {
      return this.action == ClickEvent.Action.RELEASE;
   }

   public boolean c() {
      return this.isRelease();
   }

   public boolean isDrag() {
      return this.action == ClickEvent.Action.DRAG;
   }

   public boolean d() {
      return this.isDrag();
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public boolean a() {
      return this.cancelled;
   }

   @Override
   public void cancel() {
      this.cancelled = true;
   }

   public static enum Action {
      PRESS,
      RELEASE,
      DRAG;
   }

   public static class a {
      public static final ClickEvent.Action PRESS = ClickEvent.Action.PRESS;
      public static final ClickEvent.Action RELEASE = ClickEvent.Action.RELEASE;
      public static final ClickEvent.Action DRAG = ClickEvent.Action.DRAG;
   }
}
