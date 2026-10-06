package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.Render3DUtil;
import ru.kirka.fluxclient.render.shaders.OrbitShader;
import ru.kirka.fluxclient.render.shaders.OrbitShaders;

public class BlockESP extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Стиль отрисовки граней", "Шейдер", "Шейдер", "Неон", "Обводка", "Заливка", "Градиент");
   public final ModeSetting shaderType = new ModeSetting("Шейдер", "GLSL шейдер граней блока", "Небула", "Небула", "Плазма", "Звёзды", "Паутина");
   public final NumberSetting range = new NumberSetting("Радиус", "Дистанция поиска блоков", 32.0F, 8.0F, 64.0F, 4.0F);
   public final BooleanSetting chests = new BooleanSetting("Сундуки", "Подсвечивать обычные сундуки", true);
   public final BooleanSetting enderChests = new BooleanSetting("Эндер-сундуки", "Подсвечивать сундуки Края", true);
   public final BooleanSetting shulkers = new BooleanSetting("Шалкеры", "Подсвечивать шалкеровые ящики", true);
   public final BooleanSetting spawners = new BooleanSetting("Спавнеры", "Подсвечивать спавнеры мобов", true);
   public final BooleanSetting ores = new BooleanSetting("Ценные руды", "Алмазная руда и обломки незерита", true);
   public final BooleanSetting crosshairBlock = new BooleanSetting("Блок в прицеле", "Подсвечивать блок под курсором", true);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Рентген сквозь блоки мира", true);
   public final NumberSetting lineWidth = new NumberSetting("Толщина линий", "Толщина обводки граней", 2.0F, 0.5F, 5.0F, 0.5F);
   public final NumberSetting fillAlpha = new NumberSetting("Прозрачность", "Прозрачность заливки/шейдера", 0.4F, 0.05F, 1.0F, 0.05F);
   public final ColorSetting chestColor = new ColorSetting("Цвет сундука", "Цвет обычных сундуков", new Color(245, 158, 11, 240));
   public final ColorSetting enderChestColor = new ColorSetting("Цвет эндер-сундука", "Цвет сундуков Края", new Color(147, 51, 234, 240));
   public final ColorSetting shulkerColor = new ColorSetting("Цвет шалкера", "Цвет шалкеров", new Color(236, 72, 153, 240));
   public final ColorSetting spawnerColor = new ColorSetting("Цвет спавнера", "Цвет спавнеров", new Color(239, 68, 68, 240));
   public final ColorSetting oreColor = new ColorSetting("Цвет руды", "Цвет алмазов и древних обломков", new Color(6, 182, 212, 240));
   public final ColorSetting crosshairColor = new ColorSetting("Цвет прицела", "Цвет блока под прицелом", new Color(255, 255, 255, 220));
   private final List<BlockESP.BlockTarget> cachedBlocks = new ArrayList<>();
   private long lastScanTime = 0L;

   public BlockESP() {
      super("BlockESP", "Подсветка ценных блоков и сундуков с кастомными шейдерами", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.shaderType);
      this.registerSetting(this.range);
      this.registerSetting(this.chests);
      this.registerSetting(this.enderChests);
      this.registerSetting(this.shulkers);
      this.registerSetting(this.spawners);
      this.registerSetting(this.ores);
      this.registerSetting(this.crosshairBlock);
      this.registerSetting(this.throughWalls);
      this.registerSetting(this.lineWidth);
      this.registerSetting(this.fillAlpha);
      this.registerSetting(this.chestColor);
      this.registerSetting(this.enderChestColor);
      this.registerSetting(this.shulkerColor);
      this.registerSetting(this.spawnerColor);
      this.registerSetting(this.oreColor);
      this.registerSetting(this.crosshairColor);
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         long now = System.currentTimeMillis();
         if (now - this.lastScanTime >= 350L) {
            this.lastScanTime = now;
            List<BlockESP.BlockTarget> found = new ArrayList<>();
            Vec3d pPos = mc.player.getPos();
            double rSq = this.range.get() * this.range.get();
            int chunkR = (int)(this.range.get() / 16.0F) + 1;
            int pChunkX = mc.player.getChunkPos().x;
            int pChunkZ = mc.player.getChunkPos().z;

            for (int cx = pChunkX - chunkR; cx <= pChunkX + chunkR; cx++) {
               for (int cz = pChunkZ - chunkR; cz <= pChunkZ + chunkR; cz++) {
                  WorldChunk chunk = mc.world.getChunk(cx, cz);
                  if (chunk != null) {
                     for (BlockEntity be : chunk.getBlockEntities().values()) {
                        BlockPos pos = be.getPos();
                        if (!(pPos.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rSq)) {
                           if (this.chests.get() && be instanceof ChestBlockEntity) {
                              found.add(new BlockESP.BlockTarget(this.getPreciseBox(pos), this.chestColor.get()));
                           } else if (this.enderChests.get() && be instanceof EnderChestBlockEntity) {
                              found.add(new BlockESP.BlockTarget(this.getPreciseBox(pos), this.enderChestColor.get()));
                           } else if (this.shulkers.get() && be instanceof ShulkerBoxBlockEntity) {
                              found.add(new BlockESP.BlockTarget(this.getPreciseBox(pos), this.shulkerColor.get()));
                           } else if (this.spawners.get() && be instanceof MobSpawnerBlockEntity) {
                              found.add(new BlockESP.BlockTarget(this.getPreciseBox(pos), this.spawnerColor.get()));
                           }
                        }
                     }
                  }
               }
            }

            if (this.ores.get()) {
               int scanR = Math.min((int)this.range.get().floatValue(), 16);
               BlockPos playerBlock = mc.player.getBlockPos();

               for (int dx = -scanR; dx <= scanR; dx += 2) {
                  for (int dy = -scanR; dy <= scanR; dy += 2) {
                     for (int dz = -scanR; dz <= scanR; dz += 2) {
                        BlockPos checkPos = playerBlock.add(dx, dy, dz);
                        BlockState state = mc.world.getBlockState(checkPos);
                        if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE) || state.isOf(Blocks.ANCIENT_DEBRIS)) {
                           found.add(new BlockESP.BlockTarget(new Box(checkPos), this.oreColor.get()));
                        }
                     }
                  }
               }
            }

            synchronized (this.cachedBlocks) {
               this.cachedBlocks.clear();
               this.cachedBlocks.addAll(found);
            }
         }
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos, Quaternionf camRot) {
      if (mc.world != null && mc.player != null) {
         List<BlockESP.BlockTarget> toRender;
         synchronized (this.cachedBlocks) {
            toRender = new ArrayList<>(this.cachedBlocks);
         }

         if (this.crosshairBlock.get() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult)mc.crosshairTarget;
            BlockPos bpos = bhr.getBlockPos();
            if (bpos != null && mc.world.getBlockState(bpos) != null && !mc.world.getBlockState(bpos).isAir()) {
               Box box = this.getPreciseBox(bpos);
               boolean exists = false;

               for (BlockESP.BlockTarget bt : toRender) {
                  if (Math.abs(bt.box.minX - box.minX) < 0.1 && Math.abs(bt.box.minY - box.minY) < 0.1 && Math.abs(bt.box.minZ - box.minZ) < 0.1) {
                     exists = true;
                     break;
                  }
               }

               if (!exists) {
                  toRender.add(new BlockESP.BlockTarget(box, this.crosshairColor.get()));
               }
            }
         }

         String m = this.mode.get();
         boolean wall = this.throughWalls.get();
         float width = this.lineWidth.get();
         float a = this.fillAlpha.get();
         OrbitShader shader = null;
         if (m.equals("Шейдер")) {
            String bt = this.shaderType.get();

            String shaderKey = switch (bt) {
               case "Плазма" -> "block_plasma";
               case "Звёзды" -> "block_starfield";
               case "Паутина" -> "block_cobweb";
               default -> "block_nebula";
            };
            Optional<OrbitShader> opt = OrbitShaders.get(shaderKey);
            if (opt.isPresent()) {
               shader = opt.get();
            }
         }

         for (BlockESP.BlockTarget btx : toRender) {
            if (!btx.box.contains(cameraPos.x, cameraPos.y, cameraPos.z)) {
               Box renderBox = btx.box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
               Color col = btx.color;
               Color fillCol = new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(a * 255.0F));
               switch (m) {
                  case "Шейдер":
                     if (shader != null) {
                        Render3DUtil.drawBoxShaded(matrices, renderBox, shader, col, a, wall);
                     }

                     Render3DUtil.drawBoxOutline(matrices, renderBox, col, width, wall);
                     break;
                  case "Неон":
                     Render3DUtil.drawBox(matrices, renderBox, col, fillCol, width, wall);
                     break;
                  case "Обводка":
                     Render3DUtil.drawBoxOutline(matrices, renderBox, col, width, wall);
                     break;
                  case "Заливка":
                     Render3DUtil.drawBoxFill(matrices, renderBox, fillCol, wall);
                     break;
                  case "Градиент":
                     float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.004) * 0.3 + 0.7);
                     Color pulseCol = new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(a * pulse * 255.0F));
                     Render3DUtil.drawBox(matrices, renderBox, col, pulseCol, width, wall);
               }
            }
         }
      }
   }

   private Box getPreciseBox(BlockPos pos) {
      if (mc.world == null) {
         return new Box(pos);
      } else {
         BlockState state = mc.world.getBlockState(pos);
         VoxelShape shape = state.getOutlineShape(mc.world, pos);
         return shape != null && !shape.isEmpty() ? shape.getBoundingBox().offset(pos) : new Box(pos);
      }
   }

   private record BlockTarget(Box box, Color color) {
   }
}
