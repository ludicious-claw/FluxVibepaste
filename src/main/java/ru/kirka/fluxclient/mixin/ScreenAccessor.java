package ru.kirka.fluxclient.mixin;

import java.util.List;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenAccessor {
   @Accessor("drawables")
   List<Drawable> flux$getDrawables();

   @Accessor("children")
   List<Element> flux$getChildren();

   @Accessor("selectables")
   List<Selectable> flux$getSelectables();
}
