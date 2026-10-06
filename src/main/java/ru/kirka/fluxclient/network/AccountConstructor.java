package ru.kirka.fluxclient.network;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class AccountConstructor {
   private String name;
   private boolean selected;
   private boolean favorited;

   public AccountConstructor() {
   }

   public AccountConstructor(String name, boolean selected, boolean favorited) {
      this.name = name;
      this.selected = selected;
      this.favorited = favorited;
   }

   public AccountConstructor(String name) {
      this.name = name;
      this.selected = true;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void a(String name) {
      this.name = name;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public void a(boolean selected) {
      this.selected = selected;
   }

   public void setFavorited(boolean favorited) {
      this.favorited = favorited;
   }

   public void b(boolean favorited) {
      this.favorited = favorited;
   }

   public String getName() {
      return this.name;
   }

   public String b() {
      return this.name;
   }

   public boolean isSelected() {
      return this.selected;
   }

   public boolean c() {
      return this.selected;
   }

   public boolean isFavorited() {
      return this.favorited;
   }

   public boolean d() {
      return this.favorited;
   }

   public Identifier getSkinTexture() {
      UUID uuid = this.name != null && !this.name.isEmpty()
         ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.name).getBytes(StandardCharsets.UTF_8))
         : new UUID(0L, 0L);
      return DefaultSkinHelper.getSkinTextures(uuid).texture();
   }

   public Identifier a() {
      return this.getSkinTexture();
   }
}
