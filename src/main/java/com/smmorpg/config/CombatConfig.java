package com.smmorpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Every knob of the combat overhaul. Tuned for "realistic" out of the box. */
public final class CombatConfig {
    public static final ModConfigSpec SPEC;
    public static final CombatConfig CFG;

    // --- view ---
    public final ModConfigSpec.BooleanValue fpsIsDefault;
    public final ModConfigSpec.BooleanValue allowTpsToggle;
    public final ModConfigSpec.BooleanValue renderFirstPersonBody;
    public final ModConfigSpec.DoubleValue baseFov;

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

    private CombatConfig(ModConfigSpec.Builder b) {
        b.push("view");
        fpsIsDefault = b.comment("First-person is the primary mode; TPS is only a toggle.")
                .define("fpsIsDefault", true);
        allowTpsToggle = b.define("allowTpsToggle", true);
        renderFirstPersonBody = b.comment("Render the full player body in first person (arms, torso, legs).")
                .define("renderFirstPersonBody", true);
        baseFov = b.defineInRange("baseFov", 78.0D, 30.0D, 130.0D);
        b.pop();

        b.push("feedback");
        cameraShakeScale = b.comment("0 disables shake, 1 is realistic, >1 is cinematic.")
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
