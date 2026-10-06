package ru.kirka.fluxclient.render.batching.impl;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.shader.GlProgram;

public class ShadowBatching extends Batching {
   private final MatrixStack matrices;
   private final float width;
   private final float height;
   private final float softness;
   private final BorderRadius borderRadius;

   public ShadowBatching(VertexFormat vertexFormat, MatrixStack matrices, float width, float height, float softness, BorderRadius borderRadius) {
      super(vertexFormat);
      this.matrices = matrices;
      this.width = width;
      this.height = height;
      this.softness = softness;
      this.borderRadius = borderRadius;
   }

   @Override
   public void draw() {
      GlProgram rectangleProgram = DrawUtility.rectangleProgram;
      if (rectangleProgram != null) {
         rectangleProgram.use();
         if (rectangleProgram.findUniform("Size") != null) {
            rectangleProgram.findUniform("Size").set(this.width, this.height);
         }

         if (rectangleProgram.findUniform("Radius") != null && this.borderRadius != null) {
            rectangleProgram.findUniform("Radius")
               .set(
                  this.borderRadius.topLeftRadius() * 3.0F,
                  this.borderRadius.bottomLeftRadius() * 3.0F,
                  this.borderRadius.topRightRadius() * 3.0F,
                  this.borderRadius.bottomRightRadius() * 3.0F
               );
         }

         if (rectangleProgram.findUniform("Smoothness") != null) {
            rectangleProgram.findUniform("Smoothness").set(this.softness);
         }
      }

      DrawUtility.drawSetup();
      this.build();
      DrawUtility.drawEnd();
      if (active == this) {
         active = null;
      }
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
