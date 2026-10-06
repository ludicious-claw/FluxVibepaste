package ru.kirka.fluxclient.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class OrbitShader {
   private static final FloatBuffer MATRIX_BUF = BufferUtils.createFloatBuffer(16);
   private static int sharedVao = -1;
   private static int sharedVbo = -1;
   private final int program;
   private final Map<String, Integer> locations = new HashMap<>();
   private boolean valid = true;

   public OrbitShader(String vertexSource, String fragmentSource) {
      this.program = GL30.glCreateProgram();
      this.attach(vertexSource, 35633);
      this.attach(fragmentSource, 35632);
      GL30.glBindAttribLocation(this.program, 0, "Position");
      GL30.glBindAttribLocation(this.program, 1, "UV0");
      GL30.glLinkProgram(this.program);
      if (GL30.glGetProgrami(this.program, 35714) == 0) {
         FluxLogger.error("OrbitShader link error:\n" + GL30.glGetProgramInfoLog(this.program), null);
         this.valid = false;
      }
   }

   private void attach(String source, int type) {
      if (this.valid) {
         int shader = GL30.glCreateShader(type);
         GL30.glShaderSource(shader, source);
         GL30.glCompileShader(shader);
         if (GL30.glGetShaderi(shader, 35713) == 0) {
            FluxLogger.error("OrbitShader compile error:\n" + GL30.glGetShaderInfoLog(shader), null);
            this.valid = false;
         } else {
            GL30.glAttachShader(this.program, shader);
         }
      }
   }

   public boolean isValid() {
      return this.valid;
   }

   public void bind() {
      if (this.valid) {
         GL30.glUseProgram(this.program);
         this.setMatrix("ProjMat", RenderSystem.getProjectionMatrix());
         this.setMatrix("ModelViewMat", RenderSystem.getModelViewMatrix());
         MinecraftClient mc = MinecraftClient.getInstance();
         this.setFloat2("resolution", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         this.setFloat2("screenSize", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
      }
   }

   public void unbind() {
      GL30.glUseProgram(0);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void setFloat(String name, float value) {
      if (this.valid) {
         GL30.glUniform1f(this.location(name), value);
      }
   }

   public void setFloat2(String name, float x, float y) {
      if (this.valid) {
         GL30.glUniform2f(this.location(name), x, y);
      }
   }

   public void setFloat4(String name, float x, float y, float z, float w) {
      if (this.valid) {
         GL30.glUniform4f(this.location(name), x, y, z, w);
      }
   }

   public void setFloat3(String name, float x, float y, float z) {
      if (this.valid) {
         GL30.glUniform3f(this.location(name), x, y, z);
      }
   }

   public void setInt(String name, int value) {
      if (this.valid) {
         GL30.glUniform1i(this.location(name), value);
      }
   }

   public void setModelViewMatrix(Matrix4f matrix) {
      this.setMatrix("ModelViewMat", matrix);
   }

   public void setProjectionMatrix(Matrix4f matrix) {
      this.setMatrix("ProjMat", matrix);
   }

   private void setMatrix(String name, Matrix4f matrix) {
      int loc = this.location(name);
      if (loc != -1) {
         MATRIX_BUF.clear();
         matrix.get(MATRIX_BUF);
         MATRIX_BUF.rewind();
         GL30.glUniformMatrix4fv(loc, false, MATRIX_BUF);
      }
   }

   private int location(String name) {
      Integer cached = this.locations.get(name);
      if (cached != null) {
         return cached;
      } else {
         int loc = GL30.glGetUniformLocation(this.program, name);
         this.locations.put(name, loc);
         return loc;
      }
   }

   public static void drawTriangles(float[] vertices, int triCount) {
      int prevVao = GL30.glGetInteger(34229);
      int prevVbo = GL30.glGetInteger(34964);
      if (sharedVao == -1) {
         sharedVao = GL30.glGenVertexArrays();
         sharedVbo = GL30.glGenBuffers();
         GL30.glBindVertexArray(sharedVao);
         GL30.glBindBuffer(34962, sharedVbo);
         GL30.glEnableVertexAttribArray(0);
         GL30.glVertexAttribPointer(0, 3, 5126, false, 20, 0L);
         GL30.glEnableVertexAttribArray(1);
         GL30.glVertexAttribPointer(1, 2, 5126, false, 20, 12L);
      }

      GL30.glBindVertexArray(sharedVao);
      GL30.glBindBuffer(34962, sharedVbo);
      GL30.glBufferData(34962, vertices, 35040);
      GL30.glDrawArrays(4, 0, triCount * 3);
      GL30.glBindBuffer(34962, prevVbo);
      GL30.glBindVertexArray(prevVao);
   }
}
