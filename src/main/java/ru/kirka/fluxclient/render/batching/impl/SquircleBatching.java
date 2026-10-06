package ru.kirka.fluxclient.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.shader.GlProgram;

public class SquircleBatching extends Batching {
   private final GlProgram squircleProgram;
   private final float smoothness = 0.5F;
   private final float squirt;

   public SquircleBatching(float squirt) {
      super(VertexFormats.POSITION_COLOR);
      this.squircleProgram = DrawUtility.getSquircleProgram();
      this.squirt = squirt;
   }

   @Override
   public void draw() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      if (this.squircleProgram != null) {
         this.squircleProgram.use();
         if (this.squircleProgram.findUniform("Smoothness") != null) {
            this.squircleProgram.findUniform("Smoothness").set(0.5F);
         }

         if (this.squircleProgram.findUniform("CornerSmoothness") != null) {
            this.squircleProgram.findUniform("CornerSmoothness").set(this.squirt);
         }
      }

      BuiltBuffer built = this.getBuilder().endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      if (active == this) {
         active = null;
      }
   }

   public void add(Matrix4f matrix, float x, float y, float width, float height, float radiusTL, float radiusBL, float radiusTR, float radiusBR, int rgba) {
      if (this.squircleProgram != null) {
         if (this.squircleProgram.findUniform("Size") != null) {
            this.squircleProgram.findUniform("Size").set(width, height);
         }

         if (this.squircleProgram.findUniform("Radius") != null) {
            this.squircleProgram.findUniform("Radius").set(radiusTL, radiusBL, radiusTR, radiusBR);
         }
      }

      float horizontalPadding = 0.75F;
      float verticalPadding = 0.75F;
      float ax = x - horizontalPadding / 2.0F;
      float ay = y - verticalPadding / 2.0F;
      float aw = width + horizontalPadding;
      float ah = height + verticalPadding;
      this.getBuilder().vertex(matrix, ax, ay, 0.0F).color(rgba);
      this.getBuilder().vertex(matrix, ax, ay + ah, 0.0F).color(rgba);
      this.getBuilder().vertex(matrix, ax + aw, ay + ah, 0.0F).color(rgba);
      this.getBuilder().vertex(matrix, ax + aw, ay, 0.0F).color(rgba);
   }
}
