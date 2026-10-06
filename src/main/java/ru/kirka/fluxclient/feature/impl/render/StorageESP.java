package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.event.impl.WorldChangeEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.Draw3DUtility;
import ru.kirka.fluxclient.render.draw.RenderUtility;
import ru.kirka.fluxclient.util.WorldUtility;

public class StorageESP extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Box FULL_BOX = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private static final Box EMPTY_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   public final BooleanSetting chests = new BooleanSetting("Сундуки", true);
   public final BooleanSetting enderChests = new BooleanSetting("Эндер-сундуки", true);
   public final BooleanSetting trappedChests = new BooleanSetting("Сундуки-ловушки", true);
   public final BooleanSetting barrels = new BooleanSetting("Бочки", true);
   public final BooleanSetting shulkers = new BooleanSetting("Шалкеры", true);
   public final BooleanSetting furnaces = new BooleanSetting("Печи", false);
   public final BooleanSetting hoppers = new BooleanSetting("Воронки", false);
   public final BooleanSetting droppers = new BooleanSetting("Дропперы/Раздатчики", false);
   public final BooleanSetting minecart = new BooleanSetting("Вагонетки", true);
   public final BooleanSetting fill = new BooleanSetting("Заливка", true);
   public final BooleanSetting outline = new BooleanSetting("Обводка", true);
   public final BooleanSetting diagonals = new BooleanSetting("Диагонали", true);
   public final BooleanSetting lines = new BooleanSetting("Трассеры", false);
   public final NumberSetting maxDistance = new NumberSetting("Дистанция", "Максимальная дистанция подсветки", 128.0F, 5.0F, 128.0F, 1.0F);
   private final EventListener<WorldChangeEvent> onWorldChange = event -> WorldUtility.blockEntities.clear();
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrices = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         if (this.fill.get()) {
            BufferBuilder quadsBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (BlockEntity be : WorldUtility.blockEntities) {
               if (this.isValidEntity(be)) {
                  for (Box b : this.getBoundingBox(be)) {
                     Draw3DUtility.renderFilledBox(
                        matrices, quadsBuffer, b.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z), this.getBlockColor(be).withAlpha(50.0F)
                     );
                  }
               }
            }

            for (Entity entity : mc.world.getEntities()) {
               if (this.isValidCart(entity)) {
                  Draw3DUtility.renderFilledBox(
                     matrices,
                     quadsBuffer,
                     entity.getBoundingBox().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z),
                     this.getEntityColor(entity).withAlpha(50.0F)
                  );
               }
            }

            RenderUtility.buildBuffer(quadsBuffer);
         }

         if (this.outline.get() || this.diagonals.get() || this.lines.get()) {
            BufferBuilder linesBuffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (BlockEntity bex : WorldUtility.blockEntities) {
               if (this.isValidEntity(bex)) {
                  for (Box b : this.getBoundingBox(bex)) {
                     Box relBox = b.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                     if (this.diagonals.get()) {
                        Draw3DUtility.renderBoxInternalDiagonals(matrices, linesBuffer, relBox, this.getBlockColor(bex).withAlpha(100.0F));
                     }

                     if (this.outline.get()) {
                        Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, relBox, this.getBlockColor(bex).withAlpha(100.0F));
                     }

                     if (this.lines.get()) {
                        Draw3DUtility.renderLineFromPlayer(matrices, linesBuffer, bex.getPos().toCenterPos(), this.getBlockColor(bex));
                     }
                  }
               }
            }

            for (Entity entityx : mc.world.getEntities()) {
               if (this.isValidCart(entityx)) {
                  Box relBoxx = entityx.getBoundingBox().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                  if (this.diagonals.get()) {
                     Draw3DUtility.renderBoxInternalDiagonals(matrices, linesBuffer, relBoxx, this.getEntityColor(entityx).withAlpha(100.0F));
                  }

                  if (this.outline.get()) {
                     Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, relBoxx, this.getEntityColor(entityx).withAlpha(100.0F));
                  }

                  if (this.lines.get()) {
                     Draw3DUtility.renderLineFromPlayer(matrices, linesBuffer, entityx.getPos(), this.getEntityColor(entityx));
                  }
               }
            }

            RenderUtility.buildBuffer(linesBuffer);
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   };

   public StorageESP() {
      super("StorageESP", "Подсветка хранилищ (сундуки, бочки, шалкеры, воронки)", Category.RENDER, -1);
      this.registerSetting(this.chests);
      this.registerSetting(this.enderChests);
      this.registerSetting(this.trappedChests);
      this.registerSetting(this.barrels);
      this.registerSetting(this.shulkers);
      this.registerSetting(this.furnaces);
      this.registerSetting(this.hoppers);
      this.registerSetting(this.droppers);
      this.registerSetting(this.minecart);
      this.registerSetting(this.fill);
      this.registerSetting(this.outline);
      this.registerSetting(this.diagonals);
      this.registerSetting(this.lines);
      this.registerSetting(this.maxDistance);
   }

   private List<Box> getBoundingBox(BlockEntity blockEntity) {
      if (mc.world == null) {
         return List.of(EMPTY_BOX);
      } else {
         BlockPos pos = blockEntity.getPos();
         BlockState state = mc.world.getBlockState(pos);
         VoxelShape shape = state.getOutlineShape(mc.world, pos);
         return shape.isEmpty() ? List.of(FULL_BOX.offset(pos)) : shape.getBoundingBoxes().stream().map(box -> box.offset(pos)).toList();
      }
   }

   private boolean isValidEntity(BlockEntity entity) {
      double maxDistSq = this.maxDistance.get() * this.maxDistance.get();
      if (mc.player == null || mc.player.squaredDistanceTo(entity.getPos().toCenterPos()) > maxDistSq) {
         return false;
      } else if (entity instanceof ChestBlockEntity && !(entity instanceof TrappedChestBlockEntity)) {
         return this.chests.get();
      } else if (entity instanceof EnderChestBlockEntity) {
         return this.enderChests.get();
      } else if (entity instanceof TrappedChestBlockEntity) {
         return this.trappedChests.get();
      } else if (entity instanceof BarrelBlockEntity) {
         return this.barrels.get();
      } else if (entity instanceof ShulkerBoxBlockEntity) {
         return this.shulkers.get();
      } else if (entity instanceof AbstractFurnaceBlockEntity) {
         return this.furnaces.get();
      } else if (entity instanceof HopperBlockEntity) {
         return this.hoppers.get();
      } else {
         return !(entity instanceof DropperBlockEntity) && !(entity instanceof DispenserBlockEntity) ? false : this.droppers.get();
      }
   }

   private ColorRGBA getBlockColor(BlockEntity entity) {
      if (entity instanceof ChestBlockEntity && !(entity instanceof TrappedChestBlockEntity)) {
         return new ColorRGBA(255.0F, 131.0F, 54.0F);
      } else if (entity instanceof EnderChestBlockEntity) {
         return new ColorRGBA(121.0F, 54.0F, 255.0F);
      } else if (entity instanceof TrappedChestBlockEntity) {
         return new ColorRGBA(255.0F, 101.0F, 54.0F);
      } else if (entity instanceof AbstractFurnaceBlockEntity) {
         return new ColorRGBA(126.0F, 126.0F, 126.0F);
      } else if (entity instanceof BarrelBlockEntity) {
         return new ColorRGBA(255.0F, 185.0F, 54.0F);
      } else if (entity instanceof ShulkerBoxBlockEntity) {
         return new ColorRGBA(181.0F, 54.0F, 255.0F);
      } else {
         return entity instanceof HopperBlockEntity ? new ColorRGBA(100.0F, 100.0F, 100.0F) : ColorRGBA.WHITE;
      }
   }

   private boolean isValidCart(Entity entity) {
      double maxDistSq = this.maxDistance.get() * this.maxDistance.get();
      return mc.player != null && mc.player.squaredDistanceTo(entity.getPos()) <= maxDistSq && entity instanceof ChestMinecartEntity && this.minecart.get();
   }

   private ColorRGBA getEntityColor(Entity entity) {
      return entity instanceof ChestMinecartEntity ? new ColorRGBA(255.0F, 200.0F, 100.0F) : ColorRGBA.WHITE;
   }
}
