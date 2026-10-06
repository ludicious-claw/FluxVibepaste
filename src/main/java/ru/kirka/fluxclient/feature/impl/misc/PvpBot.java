package ru.kirka.fluxclient.feature.impl.misc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.kirka.fluxclient.config.impl.BooleanSetting;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.config.impl.TextSetting;
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.logger.FluxLogger;
import ru.kirka.fluxclient.event.EventBus;
import ru.kirka.fluxclient.event.impl.DeathEvent;
import ru.kirka.fluxclient.event.impl.PlaceDeniedEvent;
import ru.kirka.fluxclient.event.impl.ServerJoinEvent;
import ru.kirka.fluxclient.event.impl.TransferEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.ModuleManager;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.feature.impl.combat.AutoTotem;
import ru.kirka.fluxclient.util.BotNavigator;

public class PvpBot extends Module {
   public final NumberSetting chaseRadius = new NumberSetting("Радиус погони", "Как далеко бежать за игроком", 30.0F, 8.0F, 80.0F, 1.0F);
   public final NumberSetting leashRadius = new NumberSetting("Радиус арены", "Дальше скольки блоков от центра нельзя отходить", 10.0F, 5.0F, 50.0F, 1.0F);
   public final BooleanSetting maceProtect = new BooleanSetting("MaceProtect", "Щит в оффхенд + блок, если кто-то выше с булавой", true);
   public final BooleanSetting moneyBlocks = new BooleanSetting("Денежные блоки", "Ломать железо/золото/изумруд/алмаз/обсидиан в зоне арены", true);
   public final BooleanSetting autoHeal = new BooleanSetting("Авто-хилка", "При низком ХП бежать на точку хилки и стоять 15 сек", true);
   public final NumberSetting healHealth = new NumberSetting("ХП для хилки", "Ниже скольки ХП бежать хилиться", 8.0F, 2.0F, 16.0F, 0.5F);
   public final NumberSetting axeCooldown = new NumberSetting(
      "Кулдаун топора", "С какой готовностью бить топором (ниже = быстрее тап)", 0.6F, 0.3F, 1.0F, 0.01F
   );
   public final NumberSetting strafeTime = new NumberSetting("Смена стрейфа", "Раз в сколько секунд менять сторону обхода", 1.0F, 0.3F, 3.0F, 0.1F);
   public final NumberSetting commandDelay = new NumberSetting("Задержка команд", "Пауза между /gm 1, /home, /rtp, /warp pvp (сек)", 5.0F, 1.0F, 10.0F, 0.5F);
   public final NumberSetting enderRadius = new NumberSetting("Радиус эндера", "Где искать эндер-сундук после /home", 14.0F, 4.0F, 32.0F, 1.0F);
   public final BooleanSetting autoGear = new BooleanSetting("Авто-экипировка", "Одевать броню и раскладывать хотбар после лута", true);
   public final BooleanSetting autoRecover = new BooleanSetting("Авто-возврат", "Бесконечный цикл: смерть → кит → /warp pvp → бой", true);
   public final BooleanSetting chatLog = new BooleanSetting("Логи в чат", "Писать действия бота в игровой чат", true);
   public final BooleanSetting autoReconnect = new BooleanSetting("Авто-реконнект", "Держать включённым AutoReconnect (возврат при полном кике)", true);
   public final TextSetting ignoreUUIDs = new TextSetting("ЧС UUID", "UUID через запятую: этих игроков бот не трогает (битые скины)", "");
   public final BooleanSetting safeCombat = new BooleanSetting("Безопасный бой", "Душить KillAura под античит: дистанция, плавность, GCD, стены", true);
   private static final double INTERACT_REACH = 4.5;
   private static final double CHASE_STOP_DIST = 2.4;
   private static final double ARENA_X = -6.0;
   private static final double ARENA_Z = 289.0;
   private static final double LOBBY_X = 3.0;
   private static final double LOBBY_Z = -43.0;
   private static final double HEAL_X = -21.0;
   private static final double HEAL_Z = 289.0;
   private static final double MINE_REACH = 4.5;
   private PvpBot.Phase phase = PvpBot.Phase.COMBAT;
   private int phaseTicks;
   private boolean enabledApple;
   private boolean enabledTotem;
   private boolean enabledRespawn;
   private boolean enabledStealer;
   private boolean enabledAura;
   private boolean wasDead;
   private volatile boolean deathSignal;
   private int interactCooldown;
   private int weaponCheckCooldown;
   private Boolean savedOnlyCrits = null;
   private String lastWeapon = "";
   private int strafeTicks;
   private int strafeDir = 1;
   private int arenaEnterTicks;
   private int arenaRunTicks;
   private final Random rng = new Random();
   private final BotNavigator navigator = new BotNavigator();
   private boolean outOfBounds;
   private boolean maceBlocking;
   private BlockPos miningPos;
   private Direction miningSide;
   private int swingCooldown;
   private BlockPos enderPos;
   private BlockPos placedChestPos;
   private int chestCountOnCloneEnter;
   private int placeAttempts;
   private int placeTimer;
   private int placeTimerAction;
   private int placeConfirmTicks;
   private int ticksSinceJoin;
   private float savedRange = -1.0F;
   private String savedRotMode;
   private boolean savedWalls;
   private boolean safeApplied;
   private float savedStealerDelay = -1.0F;
   private final Set<UUID> seenProfiles = new HashSet<>();
   private int skinScanCooldown;
   private boolean wasInGame;
   private boolean lobbyActive;
   private PvpBot.Phase savedPhase = PvpBot.Phase.COMBAT;
   private boolean walkStarted;
   private boolean lobbyReturned;
   private boolean lobbyAuraWasOn;
   private long expectTransferUntil;
   private final boolean[] linked = new boolean[6];

   public PvpBot() {
      super("PvPBot", "Авто-боец для mc.migosmc.net: бой, кит, возврат на /warp pvp", Category.MISC, -1);
      this.registerSetting(this.chaseRadius);
      this.registerSetting(this.leashRadius);
      this.registerSetting(this.axeCooldown);
      this.registerSetting(this.maceProtect);
      this.registerSetting(this.moneyBlocks);
      this.registerSetting(this.autoHeal);
      this.registerSetting(this.healHealth);
      this.registerSetting(this.strafeTime);
      this.registerSetting(this.commandDelay);
      this.registerSetting(this.enderRadius);
      this.registerSetting(this.autoGear);
      this.registerSetting(this.autoRecover);
      this.registerSetting(this.chatLog);
      this.registerSetting(this.autoReconnect);
      this.registerSetting(this.ignoreUUIDs);
      this.registerSetting(this.safeCombat);
   }

