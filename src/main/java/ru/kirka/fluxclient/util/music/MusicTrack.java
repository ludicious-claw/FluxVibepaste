package ru.kirka.fluxclient.util.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.Identifier;

public class MusicTrack {
   private final String title;
   private final String artist;
   private final String appName;
   private final boolean appOpen;
   private final boolean playing;
   private final Identifier coverTexture;
   private final List<String> lyrics;
   private final List<MusicTrack.TimedLyric> timedLyrics;
   private final long durationMs;
   private final long anchorPositionMs;
   private final long anchorTimeMs;
   private final int accentColor;

   public MusicTrack(
      String title,
      String artist,
      String appName,
      boolean appOpen,
      boolean playing,
      Identifier coverTexture,
      List<String> lyrics,
      List<MusicTrack.TimedLyric> timedLyrics,
      long durationMs,
      long anchorPositionMs,
      long anchorTimeMs,
      int accentColor
   ) {
      this.title = title != null ? title.trim() : "";
      this.artist = artist != null ? artist.trim() : "";
      this.appName = appName != null ? appName : "Music";
      this.appOpen = appOpen;
      this.playing = playing;
      this.coverTexture = coverTexture;
      this.lyrics = (List<String>)(lyrics != null ? new ArrayList<>(lyrics) : Collections.emptyList());
      this.timedLyrics = (List<MusicTrack.TimedLyric>)(timedLyrics != null ? new ArrayList<>(timedLyrics) : Collections.emptyList());
      this.durationMs = durationMs > 0L ? durationMs : 180000L;
      this.anchorPositionMs = anchorPositionMs >= 0L ? anchorPositionMs : 0L;
      this.anchorTimeMs = anchorTimeMs > 0L ? anchorTimeMs : System.currentTimeMillis();
      this.accentColor = accentColor;
   }

   public MusicTrack(
      String title,
      String artist,
      String appName,
      boolean appOpen,
      boolean playing,
      Identifier coverTexture,
      List<String> lyrics,
      long durationMs,
      int accentColor
   ) {
      this(title, artist, appName, appOpen, playing, coverTexture, lyrics, Collections.emptyList(), durationMs, 0L, System.currentTimeMillis(), accentColor);
   }

   public static MusicTrack empty() {
      return new MusicTrack("", "", "", false, false, null, Collections.emptyList(), Collections.emptyList(), 0L, 0L, System.currentTimeMillis(), -14829228);
   }

   public String getTitle() {
      return this.title;
   }

   public String getArtist() {
      return this.artist;
   }

   public String getAppName() {
      return this.appName;
   }

   public boolean isAppOpen() {
      return this.appOpen;
   }

   public boolean isPlaying() {
      return this.playing;
   }

   public Identifier getCoverTexture() {
      return this.coverTexture;
   }

   public List<String> getLyrics() {
      return this.lyrics;
   }

   public List<MusicTrack.TimedLyric> getTimedLyrics() {
      return this.timedLyrics;
   }

   public boolean hasLyrics() {
      return this.lyrics != null && !this.lyrics.isEmpty() || this.timedLyrics != null && !this.timedLyrics.isEmpty();
   }

   public boolean hasText() {
      return !this.title.isEmpty() || !this.artist.isEmpty();
   }

   public long getAnchorPositionMs() {
      return this.anchorPositionMs;
   }

   public long getAnchorTimeMs() {
      return this.anchorTimeMs;
   }

   public long getStartTime() {
      return this.anchorTimeMs;
   }

   public long getPositionMs() {
      return this.getCurrentPositionMs();
   }

   public long getDurationMs() {
      return this.durationMs;
   }

   public int getAccentColor() {
      return this.accentColor;
   }

   public long getCurrentPositionMs() {
      if (!this.playing) {
         return this.anchorPositionMs;
      } else {
         long elapsed = System.currentTimeMillis() - this.anchorTimeMs;
         long current = this.anchorPositionMs + elapsed;
         return this.durationMs > 0L ? Math.min(this.durationMs, Math.max(0L, current)) : Math.max(0L, current);
      }
   }

   public float getEstimatedProgress() {
      if (this.playing && this.durationMs > 0L) {
         long current = this.getCurrentPositionMs();
         float progress = (float)current / (float)this.durationMs;
         return Math.max(0.0F, Math.min(1.0F, progress));
      } else {
         return 0.0F;
      }
   }

   public String getCurrentLyricLine() {
      if (this.timedLyrics != null && !this.timedLyrics.isEmpty()) {
         long pos = this.getCurrentPositionMs();
         String current = "";

         for (MusicTrack.TimedLyric tl : this.timedLyrics) {
            if (tl.timeMs() > pos) {
               break;
            }

            if (!tl.text().trim().isEmpty()) {
               current = tl.text();
            }
         }

         return current;
      } else if (this.lyrics != null && !this.lyrics.isEmpty()) {
         int idx = (int)(this.getCurrentPositionMs() / 4000L % this.lyrics.size());
         return this.lyrics.get(idx);
      } else {
         return "";
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof MusicTrack that)
            ? false
            : this.appOpen == that.appOpen
               && this.playing == that.playing
               && this.title.equals(that.title)
               && this.artist.equals(that.artist)
               && this.appName.equals(that.appName);
      }
   }

   @Override
   public int hashCode() {
      int result = this.title.hashCode();
      result = 31 * result + this.artist.hashCode();
      result = 31 * result + this.appName.hashCode();
      result = 31 * result + (this.appOpen ? 1 : 0);
      return 31 * result + (this.playing ? 1 : 0);
   }

   public record TimedLyric(long timeMs, String text) {
   }
}
