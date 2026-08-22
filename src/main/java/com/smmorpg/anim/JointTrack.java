package com.smmorpg.anim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Every keyframe for one joint, in ascending time. */
public record JointTrack(String joint, List<Keyframe> frames) {

    public static final Codec<JointTrack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("joint").forGetter(JointTrack::joint),
            Keyframe.CODEC.listOf().fieldOf("frames").forGetter(JointTrack::frames)
    ).apply(i, JointTrack::new));

    public static JointTrack of(Joint joint, Keyframe... frames) {
        return new JointTrack(joint.key(), List.of(frames));
    }

    public Joint bone() { return Joint.byKey(joint); }

    /**
     * Samples the track at {@code time}, writing into {@code out}.
     *
     * <p>Clamped rather than wrapped: a clip that loops handles the wrap itself by placing
     * a final keyframe identical to the first, which keeps the sampler branch-free.
     */
    public void sample(float time, Pose out) {
        if (frames.isEmpty()) return;

        Keyframe first = frames.get(0);
        if (time <= first.time()) {
            out.set(first.xRot(), first.yRot(), first.zRot(),
                    first.xPos(), first.yPos(), first.zPos());
            return;
        }

        Keyframe last = frames.get(frames.size() - 1);
        if (time >= last.time()) {
            out.set(last.xRot(), last.yRot(), last.zRot(),
                    last.xPos(), last.yPos(), last.zPos());
            return;
        }

        for (int i = 0; i < frames.size() - 1; i++) {
            Keyframe a = frames.get(i);
            Keyframe b = frames.get(i + 1);
            if (time < a.time() || time > b.time()) continue;

            float span = b.time() - a.time();
            float t = span <= 1.0E-5F ? 0.0F : (time - a.time()) / span;
            // The outgoing frame owns the curve into the next one.
            t = a.ease(t);

            out.set(lerp(a.xRot(), b.xRot(), t), lerp(a.yRot(), b.yRot(), t), lerp(a.zRot(), b.zRot(), t),
                    lerp(a.xPos(), b.xPos(), t), lerp(a.yPos(), b.yPos(), t), lerp(a.zPos(), b.zPos(), t));
            return;
        }
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
