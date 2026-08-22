package com.smmorpg.loot;

import com.smmorpg.config.CombatConfig;
import com.smmorpg.core.ModDataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Rolls loot. Not a one-off table: every drop is generated from the killer's level and
 * the victim's difficulty, so the same mob keeps producing new gear at level 5 and at 90.
 */
public final class LootRoller {

    private LootRoller() {}

    public static Rarity rollRarity(RandomSource rng, int playerLevel, float luck) {
        double holyChance = CombatConfig.CFG.holyChance.get() * (1.0D + luck * 0.25D);
        double cursedChance = CombatConfig.CFG.cursedChance.get() * (1.0D + luck * 0.25D);

        double roll = rng.nextDouble();
        if (roll < holyChance) return Rarity.HOLY;
        if (roll < holyChance + cursedChance) return Rarity.CURSED;

        // Higher levels shift weight up the ladder without ever guaranteeing a tier.
        int total = 0;
        List<Rarity> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Rarity r : Rarity.values()) {
            if (r == Rarity.HOLY || r == Rarity.CURSED) continue;
            int w = Math.max(1, Math.round(r.weight() * levelBias(r, playerLevel, luck)));
            pool.add(r);
            weights.add(w);
            total += w;
        }
        int pick = rng.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            pick -= weights.get(i);
            if (pick < 0) return pool.get(i);
        }
        return Rarity.COMMON;
    }

    private static float levelBias(Rarity r, int level, float luck) {
        float tier = r.affixSlots();
        // Common gets rarer as you level; the top end gets steadily more likely.
        return (float) Math.pow(1.0F + level * 0.010F + luck * 0.05F, tier - 1.2F);
    }

    public static GearData rollGear(RandomSource rng, int playerLevel, float luck) {
        Rarity rarity = rollRarity(rng, playerLevel, luck);
        int ilvl = Math.max(1, playerLevel + rng.nextInt(5) - 2);

        int slots = Math.min(CombatConfig.CFG.maxAffixesPerItem.get(), rarity.affixSlots());
        List<RolledAffix> rolled = new ArrayList<>(slots);
        Set<Affix> used = EnumSet.noneOf(Affix.class);

        for (int i = 0; i < slots; i++) {
            Affix a = pickAffix(rng, rarity, used);
            if (a == null) break;
            used.add(a);
            float t = rng.nextFloat();
            // Item level pushes the roll toward the top of the affix range.
            t = Math.min(1.0F, t + ilvl * 0.004F);
            float power = a.minPower() + (a.maxPower() - a.minPower()) * t;
            rolled.add(new RolledAffix(a.key(), Math.round(power * 1000.0F) / 1000.0F));
        }
        return new GearData(rarity.key(), ilvl, List.copyOf(rolled), "");
    }

    private static Affix pickAffix(RandomSource rng, Rarity rarity, Set<Affix> used) {
        List<Affix> pool = new ArrayList<>();
        for (Affix a : Affix.values()) {
            if (used.contains(a)) continue;
            switch (a.alignment()) {
                case HOLY -> { if (rarity == Rarity.HOLY || rarity == Rarity.MYTHIC) pool.add(a); }
                case CURSED -> { if (rarity == Rarity.CURSED || rarity == Rarity.MYTHIC) pool.add(a); }
                case NEUTRAL -> pool.add(a);
            }
        }
        if (pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
    }

    /** Stamps rolled data onto a stack. */
    public static ItemStack apply(ItemStack stack, GearData data) {
        stack.set(ModDataComponents.GEAR.get(), data);
        return stack;
    }

    public static GearData of(ItemStack stack) {
        GearData d = stack.get(ModDataComponents.GEAR.get());
        return d == null ? GearData.EMPTY : d;
    }
}
