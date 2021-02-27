package com.gamesense.client.module.modules.combat;

import com.gamesense.api.event.Phase;
import com.gamesense.api.event.events.OnUpdateWalkingPlayerEvent;
import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.RenderEvent;
import com.gamesense.api.setting.Setting;
import com.gamesense.api.util.combat.CrystalUtil;
import com.gamesense.api.util.combat.DamageUtil;
import com.gamesense.api.util.combat.ca.CAMain;
import com.gamesense.api.util.combat.ca.CASettings;
import com.gamesense.api.util.combat.ca.CrystalInfo;
import com.gamesense.api.util.combat.ca.PlayerInfo;
import com.gamesense.api.util.math.RotationUtils;
import com.gamesense.api.util.misc.MessageBus;
import com.gamesense.api.util.misc.Timer;
import com.gamesense.api.util.player.InventoryUtil;
import com.gamesense.api.util.player.PlayerPacket;
import com.gamesense.api.util.render.GSColor;
import com.gamesense.api.util.render.RenderUtil;
import com.gamesense.api.util.world.EntityUtil;
import com.gamesense.client.manager.managers.PlayerPacketManager;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.gui.ColorMain;
import com.gamesense.client.module.modules.misc.AutoGG;
import com.mojang.realmsclient.gui.ChatFormatting;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEndCrystal;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.gamesense.api.util.player.RotationUtil.ROTATION_UTIL;

public class AutoCrystalRewrite extends Module {

    public AutoCrystalRewrite() {
        super("AutoCrystalRewrite", Category.Combat);
    }

    Setting.Boolean breakCrystal;
    Setting.Boolean antiWeakness;
    Setting.Boolean placeCrystal;
    Setting.Boolean autoSwitch;
    Setting.Boolean raytrace;
    Setting.Boolean rotate;
    Setting.Boolean chat;
    Setting.Boolean showDamage;
    Setting.Boolean antiSuicide;
    Setting.Boolean showOwn;
    Setting.Boolean endCrystalMode;
    Setting.Boolean noGapSwitch;
    Setting.Integer facePlaceValue;
    Setting.Integer attackSpeed;
    Setting.Integer antiSuicideValue;
    Setting.Double maxSelfDmg;
    Setting.Double wallsRange;
    Setting.Double minDmg;
    Setting.Double minBreakDmg;
    Setting.Double minFacePlaceDmg;
    Setting.Double enemyRange;
    Setting.Double placeRange;
    Setting.Double breakRange;
    Setting.Mode handBreak;
    Setting.Mode breakMode;
    Setting.Mode crystalPriority;
    Setting.Mode hudDisplay;
    Setting.Mode breakType;
    Setting.ColorSetting color;

    Setting.Integer breakThreads;
    Setting.Integer timeout;

