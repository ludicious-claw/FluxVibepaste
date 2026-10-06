package ru.kirka.fluxclient.render;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class FontData {
   private FontData.AtlasData atlas;
   private FontData.MetricsData metrics;
   private List<FontData.GlyphData> glyphs;
   @SerializedName("kerning")
   private List<FontData.KerningData> kernings;

   public FontData.AtlasData atlas() {
      return this.atlas;
   }

   public FontData.MetricsData metrics() {
      return this.metrics;
   }

   public List<FontData.GlyphData> glyphs() {
      return this.glyphs;
   }

   public List<FontData.KerningData> kernings() {
      return this.kernings;
   }

   public record AtlasData(@SerializedName("distanceRange") float range, float width, float height) {
   }

   public record BoundsData(float left, float top, float right, float bottom) {
   }

   public record GlyphData(int unicode, float advance, FontData.BoundsData planeBounds, FontData.BoundsData atlasBounds) {
   }

   public record KerningData(@SerializedName("unicode1") int leftChar, @SerializedName("unicode2") int rightChar, float advance) {
   }

   public static final class MetricsData {
      private float lineHeight;
      private float ascender;
      private float descender;

      public float lineHeight() {
         return this.lineHeight;
      }

      public float ascender() {
         return this.ascender;
      }

      public float descender() {
         return this.descender;
      }

      public float baselineHeight() {
         return this.lineHeight + this.descender;
      }
   }
}
