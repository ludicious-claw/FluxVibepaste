package ru.kirka.fluxclient.render.color;

public final class Colors {
   public static final ColorRGBA ACCENT = new ColorRGBA(168.0F, 85.0F, 247.0F, 255.0F);
   public static final ColorRGBA BACKGROUND = new ColorRGBA(18.0F, 18.0F, 24.0F, 230.0F);
   public static final ColorRGBA PANEL = new ColorRGBA(24.0F, 24.0F, 32.0F, 220.0F);
   public static final ColorRGBA TEXT = new ColorRGBA(240.0F, 240.0F, 245.0F, 255.0F);
   public static final ColorRGBA TEXT_MUTED = new ColorRGBA(160.0F, 160.0F, 175.0F, 255.0F);

   private Colors() {
   }

   public static ColorRGBA getAccent() {
      return ACCENT;
   }

   public static ColorRGBA getBackgroundColor() {
      return BACKGROUND;
   }

   public static ColorRGBA getFlatColor() {
      return ACCENT;
   }
}
