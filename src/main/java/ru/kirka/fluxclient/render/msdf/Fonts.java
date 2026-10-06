package ru.kirka.fluxclient.render.msdf;

public final class Fonts {
   public static MsdfFont BOLD;
   public static MsdfFont MEDIUM;
   public static MsdfFont REGULAR;
   public static MsdfFont SEMIBOLD;
   public static MsdfFont ROUND_BOLD;

   public static void init() {
      BOLD = MsdfFont.builder().atlas("bold").data("bold").build();
      MEDIUM = MsdfFont.builder().atlas("medium").data("medium").build();
      REGULAR = MsdfFont.builder().atlas("regular").data("regular").build();
      SEMIBOLD = MsdfFont.builder().atlas("semibold").data("semibold").build();
      ROUND_BOLD = MsdfFont.builder().atlas("roundbold").data("roundbold").build();
   }

   private Fonts() {
      throw new UnsupportedOperationException();
   }
}
