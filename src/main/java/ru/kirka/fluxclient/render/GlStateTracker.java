package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

public final class GlStateTracker {
   private static boolean blend;
   private static boolean depthTest;
   private static boolean cull;

   public static void save() {
      blend = GL11.glIsEnabled(3042);
      depthTest = GL11.glIsEnabled(2929);
      cull = GL11.glIsEnabled(2884);
   }

   public static void restore() {
      if (blend) {
         RenderSystem.enableBlend();
      } else {
         RenderSystem.disableBlend();
      }

      if (depthTest) {
         RenderSystem.enableDepthTest();
      } else {
         RenderSystem.disableDepthTest();
      }

      if (cull) {
         RenderSystem.enableCull();
      } else {
         RenderSystem.disableCull();
      }
   }
}
