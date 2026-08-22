package com.smmorpg.npc;

/**
 * How a combat bot chooses to close, strike and leave.
 *
 * <p>Each style is a different answer to distance. Mixing them across a training wave is
 * what stops the arena from becoming one solved pattern.
 */
public enum FightingStyle {
    /** Straight in, relentless pressure, no retreat. */
    AGGRESSOR(0.95F, 0.15F, 0.20F, 1.8F),
    /** Waits at the edge of reach and punishes the whiff. */
    COUNTER(0.35F, 0.80F, 0.35F, 3.0F),
    /** Wall runs, air jumps, attacks from above and behind. */
    NINJA(0.75F, 0.40F, 0.95F, 2.4F),
    /** Circles constantly, never fights from where you last saw it. */
    FLANKER(0.60F, 0.45F, 0.70F, 2.6F),
    /** Holds guard, breaks your posture, then commits once. */
    BULWARK(0.45F, 0.90F, 0.10F, 1.6F),
    /** Dashes in and out on a fixed rhythm you have to learn. */
    DUELIST(0.70F, 0.60F, 0.55F, 2.2F),
    /** Stays far, closes only when you are recovering. */
    SKIRMISHER(0.40F, 0.35F, 0.60F, 5.0F);

    private final float aggression;
    private final float patience;
    private final float acrobatics;
    private final float preferredRange;

    FightingStyle(float aggression, float patience, float acrobatics, float preferredRange) {
        this.aggression = aggression;
        this.patience = patience;
        this.acrobatics = acrobatics;
        this.preferredRange = preferredRange;
    }

    public float aggression() { return aggression; }
    public float patience() { return patience; }
    public float acrobatics() { return acrobatics; }
    public float preferredRange() { return preferredRange; }

    public static FightingStyle random(net.minecraft.util.RandomSource rng) {
        return values()[rng.nextInt(values().length)];
    }
}
