package ru.kirka.fluxclient.theme;

import java.util.ArrayList;
import java.util.List;
import ru.kirka.fluxclient.common.Interface;

public class ThemeProcessor implements Interface {
   private ThemeType currentTheme = ThemeType.DARK;
   private final List<ThemeConstructor> constructors = new ArrayList<>();

   public ThemeProcessor() {
      this.initDefaults();
   }

   public void initDefaults() {
      this.constructors.clear();

      for (ThemeInfo info : ThemeInfo.values()) {
         ThemeConstructor base = info.get(this.currentTheme);
         this.constructors.add(new ThemeConstructor(base.getName(), base.getRed(), base.getGreen(), base.getBlue(), base.getAlpha()));
      }
   }

   public ThemeType getTheme() {
      return this.currentTheme;
   }

   public ThemeType a() {
      return this.getTheme();
   }

   public void setTheme(ThemeType theme) {
      this.currentTheme = theme;
      this.initDefaults();
   }

   public void a(ThemeType theme) {
      this.setTheme(theme);
   }

   public void setPrimaryColor(int r, int g, int b) {
      for (ThemeConstructor tc : this.constructors) {
         if (tc.getName().equalsIgnoreCase("primary") || tc.getName().toLowerCase().contains("outline")) {
            tc.setRed(r);
            tc.setGreen(g);
            tc.setBlue(b);
         }
      }
   }

   public ThemeConstructor getConstructor(ThemeInfo info) {
      for (ThemeConstructor tc : this.constructors) {
         if (tc.getName().equalsIgnoreCase(info.get(this.currentTheme).getName())) {
            return tc;
         }
      }

      return info.get(this.currentTheme);
   }

   public ThemeConstructor a(ThemeInfo info) {
      return this.getConstructor(info);
   }

   public List<ThemeConstructor> getConstructors() {
      return this.constructors;
   }
}
