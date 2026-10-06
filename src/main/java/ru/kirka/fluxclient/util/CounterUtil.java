package ru.kirka.fluxclient.util;

public class CounterUtil {
   private long lastMillis;
   private long tickCount;
   private long randomSeed;

   public CounterUtil() {
      this.reset();
   }

   public void setMillis(long millis) {
      this.lastMillis = millis;
   }

   public void setTicks(long ticks) {
      this.tickCount = ticks;
   }

   public boolean hasReached(long delay) {
      return System.currentTimeMillis() - delay >= this.lastMillis;
   }

   public boolean a(long delay) {
      return this.hasReached(delay);
   }

   public boolean hasReached(long delay, long jitter) {
      return System.currentTimeMillis() - (delay + this.randomSeed % (jitter + 1L)) >= this.lastMillis;
   }

   public boolean a(long delay, long jitter) {
      return this.hasReached(delay, jitter);
   }

   public boolean hasTicksReached(long delay) {
      return this.tickCount >= delay;
   }

   public boolean b(long delay) {
      return this.hasTicksReached(delay);
   }

   public boolean hasTicksReached(long delay, long jitter) {
      return this.tickCount >= delay + this.randomSeed % (jitter + 1L);
   }

   public boolean b(long delay, long jitter) {
      return this.hasTicksReached(delay, jitter);
   }

   public void incrementTicks() {
      this.tickCount++;
   }

   public void a() {
      this.incrementTicks();
   }

   public void reset() {
      this.lastMillis = System.currentTimeMillis();
      this.randomSeed = (long)(Math.random() * 9.223372E18F);
      this.tickCount = 0L;
   }

   public void b() {
      this.reset();
   }

   public long getElapsedTime() {
      return System.currentTimeMillis() - this.lastMillis;
   }

   public long c() {
      return this.getElapsedTime();
   }

   public long getTickCount() {
      return this.tickCount;
   }

   public long d() {
      return this.getTickCount();
   }
}
