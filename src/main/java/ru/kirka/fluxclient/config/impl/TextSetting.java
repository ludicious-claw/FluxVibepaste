package ru.kirka.fluxclient.config.impl;

import ru.kirka.fluxclient.config.Setting;

public class TextSetting extends Setting<String> {
   public TextSetting(String name, String description, String defaultValue) {
      super(name, description, defaultValue != null ? defaultValue : "");
   }
}
