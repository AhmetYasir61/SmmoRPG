package com.smmorpg.mob;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * One kind of opponent: what to spawn, how it fights, and what it is called.
 *
 * <p>Every entry is a type that does <em>not</em> burn in daylight. That is not a detail —
 * an arena whose roster catches fire at dawn is an arena that fights itself, and a mob you
 * beat because the sun came up is a mob you did not beat.
 *
 * <p>Nothing that removes itself is in here either. A creeper ends a fight by ceasing to
 * exist, which is the opposite of what a training opponent is for.
 */
public record MobArchetype(String key,
                           EntityType<?> type,
                           MobTier tier,
                           int weight,
                           float healthScale,
                           float damageScale,
                           List<Holder<MobEffect>> auras,
                           Equipment equipment,
                           boolean flying,
                           boolean boss) {

    /** How a spawned opponent is dressed, which is most of what makes it read as a soldier. */
    public enum Equipment { NONE, LIGHT, SOLDIER, KNIGHT, WARLORD }

    public String translationKey() { return "mob.smmorpg." + key; }

    public static Builder of(String key, EntityType<?> type, MobTier tier) {
        return new Builder(key, type, tier);
    }

    public static final class Builder {
        private final String key;
        private final EntityType<?> type;
        private final MobTier tier;
        private int weight = 10;
        private float healthScale = 1.0F;
        private float damageScale = 1.0F;
        private List<Holder<MobEffect>> auras = List.of();
        private Equipment equipment = Equipment.NONE;
        private boolean flying;
        private boolean boss;

        private Builder(String key, EntityType<?> type, MobTier tier) {
            this.key = key;
            this.type = type;
            this.tier = tier;
        }

        public Builder weight(int weight) { this.weight = weight; return this; }
        public Builder stats(float health, float damage) {
            this.healthScale = health;
            this.damageScale = damage;
            return this;
        }
        @SafeVarargs
        public final Builder auras(Holder<MobEffect>... effects) {
            this.auras = List.of(effects);
            return this;
        }
        public Builder equipment(Equipment equipment) { this.equipment = equipment; return this; }
        public Builder flying() { this.flying = true; return this; }
        public Builder boss() { this.boss = true; this.weight = Math.max(1, weight / 3); return this; }

        public MobArchetype build() {
            return new MobArchetype(key, type, tier, weight, healthScale, damageScale,
                    auras, equipment, flying, boss);
        }
    }
}