    public void setup() {
        ArrayList<String> hands = new ArrayList<>();
        hands.add("Main");
        hands.add("Offhand");
        hands.add("Both");

        ArrayList<String> breakModes = new ArrayList<>();
        breakModes.add("All");
        breakModes.add("Smart");
        breakModes.add("Own");

        ArrayList<String> priority = new ArrayList<>();
        priority.add("Damage");
        priority.add("Closest");
        priority.add("Health");

        ArrayList<String> hudModes = new ArrayList<>();
        hudModes.add("Mode");
        hudModes.add("Target");
        hudModes.add("None");

        ArrayList<String> breakTypes = new ArrayList<>();
        breakTypes.add("Swing");
        breakTypes.add("Packet");

        breakMode = registerMode("Target", breakModes, "All");
        handBreak = registerMode("Hand", hands, "Main");
        crystalPriority = registerMode("Prioritise", priority, "Damage");
        breakType = registerMode("Type", breakTypes, "Swing");
        breakCrystal = registerBoolean("Break", true);
        placeCrystal = registerBoolean("Place", true);
        attackSpeed = registerInteger("Attack Speed", 16, 0, 20);
        breakRange = registerDouble("Hit Range", 4.4, 0.0, 10.0);
        placeRange = registerDouble("Place Range", 4.4, 0.0, 6.0);
        wallsRange = registerDouble("Walls Range", 3.5, 0.0, 10.0);
        enemyRange = registerDouble("Enemy Range", 6.0, 0.0, 16.0);
        antiWeakness = registerBoolean("Anti Weakness", true);
        antiSuicide = registerBoolean("Anti Suicide", true);
        antiSuicideValue = registerInteger("Min Health", 14, 1, 36);
        autoSwitch = registerBoolean("Switch", true);
        noGapSwitch = registerBoolean("No Gap Switch", false);
        endCrystalMode = registerBoolean("1.13 Place", false);
        minDmg = registerDouble("Min Damage", 5, 0, 36);
        minBreakDmg = registerDouble("Min Break Dmg", 5, 0,36.0);
        maxSelfDmg = registerDouble("Max Self Dmg", 10, 1.0, 36.0);
        facePlaceValue = registerInteger("FacePlace HP", 8, 0, 36);
        minFacePlaceDmg = registerDouble("FacePlace Dmg", 2.0, 1, 10);
        rotate = registerBoolean("Rotate", true);
        raytrace = registerBoolean("Raytrace", false);
        showDamage = registerBoolean("Render Dmg", true);
        showOwn = registerBoolean("Debug Own", false);
        chat = registerBoolean("Chat Msgs", true);
        hudDisplay = registerMode("HUD", hudModes, "Mode");
        color = registerColor("Color", new GSColor(0, 255, 0, 50));

        breakThreads = registerInteger("Break Threads", 2, 1, 5);
        timeout = registerInteger("Timeout (ms)", 5, 1, 10);
    }

    private boolean switchCooldown = false;
    private boolean isAttacking = false;
    public static boolean stopAC = false;
    private static boolean togglePitch = false;
    private int oldSlot = -1;
    private Entity renderEntity;
    private BlockPos render;
    Timer timer = new Timer();

    private Vec3d lastHitVec = Vec3d.ZERO;
    private boolean rotating = false;

    // stores all the locations we have attempted to place crystals
    // and the corresponding crystal for that location (if there is any)
    private final Map<BlockPos, EntityEnderCrystal> placedCrystals = Collections.synchronizedMap(new HashMap<>());

