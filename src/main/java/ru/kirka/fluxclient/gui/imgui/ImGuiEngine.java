package ru.kirka.fluxclient.gui.imgui;

import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.logger.FluxLogger;

public final class ImGuiEngine {
   private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
   private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
   private final List<Runnable> postRenderTasks = new ArrayList<>();
   private boolean initialized = false;
   private boolean rendering = false;
   private static ImFont regularFont;
   private static ImFont boldFont;
   private static ImFont smallFont;
   private static ImFont headerFont;
   private static ImFont titleFont;

   public static ImFont getRegularFont() {
      return regularFont != null ? regularFont : boldFont;
   }

   public static ImFont getBoldFont() {
      return boldFont != null ? boldFont : regularFont;
   }

   public static ImFont getSmallFont() {
      return smallFont != null ? smallFont : regularFont;
   }

   public static ImFont getHeaderFont() {
      return headerFont != null ? headerFont : boldFont;
   }

   public static ImFont getTitleFont() {
      return titleFont != null ? titleFont : boldFont;
   }

   public void init(long windowHandle) {
      if (!this.initialized) {
         FluxLogger.info("Инициализация ImGui (версия " + ImGui.getVersion() + ")...");
         ImGui.createContext();
         ImGuiIO io = ImGui.getIO();
         io.setIniFilename(null);
         ImFontGlyphRangesBuilder builder = new ImFontGlyphRangesBuilder();
         builder.addRanges(io.getFonts().getGlyphRangesCyrillic());
         builder.addRanges(new short[]{8192, 8303, 8592, 8703, 8704, 8959, 9632, 9727, 9728, 9983, 9984, 10175, 0});
         builder.addText("•—–❤★✔✕…∞↑↓←→✓✗ℹ");
         short[] extendedRanges = builder.buildRanges();
         File arialWin = new File("C:/Windows/Fonts/arial.ttf");
         File arialWinBold = new File("C:/Windows/Fonts/arialbd.ttf");
         File segoeUi = new File("C:/Windows/Fonts/seguisym.ttf");
         ImFontConfig cfgNormal = new ImFontConfig();
         cfgNormal.setGlyphRanges(extendedRanges);
         cfgNormal.setOversampleH(2);
         cfgNormal.setOversampleV(2);
         cfgNormal.setPixelSnapH(true);
         ImFontConfig cfgBold = new ImFontConfig();
         cfgBold.setGlyphRanges(extendedRanges);
         cfgBold.setOversampleH(2);
         cfgBold.setOversampleV(2);
         cfgBold.setPixelSnapH(true);
         ImFontConfig cfgMerge = new ImFontConfig();
         cfgMerge.setMergeMode(true);
         cfgMerge.setGlyphRanges(extendedRanges);
         cfgMerge.setOversampleH(2);
         cfgMerge.setOversampleV(2);
         cfgMerge.setPixelSnapH(true);
         if (arialWinBold.exists()) {
            boldFont = io.getFonts().addFontFromFileTTF(arialWinBold.getAbsolutePath(), 16.5F, cfgBold);
            if (segoeUi.exists()) {
               io.getFonts().addFontFromFileTTF(segoeUi.getAbsolutePath(), 16.5F, cfgMerge);
            }

            headerFont = io.getFonts().addFontFromFileTTF(arialWinBold.getAbsolutePath(), 19.0F, cfgBold);
            if (segoeUi.exists()) {
               io.getFonts().addFontFromFileTTF(segoeUi.getAbsolutePath(), 19.0F, cfgMerge);
            }

            titleFont = io.getFonts().addFontFromFileTTF(arialWinBold.getAbsolutePath(), 46.0F, cfgBold);
            if (segoeUi.exists()) {
               io.getFonts().addFontFromFileTTF(segoeUi.getAbsolutePath(), 46.0F, cfgMerge);
            }
         }

         if (arialWin.exists()) {
            regularFont = io.getFonts().addFontFromFileTTF(arialWin.getAbsolutePath(), 16.0F, cfgNormal);
            if (segoeUi.exists()) {
               io.getFonts().addFontFromFileTTF(segoeUi.getAbsolutePath(), 16.0F, cfgMerge);
            }

            smallFont = io.getFonts().addFontFromFileTTF(arialWin.getAbsolutePath(), 13.0F, cfgNormal);
            if (segoeUi.exists()) {
               io.getFonts().addFontFromFileTTF(segoeUi.getAbsolutePath(), 13.0F, cfgMerge);
            }
         } else {
            io.getFonts().addFontDefault(cfgNormal);
         }

         io.getFonts().build();
         cfgNormal.destroy();
         cfgBold.destroy();
         cfgMerge.destroy();
         ImGuiTheme.apply();
         this.imGuiGlfw.init(windowHandle, true);
         this.imGuiGl3.init("#version 150");
         this.initialized = true;
      }
   }

   public static void post(Runnable task) {
      if (task != null) {
         FluxContext context = FluxContext.get();
         if (context != null && context.getImGuiEngine() != null) {
            context.getImGuiEngine().queuePostTask(task);
         } else {
            task.run();
         }
      }
   }

   public synchronized void queuePostTask(Runnable task) {
      if (task != null) {
         this.postRenderTasks.add(task);
      }
   }

   public synchronized boolean beginFrame() {
      if (this.initialized && !this.rendering) {
         this.rendering = true;
         this.imGuiGl3.newFrame();
         this.imGuiGlfw.newFrame();
         ImGui.newFrame();
         return true;
      } else {
         return false;
      }
   }

   public void endFrame() {
      if (this.initialized && this.rendering) {
         try {
            ImGui.render();
            this.imGuiGl3.renderDrawData(ImGui.getDrawData());
         } finally {
            this.rendering = false;
         }

         List<Runnable> tasks;
         synchronized (this) {
            if (this.postRenderTasks.isEmpty()) {
               return;
            }

            tasks = new ArrayList<>(this.postRenderTasks);
            this.postRenderTasks.clear();
         }

         for (Runnable task : tasks) {
            try {
               task.run();
            } catch (Exception var8) {
               FluxLogger.error("Ошибка при выполнении post-render задачи", var8);
            }
         }
      }
   }

   public boolean isRendering() {
      return this.rendering;
   }

   public void dispose() {
      if (this.initialized) {
         this.rendering = false;
         synchronized (this) {
            this.postRenderTasks.clear();
         }

         ImGui.destroyContext();
         this.initialized = false;
      }
   }
}
