package ru.kirka.fluxclient.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public class MsdfGlyph {
   private final int unicode;
   private final float atlasLeft;
   private final float atlasRight;
   private final float atlasTop;
   private final float atlasBottom;
   private final float advance;
   private final float planeTop;
   private final float planeWidth;
   private final float planeHeight;

   public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
      this.unicode = data.unicode();
      this.advance = data.advance();
      FontData.BoundsData atlasBounds = data.atlasBounds();
      if (atlasBounds != null) {
         this.atlasLeft = atlasBounds.left() / atlasWidth;
         this.atlasRight = atlasBounds.right() / atlasWidth;
         this.atlasTop = 1.0F - atlasBounds.top() / atlasHeight;
         this.atlasBottom = 1.0F - atlasBounds.bottom() / atlasHeight;
      } else {
         this.atlasLeft = 0.0F;
         this.atlasRight = 0.0F;
         this.atlasTop = 0.0F;
         this.atlasBottom = 0.0F;
      }

      FontData.BoundsData planeBounds = data.planeBounds();
      if (planeBounds != null) {
         this.planeWidth = planeBounds.right() - planeBounds.left();
         this.planeHeight = planeBounds.top() - planeBounds.bottom();
         this.planeTop = planeBounds.top();
      } else {
         this.planeWidth = 0.0F;
         this.planeHeight = 0.0F;
         this.planeTop = 0.0F;
      }
   }

   public float a(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
      float adjustedY = y - this.planeTop * size;
      float scaledWidth = this.planeWidth * size;
      float scaledHeight = this.planeHeight * size;
      consumer.vertex(matrix, x, adjustedY, z).texture(this.atlasLeft, this.atlasTop).color(color);
      consumer.vertex(matrix, x, adjustedY + scaledHeight, z).texture(this.atlasLeft, this.atlasBottom).color(color);
      consumer.vertex(matrix, x + scaledWidth, adjustedY + scaledHeight, z).texture(this.atlasRight, this.atlasBottom).color(color);
      consumer.vertex(matrix, x + scaledWidth, adjustedY, z).texture(this.atlasRight, this.atlasTop).color(color);
      return this.advance * size;
   }

   public float a(float size) {
      return this.advance * size;
   }

   public float b(float size) {
      return this.planeWidth * size;
   }

   public float a() {
      return this.planeTop;
   }

   public float b() {
      return this.planeHeight;
   }

   public int c() {
      return this.unicode;
   }

   public record GlyphInfo(char character, int color) {
   }

   public record a(char a, int b) {
   }
}
