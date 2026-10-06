package ru.kirka.fluxclient.render.shaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class ShaderProgram {
   private final int programId;

   public ShaderProgram(Identifier vertexPath, Identifier fragmentPath) {
      int vShader = this.compileShader(vertexPath, 35633);
      int fShader = this.compileShader(fragmentPath, 35632);
      this.programId = GL20.glCreateProgram();
      GL20.glAttachShader(this.programId, vShader);
      GL20.glAttachShader(this.programId, fShader);
      GL20.glLinkProgram(this.programId);
      if (GL20.glGetProgrami(this.programId, 35714) == 0) {
         String log = GL20.glGetProgramInfoLog(this.programId);
         throw new RuntimeException("Ошибка линковки шейдера: " + log);
      } else {
         GL20.glDeleteShader(vShader);
         GL20.glDeleteShader(fShader);
      }
   }

   public void bind() {
      GL20.glUseProgram(this.programId);
   }

   public void unbind() {
      GL20.glUseProgram(0);
   }

   public void setUniform1f(String name, float value) {
      GL20.glUniform1f(this.getUniformLocation(name), value);
   }

   public void setUniform1i(String name, int value) {
      GL20.glUniform1i(this.getUniformLocation(name), value);
   }

   public void setUniform2f(String name, float x, float y) {
      GL20.glUniform2f(this.getUniformLocation(name), x, y);
   }

   public void setUniform3f(String name, float x, float y, float z) {
      GL20.glUniform3f(this.getUniformLocation(name), x, y, z);
   }

   public void setUniform4f(String name, float x, float y, float z, float w) {
      GL20.glUniform4f(this.getUniformLocation(name), x, y, z, w);
   }

   public int getUniformLocation(String name) {
      return GL20.glGetUniformLocation(this.programId, name);
   }

   private int compileShader(Identifier identifier, int type) {
      int shader = GL20.glCreateShader(type);
      String source = this.readSource(identifier);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      if (GL20.glGetShaderi(shader, 35713) == 0) {
         String log = GL20.glGetShaderInfoLog(shader);
         throw new RuntimeException("Ошибка компиляции " + identifier + ": " + log);
      } else {
         return shader;
      }
   }

   private String readSource(Identifier identifier) {
      String path = "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();

      try {
         label77: {
            String in;
            try (InputStream inx = ShaderProgram.class.getResourceAsStream(path)) {
               if (inx == null) {
                  break label77;
               }

               in = new String(inx.readAllBytes(), StandardCharsets.UTF_8);
            }

            return in;
         }
      } catch (Exception var12) {
      }

      try {
         Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
         if (resource.isPresent()) {
            String var5;
            try (InputStream in = resource.get().getInputStream()) {
               var5 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            return var5;
         }
      } catch (Exception var10) {
         FluxLogger.error("Не удалось прочитать шейдер: " + identifier, var10);
      }

      return "";
   }
}
