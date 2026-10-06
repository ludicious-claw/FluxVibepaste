package ru.kirka.fluxclient.feature.impl.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Direction.Type;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.ClientPlayerTickEvent;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.event.impl.ReceivePacketEvent;
import ru.kirka.fluxclient.event.impl.WorldChangeEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.context.CustomDrawContext;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Font;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class TrapESP extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final BooleanSetting showDepth = new BooleanSetting("Глубина", true);
   public final BooleanSetting showIndicator = new BooleanSetting("Индикатор редстоуна", true);
   private volatile List<TrapESP.Trap> traps = Collections.emptyList();
   private long lastScanTime;
   private final Deque<BlockPos> scanQueue = new ConcurrentLinkedDeque<>();
   private final Set<BlockPos> enqueuedColumns = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final Map<Long, TrapESP.Trap> detectedTraps = new ConcurrentHashMap<>();
   private static final long SCAN_INTERVAL_MS = 4000L;
   private static final int COLUMNS_PER_TICK = 96;
   private static final int MAX_TRAP_DEPTH = 24;
   private static final int MIN_TRAP_DEPTH = 5;
   private final Block[] INDICATOR_BLOCKS = new Block[]{
      Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.COBWEB, Blocks.TNT, Blocks.BEDROCK
   };
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player != null && mc.world != null) {
         long now = System.currentTimeMillis();
         if (now - this.lastScanTime >= 4000L && this.scanQueue.isEmpty()) {
            this.enqueueFullScan();
            this.lastScanTime = now;
         }

         int processed = 0;
         long startNs = System.nanoTime();
         long timeBudgetNs = 2000000L;

         BlockPos col;
         while (processed < 96 && (col = this.scanQueue.poll()) != null) {
            this.enqueuedColumns.remove(col);
            processed++;
            TrapESP.Trap t = this.scanColumnForTrap(col);
            long key = packXZ(col.getX(), col.getZ());
            if (t != null) {
               this.detectedTraps.put(key, t);
            } else {
               this.detectedTraps.remove(key);
            }

            if (System.nanoTime() - startNs > timeBudgetNs) {
               break;
            }
         }

         this.traps = new ArrayList<>(this.detectedTraps.values());
      }
   };
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (mc.player != null && mc.world != null) {
         try {
            Packet<?> p = event.getPacket();
            if (p instanceof ChunkDataS2CPacket chunk) {
               this.enqueueChunk(new ChunkPos(chunk.getChunkX(), chunk.getChunkZ()));
            } else if (p instanceof ChunkDeltaUpdateS2CPacket delta) {
               delta.visitUpdates((posx, state) -> this.enqueueColumn(posx.getX(), posx.getZ()));
            } else if (p instanceof BlockUpdateS2CPacket block) {
               BlockPos pos = block.getPos();
               this.enqueueColumn(pos.getX(), pos.getZ());
            }
         } catch (Throwable var7) {
         }
      }
   };
   private final EventListener<WorldChangeEvent> onWorldChange = event -> this.reset();
   private final EventListener<PreHudRenderEvent> onHud = event -> {
      if (!this.traps.isEmpty() && mc.player != null) {
         CustomDrawContext ctx = event.getContext();
         Font font = Fonts.MEDIUM.getFont(10.0F);

         for (TrapESP.Trap trap : this.traps) {
            Vec2f screen = Utils.worldToScreen(trap.pos.toCenterPos().add(0.0, 0.5, 0.0));
            if (screen != null) {
               String title = "⚠ ТРАПКА";
               String depthStr = "Глубина: " + trap.depth + " бл";
               String privateStr = trap.hasIndicators ? "Опасно (редстоун/обсидиан)" : "Свободная яма";
               float w = Math.max(
                     font.getFont().getWidth(title, font.getSize()),
                     Math.max(font.getFont().getWidth(depthStr, font.getSize()), font.getFont().getWidth(privateStr, font.getSize()))
                  )
                  + 8.0F;
               float h = font.getSize() * 3.0F + 8.0F;
               float x = screen.x - w / 2.0F;
               float y = screen.y - h - 5.0F;
               DrawUtility.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(20.0F, 0.0F, 0.0F, 190.0F));
               DrawUtility.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(4.0F), new ColorRGBA(255.0F, 60.0F, 60.0F, 220.0F));
               ctx.drawText(font, title, x + 4.0F, y + 2.0F, new ColorRGBA(255.0F, 60.0F, 60.0F, 255.0F));
               if (this.showDepth.get()) {
                  ctx.drawText(font, depthStr, x + 4.0F, y + 2.0F + font.getSize() + 1.0F, ColorRGBA.WHITE);
               }

               if (this.showIndicator.get()) {
                  ctx.drawText(
                     font,
                     privateStr,
                     x + 4.0F,
                     y + 2.0F + (font.getSize() + 1.0F) * 2.0F,
                     trap.hasIndicators ? new ColorRGBA(255.0F, 120.0F, 120.0F, 255.0F) : new ColorRGBA(160.0F, 160.0F, 160.0F, 255.0F)
                  );
               }
            }
         }
      }
   };

   public TrapESP() {
      super("TrapESP", "Сканер ловушек и глубоких ям (трапок) вокруг игрока", Category.RENDER, -1);
      this.registerSetting(this.showDepth);
      this.registerSetting(this.showIndicator);
   }

   @Override
   public void onDisable() {
      this.reset();
      super.onDisable();
   }

   @Override
   public void onEnable() {
      this.reset();
      this.enqueueFullScan();
      super.onEnable();
   }

   public void reset() {
      this.scanQueue.clear();
      this.enqueuedColumns.clear();
      this.detectedTraps.clear();
      this.traps = Collections.emptyList();
   }

   private void enqueueFullScan() {
      if (mc.player != null) {
         BlockPos playerPos = mc.player.getBlockPos();
         List<BlockPos> cols = new ArrayList<>();
         int radius = 48;

         for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
               BlockPos col = new BlockPos(playerPos.getX() + dx, playerPos.getY(), playerPos.getZ() + dz);
               if (this.enqueuedColumns.add(col)) {
                  cols.add(col);
               }
            }
         }

         cols.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(playerPos)));
         this.scanQueue.addAll(cols);
      }
   }

   private void enqueueChunk(ChunkPos chunkPos) {
      if (mc.player != null) {
         int baseY = mc.player.getBlockY();

         for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x += 2) {
            for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z += 2) {
               this.enqueueColumn(x, z);
            }
         }
      }
   }

   private void enqueueColumn(int x, int z) {
      if (mc.player != null) {
         BlockPos pos = new BlockPos(x, mc.player.getBlockY(), z);
         if (this.enqueuedColumns.add(pos)) {
            this.scanQueue.add(pos);
         }
      }
   }

   private TrapESP.Trap scanColumnForTrap(BlockPos columnBase) {
      if (mc.world == null) {
         return null;
      } else {
         int baseY = columnBase.getY();

         for (int dy = 10; dy >= -10; dy--) {
            BlockPos start = new BlockPos(columnBase.getX(), baseY + dy, columnBase.getZ());
            int depth = 0;
            boolean inShaft = false;

            for (int i = 0; i < 24; i++) {
               BlockPos pos = start.down(i);
               BlockState state = mc.world.getBlockState(pos);
               if (state.isAir() && mc.world.getFluidState(pos).isEmpty()) {
                  int walls = 0;

                  for (Direction dir : Type.HORIZONTAL) {
                     BlockState sideState = mc.world.getBlockState(pos.offset(dir));
                     if (!sideState.isAir() && !sideState.getCollisionShape(mc.world, pos.offset(dir)).isEmpty()) {
                        walls++;
                     }
                  }

                  if (walls < 4) {
                     if (inShaft) {
                        break;
                     }
                  } else {
                     inShaft = true;
                     depth++;
                  }
               } else if (inShaft) {
                  break;
               }
            }

            if (depth >= 5) {
               BlockPos bottom = start.down(depth);
               BlockState bottomState = mc.world.getBlockState(bottom);
               if (!bottomState.isAir() && !bottomState.getCollisionShape(mc.world, bottom).isEmpty()) {
                  boolean flagged = this.hasNearbyIndicators(start, 5);
                  return new TrapESP.Trap(start, depth, flagged);
               }
            }
         }

         return null;
      }
   }

   private boolean hasNearbyIndicators(BlockPos center, int radius) {
      if (mc.world == null) {
         return false;
      } else {
         BlockPos min = center.add(-radius, -radius, -radius);
         BlockPos max = center.add(radius, radius, radius);

         for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir()) {
               Block b = state.getBlock();
               if (b instanceof TripwireBlock
                  || b instanceof PressurePlateBlock
                  || b instanceof TripwireHookBlock
                  || b instanceof TrappedChestBlock
                  || b instanceof DispenserBlock
                  || b instanceof PistonBlock) {
                  return true;
               }

               for (Block ind : this.INDICATOR_BLOCKS) {
                  if (b == ind) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static long packXZ(int x, int z) {
      return (long)x << 32 ^ z & 4294967295L;
   }

   public static class Trap {
      public final BlockPos pos;
      public final int depth;
      public final boolean hasIndicators;

      public Trap(BlockPos pos, int depth, boolean hasIndicators) {
         this.pos = pos;
         this.depth = depth;
         this.hasIndicators = hasIndicators;
      }
   }
}
