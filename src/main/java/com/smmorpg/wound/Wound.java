package com.smmorpg.wound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smmorpg.combat.HitLocation;

/**
 * One mark left on a body by one blow.
 *
 * <p>{@code u}/{@code v} are normalised coordinates inside the struck body part, so the
 * decal is drawn exactly where the blade actually crossed it — not on a generic "hurt"
 * overlay. {@code angle} is the swing direction, which is why a downward cut and a
 * horizontal cut look different on the same shoulder.
 */
public record Wound(String location,
                    int type,
                    float u, float v,
                    float angle,
                    float length,
                    float depth,
                    int inflictedAt,
                    float closure) {

    public static final int TYPE_SLASH = 0;
    public static final int TYPE_PIERCE = 1;
    public static final int TYPE_BLUNT = 2;
    public static final int TYPE_SEVER = 3;
    public static final int TYPE_BURN = 4;

    public static final Codec<Wound> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("loc").forGetter(Wound::location),
            Codec.INT.fieldOf("type").forGetter(Wound::type),
            Codec.FLOAT.fieldOf("u").forGetter(Wound::u),
            Codec.FLOAT.fieldOf("v").forGetter(Wound::v),
            Codec.FLOAT.fieldOf("ang").forGetter(Wound::angle),
            Codec.FLOAT.fieldOf("len").forGetter(Wound::length),
            Codec.FLOAT.fieldOf("depth").forGetter(Wound::depth),
            Codec.INT.fieldOf("at").forGetter(Wound::inflictedAt),
            Codec.FLOAT.fieldOf("closure").forGetter(Wound::closure)
    ).apply(i, Wound::new));

    public HitLocation hitLocation() { return HitLocation.byKey(location); }

    /** 0 = freshly opened, 1 = fully closed and about to disappear. */
    public Wound heal(float amount) {
        return new Wound(location, type, u, v, angle, length, depth, inflictedAt,
                Math.min(1.0F, closure + amount));
    }

    public boolean closed() { return closure >= 1.0F; }

    /** A severed part never closes, and a deep cut bleeds far longer than a graze. */
    public float bleedRate() {
        if (type == TYPE_SEVER) return depth * 2.4F;
        if (type == TYPE_BLUNT) return 0.0F;
        return depth * (1.0F - closure) * (type == TYPE_PIERCE ? 0.7F : 1.0F);
    }

    /** How visible the decal still is; a closing wound fades into a scar rather than popping out. */
    public float visibility() { return Math.max(0.0F, 1.0F - closure * 0.85F); }
}
