package ru.kirka.fluxclient.feature;

public enum Category {
   COMBAT("Combat", "V"),
   MOVEMENT("Movement", "I"),
   RENDER("Render", "t"),
   HUD("HUD", "q"),
   MISC("Misc", "D");

   private final String displayName;
   private final String icon;

   private Category(String displayName, String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }

   public String a() {
      return this.icon;
   }
}
