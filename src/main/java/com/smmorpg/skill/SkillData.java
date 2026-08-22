package com.smmorpg.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Which skills a player has, and at what rank. */
public record SkillData(Map<String, Integer> ranks, int skillPoints) {

    public static final SkillData EMPTY = new SkillData(Map.of(), 0);

    public static final Codec<SkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ranks").forGetter(SkillData::ranks),
            Codec.INT.fieldOf("points").forGetter(SkillData::skillPoints)
    ).apply(i, SkillData::new));

    public int rank(Skill skill) { return ranks.getOrDefault(skill.id().toString(), 0); }

    public boolean has(Skill skill) { return rank(skill) > 0; }

    public float value(Skill skill) { return skill.valueAt(rank(skill)); }

    public SkillData grantPoints(int amount) {
        return new SkillData(ranks, skillPoints + amount);
    }

    /** Returns the same instance when the skill cannot be raised, so callers can compare. */
    public SkillData learn(Skill skill) {
        int current = rank(skill);
        if (current >= skill.maxRank()) return this;
        if (skillPoints < skill.cost()) return this;
        if (skill.requires() != null) {
            Skill parent = Skills.get(skill.requires());
            if (parent != null && rank(parent) == 0) return this;
        }
        Map<String, Integer> next = new HashMap<>(ranks);
        next.put(skill.id().toString(), current + 1);
        return new SkillData(Map.copyOf(next), skillPoints - skill.cost());
    }

    public SkillData reset() { return new SkillData(Map.of(), totalSpent() + skillPoints); }

    private int totalSpent() {
        int sum = 0;
        for (var e : ranks.entrySet()) {
            Skill s = Skills.get(ResourceLocation.parse(e.getKey()));
            if (s != null) sum += s.cost() * e.getValue();
        }
        return sum;
    }
}
