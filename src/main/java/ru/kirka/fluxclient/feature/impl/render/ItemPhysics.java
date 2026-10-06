package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.FluxWorldRender;

public class ItemPhysics extends Module {
   public final BooleanSetting layOnGround = new BooleanSetting("Лежать на земле", "Предметы реалистично лежат на поверхности", true);
   public final BooleanSetting glow = new BooleanSetting("Неоновое свечение", "Световой ореол вокруг предметов", true);
   public final BooleanSetting rareBeams = new BooleanSetting("Лучи для редких", "Вертикальные световые маяки над ценными вещами", true);
   public final BooleanSetting onlyValuable = new BooleanSetting("Только ценные", "Подсвечивать только алмазы, незерит, тотемы", false);
   public final NumberSetting glowSize = new NumberSetting("Размер ореола", "Радиус свечения предмета", 0.6F, 0.2F, 1.5F, 0.05F);
   public final ColorSetting glowColor = new ColorSetting("Цвет свечения", "Цвет неонового ореола", new Color(168, 85, 247, 230));

   public ItemPhysics() {
      super("ItemPhysics", "Физика и неоновое свечение лежащих предметов и световые лучи", Category.RENDER, -1);
      this.registerSetting(this.layOnGround);
      this.registerSetting(this.glow);
      this.registerSetting(this.rareBeams);
      this.registerSetting(this.onlyValuable);
      this.registerSetting(this.glowSize);
      this.registerSetting(this.glowColor);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.world != null && mc.player != null) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemEntity itemEntity) {
               ItemStack stack = itemEntity.getStack();
               if (!stack.isEmpty()) {
                  boolean isValuable = this.isItemValuable(stack);
                  if (!this.onlyValuable.get() || isValuable) {
                     double x = MathHelper.lerp(tickDelta, itemEntity.prevX, itemEntity.getX());
                     double y = MathHelper.lerp(tickDelta, itemEntity.prevY, itemEntity.getY());
                     double z = MathHelper.lerp(tickDelta, itemEntity.prevZ, itemEntity.getZ());
                     Vec3d itemPos = new Vec3d(x, y + 0.1, z);
                     Color col = isValuable ? this.getValuableColor(stack) : this.glowColor.get();
                     if (this.glow.get()) {
                        FluxWorldRender.drawBillboardGlow(matrices, cameraPos, camRot, itemPos, this.glowSize.get(), col.getRGB());
                     }

                     if (this.rareBeams.get() && isValuable) {
                        this.renderVerticalBeam(matrices, itemPos.subtract(cameraPos), col);
                     }
                  }
               }
            }
         }
      }
   }

   private void renderVerticalBeam(MatrixStack matrices, Vec3d renderPos, Color col) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Tessellator tessellator = Tessellator.getInstance();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float r = 0.12F;
      float h = 4.0F;
      int cr = col.getRed();
      int cg = col.getGreen();
      int cb = col.getBlue();
      BufferBuilder b = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      int segments = 8;

      for (int i = 0; i <= segments; i++) {
         double angle = i * 2.0 * Math.PI / segments;
         float cos = (float)(Math.cos(angle) * r);
         float sin = (float)(Math.sin(angle) * r);
         float bx = (float)renderPos.x + cos;
         float by = (float)renderPos.y;
         float bz = (float)renderPos.z + sin;
         b.vertex(m, bx, by, bz).color(cr, cg, cb, 200);
         b.vertex(m, bx, by + h, bz).color(cr, cg, cb, 0);
      }

      BufferRenderer.drawWithGlobalProgram(b.end());
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private boolean isItemValuable(ItemStack stack) {
      return stack.isOf(Items.DIAMOND)
         || stack.isOf(Items.DIAMOND_BLOCK)
         || stack.isOf(Items.NETHERITE_INGOT)
         || stack.isOf(Items.NETHERITE_BLOCK)
         || stack.isOf(Items.NETHERITE_SWORD)
         || stack.isOf(Items.NETHERITE_HELMET)
         || stack.isOf(Items.NETHERITE_CHESTPLATE)
         || stack.isOf(Items.NETHERITE_LEGGINGS)
         || stack.isOf(Items.NETHERITE_BOOTS)
         || stack.isOf(Items.TOTEM_OF_UNDYING)
         || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
         || stack.hasEnchantments();
   }

   private Color getValuableColor(ItemStack stack) {
      if (stack.isOf(Items.TOTEM_OF_UNDYING) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
         return new Color(245, 158, 11, 240);
      } else {
         return !stack.isOf(Items.NETHERITE_INGOT)
               && !stack.isOf(Items.NETHERITE_BLOCK)
               && !stack.isOf(Items.NETHERITE_SWORD)
               && !stack.isOf(Items.NETHERITE_CHESTPLATE)
            ? new Color(6, 182, 212, 240)
            : new Color(147, 51, 234, 240);
      }
   }
}
