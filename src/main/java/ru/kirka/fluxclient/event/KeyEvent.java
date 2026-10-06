package ru.kirka.fluxclient.event;

public class KeyEvent extends ru.kirka.fluxclient.core.Event implements Event {
   private final int key;
   private final int scancode;
   private final int action;
   private final int modifiers;

   public KeyEvent(int key, int scancode, int action, int modifiers) {
      this.key = key;
      this.scancode = scancode;
      this.action = action;
      this.modifiers = modifiers;
   }

   public int getKey() {
      return this.key;
   }

   public int getScancode() {
      return this.scancode;
   }

   public int getAction() {
      return this.action;
   }

   public int getModifiers() {
      return this.modifiers;
   }
}
