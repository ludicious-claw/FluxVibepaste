package ru.kirka.fluxclient.render.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public class CustomRenderTarget extends Framebuffer {
   private boolean linear;
   private float downscale = 1.0F;
   private int fixedWidth = -1;
   private int fixedHeight = -1;

   public CustomRenderTarget(boolean useDepth) {
      super(useDepth);
   }

   public CustomRenderTarget(int width, int height, boolean useDepth) {
      super(useDepth);
      this.resize(width, height);
   }

   public CustomRenderTarget setLinear() {
      this.linear = true;
      RenderSystem.recordRenderCall(() -> {
         if (this.getColorAttachment() > 0) {
            this.setTexFilter(9729);
         }
      });
      return this;
   }

   public CustomRenderTarget setDownscale(float factor) {
      this.downscale = Math.max(0.1F, Math.min(1.0F, factor));
      return this;
   }

   public CustomRenderTarget setFixedSize(int width, int height) {
      this.fixedWidth = Math.max(width, 1);
      this.fixedHeight = Math.max(height, 1);
      return this;
   }

   public void setTexFilter(int texFilter) {
      super.setTexFilter(this.linear ? 9729 : texFilter);
   }

   private void resizeFramebuffer() {
      if (this.needsNewFramebuffer()) {
         int targetWidth = this.targetWidth();
         int targetHeight = this.targetHeight();
         this.initFbo(targetWidth, targetHeight);
      }
   }

   public void setup(boolean clear) {
      this.resizeFramebuffer();
      if (clear) {
         this.clear();
      }

      this.beginWrite(false);
   }

   public void setup() {
      this.setup(true);
   }

   public void stop() {
      this.endWrite();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getFramebuffer() != null) {
         mc.getFramebuffer().beginWrite(true);
      }
   }

   private boolean needsNewFramebuffer() {
      return this.textureWidth != this.targetWidth() || this.textureHeight != this.targetHeight();
   }

   private int targetWidth() {
      MinecraftClient mc = MinecraftClient.getInstance();
      int width = this.fixedWidth > 0 ? this.fixedWidth : mc.getWindow().getFramebufferWidth();
      return Math.max((int)Math.floor(width * this.downscale), 1);
   }

   private int targetHeight() {
      MinecraftClient mc = MinecraftClient.getInstance();
      int height = this.fixedHeight > 0 ? this.fixedHeight : mc.getWindow().getFramebufferHeight();
      return Math.max((int)Math.floor(height * this.downscale), 1);
   }
}
