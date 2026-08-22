package com.smmorpg.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * The swept path of a blade's edge.
 *
 * <p>This is what makes a cut belong to the swing that made it rather than to a canned
 * animation. Every tick of a swing the edge's tip and base are sampled in world space; the
 * segments between consecutive samples form a ribbon. When that ribbon crosses a body part,
 * the crossing itself gives the wound its entry point, its direction and its length — so
 * cutting low across a thigh and cutting down through a shoulder produce different marks
 * from the same weapon, without either being authored anywhere.
 */
public final class BladeTrace {

    /** One sampled position of the edge, base and tip, at one instant. */
    public record Sample(Vec3 base, Vec3 tip, long timeNanos) {}

    /** Where the ribbon crossed a part, in that part's own coordinates. */
    public record Cut(HitLocation location, Vec3 entry, Vec3 exit,
                      float u, float v, float angle, float length, float depth) {}

    private static final int MAX_SAMPLES = 12;

    private final Deque<Sample> samples = new ArrayDeque<>(MAX_SAMPLES);

    public void clear() { samples.clear(); }

    public boolean isEmpty() { return samples.size() < 2; }

    /** Records where the edge is right now. Called once per tick while a swing is live. */
    public void sample(Vec3 base, Vec3 tip) {
        if (samples.size() >= MAX_SAMPLES) samples.pollFirst();
        samples.addLast(new Sample(base, tip, System.nanoTime()));
    }

    public List<Sample> samples() { return new ArrayList<>(samples); }

    /**
     * Intersects the swept ribbon with the target's parts.
     *
     * <p>Each consecutive pair of samples spans a quad. Rather than doing exact quad/box
     * clipping, the quad is walked as a small set of line segments (edge tip path, edge base
     * path, and the blade line at each sample), which is both cheap and accurate enough at
     * the scale a body part occupies.
     */
    public List<Cut> intersect(LivingEntity target) {
        List<Cut> cuts = new ArrayList<>();
        if (isEmpty()) return cuts;

        var parts = HitboxResolver.partBoxes(target);
        List<Sample> list = samples();

        for (int i = 1; i < list.size(); i++) {
            Sample a = list.get(i - 1);
            Sample b = list.get(i);

            for (var entry : parts.entrySet()) {
                HitLocation loc = entry.getKey();
                AABB box = entry.getValue();

                // Three probes across the swept quad: the tip path, the base path, and the
                // blade itself at the end of the step. Together they catch a slice through a
                // limb whether the tip, the middle or the heel of the blade did the cutting.
                Vec3 in = null, out = null;
                in = firstHit(box, a.tip(), b.tip(), in);
                out = lastHit(box, a.tip(), b.tip(), out);
                in = firstHit(box, a.base(), b.base(), in);
                out = lastHit(box, a.base(), b.base(), out);
                in = firstHit(box, b.base(), b.tip(), in);
                out = lastHit(box, b.base(), b.tip(), out);

                if (in == null) continue;
                if (out == null) out = in;

                Vec3 mid = in.add(out).scale(0.5D);
                float u = norm(mid.x, box.minX, box.maxX);
                float v = norm(mid.y, box.minY, box.maxY);

                // The cut's direction on the surface is the direction the edge travelled.
                Vec3 travel = b.tip().subtract(a.tip());
                float angle = (float) Math.toDegrees(Math.atan2(travel.y, travel.x));

                // Length is how far the edge stayed inside the part; depth is how deep the
                // ribbon reached into it. Both come straight from the geometry.
                float length = (float) Math.min(1.0D, in.distanceTo(out) / Math.max(0.05D, box.getYsize()));
                float depth = (float) Math.min(1.0D, penetration(box, in, out) / Math.max(0.05D, box.getZsize()));

                cuts.add(new Cut(loc, in, out, u, v, angle, Math.max(0.15F, length), Math.max(0.1F, depth)));
            }
        }
        return cuts;
    }

    /** The single deepest cut of the swing: the one that becomes the wound. */
    public Optional<Cut> primary(LivingEntity target) {
        return intersect(target).stream()
                .max((x, y) -> Float.compare(x.depth() * x.length(), y.depth() * y.length()));
    }

    private static Vec3 firstHit(AABB box, Vec3 from, Vec3 to, Vec3 current) {
        Optional<Vec3> clip = box.clip(from, to);
        if (clip.isEmpty()) return current;
        if (current == null) return clip.get();
        return clip.get().distanceToSqr(from) < current.distanceToSqr(from) ? clip.get() : current;
    }

    private static Vec3 lastHit(AABB box, Vec3 from, Vec3 to, Vec3 current) {
        Optional<Vec3> clip = box.clip(to, from);
        if (clip.isEmpty()) return current;
        if (current == null) return clip.get();
        return clip.get().distanceToSqr(from) > current.distanceToSqr(from) ? clip.get() : current;
    }

    private static double penetration(AABB box, Vec3 in, Vec3 out) {
        Vec3 centre = box.getCenter();
        double dIn = Math.abs(in.z - centre.z);
        double dOut = Math.abs(out.z - centre.z);
        return Math.max(0.0D, box.getZsize() * 0.5D - Math.min(dIn, dOut));
    }

    private static float norm(double value, double min, double max) {
        float f = (float) ((value - min) / Math.max(1.0E-4D, max - min));
        return f < 0 ? 0 : f > 1 ? 1 : f;
    }
}