   @Override
   protected void onEnable() {
      this.phase = PvpBot.Phase.COMBAT;
      this.phaseTicks = 0;
      this.interactCooldown = 0;
      this.weaponCheckCooldown = 0;
      this.strafeTicks = 0;
      this.strafeDir = 1;
      this.outOfBounds = false;
      this.maceBlocking = false;
      this.miningPos = null;
      this.miningSide = null;
      this.swingCooldown = 0;
      this.wasDead = false;
      this.deathSignal = false;
      this.wasInGame = false;
      this.lobbyActive = false;
      this.walkStarted = false;
      this.lobbyReturned = false;
      this.lobbyAuraWasOn = false;
      this.expectTransferUntil = 0L;
      this.ticksSinceJoin = 0;
      this.navigator.reset();
      this.lastWeapon = "";

      try {
         Aura a0 = FluxContext.get().getModuleManager().getModule(Aura.class);
         this.savedOnlyCrits = a0 != null ? a0.onlyCriticals.get() : null;
      } catch (Exception var3) {
         this.savedOnlyCrits = null;
      }

      this.enderPos = null;
      this.placedChestPos = null;
      this.enableCombatModules();
      Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
      if (aura != null && !aura.targets.isEnabled("Игроки")) {
         BooleanSetting opt = aura.targets.get("Игроки");
         if (opt != null) {
            opt.set(true);
         }

         this.say("включил цели-игроки в Aura (нужно боту).");
      }

      this.applySafeProfile();
      this.say("включён. Дерусь на /warp pvp, сервер mc.migosmc.net.");
      FluxLogger.info("[PvpBot] enabled, phase=COMBAT");
   }

   @Override
   protected void onDisable() {
      this.releaseMovement();
      this.stopMiningProgress();
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      this.maceBlocking = false;
      if (this.lobbyAuraWasOn) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null && !aura.isEnabled()) {
            aura.setEnabled(true);
         }

