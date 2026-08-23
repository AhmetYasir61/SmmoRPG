package com.smmorpg.mob;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.smmorpg.mob.MobArchetype.Equipment;

/**
 * Everything the arena and the world can send at you.
 *
 * <p>Two rules govern the whole table. Nothing here burns in daylight, so a fight is decided
 * by the fight. And nothing here removes itself — no creepers, nothing that wins by ceasing
 * to exist. A training opponent that blows up is a training opponent you learned nothing
 * from.
 *
 * <p>The table climbs. Mortals are soldiers and raiders; by Ascendant you are fighting
 * things out of a bestiary, and Primordial is where the dragons are.
 */
public final class MobRoster {

    private static final Map<MobTier, List<MobArchetype>> BY_TIER = new EnumMap<>(MobTier.class);
    private static final Map<String, MobArchetype> BY_KEY = new java.util.LinkedHashMap<>();

    static {
        // --- MORTAL: men with weapons ---
        add(MobArchetype.of("conscript", EntityType.HUSK, MobTier.MORTAL)
                .weight(20).stats(1.0F, 1.0F).equipment(Equipment.LIGHT));
        add(MobArchetype.of("soldier", EntityType.HUSK, MobTier.MORTAL)
                .weight(16).stats(1.2F, 1.15F).equipment(Equipment.SOLDIER));
        add(MobArchetype.of("raider", EntityType.PILLAGER, MobTier.MORTAL)
                .weight(12).stats(1.0F, 1.1F).equipment(Equipment.LIGHT));
        add(MobArchetype.of("marauder", EntityType.VINDICATOR, MobTier.MORTAL)
                .weight(12).stats(1.1F, 1.25F).equipment(Equipment.LIGHT));
        add(MobArchetype.of("bone_archer", EntityType.WITHER_SKELETON, MobTier.MORTAL)
                .weight(10).stats(0.9F, 1.1F));

        // --- VETERAN: drilled, armoured, and they hold a line ---
        add(MobArchetype.of("legionary", EntityType.HUSK, MobTier.VETERAN)
                .weight(18).stats(1.5F, 1.3F).equipment(Equipment.KNIGHT)
                .auras(MobEffects.DAMAGE_RESISTANCE));
        add(MobArchetype.of("berserker", EntityType.ZOGLIN, MobTier.VETERAN)
                .weight(10).stats(1.4F, 1.5F).auras(MobEffects.MOVEMENT_SPEED));
        add(MobArchetype.of("brute", EntityType.PIGLIN_BRUTE, MobTier.VETERAN)
                .weight(14).stats(1.5F, 1.4F).equipment(Equipment.SOLDIER));
        add(MobArchetype.of("witch_hunter", EntityType.EVOKER, MobTier.VETERAN)
                .weight(8).stats(1.2F, 1.2F));

        // --- CHAMPION: named threats, each with a trick ---
        add(MobArchetype.of("dread_knight", EntityType.WITHER_SKELETON, MobTier.CHAMPION)
                .weight(14).stats(2.0F, 1.7F).equipment(Equipment.KNIGHT)
                .auras(MobEffects.DAMAGE_BOOST));
        add(MobArchetype.of("siege_beast", EntityType.RAVAGER, MobTier.CHAMPION)
                .weight(10).stats(2.2F, 1.8F).auras(MobEffects.DAMAGE_RESISTANCE));
        add(MobArchetype.of("emberling", EntityType.BLAZE, MobTier.CHAMPION)
                .weight(9).stats(1.4F, 1.6F).flying().auras(MobEffects.FIRE_RESISTANCE));
        add(MobArchetype.of("warden_of_ash", EntityType.PIGLIN_BRUTE, MobTier.CHAMPION)
                .weight(11).stats(2.1F, 1.9F).equipment(Equipment.WARLORD)
                .auras(MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED));

        // --- ASCENDANT: out of a bestiary now ---
        add(MobArchetype.of("iron_colossus", EntityType.IRON_GOLEM, MobTier.ASCENDANT)
                .weight(10).stats(3.0F, 2.2F).auras(MobEffects.DAMAGE_RESISTANCE));
        add(MobArchetype.of("revenant_lord", EntityType.WITHER_SKELETON, MobTier.ASCENDANT)
                .weight(12).stats(2.8F, 2.4F).equipment(Equipment.WARLORD)
                .auras(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED));
        add(MobArchetype.of("shade", EntityType.VEX, MobTier.ASCENDANT)
                .weight(8).stats(1.8F, 2.6F).flying().auras(MobEffects.INVISIBILITY));
        add(MobArchetype.of("hollow_king", EntityType.HUSK, MobTier.ASCENDANT)
                .weight(11).stats(3.2F, 2.3F).equipment(Equipment.WARLORD)
                .auras(MobEffects.DAMAGE_RESISTANCE, MobEffects.DAMAGE_BOOST));

        // --- DIVINE: the things the bestiary warns about ---
        add(MobArchetype.of("wither_sovereign", EntityType.WITHER, MobTier.DIVINE)
                .weight(6).stats(2.4F, 2.6F).flying().boss());
        add(MobArchetype.of("deep_warden", EntityType.WARDEN, MobTier.DIVINE)
                .weight(5).stats(2.0F, 3.0F).boss());
        add(MobArchetype.of("titan_of_iron", EntityType.IRON_GOLEM, MobTier.DIVINE)
                .weight(9).stats(4.5F, 3.0F)
                .auras(MobEffects.DAMAGE_RESISTANCE, MobEffects.DAMAGE_BOOST));
        add(MobArchetype.of("god_slayer", EntityType.WITHER_SKELETON, MobTier.DIVINE)
                .weight(10).stats(3.6F, 3.4F).equipment(Equipment.WARLORD)
                .auras(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED,
                        MobEffects.DAMAGE_RESISTANCE));

        // --- PRIMORDIAL: dragons ---
        add(MobArchetype.of("elder_dragon", EntityType.ENDER_DRAGON, MobTier.PRIMORDIAL)
                .weight(4).stats(2.2F, 3.2F).flying().boss());
        add(MobArchetype.of("world_ender", EntityType.WITHER, MobTier.PRIMORDIAL)
                .weight(6).stats(3.5F, 3.8F).flying().boss());
        add(MobArchetype.of("first_warden", EntityType.WARDEN, MobTier.PRIMORDIAL)
                .weight(7).stats(3.4F, 4.0F).boss()
                .auras(MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED));
        add(MobArchetype.of("eternal_sovereign", EntityType.HUSK, MobTier.PRIMORDIAL)
                .weight(9).stats(5.0F, 4.2F).equipment(Equipment.WARLORD)
                .auras(MobEffects.DAMAGE_RESISTANCE, MobEffects.DAMAGE_BOOST,
                        MobEffects.MOVEMENT_SPEED, MobEffects.REGENERATION));
    }

