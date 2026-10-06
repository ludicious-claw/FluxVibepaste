package ru.kirka.fluxclient.feature.impl.render.esp;

public enum EspTargetType {
   PLAYERS("players", "Игроки", true),
   MOBS("mobs", "Мобы", false),
   ANIMALS("animals", "Животные", false),
   ITEMS("items", "Предметы", true);

   private final String id;
   private final String title;
   private final boolean hasSubTargets;

   private EspTargetType(String id, String title, boolean hasSubTargets) {
      this.id = id;
      this.title = title;
      this.hasSubTargets = hasSubTargets;
   }

   public String id() {
      return this.id;
   }

   public String title() {
      return this.title;
   }

   public boolean hasSubTargets() {
      return this.hasSubTargets;
   }
}
