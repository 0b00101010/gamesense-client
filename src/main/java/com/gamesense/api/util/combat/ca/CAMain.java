package com.gamesense.api.util.combat.ca;

import com.gamesense.api.util.combat.DamageUtil;
import com.gamesense.api.util.combat.ca.breaks.BreakThread;
import com.gamesense.api.util.combat.ca.place.PlaceThread;
import com.gamesense.client.module.modules.combat.AutoCrystalRewrite;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CAMain implements Callable<CrystalInfo> {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final CASettings settings;

    private final List<PlayerInfo> targets;
    private final List<EntityEnderCrystal> crystals;
    private final List<BlockPos> blocks;

    private final long globalTimeoutTime;

    public CAMain(CASettings settings, List<PlayerInfo> targets, List<EntityEnderCrystal> crystals, List<BlockPos> blocks, long globalTimeoutTime) {
        this.settings = settings;

        this.targets = targets;
        this.crystals = crystals;
        this.blocks = blocks;

        this.globalTimeoutTime = globalTimeoutTime;
    }

    @Override
    public CrystalInfo call() {
        List<Future<CrystalInfo.PlaceInfo>> placeFutures = null;
        List<Future<List<CrystalInfo.BreakInfo>>> breakFutures = null;
        if (settings.breakCrystals) {
            breakFutures = startBreakThreads(targets, crystals, settings);
        }
        if (settings.placeCrystals) {
            placeFutures = startPlaceThreads(targets, settings);
        }

        if (breakFutures != null) {
            CrystalInfo.BreakInfo breakInfo = getCrystalToBreak(breakFutures);
            if (breakInfo != null && (mc.player.canEntityBeSeen(breakInfo.crystal) || mc.player.getDistanceSq(breakInfo.crystal) < settings.wallsRangeSq)) {
                if (placeFutures != null) {
                    for (Future<CrystalInfo.PlaceInfo> placeFuture : placeFutures) {
                        placeFuture.cancel(true);
                    }
                }
                return breakInfo;
            }
        }
        if (placeFutures != null) {
            return getPositionToPlace(placeFutures);
        }
        return null;
    }

    private List<Future<List<CrystalInfo.BreakInfo>>> startBreakThreads(List<PlayerInfo> targets, List<EntityEnderCrystal> crystalList, CASettings settings) {
        if (crystalList.size() == 0) {
            return null;
        }

        List<Future<List<CrystalInfo.BreakInfo>>> output = new ArrayList<>();
        // split targets equally between threads
        int targetsPerThread = (int) Math.ceil((double) targets.size()/ (double) settings.breakThreads);
        int threadsPerTarget = (int) Math.floor((double) settings.breakThreads/ (double) targets.size());

        List<List<EntityEnderCrystal>> splits = new ArrayList<>();
        int smallListSize = (int) Math.ceil((double) crystalList.size()/ (double) threadsPerTarget);

        int j = 0;
        for (int i = smallListSize; i < crystalList.size(); i += smallListSize) {
            splits.add(crystalList.subList(j, i + 1));
            j += smallListSize;
        }
        splits.add(crystalList.subList(j, crystalList.size()));

        j = 0;
        for (int i = targetsPerThread; i < targets.size(); i += targetsPerThread) {
            List<PlayerInfo> sublist = targets.subList(j, i + 1);
            for (List<EntityEnderCrystal> split : splits) {
                output.add(AutoCrystalRewrite.executor.submit(new BreakThread(settings, split, sublist)));
            }
            j += targetsPerThread;
        }
        List<PlayerInfo> sublist = targets.subList(j, targets.size());
        for (List<EntityEnderCrystal> split : splits) {
            output.add(AutoCrystalRewrite.executor.submit(new BreakThread(settings, split, sublist)));
        }

        return output;
    }

    private CrystalInfo.BreakInfo getCrystalToBreak(List<Future<List<CrystalInfo.BreakInfo>>> input) {
        List<CrystalInfo.BreakInfo> crystals = new ArrayList<>();
        for (Future<List<CrystalInfo.BreakInfo>> future : input) {
            while (!future.isDone() && !future.isCancelled()) {
                if (System.currentTimeMillis() > globalTimeoutTime) {
                    break;
                }
            }
            if (future.isDone()) {
                try {
                    crystals.addAll(future.get());
                } catch (InterruptedException | ExecutionException ignored) {
                }
            } else {
                future.cancel(true);
            }
        }
        if (crystals.size() == 0) {
            return null;
        }

        // get the best crystal based on our needs
        if (settings.crystalPriority.equalsIgnoreCase("Closest")) {
            Optional<CrystalInfo.BreakInfo> out = crystals.stream().min(Comparator.comparing(info -> mc.player.getDistanceSq(info.crystal)));
            return out.get();
        } else if (settings.crystalPriority.equalsIgnoreCase("Health")) {
            Optional<CrystalInfo.BreakInfo> out = crystals.stream().min(Comparator.comparing(info -> info.target.getHealth() + info.target.getAbsorptionAmount()));
            return out.get();
        } else {
            Optional<CrystalInfo.BreakInfo> out = crystals.stream().max(Comparator.comparing(info -> info.damage));
            return out.get();
        }
    }

    private List<Future<CrystalInfo.PlaceInfo>> startPlaceThreads(List<PlayerInfo> targets, CASettings settings) {
        // remove all placements that deal more than max self damage
        // no point in checking these
        final float playerHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        blocks.removeIf(crystal -> {
            float damage = DamageUtil.calculateDamage((double) crystal.getX() + 0.5d, (double) crystal.getY() + 1.0d, (double) crystal.getZ() + 0.5d, mc.player);
            if (damage > settings.maxSelfDamage) {
                return true;
            } else return settings.antiSuicide && damage > playerHealth;
        });
        if (blocks.size() == 0) {
            return null;
        }

        List<Future<CrystalInfo.PlaceInfo>> output = new ArrayList<>();

        for (PlayerInfo target : targets) {
            output.add(AutoCrystalRewrite.executor.submit(new PlaceThread(settings, blocks, target)));
        }

        return output;
    }

    private CrystalInfo.PlaceInfo getPositionToPlace(List<Future<CrystalInfo.PlaceInfo>> input) {
        List<CrystalInfo.PlaceInfo> crystals = new ArrayList<>();
        for (Future<CrystalInfo.PlaceInfo> future : input) {
            while (!future.isDone() && !future.isCancelled()) {
                if (System.currentTimeMillis() > globalTimeoutTime) {
                    break;
                }
            }
            if (future.isDone()) {
                CrystalInfo.PlaceInfo crystal = null;
                try {
                    crystal = future.get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
                if (crystal != null) {
                    crystals.add(crystal);
                }
            } else {
                future.cancel(true);
            }
        }
        if (crystals.size() == 0) {
            return null;
        }

        // get the best crystal based on our needs
        if (settings.crystalPriority.equalsIgnoreCase("Closest")) {
            Optional<CrystalInfo.PlaceInfo> out = crystals.stream().min(Comparator.comparing(info -> mc.player.getDistanceSq(info.crystal)));
            return out.get();
        } else if (settings.crystalPriority.equalsIgnoreCase("Health")) {
            Optional<CrystalInfo.PlaceInfo> out = crystals.stream().min(Comparator.comparing(info -> info.target.getHealth() + info.target.getAbsorptionAmount()));
            return out.get();
        } else {
            Optional<CrystalInfo.PlaceInfo> out = crystals.stream().max(Comparator.comparing(info -> info.damage));
            return out.get();
        }
    }
}