    private MobRoster() {}

    private static void add(MobArchetype.Builder builder) {
        MobArchetype archetype = builder.build();
        BY_TIER.computeIfAbsent(archetype.tier(), t -> new ArrayList<>()).add(archetype);
        BY_KEY.put(archetype.key(), archetype);
    }

    public static MobArchetype byKey(String key) { return BY_KEY.get(key); }

    public static List<MobArchetype> ofTier(MobTier tier) {
        return BY_TIER.getOrDefault(tier, List.of());
    }

    /**
     * Rolls an opponent for a difficulty band.
     *
     * <p>The unlocked tier is not the only one that can appear — a band still fields the
     * ranks below it, just less often. A wave of nothing but Primordials is a spectacle;
     * a wave that is mostly champions with a dragon in it is a fight.
     */
    public static MobArchetype roll(RandomSource rng, int band) {
        MobTier top = MobTier.forBand(band);

        List<MobArchetype> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;

        for (MobTier tier : MobTier.values()) {
            if (tier.ordinal() > top.ordinal()) break;
            // Each step below the top tier is half as likely as the one above it.
            int falloff = 1 << (top.ordinal() - tier.ordinal());
            for (MobArchetype archetype : ofTier(tier)) {
                int weight = Math.max(1, archetype.weight() / falloff);
                pool.add(archetype);
                weights.add(weight);
                total += weight;
            }
        }

        if (pool.isEmpty()) return BY_KEY.get("conscript");

        int pick = rng.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            pick -= weights.get(i);
            if (pick < 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }
}
