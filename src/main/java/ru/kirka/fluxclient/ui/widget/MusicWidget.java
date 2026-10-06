package ru.kirka.fluxclient.ui.widget;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.feature.impl.hud.Music;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.Fonts;
import ru.kirka.fluxclient.render.ScissorUtil;
import ru.kirka.fluxclient.ui.element.DragInfo;
import ru.kirka.fluxclient.util.MathUtil;
import ru.kirka.fluxclient.util.music.MusicTrack;
import ru.kirka.fluxclient.util.music.MusicTracker;

public class MusicWidget extends Widget implements Interface {
   private final AnimationUtil appearAnim = new AnimationUtil();
   private final AnimationUtil contentAnim = new AnimationUtil();
   private final AnimationUtil eqPlayingAnim = new AnimationUtil();
   private float currentWidth = 68.0F;
   private float currentHeight = 18.0F;
   private float widthVelocity = 0.0F;
   private float heightVelocity = 0.0F;
   private String lastSongTitle = "";
   private String prevSongTitle = "";
   private long songChangeTimestamp = 0L;
   private Identifier lastCoverTexture = null;
   private Identifier prevCoverTexture = null;
   private long coverChangeTimestamp = 0L;
   private String currentSubtitle = "";
   private String previousSubtitle = "";
   private long lyricChangeTimestamp = 0L;

   public MusicWidget() {
      super(new DragInfo("Музыкальный остров", 0.0F, 6.0F, 180.0F, 26.0F));
      this.j().setWidget(this);
      this.j().setDragStatus(2);
   }

   @Override
   public void a(GlobalEvent event) {
      Music musicModule = this.getMusicModule();
      boolean moduleActive = musicModule != null && musicModule.isEnabled();
      MusicTrack track = this.getDisplayTrack(musicModule);
      boolean inChat = mc.currentScreen instanceof ChatScreen;
      boolean testMode = musicModule != null && musicModule.testMode.get();
      boolean shouldShow = moduleActive && (testMode || inChat || track != null && (track.isAppOpen() || track.isPlaying()));
      this.appearAnim.a(shouldShow);
      this.d().a(shouldShow);
      boolean hasContent = shouldShow && (testMode || track != null && track.hasText() || inChat && (track == null || !track.isAppOpen()));
      this.contentAnim.a(hasContent);
      if (track != null) {
         this.eqPlayingAnim.a(track.isPlaying());
         if (!track.getTitle().equals(this.lastSongTitle)) {
            this.prevSongTitle = this.lastSongTitle;
            this.lastSongTitle = track.getTitle();
            this.songChangeTimestamp = System.currentTimeMillis();
         }

         Identifier trackCover = track.getCoverTexture();
         if (trackCover != null && !trackCover.equals(this.lastCoverTexture) || trackCover == null && this.lastCoverTexture != null) {
            this.prevCoverTexture = this.lastCoverTexture;
            this.lastCoverTexture = trackCover;
            this.coverChangeTimestamp = System.currentTimeMillis();
         }

         String activeSub = this.getSubtitleText(track, musicModule);
         if (!activeSub.equals(this.currentSubtitle)) {
            this.previousSubtitle = this.currentSubtitle;
            this.currentSubtitle = activeSub;
            this.lyricChangeTimestamp = System.currentTimeMillis();
         }
      } else {
         this.eqPlayingAnim.a(false);
      }

      super.a(event);
   }

