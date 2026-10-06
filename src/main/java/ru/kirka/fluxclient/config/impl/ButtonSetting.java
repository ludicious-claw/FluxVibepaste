package ru.kirka.fluxclient.config.impl;

import ru.kirka.fluxclient.config.Setting;

public class ButtonSetting extends Setting<Runnable> {
   public ButtonSetting(String name, String description, Runnable action) {
      super(name, description, action);
   }

   public void run() {
      if (this.value != null) {
         this.value.run();
      }
   }
}
