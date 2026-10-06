package ru.kirka.fluxclient.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public class GlProgram {
   private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList<>();
   protected ShaderProgram backingProgram;
   protected ShaderProgramKey programKey;

   public GlProgram(Identifier id, VertexFormat vertexFormat) {
      String path = id.getPath();
      Identifier keyId = path.startsWith("core/") ? id : id.withPrefixedPath("core/");
      this.programKey = new ShaderProgramKey(keyId, vertexFormat, Defines.EMPTY);
      REGISTERED_PROGRAMS.add(() -> {
         try {
            this.backingProgram = MinecraftClient.getInstance().getShaderLoader().getProgramToLoad(this.programKey);
            this.setup();
         } catch (Throwable var2) {
            FluxLogger.warn("Failed to load shader program " + this.programKey.configId() + ": " + var2.getMessage());
         }
      });
   }

   public net.minecraft.client.render.RenderPhase.ShaderProgram renderPhaseProgram() {
      return new net.minecraft.client.render.RenderPhase.ShaderProgram(this.programKey);
   }

   public ShaderProgram use() {
      return RenderSystem.setShader(this.programKey);
   }

   protected void setup() {
   }

   public GlUniform findUniform(String name) {
      return this.backingProgram == null ? null : this.backingProgram.getUniform(name);
   }

   public static void loadAndSetupPrograms() {
      REGISTERED_PROGRAMS.forEach(Runnable::run);
   }
}
