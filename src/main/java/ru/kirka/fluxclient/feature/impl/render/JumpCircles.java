package ru.kirka.fluxclient.feature.impl.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.gui.imgui.ImGuiTheme;
import ru.kirka.fluxclient.render.Render3DUtil;

public class JumpCircles extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Стиль колец", "Двойная волна", "Двойная волна", "Неоновый импульс", "Классический");
   public final ModeSetting colorMode = new ModeSetting("Цвет", "Цветовая схема", "Тема клиента", "Тема клиента", "Свой цвет", "Радуга");
   public final ColorSetting color = new ColorSetting("Свой цвет", "Основной цвет кругов", new Color(168, 85, 247, 220));
   public final NumberSetting maxRadius = new NumberSetting("Радиус", "Максимальный радиус расширения", 2.5F, 1.0F, 5.0F, 0.1F);
   public final NumberSetting duration = new NumberSetting("Длительность", "Время жизни кольца (сек)", 1.1F, 0.4F, 2.5F, 0.1F);
   public final BooleanSetting onLanding = new BooleanSetting("При приземлении", "Спавнить кольцо и при падении на землю", true);
   private static final List<JumpCircles.Circle> circles = new ArrayList<>();
   private boolean wasOnGround = false;

   public JumpCircles() {
      super("JumpCircles", "Кинематографичные расходящиеся кольца при прыжках и приземлениях", Category.RENDER, -1);
      this.registerSetting(this.mode);
      this.registerSetting(this.colorMode);
      this.registerSetting(this.color);
      this.registerSetting(this.maxRadius);
      this.registerSetting(this.duration);
      this.registerSetting(this.onLanding);
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         boolean onGround = mc.player.isOnGround();
         Vec3d pPos = mc.player.getPos().add(0.0, 0.04, 0.0);
         if (!onGround && this.wasOnGround) {
            circles.add(new JumpCircles.Circle(pPos, System.currentTimeMillis()));
         } else if (onGround && !this.wasOnGround && this.onLanding.get() && mc.player.fallDistance > 0.5F) {
            circles.add(new JumpCircles.Circle(pPos, System.currentTimeMillis()));
         }

         this.wasOnGround = onGround;
      }
   }

   public void render3D(MatrixStack matrices, Vec3d cameraPos) {
      if (!circles.isEmpty() && mc.player != null) {
         long now = System.currentTimeMillis();
         float lifetimeMs = this.duration.get() * 1000.0F;
         circles.removeIf(cx -> (float)(now - cx.time) > lifetimeMs);
         String m = this.mode.get();
         float maxR = this.maxRadius.get();

         for (JumpCircles.Circle c : circles) {
            float progress = (float)(now - c.time) / lifetimeMs;
            if (!(progress > 1.0F)) {
               float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
               float curR = eased * maxR;
               Color baseCol = this.resolveColor(progress);
               int alpha = (int)((1.0F - progress) * (baseCol.getAlpha() / 255.0F) * 230.0F);
               if (alpha > 2) {
                  Vec3d renderPos = c.pos.subtract(cameraPos);
                  switch (m) {
                     case "Двойная волна":
                        Color outerCol = new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), (int)(alpha * 0.65F));
                        Render3DUtil.drawRing(matrices, renderPos, Math.max(0.0F, curR - 0.28F), curR, outerCol, 36);
                        float innerR = (float)(Math.pow(progress, 0.7) * (maxR * 0.65F));
                        int innerAlpha = (int)(alpha * 0.85F);
                        Color innerCol = new Color(255, 255, 255, innerAlpha);
                        Render3DUtil.drawRing(matrices, renderPos, Math.max(0.0F, innerR - 0.08F), innerR, innerCol, 28);
                        break;
                     case "Неоновый импульс": {
                        Color ringCol = new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), alpha);
                        Render3DUtil.drawRing(matrices, renderPos, Math.max(0.0F, curR - 0.12F), curR, ringCol, 40);
                        Color glowCol = new Color(255, 255, 255, (int)(alpha * 0.4F));
                        Render3DUtil.drawRing(matrices, renderPos, Math.max(0.0F, curR - 0.04F), curR, glowCol, 40);
                        break;
                     }
                     default: {
                        Color ringCol = new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), alpha);
                        Render3DUtil.drawRing(matrices, renderPos, Math.max(0.0F, curR - 0.22F), curR, ringCol, 32);
                     }
                  }
               }
            }
         }
      }
   }

   private Color resolveColor(float progress) {
      String var2 = this.colorMode.get();

      return switch (var2) {
         case "Тема клиента" -> ImGuiTheme.primaryColor();
         case "Радуга" -> {
            float hue = ((float)(System.currentTimeMillis() % 4000L) / 4000.0F + progress * 0.3F) % 1.0F;
            yield new Color(Color.HSBtoRGB(hue, 0.85F, 1.0F));
         }
         default -> (Color)this.color.get();
      };
   }

   private record Circle(Vec3d pos, long time) {
   }
}
