package com.smmorpg.skill;

import net.minecraft.resources.ResourceLocation;

/**
 * One learnable skill.
 *
 * <p>A skill is data, not code: it names a behaviour the combat and movement systems
 * already understand and scales it by rank. That keeps the tree fully describable over the
 * network, which is what lets the studio panel retune it live without a client update.
 */
public record Skill(ResourceLocation id,
                    String category,
                    int maxRank,
                    int cost,
                    float perRank,
                    ResourceLocation requires) {

    public static final String CATEGORY_OFFENSE = "offense";
    public static final String CATEGORY_DEFENSE = "defense";
    public static final String CATEGORY_MOVEMENT = "movement";
    public static final String CATEGORY_SURVIVAL = "survival";

    public String translationKey() { return "skill." + id.getNamespace() + "." + id.getPath(); }
    public String descriptionKey() { return translationKey() + ".desc"; }

    /** Total magnitude at a given rank. Linear per rank keeps the tooltips honest. */
    public float valueAt(int rank) { return perRank * Math.min(rank, maxRank); }
}
