package com.gamesense.api.util.combat.ca;

import net.minecraft.util.math.Vec3d;

public class CASettings {
    public final boolean breakCrystals;
    public final boolean placeCrystals;

    public final double enemyRangeSq;
    public final double breakRangeSq;
    public final double wallsRangeSq;

    public final float minDamage;
    public final float minBreakDamage;
    public final float minFacePlaceDamage;
    public final float maxSelfDamage;

    public final int breakThreads;
    public final float facePlaceHealth;

    public final boolean antiSuicide;

    public final String breakMode;
    public final String crystalPriority;

    public final Vec3d player;

    public CASettings(boolean breakCrystals, boolean placeCrystals, double enemyRange, double breakRange, double wallsRange, double minDamage, double minBreakDamage, double minFacePlaceDamage, double maxSelfDamage, int breakThreads, double facePlaceHealth, boolean antiSuicide, String breakMode, String crystalPriority, Vec3d player) {
        this.breakCrystals = breakCrystals;
        this.placeCrystals = placeCrystals;

        this.enemyRangeSq = enemyRange * enemyRange;
        this.breakRangeSq = breakRange * breakRange;
        this.wallsRangeSq = wallsRange * wallsRange;

        this.minDamage = (float) minDamage;
        this.minBreakDamage = (float) minBreakDamage;
        this.minFacePlaceDamage = (float) minFacePlaceDamage;
        this.maxSelfDamage = (float) maxSelfDamage;

        this.breakThreads = breakThreads;
        this.facePlaceHealth = (float) facePlaceHealth;

        this.antiSuicide = antiSuicide;

        this.breakMode = breakMode;
        this.crystalPriority = crystalPriority;

        this.player = player;
    }
}
