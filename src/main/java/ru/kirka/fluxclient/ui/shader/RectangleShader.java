package ru.kirka.fluxclient.ui.shader;

import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

public class RectangleShader extends Shader {
   public Uniform c;
   public Uniform d;
   public Uniform e;
   public Uniform f;

   public RectangleShader() {
      super(Identifier.of("fluxclient", "core/rect/rect"), VertexFormats.POSITION_COLOR);
   }

   @Override
   protected void b() {
      this.c = this.a("uSize");
      this.d = this.a("uRadius");
      this.e = this.a("uSmoothness");
      this.f = this.a("uOutlineWidth");
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

   public void b(float width) {
      if (this.f != null) {
         this.f.set(width);
      }
   }
}
