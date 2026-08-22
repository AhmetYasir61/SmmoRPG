package com.smmorpg.wound;

import com.smmorpg.combat.HitLocation;
import com.smmorpg.config.CombatConfig;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModAttributes;
import com.smmorpg.core.ModParticles;
import com.smmorpg.network.Net;
import com.smmorpg.network.S2CWoundSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Opens, bleeds and closes wounds. Server-authoritative; clients get told what to draw. */
public final class WoundSystem {

    private WoundSystem() {}

    public static WoundData get(LivingEntity e) { return e.getData(ModAttachments.WOUNDS.get()); }

    public static void set(LivingEntity e, WoundData data) {
        e.setData(ModAttachments.WOUNDS.get(), data);
        if (e.level() instanceof ServerLevel server) {
            Net.sendToTracking(server, e, new S2CWoundSync(e.getId(), data));
        }
    }

    /**
     * Cuts a body. Returns the wound that was opened so the caller can drive sound,
     * particles and camera shake off the same data the wound carries.
     */
    public static Wound inflict(LivingEntity target, HitLocation loc, int woundType,
                                float u, float v, float angle, float severity, Vec3 hitPos) {
        WoundData data = get(target);

        float resist = (float) target.getAttributeValue(ModAttributes.BLEED_RESISTANCE.getDelegate());
        float depth = Math.max(0.05F, severity * (1.0F - resist));
        float length = Math.min(1.0F, 0.25F + severity * 0.6F);

        Wound wound = new Wound(loc.key(), woundType, u, v, angle, length, depth,
                target.tickCount, 0.0F);

        data = data.add(wound).withBloodLoss(data.bloodLoss() + depth * 6.0F);
        set(target, data);

        if (target.level() instanceof ServerLevel server) {
            spray(server, hitPos, target, depth, woundType);
        }
        return wound;
    }

    /** Takes a limb off. Only fires when the blow is both severe enough and a cutting one. */
    public static boolean trySever(LivingEntity target, HitLocation loc, float damage,
                                   float slashFactor, Vec3 hitPos) {
        if (!CombatConfig.CFG.enableDismemberment.get()) return false;
        if (!loc.severable()) return false;

        float threshold = CombatConfig.CFG.dismemberThreshold.get().floatValue() * target.getMaxHealth();
        if (damage < threshold * (2.0F - slashFactor)) return false;

        WoundData data = get(target);
        if (data.isSevered(loc)) return false;

        data = data.sever(loc).add(new Wound(loc.key(), Wound.TYPE_SEVER, 0.5F, 0.5F, 0.0F,
                1.0F, 1.0F, target.tickCount, 0.0F));
        set(target, data);

        if (target.level() instanceof ServerLevel server) {
            spray(server, hitPos, target, 1.0F, Wound.TYPE_SEVER);
        }
        // Losing the head or throat is not survivable.
        if (loc == HitLocation.HEAD || loc == HitLocation.NECK) {
            target.hurt(target.damageSources().generic(), target.getMaxHealth() * 4.0F);
        }
        return true;
    }

    private static void spray(ServerLevel level, Vec3 pos, LivingEntity target, float depth, int type) {
        if (type == Wound.TYPE_BLUNT) return;
        int count = Math.max(3, Math.round(depth * 40.0F));
        level.sendParticles(ModParticles.BLOOD_SPRAY.get(), pos.x, pos.y, pos.z,
                count, 0.08D, 0.08D, 0.08D, 0.15D * depth);
        if (type == Wound.TYPE_SEVER) {
            level.sendParticles(ModParticles.BLOOD_SPRAY.get(), pos.x, pos.y, pos.z,
                    count * 3, 0.2D, 0.2D, 0.2D, 0.45D);
        }
    }

    /**
     * Per-tick upkeep: bleed out, then knit back together. The healing rate follows the
     * entity's own regeneration, so a mob that heals fast visibly closes its cuts fast.
     */
    public static void tick(LivingEntity e) {
        WoundData data = get(e);
        if (data.wounds().isEmpty() && data.bloodLoss() <= 0.0F) return;

        boolean changed = false;

        if (e.tickCount % 20 == 0) {
            float bleed = data.bleedRate();
            if (bleed > 0.0F) {
                e.hurt(e.damageSources().generic(), bleed);
                if (e.level() instanceof ServerLevel server) {
                    server.sendParticles(ModParticles.BLOOD_DRIP.get(),
                            e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                            Math.min(12, Math.round(bleed * 6.0F)), 0.25D, 0.35D, 0.25D, 0.01D);
                }
                changed = true;
            }
        }

        float regen = (float) e.getAttributeValue(ModAttributes.WOUND_REGENERATION.getDelegate());
        if (regen > 0.0F) {
            WoundData healed = data.tickHealing(regen);
            if (healed != data) { data = healed; changed = true; }
        }

        if (changed) set(e, data);
    }

    /** Bandages and cautery: close the freshest open wound outright. */
    public static boolean treat(LivingEntity e, float strength) {
        WoundData data = get(e);
        if (data.wounds().isEmpty()) return false;
        java.util.List<Wound> next = new java.util.ArrayList<>();
        boolean treated = false;
        for (Wound w : data.wounds()) {
            if (!treated && w.type() != Wound.TYPE_SEVER && !w.closed()) {
                treated = true;
                Wound healed = w.heal(strength);
                if (!healed.closed()) next.add(healed);
            } else {
                next.add(w);
            }
        }
        if (!treated) return false;
        set(e, new WoundData(java.util.List.copyOf(next), data.severed(),
                Math.max(0.0F, data.bloodLoss() - strength * 30.0F)));
        return true;
    }

    /** True when the entity has bled enough to be visibly failing. */
    public static boolean exsanguinating(LivingEntity e) {
        return get(e).bloodLoss() > e.getMaxHealth() * 1.5F;
    }

    public static void clear(LivingEntity e, DamageSource ignored) {
        set(e, WoundData.EMPTY);
    }
}