   @Override
   public void a(DrawEvent event) {
      Music musicModule = this.getMusicModule();
      if (musicModule == null || musicModule.isEnabled()) {
         float delta = event.getTickDelta();
         this.appearAnim.a(0.0F, 1.0F, 0.28F, EasingList.s, delta);
         this.contentAnim.a(0.0F, 1.0F, 0.25F, EasingList.h, delta);
         this.eqPlayingAnim.a(0.0F, 1.0F, 0.3F, EasingList.h, delta);
         this.d().a(0.0F, 1.0F, 0.28F, EasingList.g, delta);
         float anim = this.appearAnim.c();
         if (!(anim <= 0.001F)) {
            MusicTrack activeTrack = this.getDisplayTrack(musicModule);
            boolean isExpanded = this.contentAnim.c() > 0.05F;
            float scaleFactor = musicModule != null ? musicModule.scale.get() : 1.0F;
            float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
            float titleW = Fonts.d.a(activeTrack.getTitle(), 6.5F * scaleFactor);
            String subText = this.getSubtitleText(activeTrack, musicModule);
            if (!subText.equals(this.currentSubtitle)) {
               this.previousSubtitle = this.currentSubtitle;
               this.currentSubtitle = subText;
               this.lyricChangeTimestamp = System.currentTimeMillis();
            }

            if (!activeTrack.getTitle().equals(this.lastSongTitle)) {
               this.prevSongTitle = this.lastSongTitle;
               this.lastSongTitle = activeTrack.getTitle();
               this.songChangeTimestamp = System.currentTimeMillis();
            }

            Identifier curCover = activeTrack.getCoverTexture();
            if (curCover != null && !curCover.equals(this.lastCoverTexture) || curCover == null && this.lastCoverTexture != null) {
               this.prevCoverTexture = this.lastCoverTexture;
               this.lastCoverTexture = curCover;
               this.coverChangeTimestamp = System.currentTimeMillis();
            }

            float subW = Fonts.e.a(subText, 5.2F * scaleFactor);
            float textMaxW = Math.max(titleW, subW);
            float targetExpandedW = MathHelper.clamp(24.0F + textMaxW + 8.0F + 18.0F + 18.0F, 170.0F, 290.0F) * scaleFactor;
            float targetCompactW = 68.0F * scaleFactor;
            float targetW = MathUtil.lerp(targetCompactW, targetExpandedW, this.contentAnim.c());
            float targetH = MathUtil.lerp(18.0F, 26.0F, this.contentAnim.c()) * scaleFactor;
            float dt = MathHelper.clamp(mc.getRenderTickCounter().getLastFrameDuration(), 0.001F, 0.045F);
            float widthDiff = targetW - this.currentWidth;
            this.widthVelocity += widthDiff * 180.0F * dt;
            this.widthVelocity = this.widthVelocity * (float)Math.pow(0.82F, dt * 60.0F);
            this.currentWidth = this.currentWidth + this.widthVelocity * dt;
            float heightDiff = targetH - this.currentHeight;
            this.heightVelocity += heightDiff * 210.0F * dt;
            this.heightVelocity = this.heightVelocity * (float)Math.pow(0.8F, dt * 60.0F);
            this.currentHeight = this.currentHeight + this.heightVelocity * dt;
            float jellyCompression = MathHelper.clamp(-this.widthVelocity * 0.0022F, -1.0F, 1.0F) * scaleFactor;
            float renderHeight = Math.max(14.0F * scaleFactor, this.currentHeight + jellyCompression);
            float radius = renderHeight * 0.5F;
            float targetY = 6.0F;
            float offscreenY = -renderHeight - 10.0F;
            float currentY = MathUtil.lerp(offscreenY, targetY, anim);
            float currentX = (screenW - this.currentWidth) * 0.5F;
            this.j().setX(currentX);
            this.j().setY(currentY);
            this.j().setWidth(this.currentWidth);
            this.j().setHeight(renderHeight);
            MatrixStack matrices = event.getMatrixStack();
            Draw2DProcessor draw = event.getDraw2DProcessor();
            this.drawIslandBackground(event, currentX, currentY, this.currentWidth, renderHeight, radius, anim, activeTrack, musicModule);
            float leftAreaX = currentX + radius * 0.75F;
            float leftCenterY = currentY + renderHeight * 0.5F;
            boolean showWaves = musicModule == null || musicModule.showVisualizer.get();
            if (showWaves) {
               this.drawEqualizerWaves(matrices, draw, leftAreaX, leftCenterY, activeTrack, anim, scaleFactor);
            }

            boolean showCover = musicModule == null || musicModule.showCover.get();
            float coverSize = MathUtil.lerp(12.0F, 18.0F, this.contentAnim.c()) * scaleFactor;
            float coverX = currentX + this.currentWidth - radius * 0.75F - coverSize;
            float coverY = currentY + (renderHeight - coverSize) * 0.5F;
            if (showCover) {
               this.drawCoverOrLogo(matrices, draw, coverX, coverY, coverSize, activeTrack, anim, scaleFactor);
            }

            if (isExpanded) {
               float textStartX = leftAreaX + 14.0F * scaleFactor + 6.0F;
               float textEndX = coverX - 6.0F;
               float availableTextW = Math.max(20.0F, textEndX - textStartX);
               this.drawCenterTypography(
                  matrices, textStartX, currentY, availableTextW, renderHeight, activeTrack, subText, anim * this.contentAnim.c(), scaleFactor
               );
            }

            boolean showProgress = isExpanded && (musicModule == null || musicModule.showProgress.get());
            if (showProgress) {
               this.drawProgressBar(
                  matrices,
                  draw,
                  leftAreaX + 14.0F * scaleFactor + 6.0F,
                  coverX - 6.0F,
                  currentY + renderHeight - 2.5F,
                  activeTrack,
                  anim * this.contentAnim.c()
               );
            }

            super.a(event);
         }
      }
   }

