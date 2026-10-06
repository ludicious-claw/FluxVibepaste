package ru.kirka.fluxclient.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Setting<T> {
   private final String name;
   private final String description;
   protected T value;
   private Consumer<T> changeListener;
   private Supplier<Boolean> visible = () -> true;

   public Setting(String name, String description, T defaultValue) {
      this.name = name;
      this.description = description;
      this.value = defaultValue;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public T get() {
      return this.value;
   }

   public void set(T value) {
      this.value = value;
      if (this.changeListener != null) {
         this.changeListener.accept(value);
      }
   }

   public Setting<T> onChange(Consumer<T> listener) {
      this.changeListener = listener;
      return this;
   }

   public Setting<T> visibleWhen(Supplier<Boolean> condition) {
      this.visible = condition != null ? condition : () -> true;
      return this;
   }

   public boolean isVisible() {
      try {
         return this.visible.get();
      } catch (Exception var2) {
         return true;
      }
   }
}
