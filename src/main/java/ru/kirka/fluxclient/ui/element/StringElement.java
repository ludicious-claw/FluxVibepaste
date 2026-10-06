package ru.kirka.fluxclient.ui.element;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import ru.kirka.fluxclient.config.impl.TextSetting;

public class StringElement extends Element<TextSetting> {
   private TextField textField;

   public StringElement(TextSetting setting) {
      super(setting);
      this.a.w = this.getDefaultHeight();
   }

   @Override
   public float getDefaultHeight() {
      return 12.0F;
   }

   @Override
   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      TextField field = this.getField();
      if (field != null) {
         field.onMouseClick(mouseX, mouseY, button);
         return field.isFocused();
      } else {
         return false;
      }
   }

   @Override
   public boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.textField == null) {
         return false;
      } else {
         this.textField.onMouseDrag(mouseX, mouseY, button);
         return this.textField.isFocused();
      }
   }

   @Override
   public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
      if (this.textField != null && this.textField.isFocused()) {
         this.textField.a(keyCode, scanCode, modifiers);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onCharTyped(char chr, int modifiers) {
      if (this.textField != null && this.textField.isFocused()) {
         this.textField.a(chr, modifiers);
         return true;
      } else {
         return false;
      }
   }

   private TextField getField() {
      if (this.textField == null) {
         this.textField = new TextField(TextField.type.GUI_SETTING, false);
         this.textField.setPlaceholder(this.b.getName());
         this.textField.getTextBuffer().append(this.b.get() != null ? this.b.get() : "");
      }

      return this.textField;
   }

   @Override
   public void render(DrawContext context, double mouseX, double mouseY, float delta, float extend) {
      TextField field = this.getField();
      field.setSize(new Vector2f(this.a.z, this.a.w));
      field.setPosition(new Vector2f(this.a.x, this.a.y));
      field.render(context, mouseX, mouseY, delta, extend);
      this.b.set(field.getTextBuffer().toString());
   }
}
