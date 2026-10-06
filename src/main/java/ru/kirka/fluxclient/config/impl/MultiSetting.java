package ru.kirka.fluxclient.config.impl;

import java.util.Arrays;
import java.util.List;
import ru.kirka.fluxclient.config.Setting;

public class MultiSetting extends Setting<List<BooleanSetting>> {
   public MultiSetting(String name, String description, BooleanSetting... options) {
      super(name, description, Arrays.asList(options));
   }

   public List<BooleanSetting> getOptions() {
      return this.get();
   }

   public BooleanSetting get(String optionName) {
      for (BooleanSetting b : this.get()) {
         if (b.getName().equals(optionName)) {
            return b;
         }
      }

      return null;
   }

   public boolean isEnabled(String optionName) {
      BooleanSetting b = this.get(optionName);
      return b != null && b.get();
   }
}
