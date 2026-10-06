package ru.kirka.fluxclient.render.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class KawaseBlurProgram extends GlProgram {
   private GlUniform resolutionUniform;
   private GlUniform offsetUniform;
   private GlUniform saturationUniform;
   private GlUniform tintIntensityUniform;
   private GlUniform tintColorUniform;

   public KawaseBlurProgram(Identifier identifier) {
      super(identifier, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   public void updateUniforms(float offset) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (this.offsetUniform != null) {
         this.offsetUniform.set(offset);
      }

      if (this.resolutionUniform != null) {
         this.resolutionUniform.set(1.0F / mc.getWindow().getFramebufferWidth(), 1.0F / mc.getWindow().getFramebufferHeight());
      }

      if (this.saturationUniform != null) {
         this.saturationUniform.set(1.0F);
      }

      if (this.tintIntensityUniform != null) {
         this.tintIntensityUniform.set(0.0F);
      }

      if (this.tintColorUniform != null) {
         this.tintColorUniform.set(1.0F, 1.0F, 1.0F);
      }
   }

   public void updateUniforms(float offset, int textureWidth, int textureHeight) {
      if (this.offsetUniform != null) {
         this.offsetUniform.set(offset);
      }

      float invW = textureWidth > 0 ? 1.0F / textureWidth : 0.0F;
      float invH = textureHeight > 0 ? 1.0F / textureHeight : 0.0F;
      if (this.resolutionUniform != null) {
         this.resolutionUniform.set(invW, invH);
      }

      if (this.saturationUniform != null) {
         this.saturationUniform.set(1.0F);
      }

      if (this.tintIntensityUniform != null) {
         this.tintIntensityUniform.set(0.0F);
      }

      if (this.tintColorUniform != null) {
         this.tintColorUniform.set(1.0F, 1.0F, 1.0F);
      }
   }

   @Override
   protected void setup() {
      this.resolutionUniform = this.findUniform("Resolution");
      this.offsetUniform = this.findUniform("Offset");
      this.saturationUniform = this.findUniform("Saturation");
      this.tintIntensityUniform = this.findUniform("TintIntensity");
      this.tintColorUniform = this.findUniform("TintColor");
      super.setup();
   }
}