   private void drawIslandBackground(DrawEvent event, float x, float y, float width, float height, float radius, float anim, MusicTrack track, Music module) {
      Draw2DProcessor draw = event.getDraw2DProcessor();
      MatrixStack matrices = event.getMatrixStack();
      int accentColor = track.getAccentColor();
      boolean enableGlow = module == null || module.glow.get();
      boolean enableBlur = module == null || module.blur.get();
      float time = (float)(System.currentTimeMillis() % 100000L) * 0.001F;
      float breathe = 0.85F + 0.15F * (float)Math.sin(time * 3.2F);
      boolean isPlaying = track.isPlaying();
      float activeBreathe = isPlaying ? breathe : 0.85F;
      int glowCol = enableGlow ? ColorUtil.applyAlphaToColor(accentColor, 0.38F * anim * activeBreathe) : 0;
      float glowRad = enableGlow ? (8.5F + 2.5F * this.contentAnim.c()) * activeBreathe : 0.0F;
      int glassTint = ColorUtil.convertToARGB(6, 6, 9, (int)(238.0F * anim));
      if (enableBlur && draw.e() != null && !draw.e().e().isEmpty()) {
         draw.drawThemedBlurredPanel(matrices, x, y, width, height, radius, accentColor, 0.1F, glowCol, glowRad, glassTint);
      } else if (enableGlow && glowRad > 0.1F) {
         draw.a(matrices, x, y, width, height, radius, glassTint, anim, glowCol, glowRad);
      } else {
         draw.drawRoundedRect(matrices, x, y, width, height, radius, glassTint);
      }

      int outlineColor = ColorUtil.applyAlphaToColor(-1, 0.15F * anim);
      draw.drawOutline(matrices, x, y, width, height, radius, 0.65F, outlineColor);
   }

   private void drawEqualizerWaves(MatrixStack matrices, Draw2DProcessor draw, float startX, float centerY, MusicTrack track, float anim, float scale) {
      float time = (float)(System.currentTimeMillis() % 100000L) * 0.001F;
      float playFactor = this.eqPlayingAnim.c();
      float barW = 2.0F * scale;
      float gap = 1.4F * scale;
      int barCount = this.contentAnim.c() > 0.4F ? 4 : 3;
      int accent = track.getAccentColor();
      int barColor = ColorUtil.applyAlphaToColor(accent, anim);
      int barColorTop = ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(accent, -1, 0.45F), anim);
      float maxWaveHeight = (this.contentAnim.c() > 0.4F ? 12.5F : 8.5F) * scale;
      float minWaveHeight = 2.2F * scale;
      float[] baseFreqs = new float[]{4.2F, 6.5F, 5.2F, 8.0F};
      float[] phases = new float[]{0.0F, 1.3F, 2.7F, 4.1F};

