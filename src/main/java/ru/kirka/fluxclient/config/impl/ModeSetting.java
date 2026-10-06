package ru.kirka.fluxclient.config.impl;

import java.util.Arrays;
import java.util.List;
import ru.kirka.fluxclient.config.Setting;

public class ModeSetting extends Setting<String> {
   private final List<String> modes;
   private int index;

   public ModeSetting(String name, String description, String defaultMode, String... modes) {
      super(name, description, defaultMode);
      this.modes = Arrays.asList(modes);
      this.index = Math.max(0, this.modes.indexOf(defaultMode));
   }

   public List<String> getModes() {
      return this.modes;
   }

   public boolean is(String mode) {
      return this.value.equalsIgnoreCase(mode);
   }

   public void cycle() {
      if (!this.modes.isEmpty()) {
         this.index = (this.index + 1) % this.modes.size();
         this.set(this.modes.get(this.index));
      }
   }
}
