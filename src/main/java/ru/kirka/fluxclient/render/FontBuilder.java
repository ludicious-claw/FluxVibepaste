package ru.kirka.fluxclient.render;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.common.Interface;

public class FontBuilder implements Interface {
   private final Gson gson = new Gson();
   private Identifier fontJsonId;
   private Identifier fontTextureId;
   private String fontName;

   public FontBuilder a(String fontName) {
      this.fontName = fontName;
      this.fontJsonId = Identifier.of("fluxclient", "fonts/" + fontName + ".json");
      this.fontTextureId = Identifier.of("fluxclient", "fonts/" + fontName + ".png");
      return this;
   }

   public Font a() {
      FontData data = this.b();
      AbstractTexture texture = this.c();
      Map<Integer, MsdfGlyph> glyphs = this.a(data);
      Map<Integer, Map<Integer, Float>> kernings = this.b(data);
      return new Font(this.fontName, texture, data.atlas(), data.metrics(), glyphs, kernings);
   }

   private FontData b() {
      String json = this.a(this.fontJsonId);
      FontData data = (FontData)this.gson.fromJson(json, FontData.class);
      if (data == null) {
         throw new RuntimeException("Failed to read font data: " + this.fontJsonId);
      } else {
         return data;
      }
   }

   private AbstractTexture c() {
      AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.fontTextureId);
      RenderSystem.recordRenderCall(() -> texture.setFilter(true, false));
      return texture;
   }

   private Map<Integer, MsdfGlyph> a(FontData data) {
      float atlasWidth = data.atlas().width();
      float atlasHeight = data.atlas().height();
      Map<Integer, MsdfGlyph> map = new HashMap<>();

      for (FontData.GlyphData glyph : data.glyphs()) {
         map.put(glyph.unicode(), new MsdfGlyph(glyph, atlasWidth, atlasHeight));
      }

      return map;
   }

   private Map<Integer, Map<Integer, Float>> b(FontData data) {
      Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
      if (data.kernings() != null) {
         data.kernings().forEach(kerning -> {
            Map<Integer, Float> kerningMap = kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>());
            kerningMap.put(kerning.rightChar(), kerning.advance());
         });
      }

      return kernings;
   }

   private String a(Identifier identifier) {
      try {
         InputStream inputStream = null;

         try {
            inputStream = mc.getResourceManager().open(identifier);
         } catch (Exception var9) {
         }

         if (inputStream == null) {
            try {
               inputStream = mc.getResourceManager().open(Identifier.of("delta", identifier.getPath()));
            } catch (Exception var8) {
            }
         }

         if (inputStream == null) {
            inputStream = FontBuilder.class.getResourceAsStream("/assets/fluxclient/" + identifier.getPath());
         }

         if (inputStream == null) {
            inputStream = FontBuilder.class.getResourceAsStream("/assets/delta/" + identifier.getPath());
         }

         if (inputStream == null) {
            throw new RuntimeException("Font asset not found: " + identifier);
         } else {
            String var4;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
               var4 = reader.lines().collect(Collectors.joining("\n"));
            }

            return var4;
         }
      } catch (Exception var10) {
         throw new RuntimeException("Failed to read font resource: " + identifier, var10);
      }
   }
}
