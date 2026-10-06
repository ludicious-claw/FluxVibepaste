package ru.kirka.fluxclient.ui.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.EventTarget;
import ru.kirka.fluxclient.event.ResizeEvent;
import ru.kirka.fluxclient.feature.impl.render.Optimizations;

public class BlurShader extends Shader implements Interface {
   private final List<SimpleFramebuffer> n = new ArrayList<>();
   private final ShaderProgramKey o = new ShaderProgramKey(Identifier.of("fluxclient", "core/blur/upscale"), VertexFormats.POSITION, Defines.EMPTY);
   private final ShaderProgramKey p = new ShaderProgramKey(Identifier.of("fluxclient", "core/blur/downscale"), VertexFormats.POSITION, Defines.EMPTY);
   public Uniform c;
   public Uniform d;
   public Uniform e;
   public Uniform f;
   public Uniform g;
   public Uniform h;
   public Uniform i;
   public Uniform j;
   public Uniform k;
   public Uniform l;
   public Uniform m;

   public BlurShader() {
      super(Identifier.of("fluxclient", "core/rect/blurred_rect"), VertexFormats.POSITION_TEXTURE_COLOR);
      EventManager.a(this);
   }

   public List<SimpleFramebuffer> e() {
      if (this.n.isEmpty() && mc.getWindow() != null) {
         this.initFramebuffers();
      }

      return this.n;
   }

   private void initFramebuffers() {
      for (SimpleFramebuffer fb : this.n) {
         if (fb != null) {
            fb.delete();
         }
      }

      this.n.clear();

      for (int i = 0; i < 6; i++) {
         this.n.add(this.f());
      }
   }

   @EventTarget
   public void a(ResizeEvent event) {
      this.initFramebuffers();
   }

   @Override
   protected void b() {
      this.c = this.a("uSize");
      this.d = this.a("uRadius");
      this.e = this.a("uSmoothness");
      this.f = this.a("uMix");
      this.g = this.a("uAlpha");
      this.h = this.a("uTopLeftColor");
      this.i = this.a("uBottomLeftColor");
      this.j = this.a("uTopRightColor");
      this.k = this.a("uBottomRightColor");
      this.l = this.a("uGlowColor");
      this.m = this.a("uGlowRadius");
   }

   public void a(MatrixStack matrixStack) {
      if (this.n.isEmpty()) {
         this.initFramebuffers();
      }

      if (!this.n.isEmpty() && mc.getFramebuffer() != null) {
         float[] offsets = Optimizations.isFastGuiActive() ? new float[]{3.0F, 8.0F} : new float[]{2.5F, 6.0F, 12.0F, 22.0F, 38.0F, 60.0F};
         int passes = Math.min(this.n.size(), offsets.length);

         try {
            this.a(this.p, mc.getFramebuffer(), (Framebuffer)this.n.getFirst(), offsets[0]);

            for (int i = 0; i < passes - 1; i++) {
               this.a(this.p, (Framebuffer)this.n.get(i), (Framebuffer)this.n.get(i + 1), offsets[i + 1]);
            }

            for (int i = passes - 1; i > 0; i--) {
               this.a(this.o, (Framebuffer)this.n.get(i), (Framebuffer)this.n.get(i - 1), offsets[i - 1]);
            }
         } finally {
            mc.getFramebuffer().beginWrite(true);
         }
      }
   }

   private void a(ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, float offset) {
      destination.beginWrite(true);
      RenderSystem.setShaderTexture(0, source.getColorAttachment());
      ShaderProgram shader = RenderSystem.setShader(shaderKey);
      if (shader != null) {
         GlUniform uHalfTexelSize = shader.getUniform("uHalfTexelSize");
         GlUniform uOffset = shader.getUniform("uOffset");
         if (uHalfTexelSize != null) {
            uHalfTexelSize.set(0.5F / source.textureWidth, 0.5F / source.textureHeight);
         }

         if (uOffset != null) {
            uOffset.set(offset);
         }
      }

      Matrix4f identity = new Matrix4f().identity();
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      builder.vertex(identity, 0.0F, 0.0F, 0.0F);
      builder.vertex(identity, 0.0F, mc.getWindow().getScaledHeight(), 0.0F);
      builder.vertex(identity, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0.0F);
      builder.vertex(identity, mc.getWindow().getScaledWidth(), 0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(builder.end());
      destination.endWrite();
   }

   private SimpleFramebuffer f() {
      int w = Math.max(1, mc.getWindow().getFramebufferWidth());
      int h = Math.max(1, mc.getWindow().getFramebufferHeight());
      return new SimpleFramebuffer(w, h, false);
   }

   public void a(float width, float height) {
      if (this.c != null) {
         this.c.set(width, height);
      }
   }

   public void a(Vector4f radius) {
      if (this.d != null) {
         this.d.set(radius.x, radius.z, radius.w, radius.y);
      }
   }

   public void a(float smoothness) {
      if (this.e != null) {
         this.e.set(smoothness);
      }
   }

   public void b(float mix) {
      if (this.f != null) {
         this.f.set(mix);
      }
   }

   public void c(float alpha) {
      if (this.g != null) {
         this.g.set(alpha);
      }
   }

   public void a(float r, float g, float b, float a) {
      if (this.h != null) {
         this.h.set(r, g, b, a);
      }
   }

   public void b(float r, float g, float b, float a) {
      if (this.i != null) {
         this.i.set(r, g, b, a);
      }
   }

   public void c(float r, float g, float b, float a) {
      if (this.j != null) {
         this.j.set(r, g, b, a);
      }
   }

   public void d(float r, float g, float b, float a) {
      if (this.k != null) {
         this.k.set(r, g, b, a);
      }
   }

   public void e(float r, float g, float b, float a) {
      if (this.l != null) {
         this.l.set(r, g, b, a);
      }
   }

   public void d(float r) {
      if (this.m != null) {
         this.m.set(r);
      }
   }
}
