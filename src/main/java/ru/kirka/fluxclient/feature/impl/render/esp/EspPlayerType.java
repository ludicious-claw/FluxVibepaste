package ru.kirka.fluxclient.feature.impl.render.esp;

public enum EspPlayerType {
   OTHERS("others", "Другие"),
   LOCAL("local", "Локальный"),
   FRIENDS("friends", "Друзья");

   private final String id;
   private final String title;

   private EspPlayerType(String id, String title) {
      this.id = id;
      this.title = title;
   }

   public String id() {
      return this.id;
   }

   public String title() {
      return this.title;
   }
}
