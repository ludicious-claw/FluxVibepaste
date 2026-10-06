package ru.kirka.fluxclient;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.ConfigManager;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.EventTarget;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.User;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.event.GlobalEvent;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.ModuleManager;
import ru.kirka.fluxclient.network.AccountProcessor;
import ru.kirka.fluxclient.notification.NotificationProcessor;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.Draw3DProcessor;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.render.ScaleUtil;
import ru.kirka.fluxclient.staff.StaffProcessor;
import ru.kirka.fluxclient.theme.ThemeProcessor;
import ru.kirka.fluxclient.ui.element.DragProcessor;
import ru.kirka.fluxclient.ui.screen.GUIPanel;
import ru.kirka.fluxclient.ui.screen.GUIScreen;
import ru.kirka.fluxclient.ui.widget.ArmorWidget;
import ru.kirka.fluxclient.ui.widget.CooldownsWidget;
import ru.kirka.fluxclient.ui.widget.HotkeysWidget;
import ru.kirka.fluxclient.ui.widget.MusicWidget;
import ru.kirka.fluxclient.ui.widget.NotificationWidget;
import ru.kirka.fluxclient.ui.widget.PotionWidget;
import ru.kirka.fluxclient.ui.widget.StaffWidget;
import ru.kirka.fluxclient.ui.widget.TargetWidget;
import ru.kirka.fluxclient.ui.widget.WatermarkWidget;
import ru.kirka.fluxclient.ui.widget.Widget;
import ru.kirka.fluxclient.util.music.MusicTracker;

public class FluxClient implements Interface {
   private static FluxClient instance;
   private static volatile FluxClient instanceRef;
   private Draw2DProcessor draw2DProcessor;
   private Draw3DProcessor draw3DProcessor;
   private ThemeProcessor themeProcessor;
   private NotificationProcessor notificationProcessor;
   private DragProcessor dragProcessor;
   private AccountProcessor accountProcessor;
   private StaffProcessor staffProcessor;
   private ModuleManager moduleManager;
   private final List<Widget> widgets = new ArrayList<>();
   private GUIScreen currentScreen;
   private User currentUser;

   public FluxClient() {
      this.initialize();
   }

   public static FluxClient getInstance() {
      return instance;
   }

   protected void initialize() {
      instance = this;
      instanceRef = this;
      this.currentUser = new User("1", "Kirka_int", "Owner", "Owner", "01.01.2099 00:00", "");
      this.draw2DProcessor = new Draw2DProcessor();
      this.draw3DProcessor = new Draw3DProcessor();
      this.themeProcessor = new ThemeProcessor();
      this.notificationProcessor = new NotificationProcessor();
      this.dragProcessor = new DragProcessor();
      this.accountProcessor = new AccountProcessor();
      this.staffProcessor = new StaffProcessor();
      if (FluxContext.get() != null) {
         this.moduleManager = FluxContext.get().getModuleManager();
      }

      this.widgets.add(new WatermarkWidget());
      this.widgets.add(new TargetWidget());
      this.widgets.add(new ArmorWidget());
      this.widgets.add(new PotionWidget());
      this.widgets.add(new HotkeysWidget());
      this.widgets.add(new NotificationWidget());
      this.widgets.add(new CooldownsWidget());
      this.widgets.add(new StaffWidget());
      this.widgets.add(new MusicWidget());
      EventManager.a(this);
      EventManager.a(this.dragProcessor);
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> this.shutdown());
   }

   protected void shutdown() {
      MusicTracker.getInstance().stop();
   }

   public void openGuiScreen() {
      MinecraftClient client = Interface.mc;
      if (client != null) {
         if (this.currentScreen == null) {
            this.currentScreen = new GUIScreen(Text.literal("FluxClient GUI"));
         }

         client.setScreen(this.currentScreen);
      }
   }

   @EventTarget(a = 0)
   public void onDrawStart(DrawEvent event) {
      if (event.is2D()) {
         ScaleUtil.a(event.getDrawContext(), 2);
         if (this.moduleManager != null) {
            for (Module module : this.moduleManager.getModules()) {
               module.getEnableAnimation().a(0.0F, 1.0F, 0.3F, EasingList.i, event.getTickDelta());
               module.getEnableAnimation().a(module.isEnabled());
               module.getDisableAnimation().a(0.0F, 1.0F, 0.3F, EasingList.i, event.getTickDelta());
               module.getDisableAnimation().a(module.isBound());
               module.getExtendAnimation().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
               module.getExtendAnimation().a(module.isExtended());
            }
         }

         if (this.currentScreen != null) {
            for (GUIPanel panel : this.currentScreen.getPanels()) {
               panel.getOpenAnimation().a(0.0F, 1.0F, 0.3F, EasingList.g, event.getTickDelta());
               panel.getOpenAnimation().a(Interface.mc != null && Interface.mc.currentScreen instanceof GUIScreen);
            }
         }

         if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
            boolean inGui = mc.currentScreen instanceof GUIScreen;

            for (Widget widget : this.widgets) {
               if (!inGui || widget instanceof NotificationWidget) {
                  widget.a(event);
               }
            }
         }
      }
   }

   @EventTarget(a = 4)
   public void onDrawEnd(DrawEvent event) {
      if (event.is2D()) {
         ScaleUtil.a(event.getDrawContext());
      }
   }

   @EventTarget
   public void onGlobalEvent(GlobalEvent event) {
      for (Widget widget : this.widgets) {
         widget.a(event);
      }
   }

   public Draw2DProcessor getDraw2DProcessor() {
      return this.draw2DProcessor;
   }

   public Draw3DProcessor getDraw3DProcessor() {
      return this.draw3DProcessor;
   }

   public ThemeProcessor getThemeProcessor() {
      return this.themeProcessor;
   }

   public NotificationProcessor getNotificationProcessor() {
      return this.notificationProcessor;
   }

   public DragProcessor getDragProcessor() {
      return this.dragProcessor;
   }

   public AccountProcessor getAccountProcessor() {
      return this.accountProcessor;
   }

   public StaffProcessor getStaffProcessor() {
      return this.staffProcessor;
   }

   public ModuleManager getModuleManager() {
      if (this.moduleManager == null && FluxContext.get() != null) {
         this.moduleManager = FluxContext.get().getModuleManager();
      }

      return this.moduleManager;
   }

   public ConfigManager getConfigManager() {
      return FluxContext.get() != null ? FluxContext.get().getConfigManager() : null;
   }

   public List<Widget> getWidgets() {
      return this.widgets;
   }

   public GUIScreen getCurrentScreen() {
      if (this.currentScreen == null) {
         this.currentScreen = new GUIScreen(Text.literal("FluxClient GUI"));
      }

      return this.currentScreen;
   }

   public User getCurrentUser() {
      return this.currentUser;
   }

   public User g() {
      return this.currentUser;
   }
}