      for (int i = 0; i < barCount; i++) {
         float barX = startX + i * (barW + gap);
         float freq = baseFreqs[i % 4];
         float phase = phases[i % 4];
         float wave = (float)(Math.abs(Math.sin(time * freq + phase)) * 0.72 + Math.abs(Math.cos(time * 2.4F + phase * 0.6F)) * 0.28);
         float activeH = minWaveHeight + (maxWaveHeight - minWaveHeight) * wave;
         float h = MathUtil.lerp(minWaveHeight, activeH, playFactor);
         float barY = centerY - h * 0.5F;
         draw.drawGradientRect(matrices, barX, barY, barW, h, barW * 0.5F, barColorTop, barColorTop, barColor, barColor);
         if (playFactor > 0.3F) {
            int capCol = ColorUtil.applyAlphaToColor(-1, 0.5F * anim * playFactor);
            draw.drawRoundedRect(matrices, barX, barY, barW, 1.0F * scale, 0.5F, capCol);
         }
      }
   }

   private void drawCoverOrLogo(MatrixStack matrices, Draw2DProcessor draw, float x, float y, float size, MusicTrack track, float anim, float scale) {
      float roundRadius = 3.6F * scale;
      float cx = x + size * 0.5F;
      float cy = y + size * 0.5F;
      long now = System.currentTimeMillis();
      float elapsedTrack = (float)(now - this.songChangeTimestamp) / 380.0F;
      float trackPopProgress = MathHelper.clamp(elapsedTrack, 0.0F, 1.0F);
      float trackPop = MathUtil.lerp(0.7F, 1.0F, EasingList.s.ease(trackPopProgress));
      float playFactor = this.eqPlayingAnim.c();
      float playScale = MathUtil.lerp(0.88F, 1.0F, playFactor);
      float playAlpha = MathUtil.lerp(0.72F, 1.0F, playFactor);
      float breathe = 1.0F + 0.024F * playFactor * (float)Math.sin(now * 0.0035);
      float finalScale = trackPop * playScale * breathe;
      matrices.push();
      matrices.translate(cx, cy, 0.0F);
      matrices.scale(finalScale, finalScale, 1.0F);
      matrices.translate(-cx, -cy, 0.0F);
      int coverGlow = ColorUtil.applyAlphaToColor(track.getAccentColor(), 0.35F * anim * playFactor * breathe);
      draw.drawRoundedRect(matrices, x - 2.0F, y - 2.0F, size + 4.0F, size + 4.0F, roundRadius + 2.0F, coverGlow);
      Identifier curCover = track.getCoverTexture();
      float crossfadeTime = (float)(now - this.coverChangeTimestamp) / 320.0F;
      float crossfadeProgress = MathHelper.clamp(crossfadeTime, 0.0F, 1.0F);
      if (this.prevCoverTexture != null && !this.prevCoverTexture.equals(curCover) && crossfadeProgress < 1.0F) {
         float prevAlpha = (1.0F - crossfadeProgress) * anim * playAlpha;
         draw.drawTexture(matrices, this.prevCoverTexture, x, y, size, size, roundRadius, ColorUtil.applyAlphaToColor(-1, prevAlpha));
      }

      float curAlpha = (this.prevCoverTexture != null && !this.prevCoverTexture.equals(curCover) ? crossfadeProgress : 1.0F) * anim * playAlpha;
      if (curCover != null) {
         draw.drawTexture(matrices, curCover, x, y, size, size, roundRadius, ColorUtil.applyAlphaToColor(-1, curAlpha));
      } else {
         int accent = track.getAccentColor();
         int topCol = ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(accent, -14934746, 0.4F), curAlpha);
         int botCol = ColorUtil.applyAlphaToColor(-15724266, curAlpha);
         draw.drawGradientRect(matrices, x, y, size, size, roundRadius, topCol, topCol, botCol, botCol);
         this.drawMusicalNoteIcon(draw, matrices, cx, cy, scale, curAlpha);
      }

      draw.drawOutline(matrices, x, y, size, size, roundRadius, 0.55F, ColorUtil.applyAlphaToColor(-1, 0.22F * anim));
      draw.drawRoundedRect(matrices, x + 1.0F, y + 0.8F, size - 2.0F, 1.0F * scale, 0.5F, ColorUtil.applyAlphaToColor(-1, 0.16F * anim));
      matrices.pop();
   }

   private void drawMusicalNoteIcon(Draw2DProcessor draw, MatrixStack matrices, float cx, float cy, float scale, float alpha) {
      int noteColor = ColorUtil.applyAlphaToColor(-1, 0.85F * alpha);
      float r = 1.35F * scale;
      float leftX = cx - 2.2F * scale;
      float rightX = cx + 2.0F * scale;
      float bottomY = cy + 2.2F * scale;
      float topY = cy - 2.8F * scale;
      float stemW = 1.0F * scale;
      float beamH = 1.35F * scale;
      draw.drawRoundedRect(matrices, leftX - r, bottomY - r, r * 2.0F, r * 1.7F, r * 0.8F, noteColor);
      draw.drawRoundedRect(matrices, rightX - r, bottomY - 1.0F * scale - r, r * 2.0F, r * 1.7F, r * 0.8F, noteColor);
      draw.drawRoundedRect(matrices, leftX + r - stemW, topY + beamH, stemW, bottomY - topY - beamH, 0.4F, noteColor);
      draw.drawRoundedRect(matrices, rightX + r - stemW, topY, stemW, bottomY - 1.0F * scale - topY, 0.4F, noteColor);
      draw.drawRoundedRect(matrices, leftX + r - stemW, topY, rightX - leftX + stemW, beamH, 0.5F, noteColor);
   }

   private void drawCenterTypography(
      MatrixStack matrices, float startX, float islandY, float availableW, float islandH, MusicTrack track, String subtitle, float alpha, float scale
   ) {
      if (!(availableW <= 10.0F) && !(alpha <= 0.01F)) {
         long now = System.currentTimeMillis();
         float titleSize = 6.4F * scale;
         float subSize = 5.0F * scale;
         float titleY = islandY + 4.5F * scale;
         float subY = islandY + 12.8F * scale;
         String titleText = track.getTitle();
         if (titleText.isEmpty()) {
            titleText = track.getAppName();
         }

         float elapsedTrack = (float)(now - this.songChangeTimestamp) / 380.0F;
         float trackChangeProgress = MathHelper.clamp(elapsedTrack, 0.0F, 1.0F);
         float trackChangeEase = EasingList.g.ease(trackChangeProgress);
         ScissorUtil.pushScissor(matrices, startX, islandY, availableW, islandH);
         if (trackChangeProgress < 1.0F && !this.prevSongTitle.isEmpty() && !this.prevSongTitle.equals(titleText)) {
            float prevAlpha = alpha * (1.0F - trackChangeEase);
            float prevY = titleY - trackChangeEase * 5.0F * scale;
            Fonts.d.a(matrices, this.prevSongTitle, startX, prevY, titleSize, ColorUtil.applyAlphaToColor(-1, prevAlpha));
            float curAlpha = alpha * trackChangeEase;
            float curY = titleY + (1.0F - trackChangeEase) * 5.0F * scale;
            Fonts.d.a(matrices, titleText, startX, curY, titleSize, ColorUtil.applyAlphaToColor(-1, curAlpha));
         } else {
            float measuredTitleW = Fonts.d.a(titleText, titleSize);
            if (measuredTitleW > availableW) {
               float overflow = measuredTitleW - availableW + 4.0F;
               float scrollProgress = (float)(Math.sin(now * 0.0016) * 0.5 + 0.5);
               float scrollOffset = scrollProgress * overflow;
               Fonts.d.a(matrices, titleText, startX - scrollOffset, titleY, titleSize, ColorUtil.applyAlphaToColor(-1, alpha));
            } else {
               Fonts.d.a(matrices, titleText, startX, titleY, titleSize, ColorUtil.applyAlphaToColor(-1, alpha));
            }
         }

         ScissorUtil.popScissor(matrices);
         boolean isLyrics = track.hasLyrics() && !subtitle.equals(track.getArtist()) && !subtitle.equals(track.getAppName());
         int defaultColor = isLyrics ? -1184779 : -6381912;
         float elapsedLyric = (float)(now - this.lyricChangeTimestamp) / 340.0F;
         float lyricProg = MathHelper.clamp(elapsedLyric, 0.0F, 1.0F);
         float lyricEase = EasingList.g.ease(lyricProg);
         ScissorUtil.pushScissor(matrices, startX, islandY, availableW, islandH);
         if (lyricProg < 1.0F && !this.previousSubtitle.isEmpty() && !this.previousSubtitle.equals(subtitle)) {
            float prevAlpha = alpha * (1.0F - lyricEase);
            float prevY = subY - lyricEase * 6.5F * scale;
            int prevColor = ColorUtil.applyAlphaToColor(defaultColor, prevAlpha);
            Fonts.e.a(matrices, this.previousSubtitle, startX, prevY, subSize, prevColor);
            float curAlpha = alpha * lyricEase;
            float curY = subY + (1.0F - lyricEase) * 6.5F * scale;
            int highlightColor = ColorUtil.lerpColor(defaultColor, -1, (1.0F - lyricEase) * 0.7F);
            int curColor = ColorUtil.applyAlphaToColor(highlightColor, curAlpha);
            Fonts.e.a(matrices, subtitle, startX, curY, subSize, curColor);
         } else {
            float measuredSubW = Fonts.e.a(subtitle, subSize);
            int subColor = ColorUtil.applyAlphaToColor(defaultColor, alpha);
            if (measuredSubW > availableW) {
               float overflow = measuredSubW - availableW + 4.0F;
               long lyricAge = now - this.lyricChangeTimestamp;
               float scrollOffset = 0.0F;
               if (lyricAge > 1200L) {
                  float scrollProgress = (float)(Math.sin((lyricAge - 1200L) * 0.0016) * 0.5 + 0.5);
                  scrollOffset = scrollProgress * overflow;
               }

               Fonts.e.a(matrices, subtitle, startX - scrollOffset, subY, subSize, subColor);
            } else {
               Fonts.e.a(matrices, subtitle, startX, subY, subSize, subColor);
            }
         }

         ScissorUtil.popScissor(matrices);
      }
   }

   private void drawProgressBar(MatrixStack matrices, Draw2DProcessor draw, float startX, float endX, float barY, MusicTrack track, float alpha) {
      float barW = Math.max(10.0F, endX - startX);
      float progress = track.getEstimatedProgress();
      int trackBg = ColorUtil.applyAlphaToColor(-1, 0.12F * alpha);
      int trackFill = ColorUtil.applyAlphaToColor(track.getAccentColor(), 0.88F * alpha);
      draw.drawRoundedRect(matrices, startX, barY, barW, 1.0F, 0.5F, trackBg);
      if (progress > 0.005F) {
         float fillW = barW * progress;
         draw.drawRoundedRect(matrices, startX, barY, fillW, 1.0F, 0.5F, trackFill);
         if (track.isPlaying()) {
            float tipPulse = 0.8F + 0.2F * (float)Math.sin(System.currentTimeMillis() * 0.004);
            int tipColor = ColorUtil.applyAlphaToColor(ColorUtil.lerpColor(track.getAccentColor(), -1, 0.5F), alpha * tipPulse);
            draw.drawRoundedRect(matrices, startX + fillW - 1.0F, barY - 0.5F, 2.0F, 2.0F, 1.0F, tipColor);
         }
      }
   }

   private String getSubtitleText(MusicTrack track, Music module) {
      if (module != null && module.showLyrics.get() && track.hasLyrics()) {
         String lyric = track.getCurrentLyricLine();
         if (lyric != null && !lyric.trim().isEmpty()) {
            return lyric.trim();
         }
      }

      return !track.getArtist().isEmpty() ? track.getArtist() : track.getAppName();
   }

   private MusicTrack getDisplayTrack(Music module) {
      boolean inChat = mc.currentScreen instanceof ChatScreen;
      boolean testMode = module != null && module.testMode.get();
      MusicTrack live = MusicTracker.getInstance().getCurrentTrack();
      return !testMode && (!inChat || live.isPlaying() || live.isAppOpen()) ? live : MusicTracker.getInstance().getPreviewTrack();
   }

   private Music getMusicModule() {
      return FluxClient.getInstance() != null && FluxClient.getInstance().getModuleManager() != null
         ? FluxClient.getInstance().getModuleManager().getModule(Music.class)
         : null;
   }
}
