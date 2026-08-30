package com.smmorpg.training;

/**
 * How hard the arena gets, and when.
 *
 * <p>Difficulty is earned, not typed in. You start at level 0 and 1%, and the only way the
 * number goes up is by clearing what is in front of you — which means the percentage on
 * screen is a record of what you have actually beaten rather than a claim about what you
 * think you can handle. A slider would let anyone set 100000% in the first minute and
 * learn nothing from dying to it.
 */
public final class TrainingLevels {

    /** Each level is 35% harder than the last, so the climb is long but never flat. */
    private static final double STEP = 1.35D;

    /** Level 0 is 1%: barely a fight, on purpose. It is where you learn the controls. */
    public static final int BASE_PERCENT = 1;

    private TrainingLevels() {}

    public static int percentFor(int level) {
        if (level <= 0) return BASE_PERCENT;
        double raw = BASE_PERCENT * Math.pow(STEP, level);
        return (int) Math.min(Difficulty.MAX, Math.max(BASE_PERCENT, Math.round(raw)));
    }

    /** The level at which the scale tops out; past it the percentage cannot rise further. */
    public static int maxLevel() {
        int level = 0;
        while (percentFor(level) < Difficulty.MAX) level++;
        return level;
    }

    /**
     * How many opponents one level asks for.
     *
     * <p>Grows slowly. The lethality is already compounding per level; making the waves
     * long as well would turn the late arena into an endurance test rather than a fight.
     */
    public static int killsFor(int level) {
        return Math.min(20, 4 + level / 2);
    }

    public static Difficulty difficultyFor(int level) {
        return new Difficulty(percentFor(level));
    }
}
