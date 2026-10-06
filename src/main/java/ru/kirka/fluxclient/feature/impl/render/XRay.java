package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.WorldChunk;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.Render3DEvent;
import ru.kirka.fluxclient.event.impl.WorldChangeEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.draw.Draw3DUtility;

public class XRay extends Module {
   public final BooleanSetting diamondOre = new BooleanSetting("Алмазы", true);
   public final BooleanSetting ancientDebris = new BooleanSetting("Обломки", true);
   public final BooleanSetting goldOre = new BooleanSetting("Золото", true);
   public final BooleanSetting ironOre = new BooleanSetting("Железо", false);
   public final BooleanSetting lapisOre = new BooleanSetting("Лазурит", false);
   public final BooleanSetting emeraldOre = new BooleanSetting("Изумруды", true);
   public final BooleanSetting redstoneOre = new BooleanSetting("Редстоун", false);
   public final BooleanSetting fill = new BooleanSetting("Заливка", true);
   public final BooleanSetting outline = new BooleanSetting("Обводка", true);
   public final NumberSetting maxDistance = new NumberSetting("Дистанция", "Максимальная дистанция подсветки", 48.0F, 16.0F, 96.0F, 4.0F);
   private final Set<BlockPos> cachedBlocks = ConcurrentHashMap.newKeySet();
   private final EventListener<WorldChangeEvent> onWorldChange = event -> this.cachedBlocks.clear();
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world != null && mc.player != null && mc.gameRenderer != null && !this.cachedBlocks.isEmpty()) {
         MatrixStack matrices = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         matrices.push();
         matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         float maxDistSq = this.maxDistance.get() * this.maxDistance.get();
         if (this.fill.get()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder fillBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (BlockPos pos : this.cachedBlocks) {
               if (!(mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq)) {
                  BlockState state = mc.world.getBlockState(pos);
                  if (!state.isAir() && this.isBlockEnabled(state.getBlock())) {
                     ColorRGBA color = this.getBlockColor(state.getBlock());
                     Box box = new Box(pos);
                     Draw3DUtility.renderFilledBox(matrices, fillBuffer, box, color.withAlpha(50.0F));
                  }
               }
            }

            BuiltBuffer builtFill = fillBuffer.endNullable();
            if (builtFill != null) {
               BufferRenderer.drawWithGlobalProgram(builtFill);
            }
         }

         if (this.outline.get()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder lineBuffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);

            for (BlockPos posx : this.cachedBlocks) {
               if (!(mc.player.squaredDistanceTo(posx.toCenterPos()) > maxDistSq)) {
                  BlockState state = mc.world.getBlockState(posx);
                  if (!state.isAir() && this.isBlockEnabled(state.getBlock())) {
                     ColorRGBA color = this.getBlockColor(state.getBlock());
                     Box box = new Box(posx);
                     Draw3DUtility.renderOutlinedBox(matrices, lineBuffer, box, color.withAlpha(220.0F));
                  }
               }
            }

            BuiltBuffer builtLine = lineBuffer.endNullable();
            if (builtLine != null) {
               BufferRenderer.drawWithGlobalProgram(builtLine);
            }
         }

         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         matrices.pop();
      }
   };

   public XRay() {
      super("XRay", "Подсвечивает ценные руды и блоки сквозь стены в 3D", Category.RENDER, -1);
      this.registerSetting(this.diamondOre);
      this.registerSetting(this.ancientDebris);
      this.registerSetting(this.goldOre);
      this.registerSetting(this.ironOre);
      this.registerSetting(this.lapisOre);
      this.registerSetting(this.emeraldOre);
      this.registerSetting(this.redstoneOre);
      this.registerSetting(this.fill);
      this.registerSetting(this.outline);
      this.registerSetting(this.maxDistance);
   }

   @Override
   protected void onEnable() {
      this.cachedBlocks.clear();
      if (mc.world != null && mc.player != null) {
         new Thread(this::scanAllLoadedChunks, "XRay-Scanner").start();
      }
   }

   @Override
   protected void onDisable() {
      this.cachedBlocks.clear();
   }

   public void scanChunk(WorldChunk chunk) {
      if (mc.world != null && chunk != null && this.isEnabled()) {
         int chunkX = chunk.getPos().getStartX();
         int chunkZ = chunk.getPos().getStartZ();
         int minY = mc.world.getBottomY();
         int maxY = mc.world.getTopYInclusive();
         Mutable mutable = new Mutable();

         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = minY; y <= maxY; y++) {
                  mutable.set(chunkX + x, y, chunkZ + z);
                  BlockState state = chunk.getBlockState(mutable);
                  if (!state.isAir() && this.isBlockEnabled(state.getBlock())) {
                     this.cachedBlocks.add(mutable.toImmutable());
                  }
               }
            }
         }
      }
   }

   private void scanAllLoadedChunks() {
      if (mc.world != null && mc.player != null) {
         int centerChunkX = mc.player.getChunkPos().x;
         int centerChunkZ = mc.player.getChunkPos().z;
         int radius = Math.min(6, (int)Math.ceil(this.maxDistance.get() / 16.0F));

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (!this.isEnabled()) {
                  return;
               }

               WorldChunk chunk = mc.world.getChunk(centerChunkX + dx, centerChunkZ + dz);
               if (chunk != null) {
                  this.scanChunk(chunk);
               }
            }
         }
      }
   }

   public boolean isBlockEnabled(Block block) {
      if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
         return this.diamondOre.get();
      } else if (block == Blocks.ANCIENT_DEBRIS) {
         return this.ancientDebris.get();
      } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
         return this.goldOre.get();
      } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
         return this.ironOre.get();
      } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
         return this.lapisOre.get();
      } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
         return this.emeraldOre.get();
      } else {
         return block != Blocks.REDSTONE_ORE && block != Blocks.DEEPSLATE_REDSTONE_ORE ? false : this.redstoneOre.get();
      }
   }

   private ColorRGBA getBlockColor(Block block) {
      if (block == Blocks.ANCIENT_DEBRIS) {
         return new ColorRGBA(255.0F, 131.0F, 54.0F, 255.0F);
      } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
         return new ColorRGBA(34.0F, 211.0F, 238.0F, 255.0F);
      } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
         return new ColorRGBA(255.0F, 215.0F, 0.0F, 255.0F);
      } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
         return new ColorRGBA(210.0F, 215.0F, 220.0F, 255.0F);
      } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
         return new ColorRGBA(0.0F, 71.0F, 179.0F, 255.0F);
      } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
         return new ColorRGBA(16.0F, 185.0F, 129.0F, 255.0F);
      } else {
         return block != Blocks.REDSTONE_ORE && block != Blocks.DEEPSLATE_REDSTONE_ORE ? ColorRGBA.WHITE : new ColorRGBA(239.0F, 68.0F, 68.0F, 255.0F);
      }
   }
}
