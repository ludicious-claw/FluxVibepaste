package ru.kirka.fluxclient.render.batching.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import ru.kirka.fluxclient.render.batching.Batching;
import ru.kirka.fluxclient.render.draw.DrawUtility;

public class IconBatching extends Batching {
   private final MatrixStack matrices;

   public IconBatching(VertexFormat vertexFormat, MatrixStack matrices) {
      super(vertexFormat);
      this.matrices = matrices;
   }

   @Override
   public void draw() {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.enableBlend();
      this.build();
      DrawUtility.drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      if (active == this) {
         active = null;
      }
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
