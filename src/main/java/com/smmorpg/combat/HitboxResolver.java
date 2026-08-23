package com.smmorpg.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Splits a living entity into per-part boxes and raycasts the attack against them.
 *
 * <p>The boxes are derived from the entity's own bounding box, so this works for a
 * player, a zombie, a spider or a modded mob without a per-entity table. The result
 * carries the exact surface point of the hit, converted into the struck part's local
 * UV space — that coordinate is what places the wound decal on the model.
 */
public final class HitboxResolver {

    public record Result(HitLocation location, Vec3 point, float u, float v, float angle) {}

    private HitboxResolver() {}

    /** Part boxes in the entity's local frame, expressed as fractions of its bounding box. */
    public static Map<HitLocation, AABB> partBoxes(LivingEntity target) {
        AABB box = target.getBoundingBox();
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double w = box.getXsize(), h = box.getYsize(), d = box.getZsize();

        Map<HitLocation, AABB> map = new EnumMap<>(HitLocation.class);
        // Proportions follow the humanoid model: head 1/8, torso 3/8, legs 1/2.
        map.put(HitLocation.HEAD, sub(minX, minY, minZ, w, h, d, 0.22, 0.86, 0.22, 0.78, 1.00, 0.78));
        map.put(HitLocation.NECK, sub(minX, minY, minZ, w, h, d, 0.36, 0.80, 0.36, 0.64, 0.86, 0.64));
        map.put(HitLocation.TORSO, sub(minX, minY, minZ, w, h, d, 0.25, 0.46, 0.28, 0.75, 0.80, 0.72));
        map.put(HitLocation.LEFT_ARM, sub(minX, minY, minZ, w, h, d, 0.00, 0.48, 0.28, 0.25, 0.80, 0.72));
        map.put(HitLocation.RIGHT_ARM, sub(minX, minY, minZ, w, h, d, 0.75, 0.48, 0.28, 1.00, 0.80, 0.72));
        map.put(HitLocation.LEFT_LEG, sub(minX, minY, minZ, w, h, d, 0.20, 0.00, 0.28, 0.50, 0.46, 0.72));
        map.put(HitLocation.RIGHT_LEG, sub(minX, minY, minZ, w, h, d, 0.50, 0.00, 0.28, 0.80, 0.46, 0.72));
        return map;
    }

    private static AABB sub(double x, double y, double z, double w, double h, double d,
                            double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x + w * x0, y + h * y0, z + d * z0, x + w * x1, y + h * y1, z + d * z1);
    }

    /**
     * Resolves the blow. {@code from} is the attacker's eye position, {@code dir} the
     * direction of the swing.
     */
    public static Result resolve(LivingEntity target, Vec3 from, Vec3 dir, float swingAngle) {
        Vec3 to = from.add(dir.normalize().scale(8.0D));

        HitLocation best = null;
        Vec3 bestPoint = null;
        double bestDist = Double.MAX_VALUE;

        for (Map.Entry<HitLocation, AABB> e : partBoxes(target).entrySet()) {
            Optional<Vec3> clip = e.getValue().clip(from, to);
            if (clip.isEmpty()) continue;
            double dist = clip.get().distanceToSqr(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = e.getKey();
                bestPoint = clip.get();
            }
        }

        if (best == null) {
            // The swing connected by proximity rather than by ray (lag, wide arc):
            // fall back to the nearest part to the attacker's aim point.
            best = nearestPart(target, from.add(dir.normalize().scale(target.getBbWidth() + 1.0D)));
            bestPoint = target.getBoundingBox().getCenter();
        }

        AABB part = partBoxes(target).get(best);
        float u = clamp01((float) ((bestPoint.x - part.minX) / Math.max(1.0E-4D, part.getXsize())));
        float v = clamp01((float) ((bestPoint.y - part.minY) / Math.max(1.0E-4D, part.getYsize())));
        return new Result(best, bestPoint, u, v, swingAngle);
    }

    /**
     * Resolves a location from a point that is already known to be on the body — Epic
     * Fight's collider contact position. No ray is needed: the point is the answer, and
     * all that is left is deciding which part it fell inside.
     */
    public static Result at(LivingEntity target, Vec3 point) {
        HitLocation best = null;
        for (Map.Entry<HitLocation, AABB> e : partBoxes(target).entrySet()) {
            if (e.getValue().contains(point)) { best = e.getKey(); break; }
        }
        // A contact point can sit a hair outside every box; the nearest part is then right.
        if (best == null) best = nearestPart(target, point);

        AABB part = partBoxes(target).get(best);
        float u = clamp01((float) ((point.x - part.minX) / Math.max(1.0E-4D, part.getXsize())));
        float v = clamp01((float) ((point.y - part.minY) / Math.max(1.0E-4D, part.getYsize())));
        return new Result(best, point, u, v, 0.0F);
    }

    private static HitLocation nearestPart(LivingEntity target, Vec3 point) {
        HitLocation best = HitLocation.TORSO;
        double bestDist = Double.MAX_VALUE;
        for (Map.Entry<HitLocation, AABB> e : partBoxes(target).entrySet()) {
            double dist = e.getValue().getCenter().distanceToSqr(point);
            if (dist < bestDist) { bestDist = dist; best = e.getKey(); }
        }
        return best;
    }

    private static float clamp01(float f) { return f < 0 ? 0 : f > 1 ? 1 : f; }
}
