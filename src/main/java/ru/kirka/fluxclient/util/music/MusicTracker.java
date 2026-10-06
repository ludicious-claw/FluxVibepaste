package ru.kirka.fluxclient.util.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessHandle.Info;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public class MusicTracker {
   private static final MusicTracker INSTANCE = new MusicTracker();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, "FluxClient-MusicTracker");
      thread.setDaemon(true);
      return thread;
   });
   private final ExecutorService networkExecutor = Executors.newFixedThreadPool(3, r -> {
      Thread thread = new Thread(r, "FluxClient-MusicNetwork");
      thread.setDaemon(true);
      return thread;
   });
   private final Map<String, Identifier> coverCache = new ConcurrentHashMap<>();
   private final Map<String, MusicTracker.LyricsData> lyricsCache = new ConcurrentHashMap<>();
   private final Set<String> registeredCoverFiles = ConcurrentHashMap.newKeySet();
   private volatile MusicTrack currentTrack = MusicTrack.empty();
   private volatile MusicTrack gsmtcTrack = null;
   private volatile boolean running = false;
   private volatile long lastDaemonCheck = 0L;
   private String lastQueryKey = "";
   private Process gsmtcProcess = null;
   private Thread gsmtcReaderThread = null;
   private final long previewStartTime = System.currentTimeMillis();
   private String lastTrackKey = "";
   private boolean lastWasPlaying = false;
   private long currentAnchorPosMs = 0L;
   private long currentAnchorTimeMs = System.currentTimeMillis();

   private MusicTracker() {
   }

   private synchronized MusicTracker.TrackAnchor syncAnchor(String title, String artist, boolean isPlaying, long reportedPosMs, long durationMs) {
      long now = System.currentTimeMillis();
      String trackKey = (artist + " - " + title).toLowerCase(Locale.ROOT).trim();
      if (trackKey.isEmpty() || trackKey.equals("-")) {
         this.lastTrackKey = "";
         this.lastWasPlaying = false;
         this.currentAnchorPosMs = 0L;
         this.currentAnchorTimeMs = now;
         return new MusicTracker.TrackAnchor(0L, now);
      } else if (!trackKey.equals(this.lastTrackKey)) {
         this.lastTrackKey = trackKey;
         this.lastWasPlaying = isPlaying;
         this.currentAnchorPosMs = Math.max(0L, reportedPosMs);
         this.currentAnchorTimeMs = now;
         return new MusicTracker.TrackAnchor(this.currentAnchorPosMs, this.currentAnchorTimeMs);
      } else if (!this.lastWasPlaying && isPlaying) {
         this.lastWasPlaying = true;
         this.currentAnchorPosMs = Math.max(0L, reportedPosMs);
         this.currentAnchorTimeMs = now;
         return new MusicTracker.TrackAnchor(this.currentAnchorPosMs, this.currentAnchorTimeMs);
      } else if (this.lastWasPlaying && !isPlaying) {
         this.lastWasPlaying = false;
         this.currentAnchorPosMs = Math.max(0L, reportedPosMs);
         this.currentAnchorTimeMs = now;
         return new MusicTracker.TrackAnchor(this.currentAnchorPosMs, this.currentAnchorTimeMs);
      } else {
         if (isPlaying) {
            long currentCalculatedPos = this.currentAnchorPosMs + (now - this.currentAnchorTimeMs);
            long discrepancy = Math.abs(reportedPosMs - currentCalculatedPos);
            if (discrepancy > 1500L) {
               this.currentAnchorPosMs = reportedPosMs;
               this.currentAnchorTimeMs = now;
            } else if (discrepancy > 200L) {
               this.currentAnchorPosMs = (this.currentAnchorPosMs + reportedPosMs) / 2L;
               this.currentAnchorTimeMs = now;
            }
         }

         return new MusicTracker.TrackAnchor(this.currentAnchorPosMs, this.currentAnchorTimeMs);
      }
   }

   public static MusicTracker getInstance() {
      return INSTANCE;
   }

   public synchronized void start() {
      if (!this.running) {
         this.running = true;
         boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
         if (isWindows) {
            this.startGsmtcDaemon();
         }

         this.scheduler.scheduleWithFixedDelay(this::pollSystemMedia, 0L, 300L, TimeUnit.MILLISECONDS);
      }
   }

   public synchronized void stop() {
      this.running = false;
      this.stopGsmtcDaemon();
      this.scheduler.shutdownNow();
      this.networkExecutor.shutdownNow();
   }

   public MusicTrack getCurrentTrack() {
      return this.currentTrack;
   }

   public MusicTrack getPreviewTrack() {
      List<MusicTrack.TimedLyric> previewTimed = List.of(
         new MusicTrack.TimedLyric(0L, "И я готов распять свои чувства"),
         new MusicTrack.TimedLyric(4000L, "Но в этой темноте снова пусто"),
         new MusicTrack.TimedLyric(8000L, "Мне так больно дышать, когда ты рядом"),
         new MusicTrack.TimedLyric(12000L, "Провожая меня холодным взглядом")
      );
      List<String> previewPlain = previewTimed.stream().map(MusicTrack.TimedLyric::text).toList();
      long previewElapsed = (System.currentTimeMillis() - this.previewStartTime) % 16000L;
      return new MusicTrack(
         "распять",
         "greyrock, tewiq, madk1d",
         "Яндекс Музыка",
         true,
         true,
         this.getCachedCover("greyrock", "распять"),
         previewPlain,
         previewTimed,
         200000L,
         previewElapsed,
         System.currentTimeMillis(),
         -52429
      );
   }

   private synchronized void startGsmtcDaemon() {
      this.stopGsmtcDaemon();

      try {
         File tempScript = File.createTempFile("flux_gsmtc_", ".ps1");
         tempScript.deleteOnExit();

         try (InputStream in = this.getClass().getResourceAsStream("/assets/fluxclient/scripts/gsmtc_worker.ps1")) {
            if (in != null) {
               Files.copy(in, tempScript.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
               this.writeEmbeddedScript(tempScript);
            }
         } catch (Throwable var7) {
            this.writeEmbeddedScript(tempScript);
         }

         ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-NoProfile", "-File", tempScript.getAbsolutePath());
         pb.redirectErrorStream(true);
         this.gsmtcProcess = pb.start();
         this.gsmtcReaderThread = new Thread(() -> {
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.gsmtcProcess.getInputStream(), StandardCharsets.UTF_8))) {
               while (this.running && (line = reader.readLine()) != null) {
                  this.handleGsmtcOutput(line.trim());
               }
            } catch (Throwable var6) {
            }
         }, "FluxClient-GSMTC-Reader");
         this.gsmtcReaderThread.setDaemon(true);
         this.gsmtcReaderThread.start();
      } catch (Throwable var8) {
         FluxLogger.warn("Failed to start GSMTC daemon: " + var8.getMessage());
      }
   }

   private void writeEmbeddedScript(File file) {
      try {
         String script = "$ErrorActionPreference = 'SilentlyContinue'\n[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n$OutputEncoding = [System.Text.Encoding]::UTF8\n$runtimeDll = if ([Environment]::Is64BitProcess) {\n    \"C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\System.Runtime.WindowsRuntime.dll\"\n} else {\n    \"C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\System.Runtime.WindowsRuntime.dll\"\n}\nif (Test-Path $runtimeDll) {\n    Add-Type -Path $runtimeDll -ErrorAction SilentlyContinue\n}\n[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null\n[Windows.Media.Control.GlobalSystemMediaTransportControlsSession, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null\n$asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {\n    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'\n} | Select-Object -First 1\n$asStreamMethod = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object {\n    $_.Name -eq 'AsStream' -and $_.GetParameters().Count -eq 1\n} | Select-Object -First 1\n$coverDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), 'fluxclient_covers')\nif (-not (Test-Path $coverDir)) {\n    try { New-Item -ItemType Directory -Path $coverDir -Force | Out-Null } catch {}\n}\nfunction Await-WinRT($asyncOp, $type) {\n    if ($asyncOp -eq $null -or $asTaskGeneric -eq $null) { return $null }\n    try {\n        $method = $asTaskGeneric.MakeGenericMethod($type)\n        $task = $method.Invoke($null, @($asyncOp))\n        if ($task.Wait(1200)) { return $task.Result }\n    } catch {}\n    return $null\n}\nfunction Get-Manager {\n    try {\n        $op = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()\n        return Await-WinRT $op ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n    } catch { return $null }\n}\n$mgr = Get-Manager\nif ($mgr -eq $null) { Write-Output \"STATUS|NO_MGR\"; [Console]::Out.Flush() }\nelse { Write-Output \"STATUS|READY\"; [Console]::Out.Flush() }\n$lastOut = \"\"\n$lastCoverTrack = \"\"\n$currentCoverPath = \"\"\nwhile ($true) {\n    try {\n        if ($mgr -eq $null) { $mgr = Get-Manager }\n        $bestSession = $null\n        $bestStatus = \"Unknown\"\n        $bestProps = $null\n        if ($mgr -ne $null) {\n            $sessions = $null\n            try { $sessions = $mgr.GetSessions() } catch { $mgr = $null }\n            if ($sessions -ne $null -and $sessions.Count -gt 0) {\n                foreach ($s in $sessions) {\n                    try {\n                        $info = $s.GetPlaybackInfo()\n                        $st = if ($info -ne $null) { $info.PlaybackStatus.ToString() } else { \"Unknown\" }\n                        $pr = Await-WinRT ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n                        if ($pr -ne $null -and -not [string]::IsNullOrWhiteSpace($pr.Title)) {\n                            if ($st -eq \"Playing\") {\n                                $bestSession = $s\n                                $bestStatus = $st\n                                $bestProps = $pr\n                                break\n                            } elseif ($bestSession -eq $null) {\n                                $bestSession = $s\n                                $bestStatus = $st\n                                $bestProps = $pr\n                            }\n                        }\n                    } catch {}\n                }\n                if ($bestSession -eq $null) {\n                    try {\n                        $curr = $mgr.GetCurrentSession()\n                        if ($curr -ne $null) {\n                            $bestSession = $curr\n                            $info = $curr.GetPlaybackInfo()\n                            $bestStatus = if ($info -ne $null) { $info.PlaybackStatus.ToString() } else { \"Unknown\" }\n                            $bestProps = Await-WinRT ($curr.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n                        }\n                    } catch {}\n                }\n            }\n        }\n        if ($bestSession -ne $null) {\n            $title = if ($bestProps -ne $null -and $bestProps.Title -ne $null) { $bestProps.Title } else { \"\" }\n            $artist = if ($bestProps -ne $null -and $bestProps.Artist -ne $null) { $bestProps.Artist } else { \"\" }\n            $app = if ($bestSession.SourceAppId -ne $null) { $bestSession.SourceAppId } else { \"\" }\n\n            $trackKey = ($title + \"|||\" + $artist)\n            if ($trackKey -ne $lastCoverTrack -and -not [string]::IsNullOrWhiteSpace($title)) {\n                $lastCoverTrack = $trackKey\n                $currentCoverPath = \"\"\n                if ($bestProps -ne $null -and $bestProps.Thumbnail -ne $null -and $asStreamMethod -ne $null) {\n                    try {\n                        $stream = Await-WinRT ($bestProps.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\n                        if ($stream -ne $null) {\n                            $hash = [Math]::Abs($trackKey.GetHashCode())\n                            $covFile = [System.IO.Path]::Combine($coverDir, \"cov_\" + $hash + \".jpg\")\n                            $netStream = $asStreamMethod.Invoke($null, @($stream))\n                            $fs = [System.IO.File]::Create($covFile)\n                            $netStream.CopyTo($fs)\n                            $fs.Dispose()\n                            $netStream.Dispose()\n                            $currentCoverPath = $covFile\n                        }\n                    } catch {}\n                }\n            } elseif ([string]::IsNullOrEmpty($currentCoverPath) -and $bestProps -ne $null -and $bestProps.Thumbnail -ne $null -and -not [string]::IsNullOrWhiteSpace($title) -and $asStreamMethod -ne $null) {\n                try {\n                    $stream = Await-WinRT ($bestProps.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\n                    if ($stream -ne $null) {\n                        $hash = [Math]::Abs($trackKey.GetHashCode())\n                        $covFile = [System.IO.Path]::Combine($coverDir, \"cov_\" + $hash + \".jpg\")\n                        $netStream = $asStreamMethod.Invoke($null, @($stream))\n                        $fs = [System.IO.File]::Create($covFile)\n                        $netStream.CopyTo($fs)\n                        $fs.Dispose()\n                        $netStream.Dispose()\n                        $currentCoverPath = $covFile\n                    }\n                } catch {}\n            }\n\n            $posMs = 0\n            $endMs = 0\n            try {\n                $tl = $bestSession.GetTimelineProperties()\n                if ($tl -ne $null) {\n                    $basePos = if ($tl.Position -ne $null) { [long]$tl.Position.TotalMilliseconds } else { 0 }\n                    $now = [DateTimeOffset]::UtcNow\n                    $last = if ($tl.LastUpdatedTime -ne $null) { $tl.LastUpdatedTime.ToUniversalTime() } else { $now }\n                    $elapsed = if ($bestStatus -eq \"Playing\") { ($now - $last).TotalMilliseconds } else { 0 }\n                    if ($elapsed -lt 0) { $elapsed = 0 }\n                    $posMs = [long]($basePos + $elapsed)\n                    if ($tl.EndTime -ne $null) { $endMs = [long]$tl.EndTime.TotalMilliseconds }\n                    if ($endMs -gt 0 -and $posMs -gt $endMs) { $posMs = $endMs }\n                }\n            } catch {}\n            $titleB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($title))\n            $artistB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($artist))\n            $appB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($app))\n            $coverB64 = if (-not [string]::IsNullOrEmpty($currentCoverPath)) { [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($currentCoverPath)) } else { \"\" }\n            $line = \"B64TRACK|$bestStatus|$titleB64|$artistB64|$appB64|$posMs|$endMs|$coverB64\"\n            if ($line -ne $lastOut) {\n                $lastOut = $line\n                Write-Output $line\n                [Console]::Out.Flush()\n            }\n        } else {\n            $lastCoverTrack = \"\"\n            $currentCoverPath = \"\"\n            if ($lastOut -ne \"NO_TRACK\") {\n                $lastOut = \"NO_TRACK\"\n                Write-Output \"NO_TRACK\"\n                [Console]::Out.Flush()\n            }\n        }\n    } catch {}\n    Start-Sleep -Milliseconds 300\n}\n";
         Files.writeString(file.toPath(), script, StandardCharsets.UTF_8);
      } catch (Throwable var3) {
      }
   }

   private void stopGsmtcDaemon() {
      try {
         if (this.gsmtcProcess != null) {
            this.gsmtcProcess.destroyForcibly();
            this.gsmtcProcess = null;
         }
      } catch (Throwable var2) {
      }
   }

   private void handleGsmtcOutput(String line) {
      if (line.startsWith("B64TRACK|")) {
         String[] parts = line.split("\\|", -1);
         if (parts.length >= 5) {
            String status = parts[1].trim();
            String title = this.decodeBase64Safe(parts[2]);
            String artist = this.decodeBase64Safe(parts[3]);
            String app = this.decodeBase64Safe(parts[4]);
            long posMs = 0L;
            long endMs = 0L;
            if (parts.length >= 7) {
               try {
                  posMs = Long.parseLong(parts[5].trim());
                  endMs = Long.parseLong(parts[6].trim());
               } catch (Throwable var23) {
               }
            }

            boolean isPlaying = status.equalsIgnoreCase("Playing");
            boolean isAppOpen = isPlaying || status.equalsIgnoreCase("Paused") || status.equalsIgnoreCase("Opened") || status.equalsIgnoreCase("Changing");
            if (!title.isEmpty()) {
               isAppOpen = true;
            }

            String appName = this.resolveAppName(app, title);
            int accent = this.resolveAccentColor(appName);
            Identifier cover = this.getCachedCover(artist, title);
            MusicTracker.LyricsData lyricsData = this.getCachedLyrics(artist, title);
            long duration = endMs > 0L ? endMs : 210000L;
            MusicTracker.TrackAnchor anchor = this.syncAnchor(title, artist, isPlaying, posMs, duration);
            String coverPath = "";
            if (parts.length >= 8) {
               coverPath = this.decodeBase64Safe(parts[7]);
            }

            if (!coverPath.isEmpty() && !title.isEmpty()) {
               File coverFile = new File(coverPath);
               if (coverFile.exists() && coverFile.length() > 0L) {
                  Identifier existing = this.getCachedCover(artist, title);
                  if (existing == null) {
                     this.loadAndRegisterCoverFromFile(artist, title, coverFile);
                  }
               }
            }

            this.gsmtcTrack = new MusicTrack(
               title,
               artist,
               appName,
               isAppOpen,
               isPlaying,
               cover,
               lyricsData != null ? lyricsData.plain() : Collections.emptyList(),
               lyricsData != null ? lyricsData.timed() : Collections.emptyList(),
               duration,
               anchor.anchorPosMs(),
               anchor.anchorTimeMs(),
               accent
            );
            if (!title.isEmpty()) {
               String queryKey = (artist + " - " + title).toLowerCase(Locale.ROOT);
               if (!queryKey.equals(this.lastQueryKey)) {
                  this.lastQueryKey = queryKey;
                  this.fetchCoverArtAsync(artist, title);
                  this.fetchLyricsAsync(artist, title);
               }
            }
         }
      } else if (line.equals("NO_TRACK")) {
         this.gsmtcTrack = null;
      }
   }

   private String decodeBase64Safe(String b64) {
      if (b64 != null && !b64.isEmpty()) {
         try {
            byte[] bytes = Base64.getDecoder().decode(b64.trim());
            return new String(bytes, StandardCharsets.UTF_8).trim();
         } catch (Throwable var3) {
            return b64.trim();
         }
      } else {
         return "";
      }
   }

   private String resolveAppName(String appId, String title) {
      String lowerApp = appId.toLowerCase(Locale.ROOT);
      if (lowerApp.contains("spotify")) {
         return "Spotify";
      } else if (lowerApp.contains("yandex") || lowerApp.contains("яндекс")) {
         return "Яндекс Музыка";
      } else if (lowerApp.contains("apple") || lowerApp.contains("itunes")) {
         return "Apple Music";
      } else if (!lowerApp.contains("chrome")
         && !lowerApp.contains("edge")
         && !lowerApp.contains("browser")
         && !lowerApp.contains("opera")
         && !lowerApp.contains("firefox")) {
         return "Яндекс Музыка";
      } else if (title.contains("Яндекс Музыка") || title.contains("Яндекс.Музыка")) {
         return "Яндекс Музыка";
      } else if (title.contains("YouTube Music")) {
         return "YouTube Music";
      } else {
         return !title.contains("ВКонтакте") && !title.contains("VK") ? "Яндекс Музыка" : "VK Музыка";
      }
   }

   private int resolveAccentColor(String appName) {
      if (appName.contains("Spotify")) {
         return -14829228;
      } else if (appName.contains("Яндекс") || appName.contains("Yandex")) {
         return -52429;
      } else if (appName.contains("Apple")) {
         return -246716;
      } else if (appName.contains("YouTube")) {
         return -65536;
      } else {
         return appName.contains("VK") ? -16746497 : -52429;
      }
   }

   private void pollSystemMedia() {
      try {
         long now = System.currentTimeMillis();
         if (now - this.lastDaemonCheck > 3000L) {
            this.lastDaemonCheck = now;
            boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            if (isWindows && (this.gsmtcProcess == null || !this.gsmtcProcess.isAlive())) {
               this.startGsmtcDaemon();
            }
         }

         MusicTrack detected = null;
         if (this.gsmtcTrack != null && (this.gsmtcTrack.isAppOpen() || this.gsmtcTrack.isPlaying()) && !this.gsmtcTrack.getTitle().isEmpty()) {
            detected = this.gsmtcTrack;
         }

         if (detected == null || !detected.hasText()) {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            if (isWindows) {
               MusicTrack winTrack = this.scanWindowsMedia();
               if (winTrack != null && (winTrack.isAppOpen() || winTrack.isPlaying())) {
                  detected = winTrack;
               }
            }
         }

         if (detected == null || !detected.isAppOpen() && !detected.isPlaying()) {
            detected = this.scanFallbackProcesses();
         }

         if (detected == null) {
            detected = MusicTrack.empty();
         }

         if (detected.hasText()) {
            Identifier cachedCover = this.getCachedCover(detected.getArtist(), detected.getTitle());
            MusicTracker.LyricsData cachedLyrics = this.getCachedLyrics(detected.getArtist(), detected.getTitle());
            Identifier effectiveCover = cachedCover != null ? cachedCover : detected.getCoverTexture();
            List<String> effectivePlain = cachedLyrics != null ? cachedLyrics.plain() : detected.getLyrics();
            List<MusicTrack.TimedLyric> effectiveTimed = cachedLyrics != null ? cachedLyrics.timed() : detected.getTimedLyrics();
            detected = new MusicTrack(
               detected.getTitle(),
               detected.getArtist(),
               detected.getAppName(),
               detected.isAppOpen(),
               detected.isPlaying(),
               effectiveCover,
               effectivePlain,
               effectiveTimed,
               detected.getDurationMs(),
               detected.getAnchorPositionMs(),
               detected.getAnchorTimeMs(),
               detected.getAccentColor()
            );
         }

         this.currentTrack = detected;
         if (detected.hasText()) {
            String queryKey = (detected.getArtist() + " - " + detected.getTitle()).toLowerCase(Locale.ROOT);
            if (!queryKey.equals(this.lastQueryKey)) {
               this.lastQueryKey = queryKey;
               this.fetchCoverArtAsync(detected.getArtist(), detected.getTitle());
               this.fetchLyricsAsync(detected.getArtist(), detected.getTitle());
            }
         }
      } catch (Throwable var9) {
      }
   }

   private MusicTrack scanWindowsMedia() {
      List<MusicTracker.WindowInfo> windows = new ArrayList<>();

      try {
         User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
               return true;
            } else {
               char[] buffer = new char[512];
               int len = User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
               if (len > 0) {
                  String titlex = Native.toString(buffer).trim();
                  if (!titlex.isEmpty()) {
                     IntByReference pid = new IntByReference();
                     User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
                     String processName = this.getProcessName(pid.getValue());
                     windows.add(new MusicTracker.WindowInfo(titlex, processName));
                  }
               }

               return true;
            }
         }, null);
      } catch (Throwable var13) {
         return null;
      }

      for (MusicTracker.WindowInfo win : windows) {
         String proc = win.processName.toLowerCase(Locale.ROOT);
         String title = win.title;
         if (proc.contains("spotify")) {
            if (!title.equalsIgnoreCase("Spotify") && !title.equalsIgnoreCase("Spotify Free") && !title.equalsIgnoreCase("Spotify Premium")) {
               if (!title.contains(" - ") && !title.contains(" – ")) {
                  continue;
               }

               MusicTracker.ParsedSong parsed = this.parseArtistAndTitle(title);
               Identifier cover = this.getCachedCover(parsed.artist, parsed.title);
               MusicTracker.LyricsData lyricsData = this.getCachedLyrics(parsed.artist, parsed.title);
               MusicTracker.TrackAnchor anchor = this.syncAnchor(parsed.title, parsed.artist, true, 0L, 210000L);
               return new MusicTrack(
                  parsed.title,
                  parsed.artist,
                  "Spotify",
                  true,
                  true,
                  cover,
                  lyricsData != null ? lyricsData.plain() : Collections.emptyList(),
                  lyricsData != null ? lyricsData.timed() : Collections.emptyList(),
                  210000L,
                  anchor.anchorPosMs(),
                  anchor.anchorTimeMs(),
                  -14829228
               );
            }

            return new MusicTrack("", "", "Spotify", true, false, null, Collections.emptyList(), 0L, -14829228);
         }
      }

      for (MusicTracker.WindowInfo winx : windows) {
         String proc = winx.processName.toLowerCase(Locale.ROOT);
         String title = winx.title;
         boolean isYandexApp = proc.contains("yandexmusic") || proc.contains("ymm") || proc.contains("yandex.music");
         boolean isYandexTitle = title.contains("Яндекс Музыка") || title.contains("Яндекс.Музыка") || title.contains("Yandex Music");
         if (isYandexApp || isYandexTitle) {
            String cleanTitle = title.replace("— Яндекс Музыка", "")
               .replace("- Яндекс Музыка", "")
               .replace("— Яндекс.Музыка", "")
               .replace("- Яндекс.Музыка", "")
               .replace("— Yandex Music", "")
               .trim();
            if (!this.isYandexSlogan(cleanTitle)
               && !cleanTitle.equalsIgnoreCase("Яндекс Музыка")
               && !cleanTitle.equalsIgnoreCase("Яндекс.Музыка")
               && !cleanTitle.isEmpty()) {
               if (!cleanTitle.contains(" — ") && !cleanTitle.contains(" - ") && !cleanTitle.contains(" – ")) {
                  continue;
               }

               MusicTracker.ParsedSong parsed = this.parseArtistAndTitle(cleanTitle);
               if (this.isYandexSlogan(parsed.title)) {
                  return new MusicTrack("", "", "Яндекс Музыка", true, false, null, Collections.emptyList(), 0L, -52429);
               }

               Identifier cover = this.getCachedCover(parsed.artist, parsed.title);
               MusicTracker.LyricsData lyricsData = this.getCachedLyrics(parsed.artist, parsed.title);
               MusicTracker.TrackAnchor anchor = this.syncAnchor(parsed.title, parsed.artist, true, 0L, 195000L);
               return new MusicTrack(
                  parsed.title,
                  parsed.artist,
                  "Яндекс Музыка",
                  true,
                  true,
                  cover,
                  lyricsData != null ? lyricsData.plain() : Collections.emptyList(),
                  lyricsData != null ? lyricsData.timed() : Collections.emptyList(),
                  195000L,
                  anchor.anchorPosMs(),
                  anchor.anchorTimeMs(),
                  -52429
               );
            }

            return new MusicTrack("", "", "Яндекс Музыка", true, false, null, Collections.emptyList(), 0L, -52429);
         }
      }

      for (MusicTracker.WindowInfo winxx : windows) {
         String proc = winxx.processName.toLowerCase(Locale.ROOT);
         String title = winxx.title;
         if (proc.contains("applemusic") || proc.contains("itunes")) {
            if (!title.equalsIgnoreCase("Apple Music") && !title.equalsIgnoreCase("iTunes")) {
               if (!title.contains(" - ") && !title.contains(" — ")) {
                  continue;
               }

               MusicTracker.ParsedSong parsed = this.parseArtistAndTitle(title);
               Identifier cover = this.getCachedCover(parsed.artist, parsed.title);
               MusicTracker.LyricsData lyricsData = this.getCachedLyrics(parsed.artist, parsed.title);
               MusicTracker.TrackAnchor anchor = this.syncAnchor(parsed.title, parsed.artist, true, 0L, 210000L);
               return new MusicTrack(
                  parsed.title,
                  parsed.artist,
                  "Apple Music",
                  true,
                  true,
                  cover,
                  lyricsData != null ? lyricsData.plain() : Collections.emptyList(),
                  lyricsData != null ? lyricsData.timed() : Collections.emptyList(),
                  210000L,
                  anchor.anchorPosMs(),
                  anchor.anchorTimeMs(),
                  -246716
               );
            }

            return new MusicTrack("", "", "Apple Music", true, false, null, Collections.emptyList(), 0L, -246716);
         }
      }

      return null;
   }

   private boolean isYandexSlogan(String text) {
      if (text == null) {
         return false;
      } else {
         String lower = text.toLowerCase(Locale.ROOT).trim();
         return lower.contains("собираем музыку")
            || lower.contains("моя волна")
            || lower.contains("главное")
            || lower.contains("треки")
            || lower.contains("коллекция")
            || lower.contains("премьера")
            || lower.contains("чарт")
            || lower.contains("подкасты")
            || lower.contains("детям")
            || lower.equals("яндекс музыка")
            || lower.equals("яндекс.музыка");
      }
   }

   private MusicTrack scanFallbackProcesses() {
      try {
         boolean spotifyRunning = false;
         boolean yandexRunning = false;

         for (ProcessHandle handle : ProcessHandle.allProcesses().toList()) {
            String cmd = handle.info().command().orElse("").toLowerCase(Locale.ROOT);
            if (!cmd.endsWith("spotify.exe") && !cmd.contains("spotify")) {
               if (!cmd.endsWith("yandexmusic.exe") && !cmd.contains("ymm.exe") && !cmd.contains("yandex.music")) {
                  continue;
               }

               yandexRunning = true;
               break;
            }

            spotifyRunning = true;
            break;
         }

         if (spotifyRunning) {
            return new MusicTrack("", "", "Spotify", true, false, null, Collections.emptyList(), 0L, -14829228);
         }

         if (yandexRunning) {
            return new MusicTrack("", "", "Яндекс Музыка", true, false, null, Collections.emptyList(), 0L, -52429);
         }
      } catch (Throwable var6) {
      }

      return null;
   }

   private String getProcessName(int pid) {
      if (pid <= 0) {
         return "";
      } else {
         try {
            return ProcessHandle.of(pid).map(ProcessHandle::info).flatMap(Info::command).map(cmd -> {
               int idx = Math.max(cmd.lastIndexOf(47), cmd.lastIndexOf(92));
               return idx >= 0 ? cmd.substring(idx + 1) : cmd;
            }).orElse("");
         } catch (Throwable var3) {
            return "";
         }
      }
   }

   private MusicTracker.ParsedSong parseArtistAndTitle(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String clean = raw.trim();
         String[] split;
         if (clean.contains(" — ")) {
            split = clean.split(" — ", 2);
         } else if (clean.contains(" – ")) {
            split = clean.split(" – ", 2);
         } else {
            if (!clean.contains(" - ")) {
               return new MusicTracker.ParsedSong("", clean);
            }

            split = clean.split(" - ", 2);
         }

         if (split.length >= 2) {
            String p1 = split[0].trim();
            String p2 = split[1].trim();
            return new MusicTracker.ParsedSong(p1, p2);
         } else {
            return new MusicTracker.ParsedSong("", clean);
         }
      } else {
         return new MusicTracker.ParsedSong("", "");
      }
   }

   private Identifier getCachedCover(String artist, String title) {
      String key = (artist + " " + title).toLowerCase(Locale.ROOT).trim();
      Identifier id = this.coverCache.get(key);
      if (id != null) {
         return id;
      } else {
         String clean = this.cleanTitleForSearch(title);
         id = this.coverCache.get((artist + " " + clean).toLowerCase(Locale.ROOT).trim());
         return id != null ? id : this.coverCache.get(clean.toLowerCase(Locale.ROOT).trim());
      }
   }

   private MusicTracker.LyricsData getCachedLyrics(String artist, String title) {
      String key = (artist + " " + title).toLowerCase(Locale.ROOT).trim();
      MusicTracker.LyricsData d = this.lyricsCache.get(key);
      if (d != null) {
         return d;
      } else {
         String clean = this.cleanTitleForSearch(title);
         d = this.lyricsCache.get((artist + " " + clean).toLowerCase(Locale.ROOT).trim());
         return d != null ? d : this.lyricsCache.get(clean.toLowerCase(Locale.ROOT).trim());
      }
   }

   private String cleanTitleForSearch(String title) {
      if (title != null && !title.isEmpty()) {
         String s = title.replaceAll("(?i)\\s*[\\[\\(](?:feat|ft)\\.?\\s+[^\\)\\]]+[\\)\\]]", "");
         s = s.replaceAll("(?i)\\s*[\\[\\(](?:slowed|sped up|speed up|remix|edit|prod\\.?|version|bonus|official)[^\\)\\]]*[\\)\\]]", "");
         s = s.trim();
         return s.isEmpty() ? title : s;
      } else {
         return "";
      }
   }

   private void fetchCoverArtAsync(String artist, String title) {
      if (!title.isEmpty()) {
         String cacheKey = (artist + " " + title).toLowerCase(Locale.ROOT).trim();
         if (!this.coverCache.containsKey(cacheKey)) {
            this.networkExecutor.submit(() -> {
               try {
                  String cleanTitle = this.cleanTitleForSearch(title);
                  String deezerQuery = URLEncoder.encode(artist + " " + cleanTitle, StandardCharsets.UTF_8);
                  String coverImgUrl = this.fetchDeezerCover("https://api.deezer.com/search?q=" + deezerQuery);
                  if (coverImgUrl == null && artist.contains(",")) {
                     String firstArtist = artist.split("[,&]")[0].trim();
                     String simpleQuery = URLEncoder.encode(firstArtist + " " + cleanTitle, StandardCharsets.UTF_8);
                     coverImgUrl = this.fetchDeezerCover("https://api.deezer.com/search?q=" + simpleQuery);
                  }

                  if (coverImgUrl == null) {
                     coverImgUrl = this.fetchDeezerCover("https://api.deezer.com/search?q=" + URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8));
                  }

                  if (coverImgUrl == null) {
                     coverImgUrl = this.fetchItunesCover(artist, cleanTitle);
                  }

                  if (coverImgUrl == null && !cleanTitle.equals(title)) {
                     coverImgUrl = this.fetchItunesCover(artist, title);
                  }

                  if (coverImgUrl != null) {
                     this.downloadAndRegisterCover(cacheKey, cleanTitle, coverImgUrl);
                  }
               } catch (Throwable var9) {
               }
            });
         }
      }
   }

   private String fetchDeezerCover(String apiUrl) {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(apiUrl).toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setConnectTimeout(3000);
         conn.setReadTimeout(3000);
         conn.setRequestProperty("User-Agent", "FluxClient/1.0");
         if (conn.getResponseCode() == 200) {
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               if (root.has("data") && root.get("data").isJsonArray()) {
                  for (JsonElement el : root.getAsJsonArray("data")) {
                     if (el.isJsonObject()) {
                        JsonObject trackObj = el.getAsJsonObject();
                        if (trackObj.has("album") && trackObj.get("album").isJsonObject()) {
                           JsonObject albumObj = trackObj.getAsJsonObject("album");
                           if (albumObj.has("cover_big") && !albumObj.get("cover_big").isJsonNull()) {
                              return albumObj.get("cover_big").getAsString();
                           }

                           if (albumObj.has("cover_medium") && !albumObj.get("cover_medium").isJsonNull()) {
                              return albumObj.get("cover_medium").getAsString();
                           }
                        }
                     }
                  }
               }

               return null;
            }
         }
      } catch (Throwable var13) {
      }

      return null;
   }

   private String fetchItunesCover(String artist, String title) {
      try {
         String query = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
         String apiUri = "https://itunes.apple.com/search?term=" + query + "&entity=song&limit=1";
         HttpURLConnection conn = (HttpURLConnection)URI.create(apiUri).toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setConnectTimeout(3000);
         conn.setReadTimeout(3000);
         conn.setRequestProperty("User-Agent", "FluxClient/1.0");
         if (conn.getResponseCode() == 200) {
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               if (root.has("results") && root.get("results").isJsonArray()) {
                  JsonArray results = root.getAsJsonArray("results");
                  if (!results.isEmpty() && results.get(0).isJsonObject()) {
                     JsonObject item = results.get(0).getAsJsonObject();
                     if (item.has("artworkUrl100") && !item.get("artworkUrl100").isJsonNull()) {
                        return item.get("artworkUrl100").getAsString().replace("100x100bb.jpg", "600x600bb.jpg");
                     }

                     return null;
                  }
               }

               return null;
            }
         }
      } catch (Throwable var13) {
      }

      return null;
   }

   private void loadAndRegisterCoverFromFile(String artist, String title, File file) {
      if (file != null && file.exists() && file.length() != 0L) {
         String filePath = file.getAbsolutePath();
         if (!this.registeredCoverFiles.contains(filePath)) {
            this.registeredCoverFiles.add(filePath);
            String cacheKey = (artist + " " + title).toLowerCase(Locale.ROOT).trim();
            if (!this.coverCache.containsKey(cacheKey)) {
               this.networkExecutor.submit(() -> {
                  try {
                     byte[] bytes = Files.readAllBytes(file.toPath());
                     if (bytes.length == 0) {
                        return;
                     }

                     MinecraftClient mc = MinecraftClient.getInstance();
                     if (mc != null) {
                        mc.send(() -> {
                           try {
                              NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));

                              try {
                                 NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                                 Identifier id = Identifier.of("fluxclient", "covers/w_" + Math.abs(cacheKey.hashCode()));
                                 mc.getTextureManager().registerTexture(id, texture);
                                 this.coverCache.put(cacheKey, id);
                                 String clean = this.cleanTitleForSearch(title).toLowerCase(Locale.ROOT).trim();
                                 this.coverCache.put((artist.toLowerCase(Locale.ROOT) + " " + clean).trim(), id);
                                 this.coverCache.put(clean, id);
                                 if (this.currentTrack != null && this.matchesKey(this.currentTrack, cacheKey)) {
                                    this.currentTrack = this.withCover(this.currentTrack, id);
                                 }

                                 if (this.gsmtcTrack != null && this.matchesKey(this.gsmtcTrack, cacheKey)) {
                                    this.gsmtcTrack = this.withCover(this.gsmtcTrack, id);
                                 }
                              } catch (Throwable var11) {
                                 if (image != null) {
                                    try {
                                       image.close();
                                    } catch (Throwable var10) {
                                       var11.addSuppressed(var10);
                                    }
                                 }

                                 throw var11;
                              }

                              if (image != null) {
                                 image.close();
                              }
                           } catch (Throwable var12) {
                              FluxLogger.warn("Failed to load WinRT cover texture: " + var12.getMessage());
                           }
                        });
                     }
                  } catch (Throwable var7) {
                  }
               });
            }
         }
      }
   }

   private void downloadAndRegisterCover(String cacheKey, String cleanTitle, String imageUrl) {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(imageUrl).toURL().openConnection();
         conn.setInstanceFollowRedirects(true);
         conn.setConnectTimeout(4000);
         conn.setReadTimeout(4000);
         conn.setRequestProperty("User-Agent", "FluxClient/1.0");
         if (conn.getResponseCode() == 200) {
            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
               bytes = in.readAllBytes();
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
               mc.send(() -> {
                  try {
                     NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));

                     try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                        Identifier id = Identifier.of("fluxclient", "covers/c_" + Math.abs(cacheKey.hashCode()));
                        mc.getTextureManager().registerTexture(id, texture);
                        this.coverCache.put(cacheKey, id);
                        this.coverCache.put(cleanTitle.toLowerCase(Locale.ROOT).trim(), id);
                        if (this.currentTrack != null && this.matchesKey(this.currentTrack, cacheKey)) {
                           this.currentTrack = this.withCover(this.currentTrack, id);
                        }

                        if (this.gsmtcTrack != null && this.matchesKey(this.gsmtcTrack, cacheKey)) {
                           this.gsmtcTrack = this.withCover(this.gsmtcTrack, id);
                        }
                     } catch (Throwable var9) {
                        if (image != null) {
                           try {
                              image.close();
                           } catch (Throwable var8) {
                              var9.addSuppressed(var8);
                           }
                        }

                        throw var9;
                     }

                     if (image != null) {
                        image.close();
                     }
                  } catch (Throwable var10x) {
                     FluxLogger.warn("Failed to load music cover: " + var10x.getMessage());
                  }
               });
            }
         }
      } catch (Throwable var11) {
      }
   }

   private void fetchLyricsAsync(String artist, String title) {
      if (!title.isEmpty()) {
         String cacheKey = (artist + " " + title).toLowerCase(Locale.ROOT).trim();
         if (!this.lyricsCache.containsKey(cacheKey)) {
            this.networkExecutor.submit(() -> {
               try {
                  String cleanTitle = this.cleanTitleForSearch(title);
                  MusicTracker.LyricsData data = this.queryLrclib(artist, cleanTitle);
                  if (data.plain().isEmpty() && artist.contains(",")) {
                     String firstArtist = artist.split("[,&]")[0].trim();
                     data = this.queryLrclib(firstArtist, cleanTitle);
                  }

                  if (data.plain().isEmpty() && !artist.isEmpty()) {
                     String firstArtist = artist.split("[,&]")[0].trim();
                     data = this.searchLrclib(firstArtist + " " + cleanTitle);
                  }

                  if (data.plain().isEmpty()) {
                     data = this.searchLrclib(cleanTitle);
                  }

                  if (data.plain().isEmpty() && !cleanTitle.equals(title)) {
                     data = this.searchLrclib(title);
                  }

                  if (!data.plain().isEmpty()) {
                     this.lyricsCache.put(cacheKey, data);
                     this.lyricsCache.put((artist + " " + cleanTitle).toLowerCase(Locale.ROOT).trim(), data);
                     this.lyricsCache.put(cleanTitle.toLowerCase(Locale.ROOT).trim(), data);
                     if (this.currentTrack != null && this.matchesKey(this.currentTrack, cacheKey)) {
                        this.currentTrack = this.withLyrics(this.currentTrack, data);
                     }

                     if (this.gsmtcTrack != null && this.matchesKey(this.gsmtcTrack, cacheKey)) {
                        this.gsmtcTrack = this.withLyrics(this.gsmtcTrack, data);
                     }
                  }
               } catch (Throwable var7) {
               }
            });
         }
      }
   }

   private MusicTracker.LyricsData queryLrclib(String artist, String title) {
      try {
         String artistParam = URLEncoder.encode(artist, StandardCharsets.UTF_8);
         String titleParam = URLEncoder.encode(title, StandardCharsets.UTF_8);
         String apiUri = "https://lrclib.net/api/get?artist_name=" + artistParam + "&track_name=" + titleParam;
         HttpURLConnection conn = (HttpURLConnection)URI.create(apiUri).toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setConnectTimeout(3000);
         conn.setReadTimeout(3000);
         conn.setRequestProperty("User-Agent", "FluxClient/1.0");
         if (conn.getResponseCode() == 200) {
            MusicTracker.LyricsData var11;
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
               StringBuilder sb = new StringBuilder();
               char[] buf = new char[1024];

               int len;
               while ((len = reader.read(buf)) > 0) {
                  sb.append(buf, 0, len);
               }

               var11 = this.parseLyricsJson(sb.toString());
            }

            return var11;
         }
      } catch (Throwable var14) {
      }

      return new MusicTracker.LyricsData(Collections.emptyList(), Collections.emptyList());
   }

   private MusicTracker.LyricsData searchLrclib(String query) {
      try {
         String qParam = URLEncoder.encode(query, StandardCharsets.UTF_8);
         String apiUri = "https://lrclib.net/api/search?q=" + qParam;
         HttpURLConnection conn = (HttpURLConnection)URI.create(apiUri).toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setConnectTimeout(3000);
         conn.setReadTimeout(3000);
         conn.setRequestProperty("User-Agent", "FluxClient/1.0");
         if (conn.getResponseCode() == 200) {
            MusicTracker.LyricsData var9;
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
               StringBuilder sb = new StringBuilder();
               char[] buf = new char[1024];

               int len;
               while ((len = reader.read(buf)) > 0) {
                  sb.append(buf, 0, len);
               }

               var9 = this.parseLyricsJson(sb.toString());
            }

            return var9;
         }
      } catch (Throwable var12) {
      }

      return new MusicTracker.LyricsData(Collections.emptyList(), Collections.emptyList());
   }

   private MusicTracker.LyricsData parseLyricsJson(String json) {
      if (json != null && !json.trim().isEmpty()) {
         try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
               if (root.isJsonObject()) {
                  return this.extractLyricsFromObject(root.getAsJsonObject());
               }
            } else {
               for (JsonElement el : root.getAsJsonArray()) {
                  if (el.isJsonObject()) {
                     MusicTracker.LyricsData data = this.extractLyricsFromObject(el.getAsJsonObject());
                     if (!data.plain().isEmpty() || !data.timed().isEmpty()) {
                        return data;
                     }
                  }
               }
            }
         } catch (Throwable var7) {
         }

         return new MusicTracker.LyricsData(Collections.emptyList(), Collections.emptyList());
      } else {
         return new MusicTracker.LyricsData(Collections.emptyList(), Collections.emptyList());
      }
   }

   private MusicTracker.LyricsData extractLyricsFromObject(JsonObject obj) {
      List<String> plain = new ArrayList<>();
      List<MusicTrack.TimedLyric> timed = new ArrayList<>();
      if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
         String syncedStr = obj.get("syncedLyrics").getAsString();
         Pattern linePattern = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]\\s*(.*)$");

         for (String rawLine : syncedStr.split("\\r?\\n")) {
            String line = rawLine.trim();
            Matcher m = linePattern.matcher(line);
            if (m.find()) {
               long minutes = Long.parseLong(m.group(1));
               double seconds = Double.parseDouble(m.group(2));
               long timeMs = (long)((minutes * 60L + seconds) * 1000.0);
               String text = m.group(3).trim();
               if (!text.isEmpty()) {
                  timed.add(new MusicTrack.TimedLyric(timeMs, text));
                  plain.add(text);
               }
            }
         }
      }

      if (plain.isEmpty() && obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull()) {
         String plainStr = obj.get("plainLyrics").getAsString();

         for (String rawLinex : plainStr.split("\\r?\\n")) {
            String line = rawLinex.trim();
            if (!line.isEmpty()) {
               plain.add(line);
            }
         }
      }

      return new MusicTracker.LyricsData(plain, timed);
   }

   private boolean matchesKey(MusicTrack track, String key) {
      String trackKey = (track.getArtist() + " " + track.getTitle()).toLowerCase(Locale.ROOT).trim();
      if (trackKey.equals(key)) {
         return true;
      } else {
         String clean = this.cleanTitleForSearch(track.getTitle());
         String cleanKey = (track.getArtist() + " " + clean).toLowerCase(Locale.ROOT).trim();
         return cleanKey.equals(key) || clean.toLowerCase(Locale.ROOT).trim().equals(key);
      }
   }

   private MusicTrack withCover(MusicTrack t, Identifier cover) {
      return new MusicTrack(
         t.getTitle(),
         t.getArtist(),
         t.getAppName(),
         t.isAppOpen(),
         t.isPlaying(),
         cover,
         t.getLyrics(),
         t.getTimedLyrics(),
         t.getDurationMs(),
         t.getAnchorPositionMs(),
         t.getAnchorTimeMs(),
         t.getAccentColor()
      );
   }

   private MusicTrack withLyrics(MusicTrack t, MusicTracker.LyricsData lyricsData) {
      return new MusicTrack(
         t.getTitle(),
         t.getArtist(),
         t.getAppName(),
         t.isAppOpen(),
         t.isPlaying(),
         t.getCoverTexture(),
         lyricsData.plain(),
         lyricsData.timed(),
         t.getDurationMs(),
         t.getAnchorPositionMs(),
         t.getAnchorTimeMs(),
         t.getAccentColor()
      );
   }

   public record LyricsData(List<String> plain, List<MusicTrack.TimedLyric> timed) {
   }

   private record ParsedSong(String artist, String title) {
   }

   private record TrackAnchor(long anchorPosMs, long anchorTimeMs) {
   }

   private record WindowInfo(String title, String processName) {
   }
}
