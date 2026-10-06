package ru.kirka.fluxclient.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Last;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import ru.kirka.fluxclient.config.ConfigManager;
import ru.kirka.fluxclient.event.EventBus;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.ModuleManager;
import ru.kirka.fluxclient.feature.impl.misc.AutoReconnect;
import ru.kirka.fluxclient.feature.impl.render.BlockESP;
import ru.kirka.fluxclient.feature.impl.render.Breadcrumbs;
import ru.kirka.fluxclient.feature.impl.render.ChinaHat;
import ru.kirka.fluxclient.feature.impl.render.CosmeticWings;
import ru.kirka.fluxclient.feature.impl.render.EntityChams;
import ru.kirka.fluxclient.feature.impl.render.ItemPhysics;
import ru.kirka.fluxclient.feature.impl.render.JumpCircles;
import ru.kirka.fluxclient.feature.impl.render.PopChams;
import ru.kirka.fluxclient.feature.impl.render.ShaderSky;
import ru.kirka.fluxclient.feature.impl.render.TargetESP;
import ru.kirka.fluxclient.feature.impl.render.TargetParticles;
import ru.kirka.fluxclient.feature.impl.render.Trajectories;
import ru.kirka.fluxclient.feature.impl.render.WorldParticles;
import ru.kirka.fluxclient.feature.impl.render.WorldReflections;
import ru.kirka.fluxclient.gui.imgui.ImGuiEngine;
import ru.kirka.fluxclient.render.shaders.ShaderManager;

public final class FluxContext {
   private static FluxContext INSTANCE;
   private final EventBus eventBus;
   private final ModuleManager moduleManager;
   private final ConfigManager configManager;
   private final ImGuiEngine imGuiEngine;
   private final ShaderManager shaderManager;
   private boolean initialized = false;

   private FluxContext() {
      this.eventBus = new EventBus();
      this.configManager = new ConfigManager();
      this.moduleManager = new ModuleManager();
      this.imGuiEngine = new ImGuiEngine();
      this.shaderManager = new ShaderManager();
   }

   public static synchronized FluxContext get() {
      if (INSTANCE == null) {
         INSTANCE = new FluxContext();
      }

      return INSTANCE;
   }

   public void initialize() {
      if (!this.initialized) {
         this.shaderManager.init();
         this.moduleManager.init();
         this.configManager.load();
         WorldRenderEvents.LAST.register((Last)context -> {
            MatrixStack matrices = context.matrixStack();
            if (matrices != null && context.camera() != null) {
               Vec3d cameraPos = context.camera().getPos();
               Quaternionf camRot = context.camera().getRotation();
               safeRender(() -> {
                  JumpCircles jc = this.moduleManager.getModule(JumpCircles.class);
                  if (jc != null && jc.isEnabled()) {
                     jc.render3D(matrices, cameraPos);
                  }
               });
               safeRender(() -> {
                  TargetESP esp = this.moduleManager.getModule(TargetESP.class);
                  if (esp != null && esp.isEnabled()) {
                     esp.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  TargetParticles tp = this.moduleManager.getModule(TargetParticles.class);
                  if (tp != null && tp.isEnabled()) {
                     tp.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  WorldParticles wp = this.moduleManager.getModule(WorldParticles.class);
                  if (wp != null && wp.isEnabled()) {
                     wp.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  ShaderSky sky = this.moduleManager.getModule(ShaderSky.class);
                  if (sky != null && sky.isEnabled()) {
                     sky.render3D(matrices, cameraPos);
                  }
               });
               safeRender(() -> {
                  BlockESP besp = this.moduleManager.getModule(BlockESP.class);
                  if (besp != null && besp.isEnabled()) {
                     besp.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  CosmeticWings wings = this.moduleManager.getModule(CosmeticWings.class);
                  if (wings != null && wings.isEnabled()) {
                     wings.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  Breadcrumbs crumbs = this.moduleManager.getModule(Breadcrumbs.class);
                  if (crumbs != null && crumbs.isEnabled()) {
                     crumbs.render3D(matrices, cameraPos);
                  }
               });
               safeRender(() -> {
                  ChinaHat hat = this.moduleManager.getModule(ChinaHat.class);
                  if (hat != null && hat.isEnabled()) {
                     hat.render3D(matrices, cameraPos);
                  }
               });
               safeRender(() -> {
                  PopChams pop = this.moduleManager.getModule(PopChams.class);
                  if (pop != null && pop.isEnabled()) {
                     pop.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  EntityChams chams = this.moduleManager.getModule(EntityChams.class);
                  if (chams != null && chams.isEnabled()) {
                     chams.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  ItemPhysics itemPhys = this.moduleManager.getModule(ItemPhysics.class);
                  if (itemPhys != null && itemPhys.isEnabled()) {
                     itemPhys.render3D(matrices, cameraPos, camRot);
                  }
               });
               safeRender(() -> {
                  Trajectories traj = this.moduleManager.getModule(Trajectories.class);
                  if (traj != null && traj.isEnabled()) {
                     traj.render3D(matrices, cameraPos, camRot);
                  }
               });
            }
         });
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            if (client.player != null && client.world != null) {
               WorldReflections wr = this.moduleManager.getModule(WorldReflections.class);
               if (wr != null && !wr.isEnabled() && WorldReflections.isIrisAvailable() && WorldReflections.isShadersInUse()) {
                  WorldReflections.setIrisShadersActive(false);
               }

               for (Module module : this.moduleManager.getModules()) {
                  if (module.isEnabled()) {
                     try {
                        module.onTick();
                     } catch (Throwable var7) {
                     }
                  }
               }
            } else {
               AutoReconnect ar = this.moduleManager.getModule(AutoReconnect.class);
               if (ar != null && ar.isEnabled()) {
                  try {
                     ar.onTick();
                  } catch (Throwable var6) {
                  }
               }
            }
         });
         Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
         this.initialized = true;
      }
   }

   private static void safeRender(Runnable r) {
      try {
         r.run();
      } catch (Throwable var2) {
      }
   }

   public void shutdown() {
      this.configManager.save();
      this.imGuiEngine.dispose();
   }

   public EventBus getEventBus() {
      return this.eventBus;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public ImGuiEngine getImGuiEngine() {
      return this.imGuiEngine;
   }

   public ShaderManager getShaderManager() {
      return this.shaderManager;
   }
}
