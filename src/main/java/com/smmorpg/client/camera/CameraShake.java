package com.smmorpg.client.camera;

import com.smmorpg.config.CombatConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A decaying, band-limited shake. Two oscillators at different frequencies plus a noise
 * term, so the camera reads as a body absorbing a blow rather than as a jitter filter.
 */
public final class CameraShake {
    private static final RandomSource RNG = RandomSource.create();

    private static float trauma;          // 0..1, decays every frame
    private static float traumaDecay = 1.6F;
    private static float seed;

    // Recoil is separate: it is a directed kick with a spring return, not a wobble.
    private static float recoilPitch;
    private static float recoilYaw;
    private static float recoilPitchVel;
    private static float recoilYawVel;

    private static int hitStopTicks;

    private CameraShake() {}

    /** Adds trauma. Repeated hits stack, but saturate rather than doubling. */
    public static void addTrauma(float amount) {
        trauma = Math.min(1.0F, trauma + amount * 0.45F);
        traumaDecay = 1.4F + amount * 0.3F;
        seed = RNG.nextFloat() * 100.0F;
    }

    /** Directed kick. Positive pitch is upward, the way a swing or a bowstring throws the view. */
    public static void addRecoil(float strength) {
        recoilPitchVel -= strength * 3.2F;
        recoilYawVel += (RNG.nextFloat() - 0.5F) * strength * 2.4F;
    }

    public static void setHitStop(int ticks) { hitStopTicks = Math.max(hitStopTicks, ticks); }

    public static boolean inHitStop() { return hitStopTicks > 0; }

    public static void tick() { if (hitStopTicks > 0) hitStopTicks--; }

    /** Called every frame with the partial-tick delta in seconds. */
    public static void update(float dt) {
        if (trauma > 0.0F) {
            trauma = Math.max(0.0F, trauma - traumaDecay * dt);
        }
        // Critically damped spring pulls the recoil back to centre.
        float k = 90.0F, c = 14.0F;
        recoilPitchVel += (-k * recoilPitch - c * recoilPitchVel) * dt;
        recoilYawVel += (-k * recoilYaw - c * recoilYawVel) * dt;
        recoilPitch += recoilPitchVel * dt;
        recoilYaw += recoilYawVel * dt;
    }

    private static float shakeAmount() {
        // Squaring the trauma makes small hits subtle and heavy hits violent.
        return trauma * trauma * CombatConfig.CFG.cameraShakeScale.get().floatValue();
    }

    public static float pitchOffset(float time) {
        return osc(time, 13.7F, 0.0F) * shakeAmount() * 4.5F + recoilPitch;
    }

    public static float yawOffset(float time) {
        return osc(time, 11.3F, 31.0F) * shakeAmount() * 3.8F + recoilYaw;
    }

    public static float rollOffset(float time) {
        return osc(time, 9.1F, 67.0F) * shakeAmount() * 6.0F;
    }

    /** Small positional kick, so heavy blows push the head as well as turn it. */
    public static float positionalOffset(float time) {
        return osc(time, 17.4F, 13.0F) * shakeAmount() * 0.06F;
    }

    private static float osc(float time, float freq, float phase) {
        float t = time * freq + seed + phase;
        // Two detuned sines beat against each other; the result never loops audibly.
        return Mth.sin(t) * 0.62F + Mth.sin(t * 1.618F) * 0.38F;
    }

    public static void reset() {
        trauma = 0.0F;
        recoilPitch = recoilYaw = recoilPitchVel = recoilYawVel = 0.0F;
        hitStopTicks = 0;
    }
}
