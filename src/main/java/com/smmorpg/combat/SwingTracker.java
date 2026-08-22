package com.smmorpg.combat;

import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps a live {@link BladeTrace} for every entity mid-swing.
 *
 * <p>The edge position is reconstructed from the hand's world position and the swing's
 * current phase, which means the trace follows the actual animation rather than a fixed
 * arc: change the animation in the studio panel and the cuts change with it, for free.
 */
public final class SwingTracker {

    private static final Map<LivingEntity, BladeTrace> TRACES = new WeakHashMap<>();

    private SwingTracker() {}

    public static BladeTrace trace(LivingEntity e) {
        return TRACES.computeIfAbsent(e, k -> new BladeTrace());
    }

    /** Called every tick for every living entity. Samples only while a swing is in flight. */
    public static void tick(LivingEntity e) {
        BladeTrace trace = trace(e);
        if (!e.swinging) {
            if (!trace.isEmpty()) trace.clear();
            return;
        }

        WeaponClass wc = RpgWeaponItem.classOf(e.getMainHandItem());
        float reach = wc == null ? 2.0F : wc.reach();

        Vec3 hand = handPosition(e);
        Vec3 edge = edgeDirection(e, wc);
        trace.sample(hand, hand.add(edge.scale(reach * 0.5D)));
    }

    /** Approximate world position of the weapon hand, offset to the correct side. */
    public static Vec3 handPosition(LivingEntity e) {
        Vec3 eye = e.getEyePosition();
        Vec3 look = e.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        double side = e.getMainArm() == HumanoidArm.RIGHT ? 0.36D : -0.36D;
        return eye.add(right.scale(side)).add(0.0D, -0.22D, 0.0D).add(look.scale(0.2D));
    }

    /**
     * Direction the edge points at this instant in the swing. The blade sweeps through the
     * arc over the swing's duration, so consecutive samples describe a real curve.
     */
    private static Vec3 edgeDirection(LivingEntity e, WeaponClass wc) {
        float phase = e.swingTime / Math.max(1.0F, (float) e.getCurrentSwingDuration());
        Vec3 look = e.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 up = right.cross(look).normalize();

        if (wc == WeaponClass.SPEAR || wc == WeaponClass.BOW) {
            // A thrust barely sweeps: the edge stays on the look axis and extends.
            return look;
        }
        // Otherwise sweep from high-right to low-left across the swing.
        double a = Math.toRadians(-60.0D + 150.0D * phase);
        return look.scale(Math.cos(a) * 0.55D)
                .add(right.scale(-Math.sin(a) * 0.75D))
                .add(up.scale(Math.cos(a + 1.2D) * 0.35D))
                .normalize();
    }

    public static void forget(LivingEntity e) { TRACES.remove(e); }
}
