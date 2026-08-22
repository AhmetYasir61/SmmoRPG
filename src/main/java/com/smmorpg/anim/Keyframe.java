package com.smmorpg.anim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One pose of one joint at one instant, in radians.
 *
 * <p>Euler angles rather than quaternions on purpose: the vanilla rig is driven by
 * {@code xRot}/{@code yRot}/{@code zRot} on each {@link net.minecraft.client.model.geom.ModelPart},
 * so storing quaternions would mean converting on every frame for no gain in expressiveness
 * at the angles a humanoid limb actually reaches.
 */
public record Keyframe(float time,
                       float xRot, float yRot, float zRot,
                       float xPos, float yPos, float zPos,
                       int easing) {

    public static final int EASE_LINEAR = 0;
    public static final int EASE_IN_OUT = 1;
    /** Fast out of the pose, slow into the next — how a strike leaves the shoulder. */
    public static final int EASE_OUT = 2;
    /** Slow then sudden — a wind-up. */
    public static final int EASE_IN = 3;

    public static final Codec<Keyframe> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("t").forGetter(Keyframe::time),
            Codec.FLOAT.optionalFieldOf("rx", 0.0F).forGetter(Keyframe::xRot),
            Codec.FLOAT.optionalFieldOf("ry", 0.0F).forGetter(Keyframe::yRot),
            Codec.FLOAT.optionalFieldOf("rz", 0.0F).forGetter(Keyframe::zRot),
            Codec.FLOAT.optionalFieldOf("px", 0.0F).forGetter(Keyframe::xPos),
            Codec.FLOAT.optionalFieldOf("py", 0.0F).forGetter(Keyframe::yPos),
            Codec.FLOAT.optionalFieldOf("pz", 0.0F).forGetter(Keyframe::zPos),
            Codec.INT.optionalFieldOf("ease", EASE_IN_OUT).forGetter(Keyframe::easing)
    ).apply(i, Keyframe::new));

    public static Keyframe rot(float time, float x, float y, float z) {
        return new Keyframe(time, x, y, z, 0, 0, 0, EASE_IN_OUT);
    }

    public static Keyframe rot(float time, float x, float y, float z, int easing) {
        return new Keyframe(time, x, y, z, 0, 0, 0, easing);
    }

    /** Applies this frame's easing curve to a normalised 0..1 progress toward the next. */
    public float ease(float t) {
        return switch (easing) {
            case EASE_LINEAR -> t;
            case EASE_OUT -> 1.0F - (1.0F - t) * (1.0F - t);
            case EASE_IN -> t * t;
            default -> t * t * (3.0F - 2.0F * t);
        };
    }
}
