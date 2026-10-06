package ru.kirka.fluxclient.ui.shader;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.EventTarget;
import ru.kirka.fluxclient.event.ResizeEvent;

public class NoiseShader extends Shader implements Interface {
   private static final Identifier c = Identifier.of("fluxclient", "core/noise/noise_shader");
   private final Matrix4f d = new Matrix4f();
   private SimpleFramebuffer e;
   private Uniform f;
   private Uniform g;

   public NoiseShader() {
      super(c, VertexFormats.POSITION_COLOR);
      EventManager.a(this);
   }

   @EventTarget
   public void a(ResizeEvent event) {
      if (mc.getWindow() != null) {
         this.e = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
      }
   }

   public void e() {
      if (this.e != null && mc.getFramebuffer() != null) {
         this.e.copyDepthFrom(mc.getFramebuffer());
      }
   }

   @Override
   protected void b() {
      this.f = this.a("TintColor");
      this.g = this.a("Time");
   }

   public void a(float[] color) {
      if (this.e != null && mc.getFramebuffer() != null) {
         RenderSystem.backupProjectionMatrix();
         RenderSystem.setProjectionMatrix(this.d, ProjectionType.PERSPECTIVE);
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix().identity();
         RenderSystem.disableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
         RenderSystem.setShaderTexture(1, mc.getFramebuffer().getDepthAttachment());
         RenderSystem.setShaderTexture(2, this.e.getDepthAttachment());
         this.a();
         if (this.f != null) {
            this.f.set(color[0], color[1], color[2], color[3]);
         }

         if (this.g != null) {
            this.g.set((float)(System.currentTimeMillis() % 100000L) / 1000.0F);
         }

         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buffer.vertex(this.d, -1.0F, -1.0F, 0.0F).color(-1);
         buffer.vertex(this.d, -1.0F, 1.0F, 0.0F).color(-1);
         buffer.vertex(this.d, 1.0F, 1.0F, 0.0F).color(-1);
         buffer.vertex(this.d, 1.0F, -1.0F, 0.0F).color(-1);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         modelView.popMatrix();
         RenderSystem.restoreProjectionMatrix();
      }
   }
}
