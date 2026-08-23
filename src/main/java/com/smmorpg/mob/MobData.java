package com.smmorpg.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a mob has become.
 *
 * <p>Level and tier are given at spawn. {@code devoured} is earned afterwards, by eating,
 * and it is the only number here a mob can raise on its own.
 */
public record MobData(String archetype, String tier, int level, int devoured, boolean lord) {

    public static final MobData NONE = new MobData("", MobTier.MORTAL.key(), 0, 0, false);

    public static final Codec<MobData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("archetype").forGetter(MobData::archetype),
            Codec.STRING.fieldOf("tier").forGetter(MobData::tier),
            Codec.INT.fieldOf("level").forGetter(MobData::level),
            Codec.INT.optionalFieldOf("devoured", 0).forGetter(MobData::devoured),
            Codec.BOOL.optionalFieldOf("lord", false).forGetter(MobData::lord)
    ).apply(i, MobData::new));

    public boolean initialised() { return !archetype.isEmpty(); }

    public MobTier mobTier() { return tierOf(tier); }

    private static MobTier tierOf(String key) {
        for (MobTier t : MobTier.values()) if (t.key().equals(key)) return t;
        return MobTier.MORTAL;
    }

    public MobArchetype archetypeOf() { return MobRoster.byKey(archetype); }

    public MobData withDevoured(int count) {
        return new MobData(archetype, tier, level, count, lord);
    }

    public MobData evolved(int newLevel, MobTier newTier, boolean nowLord) {
        return new MobData(archetype, newTier.key(), newLevel, devoured, lord || nowLord);
    }

    /** Total stat multiplier from tier and level together. */
    public float statMultiplier() {
        // Levels compound gently; the tier is what makes the real jumps.
        return mobTier().statScale() * (float) Math.pow(1.035D, level) * (lord ? 1.6F : 1.0F);
    }
}
