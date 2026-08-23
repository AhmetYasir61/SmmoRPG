package com.smmorpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * SmmoRPG's own knobs.
 *
 * <p>Nothing here touches how combat plays or where the camera sits — those belong to Epic
 * Fight and Real Camera and are configured in their own files. What is tunable here is the
 * layer this mod adds: how hard a blow is felt, how badly it bleeds, and how loot rolls.
 */
public final class CombatConfig {
    public static final ModConfigSpec SPEC;
    public static final CombatConfig CFG;

    // --- feedback ---
    public final ModConfigSpec.DoubleValue cameraShakeScale;
    public final ModConfigSpec.DoubleValue recoilScale;
    public final ModConfigSpec.DoubleValue hitStopScale;

    // --- wounds ---
    public final ModConfigSpec.BooleanValue enableDismemberment;
    public final ModConfigSpec.DoubleValue dismemberThreshold;
    public final ModConfigSpec.IntValue maxWoundsPerEntity;
    public final ModConfigSpec.DoubleValue bleedPerWoundPerSecond;
    public final ModConfigSpec.IntValue woundCloseTicks;

    // --- progression ---
    public final ModConfigSpec.IntValue baseXpPerLevel;
    public final ModConfigSpec.DoubleValue xpCurveExponent;
    public final ModConfigSpec.IntValue maxLevel;

    // --- loot ---
    public final ModConfigSpec.DoubleValue holyChance;
    public final ModConfigSpec.DoubleValue cursedChance;
    public final ModConfigSpec.IntValue maxAffixesPerItem;

    // --- textures ---
    public final ModConfigSpec.IntValue decalResolution;

    // --- monsters ---
    public final ModConfigSpec.BooleanValue enableDevouring;
    public final ModConfigSpec.BooleanValue levelWorldMobs;
    public final ModConfigSpec.BooleanValue preventDaylightBurning;

    // --- compatibility ---
    public final ModConfigSpec.BooleanValue enforceModSettings;
    public final ModConfigSpec.BooleanValue disableEpicFightComputeShader;
    public final ModConfigSpec.BooleanValue pinRealCamera;

    private CombatConfig(ModConfigSpec.Builder b) {
        b.push("feedback");
        cameraShakeScale = b.comment("0 disables shake entirely. 1 is the tuned default;",
                        "2 is heavy, 4 is cinematic and hard to aim through.")
                .defineInRange("cameraShakeScale", 1.0D, 0.0D, 4.0D);
        recoilScale = b.defineInRange("recoilScale", 1.0D, 0.0D, 4.0D);
        hitStopScale = b.comment("Freeze-frame on a solid connect, in the style of a fighting game.")
                .defineInRange("hitStopScale", 1.0D, 0.0D, 3.0D);
        b.pop();

        b.push("wounds");
        enableDismemberment = b.define("enableDismemberment", true);
        dismemberThreshold = b.comment("Fraction of the target's max health a single blow must deal to sever a limb.")
                .defineInRange("dismemberThreshold", 0.45D, 0.05D, 4.0D);
        maxWoundsPerEntity = b.defineInRange("maxWoundsPerEntity", 24, 1, 128);
        bleedPerWoundPerSecond = b.defineInRange("bleedPerWoundPerSecond", 0.18D, 0.0D, 4.0D);
        woundCloseTicks = b.comment("Ticks of regeneration needed for one wound to fully close.")
                .defineInRange("woundCloseTicks", 400, 20, 24000);
        b.pop();

        b.push("progression");
        baseXpPerLevel = b.defineInRange("baseXpPerLevel", 120, 1, 100000);
        xpCurveExponent = b.defineInRange("xpCurveExponent", 1.55D, 1.0D, 3.0D);
        maxLevel = b.defineInRange("maxLevel", 120, 1, 1000);
        b.pop();

        b.push("loot");
        holyChance = b.defineInRange("holyChance", 0.045D, 0.0D, 1.0D);
        cursedChance = b.defineInRange("cursedChance", 0.055D, 0.0D, 1.0D);
        maxAffixesPerItem = b.defineInRange("maxAffixesPerItem", 4, 0, 8);
        b.pop();

        b.push("monsters");
        enableDevouring = b.comment("Monsters that kill monsters grow from it, and eating",
                        "their own kind counts double. Enough of it and they evolve,",
                        "climb a tier, and eventually become Lords.")
                .define("enableDevouring", true);
        levelWorldMobs = b.comment("Give naturally spawned mobs a tier and a level too,",
                        "scaled to how far from spawn they are. Off means only the",
                        "training arena's opponents are managed.")
                .define("levelWorldMobs", true);
        preventDaylightBurning = b.comment("Stop managed monsters burning up at dawn.",
                        "A fight decided by the sunrise is a fight you did not win.")
                .define("preventDaylightBurning", true);
        b.pop();

        b.push("compatibility");
        enforceModSettings = b.comment("Master switch for everything in this section.",
                        "Off means SmmoRPG never touches another mod's settings.")
                .define("enforceModSettings", true);
        disableEpicFightComputeShader = b.comment(
                        "Epic Fight skins its armatures on the GPU when this is on.",
                        "On drivers where it misbehaves the result is a mangled or invisible",
                        "model rather than a crash, which is miserable to diagnose.")
                .define("disableEpicFightComputeShader", true);
        pinRealCamera = b.comment("Keeps Real Camera enabled and stops it switching itself off",
                        "while sneaking, swimming or crawling. Camera offsets and bind",
                        "targets are left exactly as Real Camera ships them.")
                .define("pinRealCamera", true);
        b.pop();

        b.push("textures");
        decalResolution = b.comment("Wound / blood decal atlas resolution. The world stays vanilla-looking;",
                        "only the decals are high definition.")
                .defineInRange("decalResolution", 1024, 64, 4096);
        b.pop();
    }

    static {
        Pair<CombatConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(CombatConfig::new);
        CFG = pair.getLeft();
        SPEC = pair.getRight();
    }
}
