package com.smmorpg.mob;

import net.minecraft.ChatFormatting;

/**
 * What kind of thing the arena sends at you, and it climbs.
 *
 * <p>Tiers are chosen from the difficulty band rather than rolled freely, so raising the
 * percentage does not just inflate a health bar — it changes what walks through the gate.
 * At the bottom you fight soldiers. Past the divine line you fight the things soldiers
 * tell stories about.
 */
public enum MobTier {
    MORTAL("mortal", ChatFormatting.GRAY, 1.00F, 0),
    VETERAN("veteran", ChatFormatting.GREEN, 1.25F, 2),
    CHAMPION("champion", ChatFormatting.AQUA, 1.60F, 5),
    ASCENDANT("ascendant", ChatFormatting.LIGHT_PURPLE, 2.10F, 9),
    DIVINE("divine", ChatFormatting.GOLD, 2.90F, 14),
    PRIMORDIAL("primordial", ChatFormatting.DARK_RED, 4.20F, 20);

    private final String key;
    private final ChatFormatting color;
    private final float statScale;
    private final int minimumBand;

    MobTier(String key, ChatFormatting color, float statScale, int minimumBand) {
        this.key = key;
        this.color = color;
        this.statScale = statScale;
        this.minimumBand = minimumBand;
    }

    public String key() { return key; }
    public ChatFormatting color() { return color; }
    public float statScale() { return statScale; }
    public int minimumBand() { return minimumBand; }

    public String translationKey() { return "tier.smmorpg." + key; }

    /** The highest tier this difficulty band has unlocked. */
    public static MobTier forBand(int band) {
        MobTier best = MORTAL;
        for (MobTier tier : values()) {
            if (band >= tier.minimumBand) best = tier;
        }
        return best;
    }

    /**
     * The tier one step up, for an evolving mob. The top tier has nowhere to climb, which
     * is what makes a Primordial the end of that particular road.
     */
    public MobTier next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : this;
    }

    public boolean isAtLeast(MobTier other) { return ordinal() >= other.ordinal(); }
}
