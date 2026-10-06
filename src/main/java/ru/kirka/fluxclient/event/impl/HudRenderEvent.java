package ru.kirka.fluxclient.event.impl;

import ru.kirka.fluxclient.event.Event;
import ru.kirka.fluxclient.render.context.CustomDrawContext;

public class HudRenderEvent implements Event {
   private final CustomDrawContext context;
   private final float tickDelta;

   public HudRenderEvent(CustomDrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
   }

   public CustomDrawContext getContext() {
      return this.context;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}
