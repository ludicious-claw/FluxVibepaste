package ru.kirka.fluxclient.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.config.Setting;
import ru.kirka.fluxclient.config.impl.BindSetting;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.ButtonSetting;
import ru.kirka.fluxclient.config.impl.ColorSetting;
import ru.kirka.fluxclient.config.impl.ModeSetting;
import ru.kirka.fluxclient.config.impl.MultiSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.config.impl.TextSetting;
import ru.kirka.fluxclient.core.EventManager;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;
import ru.kirka.fluxclient.notification.Notification;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.ui.element.BindElement;
import ru.kirka.fluxclient.ui.element.BooleanElement;
import ru.kirka.fluxclient.ui.element.ButtonElement;
import ru.kirka.fluxclient.ui.element.ColorElement;
import ru.kirka.fluxclient.ui.element.Element;
import ru.kirka.fluxclient.ui.element.ModeElement;
import ru.kirka.fluxclient.ui.element.MultiModeElement;
import ru.kirka.fluxclient.ui.element.SliderElement;
import ru.kirka.fluxclient.ui.element.StringElement;

public abstract class Module implements Interface {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   private final String name;
   private final String description;
   private final Category category;
   private int keyBind;
   private boolean enabled;
   private boolean bound;
   private boolean extended;
   private final List<Setting<?>> settings = new ArrayList<>();
   private final List<Element<?>> elements = new ArrayList<>();
   private final AnimationUtil enableAnimation = new AnimationUtil();
   private final AnimationUtil disableAnimation = new AnimationUtil();
   private final AnimationUtil bindAnimation = new AnimationUtil();
   private final AnimationUtil extendAnimation = new AnimationUtil();
   private final AnimationUtil hoverAnimation = new AnimationUtil();

   public Module(String name, String description, Category category, int defaultKey) {
      if (name != null && (name.startsWith("•") || name.startsWith("·") || name.startsWith("."))) {
         name = name.replaceFirst("^[•·.\\s]+", "");
      }

      this.name = name;
      this.description = description;
      this.category = category;
      this.keyBind = defaultKey;
      this.enabled = false;
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         boolean isClickGui = this instanceof ClickGuiModule || this.getName().equalsIgnoreCase("ClickGUI");
         if (enabled) {
            FluxContext.get().getEventBus().register(this);
            EventManager.a(this);
            this.onEnable();
            if (!isClickGui && FluxClient.getInstance() != null && FluxClient.getInstance().getNotificationProcessor() != null) {
               FluxClient.getInstance()
                  .getNotificationProcessor()
                  .a(new Notification("Q", ColorUtil.convertToARGB(100, 220, 100, 255), this.getName() + " активирован", 1500));
            }
         } else {
            FluxContext.get().getEventBus().unregister(this);
            EventManager.b(this);
            this.onDisable();
            if (!isClickGui && FluxClient.getInstance() != null && FluxClient.getInstance().getNotificationProcessor() != null) {
               FluxClient.getInstance()
                  .getNotificationProcessor()
                  .a(new Notification("Q", ColorUtil.convertToARGB(230, 80, 80, 255), this.getName() + " деактивирован", 1500));
            }
         }

         if (!isClickGui && FluxContext.get() != null && FluxContext.get().getConfigManager() != null) {
            FluxContext.get().getConfigManager().save();
         }
      }
   }

   public void setEnabledSilent(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            FluxContext.get().getEventBus().register(this);
            EventManager.a(this);
         } else {
            FluxContext.get().getEventBus().unregister(this);
            EventManager.b(this);
         }
      }
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   public void onTick() {
   }

   public void registerSetting(Setting<?> setting) {
      this.settings.add(setting);
      Element<?> element = this.createElementForSetting(setting);
      if (element != null) {
         this.elements.add(element);
      }
   }

   private Element<?> createElementForSetting(Setting<?> setting) {
      if (setting instanceof BooleanSetting bs) {
         return new BooleanElement(bs);
      } else if (setting instanceof NumberSetting ns) {
         return new SliderElement(ns);
      } else if (setting instanceof ModeSetting ms) {
         return new ModeElement(ms);
      } else if (setting instanceof ColorSetting cs) {
         return new ColorElement(cs);
      } else if (setting instanceof BindSetting bs) {
         return new BindElement(bs);
      } else if (setting instanceof ButtonSetting bs) {
         return new ButtonElement(bs);
      } else if (setting instanceof MultiSetting ms) {
         return new MultiModeElement(ms);
      } else {
         return setting instanceof TextSetting ts ? new StringElement(ts) : null;
      }
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Category getCategory() {
      return this.category;
   }

   public int getKeyBind() {
      return this.keyBind;
   }

   public void setKeyBind(int keyBind) {
      this.keyBind = keyBind;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isBound() {
      return this.bound;
   }

   public void setBound(boolean bound) {
      this.bound = bound;
   }

   public boolean isExtended() {
      return this.extended;
   }

   public void setExtended(boolean extended) {
      this.extended = extended;
   }

   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   public List<Element<?>> getElements() {
      return this.elements;
   }

   public AnimationUtil getEnableAnimation() {
      return this.enableAnimation;
   }

   public AnimationUtil getDisableAnimation() {
      return this.disableAnimation;
   }

   public AnimationUtil getBindAnimation() {
      return this.bindAnimation;
   }

   public AnimationUtil getExtendAnimation() {
      return this.extendAnimation;
   }

   public AnimationUtil getHoverAnimation() {
      return this.hoverAnimation;
   }

   public void render3D(MatrixStack matrices) {
   }

   public void a() {
      this.toggle();
   }

   public void a(boolean state) {
      this.setEnabled(state);
   }

   public void a(int key) {
      this.setKeyBind(key);
   }

   public void b(boolean bind) {
      this.setBound(bind);
   }

   public void c(boolean ext) {
      this.setExtended(ext);
   }

   public List<Element<?>> d() {
      return this.getElements();
   }

   public List<Setting<?>> e() {
      return this.getSettings();
   }

   public AnimationUtil f() {
      return this.getEnableAnimation();
   }

   public AnimationUtil g() {
      return this.getDisableAnimation();
   }

   public AnimationUtil h() {
      return this.getBindAnimation();
   }

   public AnimationUtil i() {
      return this.getExtendAnimation();
   }

   public String j() {
      return this.getName();
   }

   public String k() {
      return this.getDescription();
   }

   public Category l() {
      return this.getCategory();
   }

   public boolean m() {
      return this.isEnabled();
   }

   public boolean n() {
      return this.isBound();
   }

   public boolean o() {
      return this.isExtended();
   }

   public int p() {
      return this.getKeyBind();
   }
}
