package com.smmorpg.training;

/**
 * Training difficulty, expressed as a percentage from 1 to 100000.
 *
 * <p>100 is a fair fight against a competent opponent. Past that the scale keeps going:
 * every additional 100 points is another band, and the bands above 100% are the divine
 * tiers — each one is not merely a bigger health bar but a strictly more lethal opponent,
 * because the multipliers that matter (reaction speed, aggression, damage) are compounded
 * per band rather than added.
 */
public record Difficulty(int percent) {

    public static final int MIN = 1;
    public static final int MAX = 100_000;

    public Difficulty {
        percent = Math.max(MIN, Math.min(MAX, percent));
    }

    /** 0 below 100%, then 1 for 101-200, 2 for 201-300, and so on. */
    public int band() { return Math.max(0, (percent - 1) / 100); }

    public boolean divine() { return percent > 100; }

    /** Raw scalar: 1.0 at 100%. */
    public float scalar() { return percent / 100.0F; }

    /** Health scales linearly; a divine opponent should still be killable. */
    public float healthMultiplier() { return 1.0F + scalar() * 0.9F; }

    /**
     * Damage compounds per band, which is what makes each 100 points meaningfully deadlier
     * rather than just slower to chew through.
     */
    public float damageMultiplier() {
        return (float) (Math.pow(1.16D, band()) * (0.6F + scalar() * 0.4F));
    }

    /** Ticks the bot waits before reacting. Saturates at one tick — it cannot go below. */
    public int reactionTicks() {
        return Math.max(1, Math.round(9.0F / (1.0F + scalar() * 0.85F)));
    }

    /** How often the bot commits to an attack rather than repositioning. */
    public float aggression() {
        return Math.min(0.98F, 0.30F + scalar() * 0.22F + band() * 0.012F);
    }

    /** Movement speed multiplier; divine tiers close distance frighteningly fast. */
    public float speedMultiplier() {
        return Math.min(4.5F, 1.0F + scalar() * 0.28F + band() * 0.03F);
    }

    /** Chance per opportunity that the bot parries instead of eating the hit. */
    public float parryChance() {
        return Math.min(0.95F, scalar() * 0.35F + band() * 0.02F);
    }

    /** How readily the bot uses walls, dashes and air jumps rather than walking in. */
    public float acrobatics() {
        return Math.min(1.0F, 0.10F + scalar() * 0.30F + band() * 0.02F);
    }

    /** How many bots the arena keeps alive at once. */
    public int simultaneousOpponents() {
        return Math.max(1, Math.min(12, 1 + band() / 2));
    }

    public String tierKey() {
        if (percent <= 25) return "training.smmorpg.tier.novice";
        if (percent <= 60) return "training.smmorpg.tier.adept";
        if (percent <= 100) return "training.smmorpg.tier.master";
        if (percent <= 500) return "training.smmorpg.tier.ascendant";
        if (percent <= 2000) return "training.smmorpg.tier.divine";
        if (percent <= 20000) return "training.smmorpg.tier.celestial";
        return "training.smmorpg.tier.absolute";
    }
}
