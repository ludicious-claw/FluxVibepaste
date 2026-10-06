package ru.kirka.fluxclient.config.impl;

import ru.kirka.fluxclient.config.Setting;

public class BooleanSetting extends Setting<Boolean> {
   public BooleanSetting(String name, String description, boolean defaultValue) {
      super(name, description, defaultValue);
   }

   public BooleanSetting(String name, boolean defaultValue) {
      super(name, "", defaultValue);
   }
}
