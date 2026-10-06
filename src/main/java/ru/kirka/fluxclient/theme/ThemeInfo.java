package ru.kirka.fluxclient.theme;

public enum ThemeInfo {
   PRIMARY(new ThemeConstructor("primary", 235, 45, 60, 255), new ThemeConstructor("primary", 235, 45, 60, 255)),
   BACKGROUND_HUD(new ThemeConstructor("background_hud", 18, 12, 14, 200), new ThemeConstructor("background_hud", 245, 240, 242, 200)),
   BACKGROUND_GUI(new ThemeConstructor("background_gui", 18, 10, 12, 220), new ThemeConstructor("background_gui", 253, 248, 250, 220)),
   OUTLINE_SMALL(new ThemeConstructor("outline_small", 235, 45, 60, 45), new ThemeConstructor("outline_small", 200, 30, 45, 45)),
   OUTLINE_MEDIUM(new ThemeConstructor("outline_medium", 235, 45, 60, 90), new ThemeConstructor("outline_medium", 200, 30, 45, 90)),
   TEXT(new ThemeConstructor("typography_text", 255, 255, 255, 255), new ThemeConstructor("typography_text", 20, 20, 22, 255)),
   TEXT_DISABLED(new ThemeConstructor("typography_disabled", 145, 125, 130, 255), new ThemeConstructor("typography_disabled", 160, 140, 145, 255));

   private final ThemeConstructor dark;
   private final ThemeConstructor light;

   private ThemeInfo(ThemeConstructor dark, ThemeConstructor light) {
      this.dark = dark;
      this.light = light;
   }

   public ThemeConstructor get(ThemeType theme) {
      return theme == ThemeType.LIGHT ? this.light : this.dark;
   }

   public ThemeConstructor a(ThemeType theme) {
      return this.get(theme);
   }

   public ThemeConstructor a() {
      return this.light;
   }
}