    // Threading Stuff
    public static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private final ExecutorService mainExecutor = Executors.newSingleThreadExecutor();
    private Future<CrystalInfo> mainThreadOutput;

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.player.isDead) {
            disable();
            return;
        }

        if (stopAC) {
            return;
        }

        if (antiSuicide.getValue() && (mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= antiSuicideValue.getValue()) {
            return;
        }

        doCA();
        setupCA();
    }

    private void doCA() {
        if (mainThreadOutput == null) {
            return;
        }

        while (!(mainThreadOutput.isDone() || mainThreadOutput.isCancelled())) {
        }

        CrystalInfo output = null;
        try {
            output = mainThreadOutput.get();
        } catch (InterruptedException | ExecutionException ignored) {
        }

        if (output == null) {
            return;
        }

        if (output instanceof CrystalInfo.BreakInfo) {
            CrystalInfo.BreakInfo breakInfo = (CrystalInfo.BreakInfo) output;
            if (antiWeakness.getValue() && mc.player.isPotionActive(MobEffects.WEAKNESS)) {
                if (!isAttacking) {
                    // save initial player hand
                    oldSlot = mc.player.inventory.currentItem;
                    isAttacking = true;
                }
                // search for sword and tools in hotbar
                int newSlot = InventoryUtil.findFirstItemSlot(ItemSword.class, 0, 8);
                if (newSlot == -1) {
                    InventoryUtil.findFirstItemSlot(ItemTool.class, 0, 8);
                }
                // check if any swords or tools were found
                if (newSlot != -1) {
                    mc.player.inventory.currentItem = newSlot;
                    switchCooldown = true;
                }
            }

            if (timer.getTimePassed() / 50L >= 20 - attackSpeed.getValue()) {
                timer.reset();

                rotating = rotate.getValue();
                lastHitVec = breakInfo.crystal.getPositionVector();

                if (breakType.getValue().equalsIgnoreCase("Swing")) {
                    mc.playerController.attackEntity(mc.player, breakInfo.crystal);
                    swingArm();
                } else if (breakType.getValue().equalsIgnoreCase("Packet")) {
                    mc.player.connection.sendPacket(new CPacketUseEntity(breakInfo.crystal));
                    swingArm();
                }
            }
            return;
        } else {
            rotating = false;
            if (oldSlot != -1) {
                mc.player.inventory.currentItem = oldSlot;
                oldSlot = -1;
            }
            isAttacking = false;
        }

        // check to see if we are holding crystals or not
        int crystalSlot = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ? mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            crystalSlot = InventoryUtil.findFirstItemSlot(ItemEndCrystal.class, 0, 8);
        }
        boolean offhand = false;
        if (mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            offhand = true;
        } else if (crystalSlot == -1) {
            return;
        }

        if (output instanceof CrystalInfo.PlaceInfo) {
            CrystalInfo.PlaceInfo placeInfo = (CrystalInfo.PlaceInfo) output;
            this.render = placeInfo.crystal;
            this.renderEntity = placeInfo.target;
            if (placeInfo.crystal == null) {
                ROTATION_UTIL.resetRotation();
                return;
            }

            // autoSwitch stuff
            if (!offhand && mc.player.inventory.currentItem != crystalSlot) {
                if (this.autoSwitch.getValue()) {
                    if (!noGapSwitch.getValue() || !(mc.player.getHeldItemMainhand().getItem() == Items.GOLDEN_APPLE)) {
                        mc.player.inventory.currentItem = crystalSlot;
                        ROTATION_UTIL.resetRotation();
                        this.switchCooldown = true;
                    }
                }
                return;
            }

            rotating = rotate.getValue();
            lastHitVec = new Vec3d(placeInfo.crystal).add(0.5, 0.5, 0.5);

            EnumFacing enumFacing = null;
            if (raytrace.getValue()) {
                RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double) mc.player.getEyeHeight(), mc.player.posZ), new Vec3d((double) placeInfo.crystal.getX() + 0.5d, (double) placeInfo.crystal.getY() - 0.5d, (double) placeInfo.crystal.getZ() + 0.5d));
                if (result == null || result.sideHit == null) {
                    render = null;
                    ROTATION_UTIL.resetRotation();
                    return;
                }
                else {
                    enumFacing = result.sideHit;
                }
            }

            if (this.switchCooldown) {
                this.switchCooldown = false;
                return;
            }

            if (breakMode.getValue().equalsIgnoreCase("Own")) {
                BlockPos up = placeInfo.crystal.up();
                if (!placedCrystals.containsKey(up)) {
                    placedCrystals.put(up, null);
                }
            }

            if (raytrace.getValue() && enumFacing != null) {
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(placeInfo.crystal, enumFacing, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0, 0, 0));
            } else if (placeInfo.crystal.getY() == 255) {
                // For Hoosiers. This is how we do build height. If the target block (q) is at Y 255. Then we send a placement packet to the bottom part of the block. Thus the EnumFacing.DOWN.
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(placeInfo.crystal, EnumFacing.DOWN, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0, 0, 0));
            } else {
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(placeInfo.crystal, EnumFacing.UP, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0, 0, 0));
            }
            mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));

            if (ModuleManager.isModuleEnabled("AutoGG")) {
                AutoGG.INSTANCE.addTargetedPlayer(renderEntity.getName());
            }

            if (ROTATION_UTIL.isSpoofingAngles()) {
                if (togglePitch) {
                    mc.player.rotationPitch += 4.0E-4F;
                    togglePitch = false;
                }
                else {
                    mc.player.rotationPitch -= 4.0E-4F;
                    togglePitch = true;
                }
            }
        }
    }

    private void setupCA() {
        if (mainThreadOutput != null) {
            mainThreadOutput.cancel(true);
            mainThreadOutput = null;
        }

        // entity range is the range from each crystal
        // so adding these together should solve problem
        // and reduce searching time
        double enemyDistance = enemyRange.getValue() + placeRange.getValue();
        final double entityRangeSq = (enemyDistance) * (enemyDistance);
        List<EntityPlayer> targets = mc.world.playerEntities.stream()
                .filter(entity -> mc.player.getDistanceSq(entity) <= entityRangeSq)
                .filter(entity -> !EntityUtil.basicChecksEntity(entity))
                .filter(entity -> entity.getHealth() > 0.0f)
                .collect(Collectors.toList());
        // no point continuing if there are no targets
        if (targets.size() == 0) {
            return;
        }

        List<EntityEnderCrystal> allCrystals = mc.world.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityEnderCrystal)
                .map(entity -> (EntityEnderCrystal) entity).collect(Collectors.toList());

        final boolean own = breakMode.getValue().equalsIgnoreCase("Own");
        if (own) {
            // remove own crystals that have been destroyed
            allCrystals.removeIf(crystal -> !placedCrystals.containsKey(EntityUtil.getPosition(crystal)));
            placedCrystals.values().removeIf(crystal -> {
                if (crystal == null) {
                    return false;
                }
                return crystal.isDead;
            });
        }

        // remove all crystals that deal more than max self damage
        // no point in checking these
        final boolean antiSuicideValue = antiSuicide.getValue();
        final float maxSelfDamage = (float) maxSelfDmg.getValue();
        final float playerHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        allCrystals.removeIf(crystal -> {
            float damage = DamageUtil.calculateDamage(crystal.posX, crystal.posY, crystal.posZ, mc.player);
            if (damage > maxSelfDamage) {
                return true;
            } else return antiSuicideValue && damage > playerHealth;
        });

        List<BlockPos> blocks = CrystalUtil.findCrystalBlocks((float) placeRange.getValue(), endCrystalMode.getValue());
        CASettings settings = new CASettings(breakCrystal.getValue(), placeCrystal.getValue(), enemyRange.getValue(), breakRange.getValue(), wallsRange.getValue(), minDmg.getValue(), minBreakDmg.getValue(), minFacePlaceDmg.getValue(), maxSelfDmg.getValue(), breakThreads.getValue(), facePlaceValue.getValue(), antiSuicide.getValue(), breakMode.getValue(), crystalPriority.getValue(), mc.player.getPositionVector());
        List<PlayerInfo> targetsInfo = new ArrayList<>();
        for (EntityPlayer target : targets) {
            targetsInfo.add(new PlayerInfo(target));
        }

        long timeoutTime = System.currentTimeMillis() + timeout.getValue();
        mainThreadOutput = mainExecutor.submit(new CAMain(settings, targetsInfo, allCrystals, blocks, timeoutTime));
    }

    public void onWorldRender(RenderEvent event) {
        if (this.render != null) {
            RenderUtil.drawBox(this.render,1, new GSColor(color.getValue(),50), 63);
            RenderUtil.drawBoundingBox(this.render, 1, 1.00f, new GSColor(color.getValue(),255));
        }

        if(showDamage.getValue()) {
            if (this.render != null && this.renderEntity != null) {
                String[] damageText = {String.format("%.1f", DamageUtil.calculateDamage((double) render.getX() + 0.5d, (double) render.getY() + 1.0d, (double) render.getZ() + 0.5d, renderEntity))};
                RenderUtil.drawNametag((double) render.getX() + 0.5d,(double) render.getY() + 0.5d,(double) render.getZ() + 0.5d, damageText, new GSColor(255,255,255),1);
            }
        }
        if (showOwn.getValue()) {
            placedCrystals.forEach(((blockPos, entityEnderCrystal) -> RenderUtil.drawBoundingBox(blockPos, 1, 1.00f, new GSColor(255, 255, 255,150))));
        }
    }

    private void swingArm() {
        if (handBreak.getValue().equalsIgnoreCase("Both")) {
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.swingArm(EnumHand.OFF_HAND);
        }
        else if (handBreak.getValue().equalsIgnoreCase("Offhand")) {
            mc.player.swingArm(EnumHand.OFF_HAND);
        }
        else {
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<OnUpdateWalkingPlayerEvent> onUpdateWalkingPlayerEventListener = new Listener<>(event -> {
        if (event.getPhase() != Phase.PRE || !rotating) return;

        Vec2f rotation = RotationUtils.getRotationTo(lastHitVec);
        PlayerPacket packet = new PlayerPacket(this, rotation);
        PlayerPacketManager.INSTANCE.addPacket(packet);
    });

    @EventHandler
    private final Listener<PacketEvent.Receive> packetReceiveListener = new Listener<>(event -> {
        Packet<?> packet = event.getPacket();
        if (packet instanceof SPacketSoundEffect) {
            final SPacketSoundEffect packetSoundEffect = (SPacketSoundEffect) packet;
            if (packetSoundEffect.getCategory() == SoundCategory.BLOCKS && packetSoundEffect.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                for (Entity entity : Minecraft.getMinecraft().world.loadedEntityList) {
                    if (entity instanceof EntityEnderCrystal) {
                        if (entity.getDistanceSq(packetSoundEffect.getX(), packetSoundEffect.getY(), packetSoundEffect.getZ()) <= 36.0f) {
                            entity.setDead();
                        }
                    }
                }
            }
        }
    });

    @EventHandler
    private final Listener<EntityJoinWorldEvent> entitySpawnListener = new Listener<>(event -> {
        Entity entity = event.getEntity();
        if (entity instanceof EntityEnderCrystal) {
            if (breakMode.getValue().equalsIgnoreCase("Own")) {
                EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
                BlockPos crystalPos = EntityUtil.getPosition(crystal);
                if (placedCrystals.containsKey(crystalPos)) {
                    placedCrystals.replace(crystalPos, crystal);
                }
            }
        }
    });

    public void onEnable() {
        ROTATION_UTIL.onEnable();

        if(chat.getValue() && mc.player != null) {
            MessageBus.sendClientPrefixMessage(ColorMain.getEnabledColor() + "AutoCrystal turned ON!");
        }
    }

    public void onDisable() {
        ROTATION_UTIL.onDisable();
        render = null;
        renderEntity = null;
        rotating = false;

        placedCrystals.clear();

        if(chat.getValue()) {
            MessageBus.sendClientPrefixMessage(ColorMain.getDisabledColor() + "AutoCrystal turned OFF!");
        }
    }

    private static final String stringAll = "[" + ChatFormatting.WHITE + "All" + ChatFormatting.GRAY + "]";
    private static final String stringSmart = "[" + ChatFormatting.WHITE + "Smart" + ChatFormatting.GRAY + "]";
    private static final String stringOwn = "[" + ChatFormatting.WHITE + "Own" + ChatFormatting.GRAY + "]";
    private static final String stringNone = "[" + ChatFormatting.WHITE + "None" + ChatFormatting.GRAY + "]";

    public String getHudInfo() {
        String t = "";
        if (hudDisplay.getValue().equalsIgnoreCase("Mode")){
            if (breakMode.getValue().equalsIgnoreCase("All")) {
                t = stringAll;
            }
            if (breakMode.getValue().equalsIgnoreCase("Smart")) {
                t = stringSmart;
            }
            if (breakMode.getValue().equalsIgnoreCase("Own")) {
                t = stringOwn;
            }
        } else if (hudDisplay.getValue().equalsIgnoreCase("Target")) {
            if (renderEntity == null) {
                t = stringNone;
            } else {
                t = "[" + ChatFormatting.WHITE + renderEntity.getName() + ChatFormatting.GRAY + "]";
            }
        }
        return t;
    }
}