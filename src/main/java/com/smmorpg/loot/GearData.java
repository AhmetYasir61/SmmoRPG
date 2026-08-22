package com.smmorpg.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** The rolled identity of a piece of loot, stored on the stack as a data component. */
public record GearData(String rarity, int itemLevel, List<RolledAffix> affixes, String name) {

    public static final GearData EMPTY = new GearData(Rarity.COMMON.key(), 1, List.of(), "");

    public static final Codec<GearData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("rarity").forGetter(GearData::rarity),
            Codec.INT.fieldOf("ilvl").forGetter(GearData::itemLevel),
            RolledAffix.CODEC.listOf().fieldOf("affixes").forGetter(GearData::affixes),
            Codec.STRING.optionalFieldOf("name", "").forGetter(GearData::name)
    ).apply(i, GearData::new));

    public Rarity tier() { return Rarity.byKey(rarity); }

    public boolean holy() { return tier() == Rarity.HOLY || hasAlignment(Affix.Alignment.HOLY); }
    public boolean cursed() { return tier() == Rarity.CURSED || hasAlignment(Affix.Alignment.CURSED); }

    private boolean hasAlignment(Affix.Alignment a) {
        for (RolledAffix r : affixes) if (r.type().alignment() == a) return true;
        return false;
    }

    public float powerOf(Affix affix) {
        float sum = 0.0F;
        for (RolledAffix r : affixes) if (r.type() == affix) sum += r.power();
        return sum;
    }

    /** Flat multiplier this gear applies to outgoing melee damage. */
    public float damageMultiplier() {
        float mul = tier().statScale() * (1.0F + itemLevel * 0.012F);
        mul *= 1.0F + powerOf(Affix.KEEN) + powerOf(Affix.HEAVY) + powerOf(Affix.SERRATED);
        mul *= 1.0F + powerOf(Affix.BLOODTHIRSTY) + powerOf(Affix.SOUL_EATING) + powerOf(Affix.LIFE_TITHE);
        return mul;
    }

    /** How much extra a wound from this weapon bleeds. */
    public float bleedMultiplier() {
        return 1.0F + powerOf(Affix.SERRATED) * 2.0F + powerOf(Affix.HEMORRHAGE) * 3.0F;
    }

    /** Health the wielder pays per swing for cursed power. */
    public float lifeCostPerHit() {
        return powerOf(Affix.LIFE_TITHE) * 2.5F + powerOf(Affix.ROTTING) * 1.0F;
    }

    public float lifestealFraction() {
        return powerOf(Affix.VAMPIRIC) + powerOf(Affix.BLOODTHIRSTY) * 0.5F;
    }
}
