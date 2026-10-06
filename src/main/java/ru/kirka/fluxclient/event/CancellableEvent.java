package ru.kirka.fluxclient.event;

public abstract class CancellableEvent implements Event {
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
}
