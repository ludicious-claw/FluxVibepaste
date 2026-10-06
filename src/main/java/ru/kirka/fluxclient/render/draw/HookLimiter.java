package ru.kirka.fluxclient.render.draw;

import net.minecraft.client.MinecraftClient;

public class HookLimiter {
   private long lastHookTime;
   private int accumulatedCalls;
   private final boolean useMCFrameRate;
   private int currentFps = 0;
   private long hookIntervalNS = 0L;

   public HookLimiter(boolean useMCFrameRate) {
      this.lastHookTime = System.nanoTime();
      this.useMCFrameRate = useMCFrameRate;
      this.accumulatedCalls = 0;
   }

   public void execute(int fps, IHook... calls) {
      if (this.currentFps != fps) {
         this.hookIntervalNS = 1000000000L / Math.max(1, fps);
         this.currentFps = fps;
      }

      long nanoTime = System.nanoTime();
      long elapsed = nanoTime - this.lastHookTime;
      this.accumulatedCalls = this.accumulatedCalls + (int)(elapsed / Math.max(1L, this.hookIntervalNS));
      this.lastHookTime = this.lastHookTime + this.accumulatedCalls * this.hookIntervalNS;
      int maxLimit = this.useMCFrameRate ? Math.min(this.currentFps, MinecraftClient.getInstance().getCurrentFps()) : this.currentFps;
      this.accumulatedCalls = Math.min(this.accumulatedCalls, Math.max(1, maxLimit));

      while (this.accumulatedCalls > 0) {
         this.accumulatedCalls--;

         for (IHook call : calls) {
            call.execute();
         }
      }
   }
}
