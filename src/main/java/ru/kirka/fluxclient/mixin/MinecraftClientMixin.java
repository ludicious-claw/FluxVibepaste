package ru.kirka.fluxclient.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.ui.screen.GUIScreen;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
   @Shadow
   @Final
   private Window window;

   @Inject(method = "updateWindowTitle", at = @At("HEAD"), cancellable = true)
   private void onUpdateWindowTitle(CallbackInfo ci) {
      this.window.setTitle("FluxClient от Kirka_int :3");
      ci.cancel();
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInitComplete(CallbackInfo ci) {
      FluxContext.get().getImGuiEngine().init(this.window.getHandle());
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void onSetScreen(Screen screen, CallbackInfo ci) {
      MinecraftClient mc = (MinecraftClient)(Object)this;
      if (mc.currentScreen instanceof GUIScreen && (screen == null || screen instanceof DownloadingTerrainScreen)) {
         for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String cn = element.getClassName();
            if (cn.equals(Screen.class.getName())
               || cn.equals(Keyboard.class.getName())
               || cn.contains("GUIScreen")
               || cn.contains("ClickGui")
               || cn.contains("Module")
               || cn.contains("Flux")) {
               return;
            }
         }

         ci.cancel();
      }
   }
}
