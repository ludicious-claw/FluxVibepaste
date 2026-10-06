package ru.kirka.fluxclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import ru.kirka.fluxclient.common.Interface;

public class Draw3DProcessor implements Interface {
   public void drawItem(DrawContext context, ItemStack stack, float x, float y, int z, float alpha, float scale, boolean overlay) {
      if (!stack.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         context.getMatrices().push();
         context.getMatrices().translate(x, y, z);
         context.getMatrices().scale(scale, scale, 1.0F);
         RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
         context.drawItem(stack, 0, 0);
         if (overlay) {
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         context.getMatrices().pop();
         RenderSystem.disableBlend();
      }
   }

   public void a(DrawContext context, ItemStack stack, float x, float y, int z, float alpha, float scale, boolean overlay) {
      this.drawItem(context, stack, x, y, z, alpha, scale, overlay);
   }

   public void a(DrawContext context, Sprite sprite, float x, float y, float z, float scale, float alpha) {
      if (sprite != null) {
         context.getMatrices().push();
         context.getMatrices().translate(x, y, z);
         context.getMatrices().scale(scale, scale, 1.0F);
         context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, 0, 0, 18, 18, ColorUtil.applyAlphaToColor(-1, alpha));
         context.getMatrices().pop();
      }
   }
}
