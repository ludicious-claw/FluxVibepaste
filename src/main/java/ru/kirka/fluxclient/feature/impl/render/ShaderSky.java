package ru.kirka.fluxclient.feature.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.shaders.OrbitShader;
import ru.kirka.fluxclient.render.shaders.OrbitShaders;

public class ShaderSky extends Module {
   public final ModeSetting mode = new ModeSetting(
      "Режим", "Стиль неба", "Северное сияние", "Северное сияние", "Космос и Звёзды", "Плазма", "Неоновый закат", "Полный (Кинематограф)"
   );
   public final NumberSetting auroraSpeed = new NumberSetting("Скорость", "Скорость колыхания волн", 1.2F, 0.2F, 4.0F, 0.1F);
   public final NumberSetting auroraBrightness = new NumberSetting("Яркость", "Интенсивность сияния", 1.8F, 0.5F, 4.0F, 0.1F);
   public final ColorSetting auroraColor1 = new ColorSetting("Цвет низа", "Нижний цвет сияния (Изумруд/Циан)", new Color(16, 185, 129, 255));
   public final ColorSetting auroraColor2 = new ColorSetting("Цвет верха", "Верхний цвет сияния (Фиолетовый/Маджента)", new Color(168, 85, 247, 255));
   public final BooleanSetting stars = new BooleanSetting("Звёздный купол", "Мерцающие звёзды", true);
   public final NumberSetting starDensity = new NumberSetting("Плотность звёзд", "Количество звёзд", 1.2F, 0.1F, 3.0F, 0.1F);
   public final ColorSetting zenithColor = new ColorSetting("Цвет зенита", "Цвет макушки купола неба", new Color(8, 6, 22, 255));
   public final ColorSetting horizonColor = new ColorSetting("Цвет горизонта", "Цвет полосы горизонта", new Color(45, 16, 80, 255));
   public final BooleanSetting hideClouds = new BooleanSetting("Скрыть облака", "Убрать ванильные пиксельные облака", true);
   private static final int SLICES = 32;
   private static final int STACKS = 16;
   private static final int TRIANGLE_COUNT = 1024;
   private static final float[] DOME_VERTICES = generateSkyDome(95.0F, 32, 16);

   public ShaderSky() {
      super("ShaderSky", "Шейдерное небо с фотореалистичным северным сиянием и звёздами", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.auroraSpeed);
      this.registerSetting(this.auroraBrightness);
      this.registerSetting(this.auroraColor1);
      this.registerSetting(this.auroraColor2);
      this.registerSetting(this.stars);
      this.registerSetting(this.starDensity);
      this.registerSetting(this.zenithColor);
      this.registerSetting(this.horizonColor);
      this.registerSetting(this.hideClouds);
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos) {
      if (mc.world != null && mc.player != null) {
         Optional<OrbitShader> opt = OrbitShaders.get("sky_aurora");
         if (!opt.isEmpty()) {
            OrbitShader shader = opt.get();
            if (shader.isValid()) {
               Camera camera = mc.gameRenderer.getCamera();
               float time = (float)(System.currentTimeMillis() % 10000000L / 1000.0);
               matrices.push();
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(770, 771);
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(false);
               RenderSystem.disableCull();
               shader.bind();
               float pitchRad = (float)Math.toRadians(camera.getPitch());
               float yawRad = (float)Math.toRadians(camera.getYaw() + 180.0F);
               Matrix4f viewMatrix = new Matrix4f().rotationX(pitchRad).rotateY(yawRad);
               shader.setModelViewMatrix(viewMatrix);
               shader.setProjectionMatrix(RenderSystem.getProjectionMatrix());
               shader.setFloat("time", time);
               Color c1 = this.auroraColor1.get();
               Color c2 = this.auroraColor2.get();
               Color zCol = this.zenithColor.get();
               Color hCol = this.horizonColor.get();
               shader.setFloat4("color1", c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 1.0F);
               shader.setFloat4("color2", c2.getRed() / 255.0F, c2.getGreen() / 255.0F, c2.getBlue() / 255.0F, 1.0F);
               shader.setFloat4("zenithCol", zCol.getRed() / 255.0F, zCol.getGreen() / 255.0F, zCol.getBlue() / 255.0F, 1.0F);
               shader.setFloat4("horizonCol", hCol.getRed() / 255.0F, hCol.getGreen() / 255.0F, hCol.getBlue() / 255.0F, 1.0F);
               shader.setFloat("speed", this.auroraSpeed.get());
               shader.setFloat("brightness", this.auroraBrightness.get());
               shader.setFloat("starDensity", this.stars.get() ? this.starDensity.get() : 0.0F);
               String var15 = this.mode.get();

               int modeIdx = switch (var15) {
                  case "Космос и Звёзды" -> 1;
                  case "Плазма" -> 2;
                  case "Неоновый закат" -> 3;
                  case "Полный (Кинематограф)" -> 4;
                  default -> 0;
               };
               shader.setInt("skyMode", modeIdx);
               OrbitShader.drawTriangles(DOME_VERTICES, 1024);
               shader.unbind();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
               matrices.pop();
            }
         }
      }
   }

   private static float[] generateSkyDome(float radius, int slices, int stacks) {
      List<Float> list = new ArrayList<>();

      for (int i = 0; i < stacks; i++) {
         double phi1 = i * Math.PI / stacks;
         double phi2 = (i + 1) * Math.PI / stacks;

         for (int j = 0; j < slices; j++) {
            double theta1 = j * 2.0 * Math.PI / slices;
            double theta2 = (j + 1) * 2.0 * Math.PI / slices;
            addDomeVertex(list, radius, theta1, phi1);
            addDomeVertex(list, radius, theta2, phi2);
            addDomeVertex(list, radius, theta2, phi1);
            addDomeVertex(list, radius, theta1, phi1);
            addDomeVertex(list, radius, theta1, phi2);
            addDomeVertex(list, radius, theta2, phi2);
         }
      }

      float[] arr = new float[list.size()];

      for (int i = 0; i < list.size(); i++) {
         arr[i] = list.get(i);
      }

      return arr;
   }

   private static void addDomeVertex(List<Float> list, float r, double theta, double phi) {
      float x = (float)(r * Math.sin(phi) * Math.cos(theta));
      float y = (float)(r * Math.cos(phi));
      float z = (float)(r * Math.sin(phi) * Math.sin(theta));
      list.add(x);
      list.add(y);
      list.add(z);
      list.add((float)(theta / (Math.PI * 2)));
      list.add((float)(phi / Math.PI));
   }
}
