package ru.kirka.fluxclient.feature;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import ru.kirka.fluxclient.feature.impl.combat.AimAssistant;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.feature.impl.combat.AutoTotem;
import ru.kirka.fluxclient.feature.impl.combat.Hitboxes;
import ru.kirka.fluxclient.feature.impl.combat.MaceKiller;
import ru.kirka.fluxclient.feature.impl.combat.TriggerBot;
import ru.kirka.fluxclient.feature.impl.combat.Velocity;
import ru.kirka.fluxclient.feature.impl.hud.ArmorHUD;
import ru.kirka.fluxclient.feature.impl.hud.ArrayListHUD;
import ru.kirka.fluxclient.feature.impl.hud.Keystrokes;
import ru.kirka.fluxclient.feature.impl.hud.Music;
import ru.kirka.fluxclient.feature.impl.hud.PotionsHUD;
import ru.kirka.fluxclient.feature.impl.hud.TargetHUD;
import ru.kirka.fluxclient.feature.impl.hud.Watermark;
import ru.kirka.fluxclient.feature.impl.misc.AutoEat;
import ru.kirka.fluxclient.feature.impl.misc.AutoGApple;
import ru.kirka.fluxclient.feature.impl.misc.AutoReconnect;
import ru.kirka.fluxclient.feature.impl.misc.AutoRespawn;
import ru.kirka.fluxclient.feature.impl.misc.ChestStealer;
import ru.kirka.fluxclient.feature.impl.misc.FakePlayer;
import ru.kirka.fluxclient.feature.impl.misc.Freecam;
import ru.kirka.fluxclient.feature.impl.misc.LuckyBlock;
import ru.kirka.fluxclient.feature.impl.misc.PvpBot;
import ru.kirka.fluxclient.feature.impl.misc.SeeInvinsibles;
import ru.kirka.fluxclient.feature.impl.movement.AutoSprint;
import ru.kirka.fluxclient.feature.impl.movement.Fly;
import ru.kirka.fluxclient.feature.impl.movement.Speed;
import ru.kirka.fluxclient.feature.impl.render.Ambience;
import ru.kirka.fluxclient.feature.impl.render.AntiInvisible;
import ru.kirka.fluxclient.feature.impl.render.BlockESP;
import ru.kirka.fluxclient.feature.impl.render.Breadcrumbs;
import ru.kirka.fluxclient.feature.impl.render.CameraDynamics;
import ru.kirka.fluxclient.feature.impl.render.ChinaHat;
import ru.kirka.fluxclient.feature.impl.render.ClickGuiModule;
import ru.kirka.fluxclient.feature.impl.render.CosmeticWings;
import ru.kirka.fluxclient.feature.impl.render.CustomCrosshair;
import ru.kirka.fluxclient.feature.impl.render.CustomFog;
import ru.kirka.fluxclient.feature.impl.render.CustomSky;
import ru.kirka.fluxclient.feature.impl.render.DamageIndicators;
import ru.kirka.fluxclient.feature.impl.render.ESP;
import ru.kirka.fluxclient.feature.impl.render.EntityChams;
import ru.kirka.fluxclient.feature.impl.render.FriendMarkers;
import ru.kirka.fluxclient.feature.impl.render.FullBright;
import ru.kirka.fluxclient.feature.impl.render.HandESP;
import ru.kirka.fluxclient.feature.impl.render.HandShader;
import ru.kirka.fluxclient.feature.impl.render.ItemPhysics;
import ru.kirka.fluxclient.feature.impl.render.JumpCircles;
import ru.kirka.fluxclient.feature.impl.render.KillEffects;
import ru.kirka.fluxclient.feature.impl.render.NameTagsPlus;
import ru.kirka.fluxclient.feature.impl.render.NoRender;
import ru.kirka.fluxclient.feature.impl.render.ObjectInfo;
import ru.kirka.fluxclient.feature.impl.render.Optimizations;
import ru.kirka.fluxclient.feature.impl.render.PopChams;
import ru.kirka.fluxclient.feature.impl.render.ShaderSky;
import ru.kirka.fluxclient.feature.impl.render.SoundESP;
import ru.kirka.fluxclient.feature.impl.render.StorageESP;
import ru.kirka.fluxclient.feature.impl.render.SwingAnimations;
import ru.kirka.fluxclient.feature.impl.render.TNTTimer;
import ru.kirka.fluxclient.feature.impl.render.TargetESP;
import ru.kirka.fluxclient.feature.impl.render.TargetParticles;
import ru.kirka.fluxclient.feature.impl.render.TimeChanger;
import ru.kirka.fluxclient.feature.impl.render.Tracers;
import ru.kirka.fluxclient.feature.impl.render.Trajectories;
import ru.kirka.fluxclient.feature.impl.render.TrapESP;
import ru.kirka.fluxclient.feature.impl.render.ViewModel;
import ru.kirka.fluxclient.feature.impl.render.Waypoints;
import ru.kirka.fluxclient.feature.impl.render.WorldParticles;
import ru.kirka.fluxclient.feature.impl.render.WorldReflections;
import ru.kirka.fluxclient.feature.impl.render.WorldVisuals;
import ru.kirka.fluxclient.feature.impl.render.XRay;
import ru.kirka.fluxclient.ui.screen.GUIScreen;

