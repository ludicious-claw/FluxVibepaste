package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
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
import ru.kirka.fluxclient.core.FluxContext;
import ru.kirka.fluxclient.core.logger.FluxLogger;
import ru.kirka.fluxclient.event.EventBus;
import ru.kirka.fluxclient.event.impl.DeathEvent;
import ru.kirka.fluxclient.event.impl.PlaceDeniedEvent;
import ru.kirka.fluxclient.event.impl.ServerJoinEvent;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;
import ru.kirka.fluxclient.feature.ModuleManager;
import ru.kirka.fluxclient.feature.impl.combat.Aura;
import ru.kirka.fluxclient.feature.impl.combat.AutoTotem;
import ru.kirka.fluxclient.util.BotNavigator;

public class LuckyBlock extends Module {
   public final NumberSetting searchRadius = new NumberSetting("Радиус поиска", "Радиус поиска ближайшего обсидиана", 100.0F, 6.0F, 100.0F, 1.0F);
   public final NumberSetting dangerHealth = new NumberSetting("Здоровье для защиты", "Включать KillAura при опасном уровне здоровья", 8.0F, 2.0F, 16.0F, 0.5F);
   public final NumberSetting auraHoldTime = new NumberSetting("Время защиты", "Сколько секунд держать KillAura после получения урона", 5.0F, 1.0F, 15.0F, 0.5F);
   public final BooleanSetting autoRecover = new BooleanSetting("Авто-возврат", "Бесконечный цикл: смерть → кит → /warp pvp → фарм", true);
   public final NumberSetting commandDelay = new NumberSetting("Задержка команд", "Пауза между /gm 1, /home, /rtp, /warp pvp (сек)", 5.0F, 1.0F, 10.0F, 0.5F);
   public final NumberSetting enderRadius = new NumberSetting("Радиус эндера", "Где искать эндер-сундук после /home", 14.0F, 4.0F, 32.0F, 1.0F);
   public final BooleanSetting autoGear = new BooleanSetting("Авто-экипировка", "Одевать броню и раскладывать хотбар после лута", true);
   public final BooleanSetting lobbyRun = new BooleanSetting("Бег в лобби", "При кике в лобби просто бежать прямо без прыжков (вместо кликов меню)", true);
   private static final double MINE_REACH = 4.5;
   private static final double INTERACT_REACH = 4.5;
   private LuckyBlock.Phase phase = LuckyBlock.Phase.FARM;
   private int phaseTicks;
   private BlockPos target;
   private BlockPos miningPos;
   private Direction miningSide;
   private boolean enabledApple;
   private boolean enabledTotem;
   private boolean enabledRespawn;
   private boolean enabledStealer;
   private boolean enabledAura;
   private int auraHoldTicks;
   private float lastHealth;
   private int searchCooldown;
   private int swingCooldown;
   private int interactCooldown;
   private final BotNavigator navigator = new BotNavigator();
   private boolean wasDead;
   private volatile boolean deathSignal;
   private boolean wasInGame;
   private boolean lobbyActive;
   private LuckyBlock.Phase savedPhase = LuckyBlock.Phase.FARM;
   private int lobbyWorldTicks;
   private int ticksSinceJoin;
   private int lobbyClicks;
   private int lobbyClickCooldown;
   private int usePressTicks;
   private int slotRetryCooldown;
   private boolean lobbyAuraWasOn;
   private boolean lobbyWalkStarted;
   private boolean lobbyReturned;
   private int chestCountOnCloneEnter;
   private BlockPos enderPos;
   private BlockPos placedChestPos;
   private int placeAttempts;
   private int placeTimer;
   private int placeTimerAction;
   private int placeConfirmTicks;

