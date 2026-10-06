package ru.kirka.fluxclient.event.impl;

import net.minecraft.client.sound.SoundInstance;
import ru.kirka.fluxclient.event.Event;

public class SoundEvent implements Event {
   public SoundInstance sound;

   public SoundEvent(SoundInstance sound) {
      this.sound = sound;
   }

   public SoundInstance getSound() {
      return this.sound;
   }
}