public final class ModuleManager {
   private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();
   private final Map<String, Module> modulesByName = new ConcurrentHashMap<>();

   public void init() {
      this.register(new ClickGuiModule());
      this.register(new AimAssistant());
      this.register(new Aura());
      this.register(new AutoTotem());
      this.register(new Velocity());
      this.register(new TriggerBot());
      this.register(new Hitboxes());
      this.register(new MaceKiller());
      this.register(new AutoSprint());
      this.register(new Fly());
      this.register(new Speed());
      this.register(new JumpCircles());
      this.register(new TargetESP());
      this.register(new TargetParticles());
      this.register(new WorldParticles());
      this.register(new CustomFog());
      this.register(new CustomSky());
      this.register(new FullBright());
      this.register(new NoRender());
      this.register(new TimeChanger());
      this.register(new Tracers());
      this.register(new NameTagsPlus());
      this.register(new CustomCrosshair());
      this.register(new HandESP());
      this.register(new SwingAnimations());
      this.register(new HandShader());
      this.register(new BlockESP());
      this.register(new ShaderSky());
      this.register(new CosmeticWings());
      this.register(new Breadcrumbs());
      this.register(new ChinaHat());
      this.register(new PopChams());
      this.register(new Ambience());
      this.register(new EntityChams());
      this.register(new CameraDynamics());
      this.register(new ItemPhysics());
      this.register(new Trajectories());
      this.register(new DamageIndicators());
      this.register(new WorldReflections());
      this.register(new Optimizations());
      this.register(new ESP());
      this.register(new StorageESP());
      this.register(new TrapESP());
      this.register(new SoundESP());
      this.register(new TNTTimer());
      this.register(new KillEffects());
      this.register(new ViewModel());
      this.register(new Waypoints());
      this.register(new FriendMarkers());
      this.register(new AntiInvisible());
      this.register(new ObjectInfo());
      this.register(new WorldVisuals());
      this.register(new XRay());
      this.register(new Watermark());
      this.register(new PotionsHUD());
      this.register(new TargetHUD());
      this.register(new ArrayListHUD());
      this.register(new Keystrokes());
      this.register(new ArmorHUD());
      this.register(new Music());
      this.register(new AutoEat());
      this.register(new AutoGApple());
      this.register(new FakePlayer());
      this.register(new Freecam());
      this.register(new AutoReconnect());
      this.register(new AutoRespawn());
      this.register(new ChestStealer());
      this.register(new SeeInvinsibles());
      this.register(new LuckyBlock());
      this.register(new PvpBot());
   }

   public void register(Module module) {
      this.modules.put((Class<? extends Module>)module.getClass(), module);
      this.modulesByName.put(module.getName().toLowerCase(Locale.ROOT), module);
   }

   public <T extends Module> T getModule(Class<T> clazz) {
      return (T)this.modules.get(clazz);
   }

   public Module getByName(String name) {
      return this.modulesByName.get(name.toLowerCase(Locale.ROOT));
   }

   public Collection<Module> getModules() {
      return Collections.unmodifiableCollection(this.modules.values());
   }

   public List<Module> getModulesByCategory(Category category) {
      return this.modules.values().stream().filter(m -> m.getCategory() == category).toList();
   }

   public boolean handleKeyPress(int key) {
      if (key <= 0) {
         return false;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.currentScreen != null && !(mc.currentScreen instanceof GUIScreen)) {
            return false;
         } else {
            boolean handled = false;

            for (Module module : this.modules.values()) {
               if (module.getKeyBind() == key) {
                  module.toggle();
                  handled = true;
               }
            }

            return handled;
         }
      }
   }
}