   public LuckyBlock() {
      super("Lucky Block", "АФК-фарм обсидиана + бесконечный возврат кита", Category.MISC, -1);
      this.registerSetting(this.searchRadius);
      this.registerSetting(this.dangerHealth);
      this.registerSetting(this.auraHoldTime);
      this.registerSetting(this.autoRecover);
      this.registerSetting(this.commandDelay);
      this.registerSetting(this.enderRadius);
      this.registerSetting(this.autoGear);
      this.registerSetting(this.lobbyRun);
   }

   @Override
   protected void onEnable() {
      this.target = null;
      this.miningPos = null;
      this.miningSide = null;
      this.auraHoldTicks = 0;
      this.searchCooldown = 0;
      this.swingCooldown = 0;
      this.interactCooldown = 0;
      this.phaseTicks = 0;
      this.phase = LuckyBlock.Phase.FARM;
      this.wasDead = false;
      this.deathSignal = false;
      this.wasInGame = false;
      this.lobbyActive = false;
      this.lobbyAuraWasOn = false;
      this.lobbyWalkStarted = false;
      this.lobbyReturned = false;
      this.navigator.reset();
      this.enderPos = null;
      this.placedChestPos = null;
      this.chestCountOnCloneEnter = 0;
      this.lastHealth = mc.player != null ? mc.player.getHealth() + mc.player.getAbsorptionAmount() : 20.0F;
      this.enableProtectionModules();
      this.say("включён. Фарм обсидиана, радиус " + Math.round(this.searchRadius.get()));
      FluxLogger.info("[LuckyBlock] enabled, phase=FARM");
   }

   @Override
   protected void onDisable() {
      this.stopMining();
      this.releaseMovement();
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      if (this.lobbyAuraWasOn) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null && !aura.isEnabled()) {
            aura.setEnabled(true);
         }

