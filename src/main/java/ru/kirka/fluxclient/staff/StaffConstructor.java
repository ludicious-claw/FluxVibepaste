package ru.kirka.fluxclient.staff;

import ru.kirka.fluxclient.render.AnimationUtil;

public class StaffConstructor {
   private final AnimationUtil animation = new AnimationUtil();
   private String name;

   public StaffConstructor() {
   }

   public StaffConstructor(String name) {
      this.name = name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void a(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public String a() {
      return this.name;
   }

   public AnimationUtil getAnimation() {
      return this.animation;
   }

   public AnimationUtil b() {
      return this.animation;
   }
}
