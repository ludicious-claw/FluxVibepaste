package ru.kirka.fluxclient.feature.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.event.EventListener;
import ru.kirka.fluxclient.event.impl.PreHudRenderEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.render.color.ColorRGBA;
import ru.kirka.fluxclient.render.color.Colors;
import ru.kirka.fluxclient.render.context.CustomDrawContext;
import ru.kirka.fluxclient.render.draw.DrawUtility;
import ru.kirka.fluxclient.render.draw.Utils;
import ru.kirka.fluxclient.render.geometry.BorderRadius;
import ru.kirka.fluxclient.render.msdf.Font;
import ru.kirka.fluxclient.render.msdf.Fonts;

public class NameTagsPlus extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final BooleanSetting armor = new BooleanSetting("Броня", "Показывать надетую броню и предметы", true);
   public final BooleanSetting heldItem = new BooleanSetting("Предмет в руке", "Показывать предмет в главной руке", true);
   public final BooleanSetting hpBar = new BooleanSetting("Полоска HP", "Плавная полоска здоровья под ником", true);
   public final BooleanSetting info = new BooleanSetting("Инфо", "Дистанция и пинг игрока", true);
   public final BooleanSetting borderGlow = new BooleanSetting("Свечение рамки", "Неоновый акцент вокруг плашки", true);
   public final BooleanSetting monsters = new BooleanSetting("Мобы", "Отображать теги и над мобами", false);
   public final NumberSetting scale = new NumberSetting("Масштаб", "Масштаб плашки", 1.0F, 0.5F, 2.0F, 0.05F);
   private final Map<Integer, Float> animatedHealthMap = new HashMap<>();
   private final EventListener<PreHudRenderEvent> onRender = event -> {
      if (mc.world != null && mc.player != null) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
         CustomDrawContext context = event.getContext();

         for (PlayerEntity player : mc.world.getPlayers()) {
            if ((player != mc.player || !mc.options.getPerspective().isFirstPerson()) && player.isAlive()) {
               this.renderTag(context, player, tickDelta);
            }
         }

         if (this.monsters.get()) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof LivingEntity living && !(entity instanceof PlayerEntity) && entity.isAlive()) {
                  this.renderTag(context, living, tickDelta);
               }
            }
         }
      }
   };

   public NameTagsPlus() {
      super("NameTags+", "Экранные плашки над игроками: здоровье, броня, пинг", Category.RENDER, -1);
      this.registerSetting(this.armor);
      this.registerSetting(this.heldItem);
      this.registerSetting(this.hpBar);
      this.registerSetting(this.info);
      this.registerSetting(this.borderGlow);
      this.registerSetting(this.monsters);
      this.registerSetting(this.scale);
   }

   private void renderTag(CustomDrawContext context, LivingEntity entity, float tickDelta) {
      Vec3d headPos = Utils.getInterpolatedPos(entity, tickDelta).add(0.0, entity.getHeight() + 0.45, 0.0);
      Vec2f screenPos = Utils.worldToScreen(headPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getPos().distanceTo(entity.getPos());
         float scaleFactor = MathHelper.clamp(1.0F - distance / 100.0F, 0.65F, 1.2F) * this.scale.get();
         context.getMatrices().push();
         context.getMatrices().translate(screenPos.x, screenPos.y, 0.0F);
         context.getMatrices().scale(scaleFactor, scaleFactor, 1.0F);
         float health = entity.getHealth() + entity.getAbsorptionAmount();
         float maxHealth = entity.getMaxHealth() + entity.getAbsorptionAmount();
         float animatedHp = this.animatedHealthMap.compute(entity.getId(), (k, v) -> v == null ? health : MathHelper.lerp(0.1F, v, health));
         StringBuilder sb = new StringBuilder();
         sb.append(entity.getName().getString());
         if (this.info.get()) {
            sb.append(" §7[").append((int)distance).append("m]");
            if (entity instanceof PlayerEntity player) {
               PlayerListEntry entry = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
               if (entry != null) {
                  sb.append(" §8[").append(entry.getLatency()).append("ms]");
               }
            }
         }

         String hpStr = String.format(" §c%.1f", health);
         String fullText = sb.toString() + hpStr;
         Font font = Fonts.MEDIUM.getFont(12.0F);
         float textWidth = font.getFont().getWidth(fullText, font.getSize());
         float pad = 4.0F;
         float w = textWidth + pad * 2.0F;
         float h = font.getSize() + 6.0F;
         float x = -w / 2.0F;
         float y = -h;
         DrawUtility.drawRoundedRect(context.getMatrices(), x, y, w, h, BorderRadius.all(4.0F), Colors.getBackgroundColor().withAlpha(180.0F));
         if (this.borderGlow.get()) {
            DrawUtility.drawRoundedBorder(context.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(4.0F), Colors.getAccent().withAlpha(150.0F));
         }

         context.drawText(font, fullText, x + pad, y + pad - 1.0F, ColorRGBA.WHITE);
         if (this.hpBar.get()) {
            float barY = y + h - 2.0F;
            float barW = w - 4.0F;
            float hpPercent = MathHelper.clamp(animatedHp / maxHealth, 0.0F, 1.0F);
            DrawUtility.drawRoundedRect(context.getMatrices(), x + 2.0F, barY, barW, 2.0F, BorderRadius.all(1.0F), new ColorRGBA(50.0F, 50.0F, 50.0F, 200.0F));
            ColorRGBA hpColor = hpPercent > 0.6F
               ? new ColorRGBA(94.0F, 252.0F, 132.0F, 255.0F)
               : (hpPercent > 0.3F ? new ColorRGBA(255.0F, 214.0F, 92.0F, 255.0F) : new ColorRGBA(255.0F, 85.0F, 85.0F, 255.0F));
            DrawUtility.drawRoundedRect(context.getMatrices(), x + 2.0F, barY, barW * hpPercent, 2.0F, BorderRadius.all(1.0F), hpColor);
         }

         if (this.armor.get()) {
            List<ItemStack> items = new ArrayList<>();
            if (this.heldItem.get()) {
               ItemStack mainHand = entity.getMainHandStack();
               if (!mainHand.isEmpty()) {
                  items.add(mainHand);
               }
            }

            for (ItemStack armorStack : entity.getArmorItems()) {
               if (!armorStack.isEmpty()) {
                  items.add(armorStack);
               }
            }

            if (this.heldItem.get()) {
               ItemStack offHand = entity.getOffHandStack();
               if (!offHand.isEmpty()) {
                  items.add(offHand);
               }
            }

            if (!items.isEmpty()) {
               float itemSize = 14.0F;
               float startX = -(items.size() * itemSize) / 2.0F;
               float itemY = y - itemSize - 2.0F;

               for (int i = 0; i < items.size(); i++) {
                  ItemStack st = items.get(i);
                  float ix = startX + i * itemSize;
                  context.getMatrices().push();
                  context.getMatrices().translate(ix, itemY, 0.0F);
                  context.getMatrices().scale(0.85F, 0.85F, 1.0F);
                  context.drawItem(st, 0, 0);
                  context.getMatrices().pop();
               }
            }
         }

         context.getMatrices().pop();
      }
   }
}
