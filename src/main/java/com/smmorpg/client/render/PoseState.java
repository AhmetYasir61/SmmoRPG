package com.smmorpg.client.render;

import net.minecraft.util.Mth;

/**
 * One player's live animation state.
 *
 * <p>This is the bridge that makes the FPS view and everyone else's TPS view agree: the
 * local player writes it from their own input, sends it up, and every other client reads
 * the same fields to drive the third-person rig. Values are interpolated toward their
 * targets so a 20 Hz network update still renders smoothly at any framerate.
 */
public class PoseState {
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_SLASH_DOWN = 1;
    public static final int ANIM_SLASH_RISING = 2;
    public static final int ANIM_SLASH_HORIZONTAL = 3;
    public static final int ANIM_THRUST = 4;
    public static final int ANIM_PARRY = 5;
    public static final int ANIM_GUARD = 6;
    public static final int ANIM_STAGGER = 7;
    public static final int ANIM_DRAW_BOW = 8;
    public static final int ANIM_SHEATHE = 9;

    public int animation = ANIM_IDLE;
    public int frame = 0;

    public float aimPitch, aimPitchPrev;
    public float leanX, leanXPrev;
    public float leanZ, leanZPrev;
    public boolean aiming, blocking;

    private float targetAimPitch, targetLeanX, targetLeanZ;

    public void set(int animation, int frame, float aimPitch, float leanX, float leanZ,
                    boolean aiming, boolean blocking) {
        this.animation = animation;
        this.frame = frame;
        this.targetAimPitch = aimPitch;
        this.targetLeanX = leanX;
        this.targetLeanZ = leanZ;
        this.aiming = aiming;
        this.blocking = blocking;
    }

    public void tick() {
        aimPitchPrev = aimPitch;
        leanXPrev = leanX;
        leanZPrev = leanZ;
        aimPitch = Mth.lerp(0.45F, aimPitch, targetAimPitch);
        leanX = Mth.lerp(0.35F, leanX, targetLeanX);
        leanZ = Mth.lerp(0.35F, leanZ, targetLeanZ);
        if (frame > 0) frame++;
    }

    public float aimPitch(float partial) { return Mth.lerp(partial, aimPitchPrev, aimPitch); }
    public float leanX(float partial) { return Mth.lerp(partial, leanXPrev, leanX); }
    public float leanZ(float partial) { return Mth.lerp(partial, leanZPrev, leanZ); }

    public boolean swinging() {
        return animation >= ANIM_SLASH_DOWN && animation <= ANIM_THRUST;
    }

    /** 0..1 through the current animation, used to drive limb angles. */
    public float progress(float partial) {
        int length = switch (animation) {
            case ANIM_SLASH_DOWN, ANIM_SLASH_HORIZONTAL -> 12;
            case ANIM_SLASH_RISING -> 14;
            case ANIM_THRUST -> 9;
            case ANIM_PARRY -> 8;
            case ANIM_STAGGER -> 40;
            case ANIM_DRAW_BOW -> 20;
            default -> 20;
        };
        return Mth.clamp((frame + partial) / length, 0.0F, 1.0F);
    }
}
