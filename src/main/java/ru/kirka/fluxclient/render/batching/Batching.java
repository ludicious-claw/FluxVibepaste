package ru.kirka.fluxclient.render.batching;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;

public abstract class Batching {
   protected static Batching active;
   protected BufferBuilder builder;

   public Batching(VertexFormat vertexFormat) {
      this.builder = Tessellator.getInstance().begin(DrawMode.QUADS, vertexFormat);
      active = this;
   }

   protected void build() {
      BuiltBuffer builtBuffer = this.builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   public abstract void draw();

   public BufferBuilder getBuilder() {
      return this.builder;
   }

   public static Batching getActive() {
      return active;
   }

   public static void setActive(Batching batching) {
      active = batching;
   }
}
