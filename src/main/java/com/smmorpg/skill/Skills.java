package com.smmorpg.skill;

import com.smmorpg.SmmoRPG;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** The built-in skill tree. Third-party mods add to this through the public API. */
public final class Skills {

    private static final Map<ResourceLocation, Skill> REGISTRY = new LinkedHashMap<>();

    // --- offense ---
    public static final Skill DEEP_CUT = register("deep_cut", Skill.CATEGORY_OFFENSE, 5, 1, 0.08F, null);
    public static final Skill BLEED_MASTERY = register("bleed_mastery", Skill.CATEGORY_OFFENSE, 5, 1, 0.12F, DEEP_CUT_ID());
    public static final Skill EXECUTIONER = register("executioner", Skill.CATEGORY_OFFENSE, 3, 2, 0.10F, DEEP_CUT_ID());
    public static final Skill COMBO_FLOW = register("combo_flow", Skill.CATEGORY_OFFENSE, 5, 1, 0.06F, null);

    // --- defense ---
    public static final Skill WIDE_PARRY = register("wide_parry", Skill.CATEGORY_DEFENSE, 4, 2, 1.0F, null);
    public static final Skill IRON_POSTURE = register("iron_posture", Skill.CATEGORY_DEFENSE, 5, 1, 0.10F, null);
    public static final Skill DEFLECT_RIPOSTE = register("deflect_riposte", Skill.CATEGORY_DEFENSE, 3, 2, 0.25F, id("wide_parry"));

    // --- movement ---
    public static final Skill WALL_KICK = register("wall_kick", Skill.CATEGORY_MOVEMENT, 3, 1, 0.15F, null);
    public static final Skill AIR_STEP = register("air_step", Skill.CATEGORY_MOVEMENT, 5, 1, 1.0F, id("wall_kick"));
    public static final Skill SHADOW_DASH = register("shadow_dash", Skill.CATEGORY_MOVEMENT, 4, 2, 0.12F, null);
    public static final Skill WALL_RUN = register("wall_run", Skill.CATEGORY_MOVEMENT, 3, 2, 5.0F, id("wall_kick"));

    // --- survival ---
    public static final Skill FIELD_SURGERY = register("field_surgery", Skill.CATEGORY_SURVIVAL, 4, 1, 0.15F, null);
    public static final Skill THICK_SKIN = register("thick_skin", Skill.CATEGORY_SURVIVAL, 5, 1, 0.06F, null);
    public static final Skill SECOND_WIND = register("second_wind", Skill.CATEGORY_SURVIVAL, 3, 2, 0.20F, id("thick_skin"));

    private Skills() {}

    private static ResourceLocation id(String path) { return SmmoRPG.id(path); }
    private static ResourceLocation DEEP_CUT_ID() { return id("deep_cut"); }

    private static Skill register(String path, String category, int maxRank, int cost,
                                  float perRank, ResourceLocation requires) {
        Skill s = new Skill(id(path), category, maxRank, cost, perRank, requires);
        REGISTRY.put(s.id(), s);
        return s;
    }

    /** Public registration hook used by {@code com.smmorpg.api.SmmoRPGApi}. */
    public static Skill add(Skill skill) {
        REGISTRY.put(skill.id(), skill);
        return skill;
    }

    public static Skill get(ResourceLocation id) { return REGISTRY.get(id); }
    public static Map<ResourceLocation, Skill> all() { return java.util.Collections.unmodifiableMap(REGISTRY); }
}
