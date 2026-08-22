package com.smmorpg.api;

import com.smmorpg.combat.HitLocation;
import com.smmorpg.combat.ImpactMaterial;
import com.smmorpg.wound.Wound;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Events other mods can listen to on the NeoForge game bus.
 *
 * <p>They fire at the exact points the mod makes its own decisions, so a listener can veto
 * a dismemberment or rewrite a damage number without reimplementing anything.
 */
public final class SmmoRPGEvents {

    private SmmoRPGEvents() {}

    /** Fired after the blow is located but before damage is applied. Cancellable. */
    public static class PreHit extends Event implements ICancellableEvent {
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final HitLocation location;
        private final ImpactMaterial material;
        private float damage;

        public PreHit(LivingEntity attacker, LivingEntity target, HitLocation location,
                      ImpactMaterial material, float damage) {
            this.attacker = attacker;
            this.target = target;
            this.location = location;
            this.material = material;
            this.damage = damage;
        }

        public LivingEntity attacker() { return attacker; }
        public LivingEntity target() { return target; }
        public HitLocation location() { return location; }
        public ImpactMaterial material() { return material; }
        public float damage() { return damage; }
        public void setDamage(float damage) { this.damage = damage; }
    }

    /** Fired when a wound is about to be opened. Cancel to leave the target unmarked. */
    public static class WoundInflicted extends Event implements ICancellableEvent {
        private final LivingEntity target;
        private final Wound wound;

        public WoundInflicted(LivingEntity target, Wound wound) {
            this.target = target;
            this.wound = wound;
        }

        public LivingEntity target() { return target; }
        public Wound wound() { return wound; }
    }

    /** Fired before a limb comes off. Cancel to keep it attached. */
    public static class Dismember extends Event implements ICancellableEvent {
        private final LivingEntity target;
        private final HitLocation location;

        public Dismember(LivingEntity target, HitLocation location) {
            this.target = target;
            this.location = location;
        }

        public LivingEntity target() { return target; }
        public HitLocation location() { return location; }
    }

    /** Fired after a player gains a level; not cancellable, the level is already banked. */
    public static class LevelUp extends Event {
        private final Player player;
        private final int newLevel;

        public LevelUp(Player player, int newLevel) {
            this.player = player;
            this.newLevel = newLevel;
        }

        public Player player() { return player; }
        public int newLevel() { return newLevel; }
    }

    /** Fired when a parry connects, so an add-on can hang a riposte window off it. */
    public static class Parry extends Event {
        private final LivingEntity defender;
        private final LivingEntity attacker;

        public Parry(LivingEntity defender, LivingEntity attacker) {
            this.defender = defender;
            this.attacker = attacker;
        }

        public LivingEntity defender() { return defender; }
        public LivingEntity attacker() { return attacker; }
    }
}