         this.lobbyAuraWasOn = false;
      }

      this.lobbyActive = false;
      this.wasInGame = false;
      this.restoreModules();
      this.target = null;
      this.enderPos = null;
      this.placedChestPos = null;
      FluxLogger.info("[LuckyBlock] disabled");
   }

   @Override
   public void onTick() {
      boolean inGame = mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.networkHandler != null;
      if (!inGame) {
         if (this.wasInGame && !this.lobbyActive) {
            this.enterLobby();
         }

         if (this.lobbyActive) {
            this.stopMining();
         }
      } else {
         this.wasInGame = true;
         if (this.lobbyActive) {
            this.doLobby();
         } else {
            if (this.phase == LuckyBlock.Phase.WAIT_DEATH) {
               this.suppressSustain();
               if (mc.player != null) {
                  this.lastHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
               }
            } else {
               this.enableProtectionModules();
               this.updateEmergencyAura();
            }

            boolean dead = this.deathSignal || this.isDead();
            if (dead) {
               this.wasDead = true;
               this.deathSignal = false;
               this.stopMiningProgress();
               this.releaseMovement();
            } else if (this.wasDead) {
               this.wasDead = false;
               this.deathSignal = false;
               this.onRespawned();
               if (this.autoRecover.get()) {
                  this.enterPhase(LuckyBlock.Phase.R_GM1);
               } else {
                  this.enterPhase(LuckyBlock.Phase.FARM);
               }
            } else {
               this.phaseTicks++;
               switch (this.phase) {
                  case FARM:
                     this.doFarm();
                     break;
                  case WAIT_DEATH:
                     this.doWaitDeath();
                     break;
                  case R_GM1:
                     this.doWaitDelay(LuckyBlock.Phase.R_HOME);
                     break;
                  case R_HOME:
                     this.doWaitDelay(LuckyBlock.Phase.R_ENDER_OPEN);
                     break;
                  case R_ENDER_OPEN:
                     this.doEnderOpen();
                     break;
                  case R_ENDER_CLONE:
                     this.doEnderClone();
                     break;
                  case R_RTP:
                     this.doWaitDelay(LuckyBlock.Phase.R_PLACE);
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
                     this.doWaitDelay(LuckyBlock.Phase.FARM);
                     break;
                  default:
                     this.enterPhase(LuckyBlock.Phase.FARM);
               }
            }
         }
      }
   }

   private void enterLobby() {
      this.lobbyActive = true;
      this.savedPhase = this.phase;
      this.lobbyWorldTicks = 0;
      this.ticksSinceJoin = 0;
      this.lobbyClicks = 0;
      this.lobbyClickCooldown = 0;
      this.usePressTicks = 0;
      this.slotRetryCooldown = 0;
      this.lobbyWalkStarted = false;
      this.lobbyReturned = false;
      this.stopMining();
      this.releaseMovement();
      mc.options.useKey.setPressed(false);
      Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
      this.lobbyAuraWasOn = aura != null && aura.isEnabled();
      if (this.lobbyAuraWasOn) {
         aura.setEnabled(false);
      }

      this.say("выкинуло в лобби. Жду, открываю меню и возвращаюсь…");
      FluxLogger.info("[LuckyBlock] lobby mode, saved phase=" + this.savedPhase);
   }

   private void doLobby() {
      this.stopMining();
      mc.options.jumpKey.setPressed(false);
      this.ticksSinceJoin++;
      if (this.lobbyRun.get()) {
         if (!this.lobbyWalkStarted) {
            this.lobbyWalkStarted = true;
            this.ticksSinceJoin = 0;
            this.lobbyReturned = false;
            this.navigator.reset();
         }

         if (this.lobbyReturned && this.ticksSinceJoin >= 60) {
            this.exitLobbyResume();
         } else {
            mc.options.useKey.setPressed(false);
            this.navigator.moveTo(mc, 3.0, -43.0, 1.5, true, false);
         }
      } else if (this.lobbyClicks > 0 && this.ticksSinceJoin >= 60) {
         this.exitLobbyResume();
      } else if (this.isContainerOpen()) {
         this.releaseMovement();
         mc.options.useKey.setPressed(false);
         if (this.lobbyClickCooldown > 0) {
            this.lobbyClickCooldown--;
         } else {
            ScreenHandler handler = mc.player.currentScreenHandler;

            try {
               if (handler.slots.size() > 11) {
                  mc.interactionManager.clickSlot(handler.syncId, 11, 0, SlotActionType.PICKUP, mc.player);
                  this.lobbyClicks++;
               } else if (this.lobbyClicks == 0) {
                  this.closeScreen();
               }
            } catch (Exception var3) {
               FluxLogger.warn("[LuckyBlock] lobby click failed: " + var3.getMessage());
            }

            this.lobbyClickCooldown = 20;
         }
      } else {
         this.releaseMovement();
         this.lobbyWorldTicks++;
         if (this.lobbyWorldTicks < 100) {
            mc.options.useKey.setPressed(false);
         } else if (this.usePressTicks > 0) {
            this.usePressTicks--;
            if (this.usePressTicks == 0) {
               mc.options.useKey.setPressed(false);
            }
         } else if (this.slotRetryCooldown > 0) {
            this.slotRetryCooldown--;
         } else {
            mc.player.getInventory().selectedSlot = (mc.player.getInventory().selectedSlot + 1) % 9;
            mc.options.useKey.setPressed(true);
            this.usePressTicks = 6;
            this.slotRetryCooldown = 40;
         }
      }
   }

   private void exitLobbyResume() {
      this.lobbyActive = false;
      this.lobbyWalkStarted = false;
      this.lobbyReturned = false;
      mc.options.useKey.setPressed(false);
      this.releaseMovement();
      if (this.lobbyAuraWasOn) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null && !aura.isEnabled()) {
            aura.setEnabled(true);
         }

         this.lobbyAuraWasOn = false;
      }

      this.say("вернулся на сервер, продолжаю…");
      FluxLogger.info("[LuckyBlock] back from lobby, resume phase=" + this.savedPhase);
      this.enterPhase(this.savedPhase);
   }

   private void doFarm() {
      if (this.miningPos != null && !mc.world.getBlockState(this.miningPos).isOf(Blocks.OBSIDIAN)) {
         FluxLogger.info("[LuckyBlock] obsidian broken, waiting for death");
         this.say("лаки-блок сломан. Жду смерти для авто-возврата…");
         this.stopMining();
         this.releaseMovement();
         this.target = null;
         if (this.autoRecover.get()) {
            this.enterPhase(LuckyBlock.Phase.WAIT_DEATH);
         }
      } else {
         double maxDistSq = this.searchRadius.get() * this.searchRadius.get();
         if (this.target == null
            || !mc.world.getBlockState(this.target).isOf(Blocks.OBSIDIAN)
            || mc.player.squaredDistanceTo(this.target.toCenterPos()) > maxDistSq) {
            this.stopMining();
            if (this.searchCooldown <= 0) {
               this.target = this.findNearestObsidian();
               this.searchCooldown = 20;
            } else {
               this.searchCooldown--;
            }
         }

         if (this.target == null) {
            this.stopMining();
            this.releaseMovement();
            mc.options.jumpKey.setPressed(true);
         } else {
            BlockState targetState = mc.world.getBlockState(this.target);
            int pickaxeSlot = this.selectBestPickaxe(targetState);
            if (pickaxeSlot < 0) {
               this.stopMining();
               this.releaseMovement();
            } else {
               this.lookAt(this.target);
               this.navigator.moveTo(mc, this.target.getX() + 0.5, this.target.getZ() + 0.5, 2.2, true, true);
               if (mc.player.isUsingItem()) {
                  this.stopMiningProgress();
               } else {
                  double eyeDist = mc.player.getEyePos().distanceTo(this.target.toCenterPos());
                  if (eyeDist > 4.5) {
                     this.stopMiningProgress();
                  } else {
                     if (pickaxeSlot != mc.player.getInventory().selectedSlot) {
                        mc.player.getInventory().selectedSlot = pickaxeSlot;
                     }

                     this.mineBlock(this.target);
                  }
               }
            }
         }
      }
   }

   private void doWaitDeath() {
      this.stopMining();
      this.releaseMovement();
   }

   private void onRespawned() {
      FluxLogger.info("[LuckyBlock] respawned, starting recover cycle");
      this.say("возродился. Начинаю возврат кита…");
      this.stopMining();
      this.releaseMovement();
      mc.player.getInventory().selectedSlot = 1;
      this.target = null;
      this.enderPos = null;
      this.placedChestPos = null;
   }

   private void enterPhase(LuckyBlock.Phase next) {
      this.phase = next;
      this.phaseTicks = 0;
      this.interactCooldown = 0;
      switch (next) {
         case FARM:
            this.target = null;
            this.miningPos = null;
            this.miningSide = null;
            this.searchCooldown = 0;
            this.say("возврат завершён. Снова ищу обсидиан.");
            FluxLogger.info("[LuckyBlock] phase=FARM");
            break;
         case WAIT_DEATH:
            this.say("стою и жду смерти…");
            break;
         case R_GM1:
            this.say("/gm 1 …");
            this.sendCmd("gm 1");
            break;
         case R_HOME:
            this.say("/home …");
            this.sendCmd("home");
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
      }

      FluxLogger.info("[LuckyBlock] phase=" + next);
   }

   private void doWaitDelay(LuckyBlock.Phase next) {
      this.stopMining();
      this.releaseMovement();
      if (this.phaseTicks >= this.delayTicks()) {
         this.enterPhase(next);
      }
   }

   private int delayTicks() {
      return Math.max(20, Math.round(this.commandDelay.get() * 20.0F));
   }

   private void doEnderOpen() {
      this.stopMining();
      if (mc.player.getInventory().selectedSlot != 1) {
         mc.player.getInventory().selectedSlot = 1;
      }

      if (this.isContainerOpen()) {
         this.enterPhase(LuckyBlock.Phase.R_ENDER_CLONE);
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

            if (this.phaseTicks % 200 == 0) {
               this.say("всё ещё открываю эндер-сундук…");
            }
         }
      }
   }

   private void doEnderClone() {
      this.stopMining();
      this.releaseMovement();
      if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h)) {
         this.enterPhase(LuckyBlock.Phase.R_ENDER_OPEN);
      } else {
         if (this.interactCooldown > 0) {
            this.interactCooldown--;
         }

         if (this.countItem(Items.CHEST) > this.chestCountOnCloneEnter) {
            this.closeScreen();
            this.enterPhase(LuckyBlock.Phase.R_RTP);
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
                     } else if (this.phaseTicks % 100 == 1) {
                        this.say("нет пустого слота под копию сундука, жду…");
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
                  FluxLogger.warn("[LuckyBlock] clone click failed: " + var6.getMessage());
               }

               this.interactCooldown = 6;
            }

            if (this.phaseTicks > 600) {
               this.closeScreen();
               this.enterPhase(LuckyBlock.Phase.R_ENDER_OPEN);
            }
         }
      }
   }

   private void doPlace() {
      this.stopMining();
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
         this.enterPhase(LuckyBlock.Phase.R_HOME);
      } else {
         this.ensureInHotbarAndSelect(Items.CHEST, 8);
         if (!mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).isOf(Items.CHEST)) {
            this.releaseMovement();
         } else {
            BlockPos existing = this.findNearestBlock(Blocks.CHEST, 5, 4);
            if (existing != null) {
               this.placedChestPos = existing.toImmutable();
               this.enterPhase(LuckyBlock.Phase.R_LOOT);
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
                        this.enterPhase(LuckyBlock.Phase.R_LOOT);
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
                        FluxLogger.warn("[LuckyBlock] place failed: " + var6.getMessage());
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
      FluxLogger.info("[LuckyBlock] place failed x5 (" + reason + "), retry /rtp in 20s");
      this.placeTimer = 400;
      this.placeTimerAction = 1;
   }

   private void enterPlaceRetry() {
      this.phase = LuckyBlock.Phase.R_PLACE;
      this.phaseTicks = 0;
      this.interactCooldown = 0;
      this.placeConfirmTicks = 0;
      this.say("ставлю сундук…");
      FluxLogger.info("[LuckyBlock] phase=R_PLACE (retry, attempts=" + this.placeAttempts + ")");
   }

   private void doLoot() {
      this.stopMining();
      if (this.interactCooldown > 0) {
         this.interactCooldown--;
      }

      if (this.placedChestPos != null && !mc.world.getBlockState(this.placedChestPos).isOf(Blocks.CHEST)) {
         if (this.hasKit()) {
            this.enterPhase(LuckyBlock.Phase.R_GEAR);
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
            this.enterPhase(LuckyBlock.Phase.R_GEAR);
         }
      } else if (this.phaseTicks > 20 && this.hasKit()) {
         this.enterPhase(LuckyBlock.Phase.R_GEAR);
      } else if (this.phaseTicks > 400) {
         this.enterPhase(LuckyBlock.Phase.R_GEAR);
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
      this.stopMining();
      this.releaseMovement();
      if (this.interactCooldown > 0) {
         this.interactCooldown--;
      }

      if (!this.autoGear.get()) {
         this.enterPhase(LuckyBlock.Phase.R_WARP);
      } else if (this.isContainerOpen()) {
         this.closeScreen();
      } else if (this.interactCooldown <= 0) {
         int armorSlot = this.findInventoryHandlerSlot(s -> s.getItem() instanceof ArmorItem);
         if (armorSlot >= 0) {
            try {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, armorSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            } catch (Exception var3) {
               FluxLogger.warn("[LuckyBlock] armor equip failed: " + var3.getMessage());
            }

            this.interactCooldown = 3;
         } else if (!this.layoutOne(0, s -> s.isOf(Items.DIAMOND_SWORD))) {
            if (!this.layoutOne(1, s -> s.isOf(Items.DIAMOND_SWORD))) {
               if (!this.layoutOne(2, s -> s.isOf(Items.DIAMOND_AXE))) {
                  if (!this.layoutOne(3, s -> this.isAnyPickaxe(s))) {
                     if (!this.layoutOne(4, s -> s.isOf(Items.ENCHANTED_GOLDEN_APPLE) || s.isOf(Items.GOLDEN_APPLE))) {
                        if (!this.layoutOne(5, s -> s.isOf(Items.ENDER_PEARL))) {
                           if (!this.layoutOne(6, s -> s.isOf(Items.SHIELD))) {
                              if (this.phaseTicks > 100 || this.isGearReady()) {
                                 this.enterPhase(LuckyBlock.Phase.R_WARP);
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

   private void mineBlock(BlockPos block) {
      Direction side = this.bestMiningSide(block);
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
      if (this.miningPos != null && mc.interactionManager != null) {
         try {
            mc.interactionManager.cancelBlockBreaking();
         } catch (Exception var2) {
         }

         this.miningPos = null;
         this.miningSide = null;
      } else {
         this.miningPos = null;
         this.miningSide = null;
      }

      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }

      this.swingCooldown = 0;
   }

   private void stopMining() {
      if (mc.interactionManager != null) {
         this.stopMiningProgress();
      } else {
         this.miningPos = null;
         this.miningSide = null;
      }
   }

   private BlockPos findNearestObsidian() {
      BlockPos origin = mc.player.getBlockPos();
      int radius = Math.round(this.searchRadius.get());
      int yRadius = Math.min(radius, 32);
      BlockPos closest = null;
      double closestDistance = Double.MAX_VALUE;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -yRadius; y <= yRadius; y++) {
            for (int z = -radius; z <= radius; z++) {
               double distSq = (double)x * x + (double)y * y + (double)z * z;
               if (!(distSq >= closestDistance)) {
                  BlockPos candidate = origin.add(x, y, z);
                  if (!mc.world.isOutOfHeightLimit(candidate.getY())
                     && mc.world.isChunkLoaded(candidate)
                     && mc.world.getBlockState(candidate).isOf(Blocks.OBSIDIAN)) {
                     closestDistance = distSq;
                     closest = candidate.toImmutable();
                  }
               }
            }
         }
      }

      return closest;
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

   private int selectBestPickaxe(BlockState state) {
      int bestSlot = -1;
      float bestSpeed = 0.0F;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && this.isAnyPickaxe(stack)) {
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
               bestSpeed = speed;
               bestSlot = slot;
            }
         }
      }

      return bestSlot;
   }

   private boolean isAnyPickaxe(ItemStack stack) {
      return stack.isOf(Items.WOODEN_PICKAXE)
         || stack.isOf(Items.STONE_PICKAXE)
         || stack.isOf(Items.IRON_PICKAXE)
         || stack.isOf(Items.GOLDEN_PICKAXE)
         || stack.isOf(Items.DIAMOND_PICKAXE)
         || stack.isOf(Items.NETHERITE_PICKAXE);
   }

   private Direction bestMiningSide(BlockPos block) {
      ClientPlayerEntity player = mc.player;
      double x = player.getX() - (block.getX() + 0.5);
      double y = player.getEyeY() - (block.getY() + 0.5);
      double z = player.getZ() - (block.getZ() + 0.5);
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

   private void lookAt(BlockPos block) {
      double dx = block.getX() + 0.5 - mc.player.getX();
      double dy = block.getY() + 0.5 - mc.player.getEyeY();
      double dz = block.getZ() + 0.5 - mc.player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      mc.player.setYaw(BotNavigator.turnToward(mc.player.getYaw(), (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0), 60.0F));
      mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 89.0F));
   }

   private void enableProtectionModules() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      AutoGApple apple = mm.getModule(AutoGApple.class);
      AutoTotem totem = mm.getModule(AutoTotem.class);
      AutoRespawn respawn = mm.getModule(AutoRespawn.class);
      if (apple != null && !apple.isEnabled()) {
         apple.setEnabled(true);
         this.enabledApple = true;
      }

      if (totem != null && !totem.isEnabled()) {
         totem.setEnabled(true);
         this.enabledTotem = true;
      }

      if (respawn != null && !respawn.isEnabled()) {
         respawn.setEnabled(true);
         this.enabledRespawn = true;
      }
   }

   private void enableStealer() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      ChestStealer stealer = mm.getModule(ChestStealer.class);
      if (stealer != null && !stealer.isEnabled()) {
         stealer.setEnabled(true);
         this.enabledStealer = true;
      }
   }

   private void disableStealer() {
      if (this.enabledStealer) {
         ChestStealer stealer = FluxContext.get().getModuleManager().getModule(ChestStealer.class);
         if (stealer != null && stealer.isEnabled()) {
            stealer.setEnabled(false);
         }

         this.enabledStealer = false;
      }
   }

   private void updateEmergencyAura() {
      if (this.phase != LuckyBlock.Phase.WAIT_DEATH) {
         Aura aura = FluxContext.get().getModuleManager().getModule(Aura.class);
         if (aura != null && mc.player != null) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            boolean justDamaged = mc.player.hurtTime > 0 || hp < this.lastHealth - 0.5F;
            this.lastHealth = hp;
            if (justDamaged || hp <= this.dangerHealth.get()) {
               this.auraHoldTicks = Math.round(this.auraHoldTime.get() * 20.0F);
            } else if (this.auraHoldTicks > 0) {
               this.auraHoldTicks--;
            }

            boolean wantAura = this.auraHoldTicks > 0;
            if (wantAura && !aura.isEnabled()) {
               aura.setEnabled(true);
               this.enabledAura = true;
            }

            if (!wantAura && this.enabledAura) {
               aura.setEnabled(false);
               this.enabledAura = false;
            }

            if (wantAura && this.phase == LuckyBlock.Phase.FARM && !mc.player.isUsingItem() && this.miningPos == null) {
               this.selectWeapon();
            }
         }
      }
   }

   private void selectWeapon() {
      for (int i = 0; i < 9; i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(Items.DIAMOND_SWORD) || s.isOf(Items.NETHERITE_SWORD) || s.isOf(Items.DIAMOND_AXE) || s.isOf(Items.NETHERITE_AXE)) {
            if (mc.player.getInventory().selectedSlot != i) {
               mc.player.getInventory().selectedSlot = i;
            }

            return;
         }
      }
   }

   private void suppressSustain() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      Aura aura = mm.getModule(Aura.class);
      if (aura != null && aura.isEnabled()) {
         aura.setEnabled(false);
      }

      AutoGApple apple = mm.getModule(AutoGApple.class);
      if (apple != null && apple.isEnabled()) {
         apple.setEnabled(false);
      }

      AutoTotem totem = mm.getModule(AutoTotem.class);
      if (totem != null && totem.isEnabled()) {
         totem.setEnabled(false);
      }

      this.auraHoldTicks = 0;
      AutoRespawn respawn = mm.getModule(AutoRespawn.class);
      if (respawn != null && !respawn.isEnabled()) {
         respawn.setEnabled(true);
         this.enabledRespawn = true;
      }
   }

   private void releaseMovement() {
      if (mc.player != null && mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }
   }

   private void restoreModules() {
      ModuleManager mm = FluxContext.get().getModuleManager();
      if (this.enabledApple) {
         AutoGApple m = mm.getModule(AutoGApple.class);
         if (m != null && m.isEnabled()) {
            m.setEnabled(false);
         }
      }

      if (this.enabledTotem) {
         AutoTotem m = mm.getModule(AutoTotem.class);
         if (m != null && m.isEnabled()) {
            m.setEnabled(false);
         }
      }

      if (this.enabledRespawn) {
         AutoRespawn m = mm.getModule(AutoRespawn.class);
         if (m != null && m.isEnabled()) {
            m.setEnabled(false);
         }
      }

      if (this.enabledStealer) {
         ChestStealer m = mm.getModule(ChestStealer.class);
         if (m != null && m.isEnabled()) {
            m.setEnabled(false);
         }
      }

      if (this.enabledAura) {
         Aura m = mm.getModule(Aura.class);
         if (m != null && m.isEnabled()) {
            m.setEnabled(false);
         }
      }

      this.enabledApple = false;
      this.enabledTotem = false;
      this.enabledRespawn = false;
      this.enabledStealer = false;
      this.enabledAura = false;
      this.auraHoldTicks = 0;
   }

   @EventBus.Subscribe
   public void onDeathEvent(DeathEvent event) {
      this.deathSignal = true;
   }

   @EventBus.Subscribe
   public void onServerJoin(ServerJoinEvent event) {
      this.ticksSinceJoin = 0;
      if (this.lobbyActive && this.lobbyWalkStarted) {
         this.lobbyReturned = true;
      }
   }

   @EventBus.Subscribe
   public void onPlaceDenied(PlaceDeniedEvent event) {
      if ((this.phase == LuckyBlock.Phase.R_PLACE || this.phase == LuckyBlock.Phase.R_LOOT) && this.placeTimer == 0) {
         this.placeAttempts++;
         FluxLogger.info("[LuckyBlock] place denied x" + this.placeAttempts);
         if (this.placeAttempts >= 5) {
            if (this.phase != LuckyBlock.Phase.R_PLACE) {
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

   private int findInventoryHandlerSlot(LuckyBlock.StackPred pred) {
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

   private boolean layoutOne(int hotbar, LuckyBlock.StackPred pred) {
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
               FluxLogger.warn("[LuckyBlock] hotbar layout failed: " + var8.getMessage());
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
            FluxLogger.warn("[LuckyBlock] hotbar swap failed: " + var6.getMessage());
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
         || this.isAnyPickaxeInInventory();
   }

   private boolean isAnyPickaxeInInventory() {
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         if (this.isAnyPickaxe(mc.player.getInventory().getStack(i))) {
            return true;
         }
      }

      return false;
   }

   private boolean isGearReady() {
      boolean sword = this.countItem(Items.DIAMOND_SWORD) > 0;
      boolean axe = this.countItem(Items.DIAMOND_AXE) > 0;
      boolean apple = this.countItem(Items.ENCHANTED_GOLDEN_APPLE) > 0 || this.countItem(Items.GOLDEN_APPLE) > 0;
      boolean pearl = this.countItem(Items.ENDER_PEARL) > 0;
      return sword && axe && apple && pearl && this.isAnyPickaxeInInventory();
   }

   private void interactBlock(BlockPos pos) {
      Direction side = this.bestMiningSide(pos);
      Vec3d hit = Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
      BlockHitResult bhr = new BlockHitResult(hit, side, pos, false);

      try {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
         mc.player.swingHand(Hand.MAIN_HAND);
      } catch (Exception var6) {
         FluxLogger.warn("[LuckyBlock] interact failed: " + var6.getMessage());
      }
   }

   private void sendCmd(String cmdWithoutSlash) {
      try {
         mc.player.networkHandler.sendChatCommand(cmdWithoutSlash);
         FluxLogger.info("[LuckyBlock] -> /" + cmdWithoutSlash);
      } catch (Exception var3) {
         FluxLogger.warn("[LuckyBlock] command failed /" + cmdWithoutSlash + ": " + var3.getMessage());
      }
   }

   private void say(String msg) {
      try {
         if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§b[LuckyBlock] §f" + msg));
         }
      } catch (Exception var3) {
      }
   }

   private static enum Phase {
      FARM,
      WAIT_DEATH,
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