         this.lobbyAuraWasOn = false;
      }

      this.lobbyActive = false;
      this.wasInGame = false;
      this.expectTransferUntil = 0L;
      this.restoreModules();
      this.enderPos = null;
      this.placedChestPos = null;
      FluxLogger.info("[PvpBot] disabled");
   }

   @Override
   public void onTick() {
      boolean inGame = mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.networkHandler != null;
      if (!inGame) {
         if (this.wasInGame && !this.lobbyActive) {
            this.enterLobby();
         }
      } else {
         this.wasInGame = true;
         if (this.lobbyActive) {
            this.doLobbyWalk();
         } else {
            this.scanSkins();
            boolean dead = this.deathSignal || this.isDead();
            if (dead) {
               this.wasDead = true;
               this.deathSignal = false;
               this.releaseMovement();
            } else if (this.wasDead) {
               this.wasDead = false;
               this.deathSignal = false;
               this.onRespawned();
               if (this.autoRecover.get()) {
                  this.enterPhase(PvpBot.Phase.R_GM1);
               } else {
                  this.enterPhase(PvpBot.Phase.COMBAT);
               }
            } else {
               this.phaseTicks++;
               switch (this.phase) {
                  case COMBAT:
                     this.doCombat();
                     break;
                  case ARENA_ENTER:
                     this.doArenaEnter();
                     break;
                  case HEAL_GOTO:
                     this.doHealGoto();
                     break;
                  case HEAL_WAIT:
                     this.doHealWait();
                     break;
                  case R_GM1:
                     this.doWaitDelay(PvpBot.Phase.R_HOME);
                     break;
                  case R_HOME:
                     this.doWaitDelay(PvpBot.Phase.R_ENDER_OPEN);
                     break;
                  case R_ENDER_OPEN:
                     this.doEnderOpen();
                     break;
                  case R_ENDER_CLONE:
                     this.doEnderClone();
                     break;
                  case R_RTP:
                     this.doWaitDelay(PvpBot.Phase.R_PLACE);
                     break;
                  case R_PLACE:
                     this.doPlace();
                     break;
                  case R_LOOT:
                     this.doLoot();
                     break;
                  case R_GEAR:
                     this.doGear();
                     break;
                  case R_WARP:
                     this.doWaitDelay(PvpBot.Phase.ARENA_ENTER);
                     break;
                  default:
                     this.enterPhase(PvpBot.Phase.COMBAT);
               }
            }
         }
      }
   }

   private void enterLobby() {
      this.lobbyActive = true;
      this.savedPhase = this.phase;
      this.walkStarted = false;
      this.lobbyReturned = false;
      this.ticksSinceJoin = 0;
      this.expectTransferUntil = 0L;
      this.wasDead = false;
      this.deathSignal = false;
      this.releaseMovement();
      Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
      this.lobbyAuraWasOn = aura != null && aura.isEnabled();
      if (this.lobbyAuraWasOn) {
         aura.setEnabled(false);
      }

      this.say("выкинуло (лобби). Фаза сохранена, иду вперёд до возврата…");
      FluxLogger.info("[PvpBot] lobby mode, saved phase=" + this.savedPhase);
   }

   private void doLobbyWalk() {
      if (!this.walkStarted) {
         this.walkStarted = true;
         this.ticksSinceJoin = 0;
         this.lobbyReturned = false;
         this.navigator.reset();
         double dx0 = -6.0 - mc.player.getX();
         double dz0 = 289.0 - mc.player.getZ();
         if (Math.sqrt(dx0 * dx0 + dz0 * dz0) < 40.0) {
            if (this.savedPhase == PvpBot.Phase.COMBAT || this.savedPhase == PvpBot.Phase.ARENA_ENTER) {
               this.say("похоже, я уже у арены — продолжаю.");
               this.finishLobbyWalk(this.savedPhase, null);
            } else if (this.hasKit()) {
               this.say("похоже, я уже у арены с китом — в бой.");
               this.finishLobbyWalk(PvpBot.Phase.ARENA_ENTER, null);
            } else {
               this.say("похоже, я уже у арены, но без кита — ресток заново.");
               this.finishLobbyWalk(PvpBot.Phase.R_GM1, null);
            }

            return;
         }
      }

      this.ticksSinceJoin++;
      if (this.lobbyReturned && this.ticksSinceJoin >= 60) {
         this.exitLobbyResume();
      } else {
         this.navigator.moveTo(mc, 3.0, -43.0, 1.5, true, false);
      }
   }

   private void exitLobbyResume() {
      this.finishLobbyWalk(this.savedPhase, "вернули обратно, продолжаю с фазы " + this.savedPhase + "…");
   }

   private void finishLobbyWalk(PvpBot.Phase target, String msg) {
      this.lobbyActive = false;
      this.walkStarted = false;
      this.lobbyReturned = false;
      this.releaseMovement();
      if (this.lobbyAuraWasOn) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null && !aura.isEnabled()) {
            aura.setEnabled(true);
         }

         this.lobbyAuraWasOn = false;
      }

      if (msg != null) {
         this.say(msg);
      }

      FluxLogger.info("[PvpBot] back from lobby, resume phase=" + target);
      this.enterPhase(target);
   }

   private void scanSkins() {
      if (this.skinScanCooldown > 0) {
         this.skinScanCooldown--;
      } else {
         this.skinScanCooldown = 100;

         try {
            if (mc.getNetworkHandler() == null) {
               return;
            }

            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
               UUID id;
               String gameName;
               try {
                  id = entry.getProfile().getId();
                  gameName = entry.getProfile().getName();
               } catch (Exception var9) {
                  continue;
               }

               if (id != null && this.seenProfiles.add(id)) {
                  String line = (gameName != null ? gameName : "?") + " | " + id;
                  FluxLogger.info("[SkinWatch] " + line);

                  try {
                     File f = new File(mc.runDirectory, "fluxclient/skinwatch.log");
                     File parent = f.getParentFile();
                     if (parent != null) {
                        parent.mkdirs();
                     }

                     Files.writeString(f.toPath(), line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                  } catch (Exception var8) {
                  }
               }
            }
         } catch (Exception var10) {
         }
      }
   }

   private void updateAuraGate(PlayerEntity insider) {
      try {
         LivingEntity target = Aura.target;
         if (target != null && target.isAlive() && this.isOutsideArena(target)) {
            float reach = 4.3F;

            try {
               Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
               if (aura != null) {
                  reach = aura.attackDistance.get() + 1.0F;
               }
            } catch (Exception var5) {
            }

            if (insider == null || mc.player.distanceTo(insider) > reach) {
               this.setAuraEnabled(false);
               return;
            }
         }
      } catch (Exception var6) {
      }

      this.setAuraEnabled(true);
   }

   private boolean isOutsideArena(Entity e) {
      try {
         double leash = this.leashRadius.get().floatValue() + 1.5;
         double dx = e.getX() - -6.0;
         double dz = e.getZ() - 289.0;
         return Math.sqrt(dx * dx + dz * dz) > leash;
      } catch (Exception var8) {
         return false;
      }
   }

   private void doCombat() {
      PlayerEntity enemy = this.findNearestEnemy();
      this.updateAuraGate(enemy);
      if (this.maceProtect.get()) {
         PlayerEntity threat = this.findMaceThreat();
         if (threat != null) {
            if (!this.maceBlocking) {
               this.maceBlocking = true;
               this.say("булава сверху — блок щитом!");
            }

            this.lookAtEntity(threat);
            this.releaseMovement();
            this.ensureShieldOffhand();
            mc.options.useKey.setPressed(true);
            return;
         }
      }

      if (this.maceBlocking) {
         this.maceBlocking = false;
         mc.options.useKey.setPressed(false);
      }

      float hpNow = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      if (this.autoHeal.get() && hpNow < this.healHealth.get()) {
         this.stopMiningProgress();
         this.enterPhase(PvpBot.Phase.HEAL_GOTO);
      } else {
         double leash = this.leashRadius.get().floatValue();
         double dxA = -6.0 - mc.player.getX();
         double dzA = 289.0 - mc.player.getZ();
         double distA = Math.sqrt(dxA * dxA + dzA * dzA);
         if (distA > leash) {
            if (!this.outOfBounds) {
               this.outOfBounds = true;
               this.say("вышел за арену, возвращаюсь…");
            }
         } else if (distA < leash - 3.0) {
            this.outOfBounds = false;
         }

         if (this.outOfBounds) {
            if (this.navigator.moveTo(mc, -6.0, 289.0, Math.max(1.5, leash - 3.0), true, true)) {
               this.outOfBounds = false;
            }
         } else if (enemy == null) {
            if (!this.moneyBlocks.get() || !this.doMineTick()) {
               this.stopMiningProgress();
               this.releaseMovement();
            }
         } else if (!this.moneyBlocks.get() || !(mc.player.distanceTo(enemy) > 12.0) || !this.doMineTick()) {
            this.stopMiningProgress();
            ItemStack held = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
            if (this.isWeaponItem(held) && this.weaponCheckCooldown > 0) {
               this.weaponCheckCooldown--;
            } else {
               this.selectCombatWeapon();
               this.weaponCheckCooldown = 10;
            }

            this.lookAtEntity(enemy);
            double dist = mc.player.distanceTo(enemy);
            if (dist > 2.4) {
               this.navigator.moveTo(mc, enemy.getX(), enemy.getZ(), 2.4, true, true);
            } else {
               mc.options.forwardKey.setPressed(true);
               mc.options.sprintKey.setPressed(false);
               if (this.strafeTicks <= 0) {
                  this.strafeDir = -this.strafeDir;
                  this.strafeTicks = Math.max(6, Math.round(this.strafeTime.get() * 20.0F * (0.7F + this.rng.nextFloat() * 0.6F)));
               } else {
                  this.strafeTicks--;
               }

               mc.options.leftKey.setPressed(this.strafeDir < 0);
               mc.options.rightKey.setPressed(this.strafeDir > 0);
               mc.options.jumpKey.setPressed(false);
            }
         }
      }
   }

   private PlayerEntity findNearestEnemy() {
      if (mc.world != null && mc.player != null) {
         double maxDist = this.chaseRadius.get().floatValue();
         double leash = this.leashRadius.get().floatValue();
         double hunt = leash + 1.5;
         PlayerEntity best = null;
         double bestDist = Double.MAX_VALUE;

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof PlayerEntity p && p != mc.player && p.isAlive() && !(p.getHealth() <= 0.0F) && !p.isSpectator() && !this.isIgnored(p)) {
               double d = mc.player.distanceTo(p);
               if (!(d > maxDist) && !(d >= bestDist)) {
                  double pdx = p.getX() - -6.0;
                  double pdz = p.getZ() - 289.0;
                  if (!(Math.sqrt(pdx * pdx + pdz * pdz) > hunt)) {
                     bestDist = d;
                     best = p;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void selectCombatWeapon() {
      if (mc.player != null && !mc.player.isUsingItem()) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         int axe = this.findWeaponSlot(true, true);
         if (axe < 0) {
            axe = this.findWeaponSlot(true, false);
         }

         if (axe >= 0) {
            this.selectSlot(axe);
            if (aura != null && aura.onlyCriticals.get()) {
               aura.onlyCriticals.set(false);
               this.say("режим: топор-спам (без критов).");
            }

            this.logWeapon(axe);
         } else {
            int sword = this.findWeaponSlot(false, true);
            if (sword < 0) {
               sword = this.findWeaponSlot(false, false);
            }

            if (sword >= 0) {
               this.selectSlot(sword);
               if (aura != null && !aura.onlyCriticals.get()) {
                  aura.onlyCriticals.set(true);
                  this.say("режим: меч-криты.");
               }

               this.logWeapon(sword);
            } else {
               int any = this.findAnyWeapon();
               if (any >= 0) {
                  this.selectSlot(any);
                  if (aura != null && !aura.onlyCriticals.get()) {
                     aura.onlyCriticals.set(true);
                  }

                  this.logWeapon(any);
               }
            }
         }
      }
   }

   private void logWeapon(int hotbar) {
      try {
         String n = mc.player.getInventory().getStack(hotbar).getName().getString();
         if (!n.equals(this.lastWeapon)) {
            this.lastWeapon = n;
            this.say("оружие: " + n + " (слот " + (hotbar + 1) + ").");
         }
      } catch (Exception var3) {
      }
   }

   private int findWeaponSlot(boolean axe, boolean namedOnly) {
      PlayerInventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (this.matchesWeapon(inv.getStack(i), axe, namedOnly)) {
            return i;
         }
      }

      for (int ix = 9; ix < inv.size(); ix++) {
         if (this.matchesWeapon(inv.getStack(ix), axe, namedOnly)) {
            this.swapToHotbar(ix, 0);
            return 0;
         }
      }

      return -1;
   }

   private boolean matchesWeapon(ItemStack s, boolean axe, boolean namedOnly) {
      if (s.isEmpty()) {
         return false;
      } else {
         boolean itemOk = axe ? s.isOf(Items.DIAMOND_AXE) : s.isOf(Items.DIAMOND_SWORD);
         if (!itemOk) {
            return false;
         } else if (!namedOnly) {
            return true;
         } else {
            String n = s.getName().getString().toLowerCase(Locale.ROOT);
            return axe ? n.contains("rangeros") : n.contains("k'thun") || n.contains("kthun");
         }
      }
   }

   private boolean isWeaponItem(ItemStack s) {
      return s.isEmpty()
         ? false
         : s.isOf(Items.DIAMOND_AXE)
            || s.isOf(Items.DIAMOND_SWORD)
            || s.isOf(Items.NETHERITE_AXE)
            || s.isOf(Items.NETHERITE_SWORD)
            || s.isOf(Items.IRON_AXE)
            || s.isOf(Items.IRON_SWORD);
   }

   private int findAnyWeapon() {
      PlayerInventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getStack(i);
         if (s.isOf(Items.DIAMOND_AXE)
            || s.isOf(Items.DIAMOND_SWORD)
            || s.isOf(Items.NETHERITE_AXE)
            || s.isOf(Items.NETHERITE_SWORD)
            || s.isOf(Items.IRON_AXE)
            || s.isOf(Items.IRON_SWORD)) {
            return i;
         }
      }

      return -1;
   }

   private void selectSlot(int hotbar) {
      if (mc.player.getInventory().selectedSlot != hotbar) {
         mc.player.getInventory().selectedSlot = hotbar;
      }
   }

   private void swapToHotbar(int invSlot, int hotbar) {
      try {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, invSlot, hotbar, SlotActionType.SWAP, mc.player);
      } catch (Exception var4) {
         FluxLogger.warn("[PvpBot] hotbar swap failed: " + var4.getMessage());
      }
   }

   private void onArenaEnter() {
      this.arenaEnterTicks = 0;
      this.arenaRunTicks = 100 + this.rng.nextInt(41);
      this.weaponCheckCooldown = 0;
      this.selectCombatWeapon();
      this.say("на арене. Бегу прямо " + this.arenaRunTicks / 20 + " сек с прыжками…");
   }

   private void doArenaEnter() {
      this.setAuraEnabled(false);
      this.arenaEnterTicks++;
      if (this.arenaEnterTicks >= this.arenaRunTicks) {
         this.say("добежал, перехожу в бой.");
         this.enterPhase(PvpBot.Phase.COMBAT);
      } else {
         mc.options.forwardKey.setPressed(true);
         mc.options.sprintKey.setPressed(mc.player.getHungerManager().getFoodLevel() > 6);
         mc.options.jumpKey.setPressed(true);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
      }
   }

   private PlayerEntity findMaceThreat() {
      if (mc.world != null && mc.player != null) {
         PlayerEntity best = null;
         double bestDist = Double.MAX_VALUE;

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof PlayerEntity p && p != mc.player && p.isAlive() && !(p.getHealth() <= 0.0F) && !p.isSpectator() && !this.isIgnored(p)) {
               double dx = p.getX() - mc.player.getX();
               double dz = p.getZ() - mc.player.getZ();
               double horiz = Math.sqrt(dx * dx + dz * dz);
               double dy = p.getY() - mc.player.getY();
               if (!(dy < 2.0) && !(dy > 12.0) && !(horiz > 4.5) && !(horiz >= bestDist)) {
                  boolean falling = p.getVelocity().y < -0.05 || p.fallDistance > 0.3F;
                  if (falling) {
                     bestDist = horiz;
                     best = p;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void ensureShieldOffhand() {
      try {
         if (mc.player.getOffHandStack().isOf(Items.SHIELD)) {
            return;
         }

         PlayerScreenHandler handler = mc.player.playerScreenHandler;

         for (int slot = 9; slot <= 44; slot++) {
            if (handler.getSlot(slot).getStack().isOf(Items.SHIELD)) {
               mc.interactionManager.clickSlot(handler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
               this.interactCooldown = 4;
               return;
            }
         }
      } catch (Exception var3) {
         FluxLogger.warn("[PvpBot] shield to offhand failed: " + var3.getMessage());
      }
   }

   private void doHealGoto() {
      this.setAuraEnabled(false);
      if (this.navigator.moveTo(mc, -21.0, 289.0, 2.0, true, true)) {
         this.enterPhase(PvpBot.Phase.HEAL_WAIT);
      }
   }

   private void doHealWait() {
      this.setAuraEnabled(false);
      this.releaseMovement();
      if (this.phaseTicks >= 300) {
         this.say("похилился, возвращаюсь.");
         this.enterPhase(PvpBot.Phase.COMBAT);
      }
   }

   private boolean isMoneyBlock(BlockState st) {
      return st.isOf(Blocks.IRON_BLOCK)
         || st.isOf(Blocks.GOLD_BLOCK)
         || st.isOf(Blocks.EMERALD_BLOCK)
         || st.isOf(Blocks.DIAMOND_BLOCK)
         || st.isOf(Blocks.OBSIDIAN);
   }

   private BlockPos findMoneyBlock() {
      double leash = this.leashRadius.get().floatValue();
      BlockPos origin = mc.player.getBlockPos();
      int r = Math.max(6, Math.round((float)leash));
      BlockPos closest = null;
      double best = Double.MAX_VALUE;

      for (int x = -r; x <= r; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -r; z <= r; z++) {
               double d = (double)x * x + (double)y * y + (double)z * z;
               if (!(d >= best)) {
                  BlockPos p = origin.add(x, y, z);
                  if (!mc.world.isOutOfHeightLimit(p.getY()) && mc.world.isChunkLoaded(p) && this.isMoneyBlock(mc.world.getBlockState(p))) {
                     double adx = p.getX() + 0.5 - -6.0;
                     double adz = p.getZ() + 0.5 - 289.0;
                     if (!(Math.sqrt(adx * adx + adz * adz) > leash)) {
                        best = d;
                        closest = p.toImmutable();
                     }
                  }
               }
            }
         }
      }

      return closest;
   }

   private int bestPickaxeHotbar() {
      int fallback = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(Items.NETHERITE_PICKAXE)) {
            return i;
         }

         if (fallback < 0
            && (
               s.isOf(Items.DIAMOND_PICKAXE)
                  || s.isOf(Items.IRON_PICKAXE)
                  || s.isOf(Items.STONE_PICKAXE)
                  || s.isOf(Items.WOODEN_PICKAXE)
                  || s.isOf(Items.GOLDEN_PICKAXE)
            )) {
            fallback = i;
         }
      }

      return fallback;
   }

   private boolean doMineTick() {
      int pick = this.bestPickaxeHotbar();
      if (pick < 0) {
         this.stopMiningProgress();
         return false;
      } else {
         BlockPos block = this.findMoneyBlock();
         if (block == null) {
            this.stopMiningProgress();
            return false;
         } else if (mc.player.isUsingItem()) {
            this.stopMiningProgress();
            return true;
         } else {
            this.lookAt(block);
            this.navigator.moveTo(mc, block.getX() + 0.5, block.getZ() + 0.5, 2.2, false, true);
            double eyeDist = mc.player.getEyePos().distanceTo(block.toCenterPos());
            if (eyeDist > 4.5) {
               this.stopMiningProgress();
               return true;
            } else {
               if (mc.player.getInventory().selectedSlot != pick) {
                  mc.player.getInventory().selectedSlot = pick;
               }

               this.mineBlock(block);
               return true;
            }
         }
      }
   }

   private void mineBlock(BlockPos block) {
      Direction side = this.bestSide(block);
      boolean isNewTarget = this.miningPos == null || !this.miningPos.equals(block) || this.miningSide != side;
      if (!isNewTarget && mc.interactionManager.isBreakingBlock()) {
         mc.interactionManager.updateBlockBreakingProgress(block, side);
      } else {
         if (isNewTarget && this.miningPos != null) {
            mc.interactionManager.cancelBlockBreaking();
         }

         mc.interactionManager.attackBlock(block, side);
         this.miningPos = block.toImmutable();
         this.miningSide = side;
      }

      mc.options.attackKey.setPressed(true);
      if (this.swingCooldown <= 0) {
         mc.player.swingHand(Hand.MAIN_HAND);
         this.swingCooldown = 4;
      } else {
         this.swingCooldown--;
      }
   }

   private void stopMiningProgress() {
      this.miningPos = null;
      this.miningSide = null;
      this.swingCooldown = 0;

      try {
         if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
         }
      } catch (Exception var3) {
      }

      try {
         if (mc.options != null) {
            mc.options.attackKey.setPressed(false);
         }
      } catch (Exception var2) {
      }
   }

   private void onRespawned() {
      FluxLogger.info("[PvpBot] respawned, starting recover cycle");
      this.say("возродился. Начинаю возврат кита…");
      this.releaseMovement();
      mc.player.getInventory().selectedSlot = 1;
      this.enderPos = null;
      this.placedChestPos = null;
   }

   private void enterPhase(PvpBot.Phase next) {
      if (next.name().startsWith("R_")) {
         this.setAuraEnabled(false);
      }

      this.phase = next;
      this.phaseTicks = 0;
      this.interactCooldown = 0;
      switch (next) {
         case COMBAT:
            this.say("в бою. Отбиваюсь.");
            this.weaponCheckCooldown = 0;
            this.selectCombatWeapon();
            break;
         case ARENA_ENTER:
            this.onArenaEnter();
            break;
         case HEAL_GOTO:
            this.say("мало хп, бегу хилиться…");
            break;
         case HEAL_WAIT:
            this.say("стою хилиюсь 15 сек…");
            break;
         case R_GM1:
            this.say("/gm 1 …");
            this.sendCmd("gm 1");
            break;
         case R_HOME:
            this.say("/home …");
            this.sendCmd("home");
            this.expectTransferUntil = System.currentTimeMillis() + 15000L;
            break;
         case R_ENDER_OPEN:
            this.say("ищу эндер-сундук…");
            this.enderPos = null;
            break;
         case R_ENDER_CLONE:
            this.chestCountOnCloneEnter = this.countItem(Items.CHEST);
            this.say("копирую сундук колесом (CLONE)…");
            break;
         case R_RTP:
            this.closeScreen();
            this.say("/rtp …");
            this.sendCmd("rtp");
            this.expectTransferUntil = System.currentTimeMillis() + 15000L;
            break;
         case R_PLACE:
            this.placeAttempts = 0;
            this.placeTimer = 0;
            this.placeTimerAction = 0;
            this.placeConfirmTicks = 0;
            this.say("ставлю сундук…");
            break;
         case R_LOOT:
            this.say("открываю сундук, ChestStealer забирает лут…");
            this.enableStealer();
            break;
         case R_GEAR:
            this.say("одеваюсь и раскладываю хотбар…");
            break;
         case R_WARP:
            this.closeScreen();
            this.disableStealer();
            this.say("/warp pvp …");
            this.sendCmd("warp pvp");
            this.expectTransferUntil = System.currentTimeMillis() + 15000L;
      }

      FluxLogger.info("[PvpBot] phase=" + next);
   }

   private void doWaitDelay(PvpBot.Phase next) {
      this.releaseMovement();
      if (this.phaseTicks >= this.delayTicks()) {
         this.enterPhase(next);
      }
   }

   private int delayTicks() {
      return Math.max(20, Math.round(this.commandDelay.get() * 20.0F));
   }

   private void doEnderOpen() {
      if (mc.player.getInventory().selectedSlot != 1) {
         mc.player.getInventory().selectedSlot = 1;
      }

      if (this.isContainerOpen()) {
         this.enterPhase(PvpBot.Phase.R_ENDER_CLONE);
      } else {
         if (this.interactCooldown > 0) {
            this.interactCooldown--;
         }

         int radius = Math.round(this.enderRadius.get());
         if (this.enderPos == null || !mc.world.getBlockState(this.enderPos).isOf(Blocks.ENDER_CHEST)) {
            this.enderPos = this.findNearestBlock(Blocks.ENDER_CHEST, radius, Math.min(radius, 12));
            if (this.enderPos == null) {
               this.releaseMovement();
               if (this.phaseTicks % 100 == 1) {
                  this.say("эндер-сундук не найден в радиусе " + radius + ", жду…");
               }

               return;
            }
         }

         double eyeDist = mc.player.getEyePos().distanceTo(this.enderPos.toCenterPos());
         if (eyeDist > 4.5) {
            this.lookAt(this.enderPos);
            this.navigator.moveTo(mc, this.enderPos.getX() + 0.5, this.enderPos.getZ() + 0.5, 2.2, true, true);
         } else {
            this.releaseMovement();
            this.lookAt(this.enderPos);
            if (this.interactCooldown <= 0) {
               this.interactBlock(this.enderPos);
               this.interactCooldown = 12;
            }
         }
      }
   }

   private void doEnderClone() {
      this.releaseMovement();
      if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h)) {
         this.enterPhase(PvpBot.Phase.R_ENDER_OPEN);
      } else {
         if (this.interactCooldown > 0) {
            this.interactCooldown--;
         }

         if (this.countItem(Items.CHEST) > this.chestCountOnCloneEnter) {
            this.closeScreen();
            this.enterPhase(PvpBot.Phase.R_RTP);
         } else {
            if (this.interactCooldown <= 0) {
               try {
                  ItemStack cursor = h.getCursorStack();
                  if (!cursor.isEmpty() && cursor.isOf(Items.CHEST)) {
                     int empty = -1;
                     int invSize = h.getRows() * 9;

                     for (int i = invSize; i < h.slots.size(); i++) {
                        if (h.getSlot(i).getStack().isEmpty()) {
                           empty = i;
                           break;
                        }
                     }

                     if (empty >= 0) {
                        mc.interactionManager.clickSlot(h.syncId, empty, 0, SlotActionType.PICKUP, mc.player);
                     }
                  } else {
                     int chestSlot = this.findContainerSlot(Items.CHEST);
                     if (chestSlot < 0) {
                        if (this.phaseTicks % 100 == 1) {
                           this.say("в эндере нет сундуков, жду…");
                        }
                     } else if (this.phaseTicks < 400) {
                        mc.interactionManager.clickSlot(h.syncId, chestSlot, 2, SlotActionType.CLONE, mc.player);
                     } else {
                        mc.interactionManager.clickSlot(h.syncId, chestSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                     }
                  }
               } catch (Exception var6) {
                  FluxLogger.warn("[PvpBot] clone click failed: " + var6.getMessage());
               }

               this.interactCooldown = 6;
            }

            if (this.phaseTicks > 600) {
               this.closeScreen();
               this.enterPhase(PvpBot.Phase.R_ENDER_OPEN);
            }
         }
      }
   }

   private void doPlace() {
      if (this.interactCooldown > 0) {
         this.interactCooldown--;
      }

      if (this.placeTimer > 0) {
         this.releaseMovement();
         this.placeTimer--;
         if (this.placeTimer == 0) {
            if (this.placeTimerAction == 1) {
               this.say("повторяю /rtp…");
               this.sendCmd("rtp");
               this.placeTimer = this.delayTicks();
               this.placeTimerAction = 2;
            } else {
               this.placeAttempts = 0;
               this.placeTimerAction = 0;
               this.placeConfirmTicks = 0;
            }
         }
      } else if (this.countItem(Items.CHEST) <= 0) {
         this.say("нет сундука в инвентаре — иду за новым…");
         this.enterPhase(PvpBot.Phase.R_HOME);
      } else {
         this.ensureInHotbarAndSelect(Items.CHEST, 8);
         if (!mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).isOf(Items.CHEST)) {
            this.releaseMovement();
         } else {
            BlockPos existing = this.findNearestBlock(Blocks.CHEST, 5, 4);
            if (existing != null) {
               this.placedChestPos = existing.toImmutable();
               this.enterPhase(PvpBot.Phase.R_LOOT);
            } else {
               BlockPos ground = this.findPlaceGround();
               if (ground == null) {
                  this.releaseMovement();
                  if (this.phaseTicks % 40 == 1) {
                     this.placeAttempts++;
                     if (this.placeAttempts >= 5) {
                        this.startRtpRetry("нет места для сундука");
                        return;
                     }
                  }

                  if (this.phaseTicks % 100 == 1) {
                     this.say("нет места для сундука, жду…");
                  }
               } else {
                  this.releaseMovement();
                  this.lookAt(ground);
                  if (this.placeConfirmTicks > 0) {
                     this.placeConfirmTicks--;
                     if (this.placeConfirmTicks != 0) {
                        return;
                     }

                     BlockPos confirmed = this.findNearestBlock(Blocks.CHEST, 6, 4);
                     if (confirmed != null) {
                        this.placedChestPos = confirmed.toImmutable();
                        this.enterPhase(PvpBot.Phase.R_LOOT);
                        return;
                     }

                     if (++this.placeAttempts >= 5) {
                        this.startRtpRetry("сундук не ставится");
                        return;
                     }
                  }

                  if (this.interactCooldown <= 0) {
                     Vec3d hit = Vec3d.ofCenter(ground).add(0.0, 0.5, 0.0);
                     BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, ground, false);

                     try {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.placeConfirmTicks = 15;
                     } catch (Exception var6) {
                        FluxLogger.warn("[PvpBot] place failed: " + var6.getMessage());
                     }

                     this.interactCooldown = 10;
                  }
               }
            }
         }
      }
   }

   private void startRtpRetry(String reason) {
      this.say(reason + " (5 попыток) — жду 20 сек и повторяю /rtp…");
      FluxLogger.info("[PvpBot] place failed x5 (" + reason + "), retry /rtp in 20s");
      this.placeTimer = 400;
      this.placeTimerAction = 1;
   }

   private void enterPlaceRetry() {
      this.phase = PvpBot.Phase.R_PLACE;
      this.phaseTicks = 0;
      this.interactCooldown = 0;
      this.placeConfirmTicks = 0;
      this.say("ставлю сундук…");
      FluxLogger.info("[PvpBot] phase=R_PLACE (retry, attempts=" + this.placeAttempts + ")");
   }

   private void doLoot() {
      if (this.interactCooldown > 0) {
         this.interactCooldown--;
      }

      if (this.placedChestPos != null && !mc.world.getBlockState(this.placedChestPos).isOf(Blocks.CHEST)) {
         if (this.hasKit()) {
            this.enterPhase(PvpBot.Phase.R_GEAR);
         } else {
            this.placedChestPos = this.findNearestBlock(Blocks.CHEST, 6, 4);
            if (this.placedChestPos == null) {
               this.enterPlaceRetry();
            }
         }
      } else if (this.isContainerOpen()) {
         this.releaseMovement();
         if (this.phaseTicks > 300) {
            this.closeScreen();
            this.enterPhase(PvpBot.Phase.R_GEAR);
         }
      } else if (this.phaseTicks > 20 && this.hasKit()) {
         this.enterPhase(PvpBot.Phase.R_GEAR);
      } else if (this.phaseTicks > 400) {
         this.enterPhase(PvpBot.Phase.R_GEAR);
      } else {
         if (this.placedChestPos == null) {
            this.placedChestPos = this.findNearestBlock(Blocks.CHEST, 8, 5);
            if (this.placedChestPos == null) {
               this.enterPlaceRetry();
               return;
            }
         }

         double eyeDist = mc.player.getEyePos().distanceTo(this.placedChestPos.toCenterPos());
         if (eyeDist > 4.5) {
            this.lookAt(this.placedChestPos);
            this.navigator.moveTo(mc, this.placedChestPos.getX() + 0.5, this.placedChestPos.getZ() + 0.5, 2.2, true, true);
         } else {
            this.releaseMovement();
            this.lookAt(this.placedChestPos);
            if (this.interactCooldown <= 0) {
               this.interactBlock(this.placedChestPos);
               this.interactCooldown = 12;
            }
         }
      }
   }

   private void doGear() {
      this.releaseMovement();
      if (this.interactCooldown > 0) {
         this.interactCooldown--;
      }

      if (!this.autoGear.get()) {
         this.enterPhase(PvpBot.Phase.R_WARP);
      } else if (this.isContainerOpen()) {
         this.closeScreen();
      } else if (this.interactCooldown <= 0) {
         int armorSlot = this.findInventoryHandlerSlot(s -> s.getItem() instanceof ArmorItem);
         if (armorSlot >= 0) {
            try {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, armorSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            } catch (Exception var3) {
               FluxLogger.warn("[PvpBot] armor equip failed: " + var3.getMessage());
            }

            this.interactCooldown = 3;
         } else if (!this.layoutOne(0, s -> s.isOf(Items.DIAMOND_SWORD))) {
            if (!this.layoutOne(1, s -> s.isOf(Items.DIAMOND_SWORD))) {
               if (!this.layoutOne(2, s -> s.isOf(Items.DIAMOND_AXE))) {
                  if (!this.layoutOne(3, s -> s.isOf(Items.NETHERITE_PICKAXE))) {
                     if (!this.layoutOne(4, s -> s.isOf(Items.ENCHANTED_GOLDEN_APPLE) || s.isOf(Items.GOLDEN_APPLE))) {
                        if (!this.layoutOne(5, s -> s.isOf(Items.ENDER_PEARL))) {
                           if (!this.layoutOne(6, s -> s.isOf(Items.SHIELD))) {
                              if (this.phaseTicks > 100 || this.isGearReady()) {
                                 this.enterPhase(PvpBot.Phase.R_WARP);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void enableCombatModules() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      this.setLinked(mm.getModule(Aura.class), true, 0);
      this.setLinked(mm.getModule(AutoTotem.class), true, 2);
      this.setLinked(mm.getModule(AutoRespawn.class), true, 3);
      if (this.autoReconnect.get()) {
         this.setLinked(mm.getModule(AutoReconnect.class), true, 5);
      }
   }

   private void setLinked(Module m, boolean on, int idx) {
      if (m != null) {
         boolean[] flags = this.linkedFlags();
         if (on && !m.isEnabled()) {
            m.setEnabled(true);
            flags[idx] = true;
         }

         if (!on && flags[idx]) {
            if (m.isEnabled()) {
               m.setEnabled(false);
            }

            flags[idx] = false;
         }
      }
   }

   private boolean[] linkedFlags() {
      return this.linked;
   }

   private void setAuraEnabled(boolean on) {
      this.setLinked(FluxContext.get().getModuleManager().getModule(Aura.class), on, 0);
   }

   private void enableStealer() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      ChestStealer stealer = mm.getModule(ChestStealer.class);
      if (stealer != null && !stealer.isEnabled()) {
         stealer.setEnabled(true);
         this.linkedFlags()[4] = true;
      }
   }

   private void disableStealer() {
      if (this.linkedFlags()[4]) {
         ChestStealer stealer = FluxContext.get().getModuleManager().getModule(ChestStealer.class);
         if (stealer != null && stealer.isEnabled()) {
            stealer.setEnabled(false);
         }

         this.linkedFlags()[4] = false;
      }
   }

   private void applySafeProfile() {
      if (this.safeCombat.get()) {
         try {
            Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
            if (aura == null) {
               return;
            }

            this.savedRange = aura.attackDistance.get();
            this.savedRotMode = aura.rotationMode.get();
            this.savedWalls = aura.walls.get();
            if (aura.attackDistance.get() > 3.3F) {
               aura.attackDistance.set(3.3F);
            }

            if (!aura.rotationMode.is("FunTime")) {
               aura.rotationMode.set("FunTime");
            }

            if (aura.walls.get()) {
               aura.walls.set(false);
            }

            ChestStealer stealer = FluxContext.get().getModuleManager().getModule(ChestStealer.class);
            if (stealer != null) {
               this.savedStealerDelay = stealer.delay.get();
               if (stealer.delay.get() < 3.0F) {
                  stealer.delay.set(3.0F);
               }
            }

            this.safeApplied = true;
            this.say("безопасный профиль Aura включён (античит).");
         } catch (Exception var3) {
            FluxLogger.warn("[PvpBot] safe profile failed: " + var3.getMessage());
         }
      }
   }

   private void restoreSafeProfile() {
      if (this.safeApplied) {
         this.safeApplied = false;

         try {
            Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
            if (aura == null) {
               return;
            }

            if (this.savedRange > 0.0F) {
               aura.attackDistance.set(this.savedRange);
            }

            if (this.savedRotMode != null) {
               aura.rotationMode.set(this.savedRotMode);
            }

            aura.walls.set(this.savedWalls);
            ChestStealer stealer = FluxContext.get().getModuleManager().getModule(ChestStealer.class);
            if (stealer != null && this.savedStealerDelay >= 0.0F) {
               stealer.delay.set(this.savedStealerDelay);
            }
         } catch (Exception var3) {
         }

         this.savedRange = -1.0F;
         this.savedRotMode = null;
         this.savedStealerDelay = -1.0F;
      }
   }

   private boolean isIgnored(PlayerEntity p) {
      try {
         String raw = this.ignoreUUIDs.get();
         if (raw == null || raw.isBlank()) {
            return false;
         }

         String uuid = p.getUuid().toString().toLowerCase(Locale.ROOT);

         for (String part : raw.split("[,;\\s]+")) {
            if (!part.isEmpty() && uuid.equals(part.toLowerCase(Locale.ROOT))) {
               return true;
            }
         }
      } catch (Exception var8) {
      }

      return false;
   }

   private void restoreModules() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      this.setLinked(mm.getModule(Aura.class), false, 0);
      this.setLinked(mm.getModule(AutoTotem.class), false, 2);
      this.setLinked(mm.getModule(AutoRespawn.class), false, 3);
      this.setLinked(mm.getModule(AutoReconnect.class), false, 5);
      this.disableStealer();
      this.restoreSafeProfile();
      if (this.savedOnlyCrits != null) {
         try {
            Aura aura = mm.getModule(Aura.class);
            if (aura != null) {
               aura.onlyCriticals.set(this.savedOnlyCrits);
            }
         } catch (Exception var3) {
         }

         this.savedOnlyCrits = null;
      }

      this.lastWeapon = "";
   }

   @EventBus.Subscribe
   public void onDeathEvent(DeathEvent event) {
      this.deathSignal = true;
   }

   @EventBus.Subscribe
   public void onServerJoin(ServerJoinEvent event) {
      this.ticksSinceJoin = 0;
      if (this.lobbyActive && this.walkStarted) {
         this.lobbyReturned = true;
      } else if (this.wasInGame) {
         if (System.currentTimeMillis() < this.expectTransferUntil) {
            this.expectTransferUntil = 0L;
            FluxLogger.info("[PvpBot] own warp transfer, continue");
         } else {
            if (!this.lobbyActive) {
               this.say("зафиксировал перемещение — ухожу в режим лобби…");
               this.enterLobby();
            }
         }
      }
   }

   @EventBus.Subscribe
   public void onTransfer(TransferEvent event) {
      this.wasInGame = true;
      this.expectTransferUntil = 0L;
      if (!this.lobbyActive) {
         this.say("зафиксировал перемещение — ухожу в режим лобби…");
         this.enterLobby();
      }
   }

   @EventBus.Subscribe
   public void onPlaceDenied(PlaceDeniedEvent event) {
      if ((this.phase == PvpBot.Phase.R_PLACE || this.phase == PvpBot.Phase.R_LOOT) && this.placeTimer == 0) {
         this.placeAttempts++;
         FluxLogger.info("[PvpBot] place denied x" + this.placeAttempts);
         if (this.placeAttempts >= 5) {
            if (this.phase != PvpBot.Phase.R_PLACE) {
               this.enterPlaceRetry();
            }

            this.startRtpRetry("сервер запрещает ставить");
         }
      }
   }

   private boolean isDead() {
      if (mc.currentScreen instanceof DeathScreen) {
         return true;
      } else if (mc.player == null) {
         return false;
      } else {
         try {
            return mc.player.isDead() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0F;
         } catch (Exception var2) {
            return false;
         }
      }
   }

   private boolean isContainerOpen() {
      return mc.player != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private void closeScreen() {
      try {
         if (mc.player != null && this.isContainerOpen()) {
            mc.player.closeHandledScreen();
         }
      } catch (Exception var2) {
      }
   }

   private int findContainerSlot(Item item) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h) {
         int var7 = h.getRows() * 9;

         for (int i = 0; i < var7; i++) {
            try {
               if (h.getSlot(i).getStack().isOf(item)) {
                  return i;
               }
            } catch (Exception var6) {
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private int countItem(Item item) {
      int n = 0;

      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(item)) {
            n += s.getCount();
         }
      }

      return n;
   }

   private int findInventoryHandlerSlot(PvpBot.StackPred pred) {
      PlayerScreenHandler handler = mc.player.playerScreenHandler;

      for (int slot = 9; slot <= 44; slot++) {
         try {
            ItemStack s = handler.getSlot(slot).getStack();
            if (!s.isEmpty() && pred.test(s)) {
               return slot;
            }
         } catch (Exception var5) {
         }
      }

      return -1;
   }

   private boolean layoutOne(int hotbar, PvpBot.StackPred pred) {
      ItemStack cur = mc.player.getInventory().getStack(hotbar);
      if (!cur.isEmpty() && pred.test(cur)) {
         return false;
      } else {
         int src = -1;
         PlayerScreenHandler handler = mc.player.playerScreenHandler;

         for (int slot = 9; slot <= 35; slot++) {
            try {
               ItemStack s = handler.getSlot(slot).getStack();
               if (!s.isEmpty() && pred.test(s)) {
                  src = slot;
                  break;
               }
            } catch (Exception var9) {
            }
         }

         if (src < 0) {
            return false;
         } else {
            try {
               mc.interactionManager.clickSlot(handler.syncId, src, hotbar, SlotActionType.SWAP, mc.player);
            } catch (Exception var8) {
               FluxLogger.warn("[PvpBot] hotbar layout failed: " + var8.getMessage());
               return false;
            }

            this.interactCooldown = 3;
            return true;
         }
      }
   }

   private void ensureInHotbarAndSelect(Item item, int preferHotbar) {
      PlayerInventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (inv.getStack(i).isOf(item)) {
            inv.selectedSlot = i;
            return;
         }
      }

      int src = this.findInventoryHandlerSlot(s -> s.isOf(item));
      if (src >= 0) {
         try {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, src, preferHotbar, SlotActionType.SWAP, mc.player);
         } catch (Exception var6) {
            FluxLogger.warn("[PvpBot] hotbar swap failed: " + var6.getMessage());
         }

         inv.selectedSlot = preferHotbar;
      }
   }

   private boolean hasKit() {
      return this.countItem(Items.DIAMOND_SWORD) > 0
         || this.countItem(Items.DIAMOND_AXE) > 0
         || this.countItem(Items.ENCHANTED_GOLDEN_APPLE) > 0
         || this.countItem(Items.GOLDEN_APPLE) > 0
         || this.countItem(Items.ENDER_PEARL) > 0
         || this.countItem(Items.SHIELD) > 0
         || this.countItem(Items.NETHERITE_PICKAXE) > 0;
   }

   private boolean isGearReady() {
      boolean sword = this.countItem(Items.DIAMOND_SWORD) > 0;
      boolean axe = this.countItem(Items.DIAMOND_AXE) > 0;
      boolean apple = this.countItem(Items.ENCHANTED_GOLDEN_APPLE) > 0 || this.countItem(Items.GOLDEN_APPLE) > 0;
      boolean pearl = this.countItem(Items.ENDER_PEARL) > 0;
      boolean shield = this.countItem(Items.SHIELD) > 0;
      boolean pick = this.countItem(Items.NETHERITE_PICKAXE) > 0;
      return sword && axe && apple && pearl && shield && pick;
   }

   private BlockPos findNearestBlock(Block block, int radius, int yRadius) {
      BlockPos origin = mc.player.getBlockPos();
      BlockPos closest = null;
      double best = Double.MAX_VALUE;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -yRadius; y <= yRadius; y++) {
            for (int z = -radius; z <= radius; z++) {
               double d = (double)x * x + (double)y * y + (double)z * z;
               if (!(d >= best)) {
                  BlockPos p = origin.add(x, y, z);
                  if (!mc.world.isOutOfHeightLimit(p.getY()) && mc.world.isChunkLoaded(p) && mc.world.getBlockState(p).isOf(block)) {
                     best = d;
                     closest = p.toImmutable();
                  }
               }
            }
         }
      }

      return closest;
   }

   private BlockPos findPlaceGround() {
      BlockPos origin = mc.player.getBlockPos();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (int x = -3; x <= 3; x++) {
         for (int y = -2; y <= 1; y++) {
            for (int z = -3; z <= 3; z++) {
               if (Math.abs(x) > 1 || Math.abs(z) > 1) {
                  BlockPos ground = origin.add(x, y, z);
                  BlockPos air = ground.up();
                  if (!mc.world.isOutOfHeightLimit(ground.getY())
                     && !mc.world.isOutOfHeightLimit(air.getY())
                     && mc.world.isChunkLoaded(ground)
                     && mc.world.isChunkLoaded(air)
                     && mc.world.getBlockState(air).isAir()
                     && mc.world.getBlockState(ground).isSolidBlock(mc.world, ground)
                     && !mc.world.getBlockState(air).isOf(Blocks.CHEST)) {
                     double d = mc.player.getEyePos().distanceTo(air.toCenterPos());
                     if (!(d > 4.5) && !(d >= bestDist)) {
                        bestDist = d;
                        best = ground.toImmutable();
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private void lookAt(BlockPos block) {
      double dx = block.getX() + 0.5 - mc.player.getX();
      double dy = block.getY() + 0.5 - mc.player.getEyeY();
      double dz = block.getZ() + 0.5 - mc.player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      mc.player.setYaw(BotNavigator.turnToward(mc.player.getYaw(), (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0), 60.0F));
      mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 89.0F));
   }

   private void lookAtEntity(Entity e) {
      Vec3d eyes = mc.player.getEyePos();
      Vec3d p = e.getEyePos();
      double dx = p.x - eyes.x;
      double dy = p.y - eyes.y;
      double dz = p.z - eyes.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      mc.player.setYaw(BotNavigator.turnToward(mc.player.getYaw(), (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0), 45.0F));
      mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 89.0F));
   }

   private void interactBlock(BlockPos pos) {
      Direction side = this.bestSide(pos);
      Vec3d hit = Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
      BlockHitResult bhr = new BlockHitResult(hit, side, pos, false);

      try {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
         mc.player.swingHand(Hand.MAIN_HAND);
      } catch (Exception var6) {
         FluxLogger.warn("[PvpBot] interact failed: " + var6.getMessage());
      }
   }

   private Direction bestSide(BlockPos block) {
      double x = mc.player.getX() - (block.getX() + 0.5);
      double y = mc.player.getEyeY() - (block.getY() + 0.5);
      double z = mc.player.getZ() - (block.getZ() + 0.5);
      double ax = Math.abs(x);
      double ay = Math.abs(y);
      double az = Math.abs(z);
      if (ax >= ay && ax >= az) {
         return x > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (ay >= az) {
         return y > 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private void releaseMovement() {
      if (mc.player != null && mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
      }
   }

   private void sendCmd(String cmdWithoutSlash) {
      try {
         mc.player.networkHandler.sendChatCommand(cmdWithoutSlash);
         FluxLogger.info("[PvpBot] -> /" + cmdWithoutSlash);
      } catch (Exception var3) {
         FluxLogger.warn("[PvpBot] command failed /" + cmdWithoutSlash + ": " + var3.getMessage());
      }
   }

   private void say(String msg) {
      try {
         if (!this.chatLog.get()) {
            return;
         }

         if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§c[PvPBot] §f" + msg));
         }
      } catch (Exception var3) {
      }
   }

   private static enum Phase {
      COMBAT,
      ARENA_ENTER,
      HEAL_GOTO,
      HEAL_WAIT,
      R_GM1,
      R_HOME,
      R_ENDER_OPEN,
      R_ENDER_CLONE,
      R_RTP,
      R_PLACE,
      R_LOOT,
      R_GEAR,
      R_WARP;
   }

   private interface StackPred {
      boolean test(ItemStack var1);
   }
}
