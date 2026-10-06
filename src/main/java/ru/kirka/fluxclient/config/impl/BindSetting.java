package ru.kirka.fluxclient.config.impl;

import ru.kirka.fluxclient.config.Setting;

public class BindSetting extends Setting<Integer> {
   public BindSetting(String name, String description, int defaultKey) {
      super(name, description, defaultKey);
   }

   public int getKey() {
      return this.value != null ? this.value : -1;
   }

   public void setKey(int key) {
      this.set(key);
   }
}
