package ru.kirka.fluxclient.core;

public abstract class Event {
   private boolean cancelled = false;

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }

   public boolean a() {
      return this.cancelled;
   }
}
