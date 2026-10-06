package ru.kirka.fluxclient.feature.impl.render.esp;

public enum EspItemType {
   HELD("held", "В руках"),
   DROPPED("dropped", "Дроп");

   private final String id;
   private final String title;

   private EspItemType(String id, String title) {
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
