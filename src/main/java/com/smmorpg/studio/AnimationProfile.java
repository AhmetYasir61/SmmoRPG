package com.smmorpg.studio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * The tunable shape of one animation, as data.
 *
 * <p>Everything the fighting system does visually reads from a profile rather than from
 * constants in the renderer. That is what lets the studio panel retune a swing, a lean or
 * a dash while the game is running, and push the result to every client.
 */
public record AnimationProfile(String id,
                               int durationTicks,
                               float strikeAtFraction,
                               float armSwingDegrees,
                               float bodyTwistDegrees,
                               float leanDegrees,
                               float recoveryEase,
                               float cameraKick,
                               Map<String, Float> extra) {

    public static final Codec<AnimationProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(AnimationProfile::id),
            Codec.INT.fieldOf("duration").forGetter(AnimationProfile::durationTicks),
            Codec.FLOAT.fieldOf("strike_at").forGetter(AnimationProfile::strikeAtFraction),
            Codec.FLOAT.fieldOf("arm_swing").forGetter(AnimationProfile::armSwingDegrees),
            Codec.FLOAT.fieldOf("body_twist").forGetter(AnimationProfile::bodyTwistDegrees),
            Codec.FLOAT.fieldOf("lean").forGetter(AnimationProfile::leanDegrees),
            Codec.FLOAT.fieldOf("recovery_ease").forGetter(AnimationProfile::recoveryEase),
            Codec.FLOAT.fieldOf("camera_kick").forGetter(AnimationProfile::cameraKick),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("extra", Map.of())
                    .forGetter(AnimationProfile::extra)
    ).apply(i, AnimationProfile::new));

    public AnimationProfile with(String field, float value) {
        return switch (field) {
            case "duration" -> new AnimationProfile(id, Math.max(1, Math.round(value)), strikeAtFraction,
                    armSwingDegrees, bodyTwistDegrees, leanDegrees, recoveryEase, cameraKick, extra);
            case "strike_at" -> new AnimationProfile(id, durationTicks, clamp01(value),
                    armSwingDegrees, bodyTwistDegrees, leanDegrees, recoveryEase, cameraKick, extra);
            case "arm_swing" -> new AnimationProfile(id, durationTicks, strikeAtFraction,
                    value, bodyTwistDegrees, leanDegrees, recoveryEase, cameraKick, extra);
            case "body_twist" -> new AnimationProfile(id, durationTicks, strikeAtFraction,
                    armSwingDegrees, value, leanDegrees, recoveryEase, cameraKick, extra);
            case "lean" -> new AnimationProfile(id, durationTicks, strikeAtFraction,
                    armSwingDegrees, bodyTwistDegrees, value, recoveryEase, cameraKick, extra);
            case "recovery_ease" -> new AnimationProfile(id, durationTicks, strikeAtFraction,
                    armSwingDegrees, bodyTwistDegrees, leanDegrees, value, cameraKick, extra);
            case "camera_kick" -> new AnimationProfile(id, durationTicks, strikeAtFraction,
                    armSwingDegrees, bodyTwistDegrees, leanDegrees, recoveryEase, value, extra);
            default -> this;
        };
    }

    private static float clamp01(float f) { return f < 0 ? 0 : f > 1 ? 1 : f; }

    /** The eased 0..1 position through the animation at a given frame. */
    public float phase(int frame, float partialTick) {
        float t = clamp01((frame + partialTick) / durationTicks);
        if (t <= strikeAtFraction) return t / Math.max(1.0E-3F, strikeAtFraction);
        float back = (t - strikeAtFraction) / Math.max(1.0E-3F, 1.0F - strikeAtFraction);
        return 1.0F - (float) Math.pow(back, Math.max(0.1F, recoveryEase));
    }
}
