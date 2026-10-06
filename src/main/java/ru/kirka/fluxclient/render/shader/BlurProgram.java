package ru.kirka.fluxclient.render.shader;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.kirka.fluxclient.render.draw.CustomRenderTarget;

public class BlurProgram {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final Supplier<CustomRenderTarget> CACHE = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());
   public static final Supplier<CustomRenderTarget> BUFFER = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());
   private long lastTime = 0L;
   private static KawaseBlurProgram kawaseDownProgram;
   private static KawaseBlurProgram kawaseUpProgram;
   private float blurOffset = 1.0F;
   private float blurDownscale = 0.5F;
   private final CustomRenderTarget[] pingPong = new CustomRenderTarget[2];

   public void initShaders() {
      kawaseDownProgram = new KawaseBlurProgram(Identifier.of("rockstar", "kawase_down/data"));
      kawaseUpProgram = new KawaseBlurProgram(Identifier.of("rockstar", "kawase_up/data"));
   }

   public void draw() {
      long now = System.currentTimeMillis();
      if (now - this.lastTime >= 25L) {
         this.lastTime = now;
         Framebuffer mainFbo = mc.getFramebuffer();
         if (mainFbo != null && mainFbo.getColorAttachment() > 0 && mainFbo.textureWidth > 0 && mainFbo.textureHeight > 0) {
            float shifted = this.blurOffset - 0.5F;
            float effectiveOffset;
            float effectiveDownscale;
            int steps;
            if (shifted >= 0.0F) {
               effectiveOffset = Math.max(0.005F, shifted);
               effectiveDownscale = 0.5F;
               if (shifted > 5.0F) {
                  steps = 7;
               } else if (shifted > 3.0F) {
                  steps = 5;
               } else {
                  steps = 3;
               }
            } else {
               float blend = MathHelper.clamp(this.blurOffset / 0.5F, 0.0F, 1.0F);
               effectiveDownscale = 1.0F - 0.5F * blend;
               effectiveOffset = 0.005F;
               steps = 3;
            }

            CustomRenderTarget cache = (CustomRenderTarget)CACHE.get();
            CustomRenderTarget buffer = (CustomRenderTarget)BUFFER.get();
            cache.setDownscale(effectiveDownscale).setLinear();
            buffer.setDownscale(effectiveDownscale).setLinear();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int scaledWidth = mc.getWindow().getFramebufferWidth();
            int scaledHeight = mc.getWindow().getFramebufferHeight();
            kawaseDownProgram.use();
            kawaseDownProgram.updateUniforms(effectiveOffset, mainFbo.textureWidth, mainFbo.textureHeight);
            cache.setup();
            mainFbo.beginRead();
            RenderSystem.setShaderTexture(0, mainFbo.getColorAttachment());
            this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
            cache.stop();
            CustomRenderTarget[] buffers = this.pingPong;
            buffers[0] = cache;
            buffers[1] = buffer;

            for (int i = 1; i < steps; i++) {
               int step = i & 1;
               int previous = step ^ 1;
               CustomRenderTarget source = buffers[previous];
               CustomRenderTarget target = buffers[step];
               target.setup();
               source.beginRead();
               RenderSystem.setShaderTexture(0, source.getColorAttachment());
               kawaseDownProgram.updateUniforms(effectiveOffset, source.textureWidth, source.textureHeight);
               this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
               source.endRead();
               target.stop();
            }

            kawaseUpProgram.use();

            for (int i = 0; i < steps; i++) {
               int step = i & 1;
               int next = step ^ 1;
               CustomRenderTarget source = buffers[step];
               CustomRenderTarget target = buffers[next];
               target.setup();
               source.beginRead();
               RenderSystem.setShaderTexture(0, source.getColorAttachment());
               kawaseUpProgram.updateUniforms(effectiveOffset, source.textureWidth, source.textureHeight);
               this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
               source.endRead();
               source.stop();
            }

            mainFbo.endRead();
            mainFbo.beginWrite(false);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
         }
      }
   }

   private void drawQuad(float x, float y, float width, float height) {
      int color = -1;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(x, y, 0.0F).texture(0.0F, 1.0F).color(color);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, 0.0F).color(color);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static int getTexture() {
      return ((CustomRenderTarget)BUFFER.get()).getColorAttachment();
   }

   public void setBlurOffset(float blurOffset) {
      this.blurOffset = blurOffset;
   }

   public void setBlurDownscale(float blurDownscale) {
      this.blurDownscale = blurDownscale;
   }
}
