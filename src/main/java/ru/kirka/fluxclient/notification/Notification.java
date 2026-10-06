package ru.kirka.fluxclient.notification;

import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.util.CounterUtil;

public class Notification {
   private final AnimationUtil animation = new AnimationUtil();
   private final CounterUtil counter = new CounterUtil();
   private final Object message;
   private final Object symbol;
   private final int color;
   private int time;

   public Notification(Object symbol, int color, Object message, int time) {
      this.symbol = symbol;
      this.color = color;
      this.message = message;
      this.time = time;
   }

   public Notification(Object symbol, Object message, int time) {
      this(symbol, -1, message, time);
   }

   public void setTime(int time) {
      this.time = time;
   }

   public void a(int time) {
      this.setTime(time);
   }

   public AnimationUtil getAnimation() {
      return this.animation;
   }

   public AnimationUtil a() {
      return this.getAnimation();
   }

   public CounterUtil getCounter() {
      return this.counter;
   }

   public CounterUtil b() {
      return this.getCounter();
   }

   public Object getMessage() {
      return this.message;
   }

   public Object c() {
      return this.getMessage();
   }

   public Object getSymbol() {
      return this.symbol;
   }

   public Object d() {
      return this.getSymbol();
   }

   public int getColor() {
      return this.color;
   }

   public int e() {
      return this.getColor();
   }

   public int getTime() {
      return this.time;
   }

   public int f() {
      return this.getTime();
   }
}
