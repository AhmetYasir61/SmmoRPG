package com.smmorpg.anim;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.smmorpg.anim.Keyframe.EASE_IN;
import static com.smmorpg.anim.Keyframe.EASE_IN_OUT;
import static com.smmorpg.anim.Keyframe.EASE_OUT;
import static com.smmorpg.anim.Keyframe.rot;

/**
 * The built-in animation library.
 *
 * <p>Every clip is authored as real keyframes, in radians, on the vanilla humanoid rig.
 * The shape of an attack is always the same three beats — wind-up, strike, recovery — but
 * the timing is what separates them: a dagger's wind-up is two ticks and a kanabo's is
 * seven, and that difference is the entire reason the two weapons feel different to hold.
 *
 * <p>The damage window is authored alongside the frames rather than derived from them, so
 * a blade is only live while it is actually travelling through its arc.
 */
public final class Animations {

    private static final Map<String, AnimationClip> CLIPS = new LinkedHashMap<>();
    private static volatile int revision = 0;

    // Angles that come up repeatedly. Named so the tables below read as poses, not numbers.
    private static final float OVERHEAD = -2.85F;   // arm straight up, behind the head
    private static final float HIGH = -2.10F;
    private static final float FORWARD = -1.55F;    // arm pointing where you look
    private static final float LOW = -0.35F;
    private static final float REST = 0.0F;

    private Animations() {}

    // ------------------------------------------------------------------
    // idle and locomotion
    // ------------------------------------------------------------------

    static {
        // A weapon-ready idle: not vanilla's dangling arms, but not a rigid stance either.
        clip("idle", 60.0F, true, -1, -1, 0.0F, 0.0F, 0.0F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.55F, -0.30F, 0.25F),
                        rot(30, -0.48F, -0.30F, 0.22F),
                        rot(60, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(30, -0.24F, 0.22F, -0.16F),
                        rot(60, -0.30F, 0.22F, -0.18F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.04F, -0.18F, 0.0F),
                        rot(30, 0.02F, -0.18F, 0.0F),
                        rot(60, 0.04F, -0.18F, 0.0F)));

        // Sprint leans in and drives the arms; the camera shake is hung off this clip.
        clip("sprint", 20.0F, true, -1, -1, 0.0F, 0.0F, 0.14F,
                JointTrack.of(Joint.BODY,
                        rot(0, 0.28F, 0.0F, 0.0F),
                        rot(10, 0.32F, 0.0F, 0.0F),
                        rot(20, 0.28F, 0.0F, 0.0F)),
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.90F, -0.20F, 0.30F),
                        rot(10, -0.30F, -0.20F, 0.30F),
                        rot(20, -0.90F, -0.20F, 0.30F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.20F, -0.30F),
                        rot(10, -0.90F, 0.20F, -0.30F),
                        rot(20, -0.30F, 0.20F, -0.30F)));

        clip("guard", 20.0F, true, -1, -1, 0.0F, 0.0F, 0.0F,
                JointTrack.of(Joint.RIGHT_ARM, rot(0, -1.70F, -0.55F, 0.45F)),
                JointTrack.of(Joint.LEFT_ARM, rot(0, -1.50F, 0.50F, -0.30F)),
                JointTrack.of(Joint.BODY, rot(0, 0.10F, -0.40F, 0.0F)));
    }

    // ------------------------------------------------------------------
    // sword: a four-hit chain that alternates the plane of the cut
    // ------------------------------------------------------------------

    static {
        // 1: overhead downward cut.
        clip("sword_1", 13.0F, false, 4.0F, 7.0F, 0.62F, 5.0F, 0.55F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.55F, -0.30F, 0.25F, EASE_IN),
                        rot(4, OVERHEAD, -0.20F, 0.10F, EASE_OUT),   // wind-up peak
                        rot(7, LOW, 0.05F, -0.20F, EASE_OUT),        // through the target
                        rot(13, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, -0.18F, 0.0F),
                        rot(4, -0.12F, -0.40F, 0.0F),
                        rot(7, 0.22F, 0.15F, 0.0F),
                        rot(13, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(7, -0.60F, 0.45F, -0.35F),
                        rot(13, -0.30F, 0.22F, -0.18F)));

        // 2: horizontal cut back the other way, faster because the body is already turning.
        clip("sword_2", 11.0F, false, 3.0F, 6.0F, 0.60F, 4.0F, 0.50F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, LOW, 0.05F, -0.20F, EASE_IN),
                        rot(3, -1.45F, 1.30F, 0.30F, EASE_OUT),
                        rot(6, -1.40F, -1.20F, 0.10F, EASE_OUT),
                        rot(11, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.10F, 0.15F, 0.0F),
                        rot(3, 0.05F, 0.70F, 0.0F),
                        rot(6, 0.05F, -0.55F, 0.0F),
                        rot(11, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.HEAD,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(3, 0.0F, -0.35F, 0.0F),
                        rot(6, 0.0F, 0.30F, 0.0F),
                        rot(11, 0.0F, 0.0F, 0.0F)));

        // 3: rising diagonal from the hip.
        clip("sword_3", 14.0F, false, 4.0F, 8.0F, 0.64F, 6.0F, 0.60F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -1.40F, -1.20F, 0.10F, EASE_IN),
                        rot(4, 0.45F, -0.70F, 0.55F, EASE_OUT),      // dropped low, coiled
                        rot(8, HIGH, 0.35F, -0.45F, EASE_OUT),       // driven up through
                        rot(14, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.55F, 0.0F),
                        rot(4, 0.30F, -0.30F, 0.0F),
                        rot(8, -0.18F, 0.25F, 0.0F),
                        rot(14, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.RIGHT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(4, 0.35F, 0.0F, 0.0F),
                        rot(8, -0.20F, 0.0F, 0.0F),
                        rot(14, 0.0F, 0.0F, 0.0F)));

        // 4: the finisher. Slower, bigger, and it steps into the blow.
        clip("sword_4", 20.0F, false, 7.0F, 12.0F, 0.80F, 11.0F, 1.10F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, HIGH, 0.35F, -0.45F, EASE_IN),
                        rot(7, -3.05F, -0.45F, 0.40F, EASE_IN),      // wound all the way back
                        rot(12, 0.30F, 0.25F, -0.35F, EASE_OUT),     // full follow-through
                        rot(20, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(7, -2.60F, 0.30F, -0.30F),
                        rot(12, 0.20F, 0.40F, -0.40F),
                        rot(20, -0.30F, 0.22F, -0.18F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, 0.15F, 0.0F),
                        rot(7, -0.30F, -0.60F, 0.0F),
                        rot(12, 0.45F, 0.35F, 0.0F),
                        rot(20, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.LEFT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(7, -0.25F, 0.0F, 0.0F),
                        rot(12, -0.75F, 0.0F, 0.0F),                 // the step forward
                        rot(20, 0.0F, 0.0F, 0.0F)));
    }

    // ------------------------------------------------------------------
    // katana: tighter, faster, and it finishes in a drawn stance
    // ------------------------------------------------------------------

    static {
        clip("katana_1", 10.0F, false, 3.0F, 5.0F, 0.55F, 3.0F, 0.45F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.70F, -0.55F, 0.35F, EASE_IN),
                        rot(3, -2.55F, -0.35F, 0.20F, EASE_OUT),
                        rot(5, -0.20F, 0.30F, -0.30F, EASE_OUT),
                        rot(10, -0.70F, -0.55F, 0.35F)),
                JointTrack.of(Joint.LEFT_ARM,                        // two hands on the tsuka
                        rot(0, -0.60F, 0.40F, -0.30F),
                        rot(3, -2.30F, 0.30F, -0.25F),
                        rot(5, -0.30F, 0.35F, -0.35F),
                        rot(10, -0.60F, 0.40F, -0.30F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.30F, 0.0F),
                        rot(5, 0.20F, 0.10F, 0.0F),
                        rot(10, 0.05F, -0.30F, 0.0F)));

        clip("katana_2", 9.0F, false, 2.0F, 5.0F, 0.55F, 3.0F, 0.42F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.20F, 0.30F, -0.30F, EASE_IN),
                        rot(2, -1.30F, 1.40F, 0.35F, EASE_OUT),
                        rot(5, -1.35F, -1.35F, 0.05F, EASE_OUT),
                        rot(9, -0.70F, -0.55F, 0.35F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.35F, -0.35F),
                        rot(5, -1.20F, -0.90F, -0.20F),
                        rot(9, -0.60F, 0.40F, -0.30F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.10F, 0.10F, 0.0F),
                        rot(2, 0.05F, 0.75F, 0.0F),
                        rot(5, 0.05F, -0.65F, 0.0F),
                        rot(9, 0.05F, -0.30F, 0.0F)));

        // The iai finisher: almost no wind-up, then everything at once.
        clip("katana_3", 16.0F, false, 5.0F, 7.0F, 0.78F, 8.0F, 1.25F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -1.35F, -1.35F, 0.05F, EASE_IN),
                        rot(5, -0.95F, -1.75F, 0.60F, EASE_IN),      // coiled across the body
                        rot(7, -1.50F, 1.60F, -0.20F, EASE_OUT),     // the draw
                        rot(16, -0.70F, -0.55F, 0.35F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.65F, 0.0F),
                        rot(5, 0.0F, -0.95F, 0.0F),
                        rot(7, 0.10F, 0.85F, 0.0F),
                        rot(16, 0.05F, -0.30F, 0.0F)),
                JointTrack.of(Joint.HEAD,
                        rot(0, 0.0F, 0.30F, 0.0F),
                        rot(5, 0.0F, 0.50F, 0.0F),
                        rot(7, 0.0F, -0.40F, 0.0F),
                        rot(16, 0.0F, 0.0F, 0.0F)));
    }

    // ------------------------------------------------------------------
    // the rest of the weapon families
    // ------------------------------------------------------------------

    static {
        // Dagger: barely any wind-up, and it can chain almost immediately.
        clip("dagger_1", 7.0F, false, 2.0F, 3.5F, 0.45F, 2.0F, 0.25F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.80F, -0.40F, 0.30F, EASE_IN),
                        rot(2, -1.90F, -0.60F, 0.45F, EASE_OUT),
                        rot(3.5F, -1.60F, 0.55F, -0.30F, EASE_OUT),
                        rot(7, -0.80F, -0.40F, 0.30F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.25F, 0.0F),
                        rot(3.5F, 0.12F, 0.25F, 0.0F),
                        rot(7, 0.05F, -0.25F, 0.0F)));

        clip("dagger_2", 6.0F, false, 1.5F, 3.0F, 0.42F, 2.0F, 0.25F,
                JointTrack.of(Joint.LEFT_ARM,                        // the off hand answers
                        rot(0, -0.40F, 0.30F, -0.25F, EASE_IN),
                        rot(1.5F, -1.85F, 0.65F, -0.50F, EASE_OUT),
                        rot(3, -1.55F, -0.50F, 0.25F, EASE_OUT),
                        rot(6, -0.40F, 0.30F, -0.25F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, 0.25F, 0.0F),
                        rot(3, 0.12F, -0.28F, 0.0F),
                        rot(6, 0.05F, -0.25F, 0.0F)));

        // Spear: a straight thrust. The arm barely rotates; the body drives it.
        clip("spear_1", 11.0F, false, 3.0F, 5.0F, 0.58F, 4.0F, 0.40F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -1.20F, -0.45F, 0.20F, EASE_IN),
                        rot(3, -0.95F, -0.85F, 0.30F, EASE_IN),      // drawn back to the hip
                        rot(5, FORWARD, -0.10F, 0.05F, EASE_OUT),    // driven out
                        rot(11, -1.20F, -0.45F, 0.20F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -1.35F, 0.35F, -0.20F),
                        rot(5, FORWARD, 0.15F, -0.08F),
                        rot(11, -1.35F, 0.35F, -0.20F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.45F, 0.0F),
                        rot(3, 0.05F, -0.70F, 0.0F),
                        rot(5, 0.15F, -0.10F, 0.0F),
                        rot(11, 0.05F, -0.45F, 0.0F)));

        clip("spear_2", 13.0F, false, 4.0F, 7.0F, 0.66F, 6.0F, 0.55F,
                JointTrack.of(Joint.RIGHT_ARM,                       // sweeping butt-end strike
                        rot(0, FORWARD, -0.10F, 0.05F, EASE_IN),
                        rot(4, -1.10F, 1.30F, 0.40F, EASE_OUT),
                        rot(7, -1.25F, -1.25F, -0.20F, EASE_OUT),
                        rot(13, -1.20F, -0.45F, 0.20F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.10F, -0.10F, 0.0F),
                        rot(4, 0.05F, 0.65F, 0.0F),
                        rot(7, 0.05F, -0.70F, 0.0F),
                        rot(13, 0.05F, -0.45F, 0.0F)));

        // Heavy: everything is slow, and the recovery is the price of the damage.
        clip("heavy_1", 26.0F, false, 9.0F, 15.0F, 0.85F, 16.0F, 1.60F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.45F, -0.40F, 0.35F, EASE_IN),
                        rot(9, -3.10F, -0.55F, 0.50F, EASE_IN),
                        rot(15, 0.55F, 0.30F, -0.45F, EASE_OUT),
                        rot(26, -0.45F, -0.40F, 0.35F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.40F, 0.35F, -0.30F),
                        rot(9, -2.85F, 0.40F, -0.45F),
                        rot(15, 0.45F, 0.35F, -0.40F),
                        rot(26, -0.40F, 0.35F, -0.30F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.20F, 0.0F),
                        rot(9, -0.42F, -0.55F, 0.0F),
                        rot(15, 0.60F, 0.30F, 0.0F),                 // folded over the swing
                        rot(26, 0.05F, -0.20F, 0.0F)),
                JointTrack.of(Joint.LEFT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(15, -0.85F, 0.0F, 0.0F),
                        rot(26, 0.0F, 0.0F, 0.0F)));

        clip("heavy_2", 30.0F, false, 11.0F, 18.0F, 0.88F, 20.0F, 1.90F,
                JointTrack.of(Joint.RIGHT_ARM,                       // full horizontal sweep
                        rot(0, 0.55F, 0.30F, -0.45F, EASE_IN),
                        rot(11, -1.30F, 1.75F, 0.55F, EASE_IN),
                        rot(18, -1.35F, -1.80F, -0.30F, EASE_OUT),
                        rot(30, -0.45F, -0.40F, 0.35F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.20F, 0.30F, 0.0F),
                        rot(11, 0.0F, 1.05F, 0.0F),
                        rot(18, 0.05F, -1.00F, 0.0F),
                        rot(30, 0.05F, -0.20F, 0.0F)));
    }

    // ------------------------------------------------------------------
    // reactions and traversal
    // ------------------------------------------------------------------

    static {
        clip("parry", 9.0F, false, -1, -1, 0.30F, 3.0F, 0.85F,
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.70F, -0.55F, 0.35F, EASE_OUT),
                        rot(2, -2.25F, -0.85F, 0.70F, EASE_IN),      // blade snapped across
                        rot(9, -0.70F, -0.55F, 0.35F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.60F, 0.40F, -0.30F),
                        rot(2, -1.95F, 0.70F, -0.55F),
                        rot(9, -0.60F, 0.40F, -0.30F)),
                JointTrack.of(Joint.BODY,
                        rot(0, 0.05F, -0.30F, 0.0F),
                        rot(2, -0.10F, -0.55F, 0.0F),
                        rot(9, 0.05F, -0.30F, 0.0F)));

        clip("stagger", 34.0F, false, -1, -1, 0.90F, 26.0F, 1.20F,
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, 0.0F, 0.0F, EASE_OUT),
                        rot(4, 0.50F, 0.20F, 0.25F),                 // knocked off balance
                        rot(16, 0.25F, -0.15F, -0.12F),
                        rot(34, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.55F, -0.30F, 0.25F),
                        rot(4, 0.70F, -0.60F, 0.60F),
                        rot(34, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(4, 0.75F, 0.65F, -0.65F),
                        rot(34, -0.30F, 0.22F, -0.18F)),
                JointTrack.of(Joint.HEAD,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(4, 0.45F, 0.25F, 0.0F),
                        rot(34, 0.0F, 0.0F, 0.0F)));

        clip("dodge", 12.0F, false, -1, -1, 0.55F, 0.0F, 0.35F,
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, 0.0F, 0.0F, EASE_OUT),
                        rot(3, 0.70F, 0.0F, 0.0F),                   // tucked into the roll
                        rot(7, 0.30F, 0.0F, 0.0F),
                        rot(12, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -0.55F, -0.30F, 0.25F),
                        rot(3, -1.30F, -0.50F, 0.55F),
                        rot(12, -0.55F, -0.30F, 0.25F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(3, -1.20F, 0.50F, -0.55F),
                        rot(12, -0.30F, 0.22F, -0.18F)),
                JointTrack.of(Joint.RIGHT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(3, -1.10F, 0.0F, 0.0F),
                        rot(12, 0.0F, 0.0F, 0.0F)),
                JointTrack.of(Joint.LEFT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(3, 0.60F, 0.0F, 0.0F),
                        rot(12, 0.0F, 0.0F, 0.0F)));

        clip("wall_kick", 12.0F, false, -1, -1, 0.50F, 0.0F, 0.75F,
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, 0.0F, 0.0F, EASE_OUT),
                        rot(3, -0.35F, 0.45F, 0.30F),
                        rot(12, 0.0F, -0.18F, 0.0F)),
                JointTrack.of(Joint.RIGHT_LEG,
                        rot(0, 0.0F, 0.0F, 0.0F),
                        rot(3, -1.40F, 0.0F, 0.35F),                 // the leg that pushed off
                        rot(12, 0.0F, 0.0F, 0.0F)),
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -0.30F, 0.22F, -0.18F),
                        rot(3, -2.20F, 0.60F, -0.70F),
                        rot(12, -0.30F, 0.22F, -0.18F)));

        clip("draw_bow", 22.0F, true, -1, -1, 0.0F, 0.0F, 0.10F,
                JointTrack.of(Joint.LEFT_ARM,
                        rot(0, -1.35F, 0.30F, -0.10F),
                        rot(22, -1.50F, 0.32F, -0.08F)),
                JointTrack.of(Joint.RIGHT_ARM,
                        rot(0, -1.20F, -0.35F, 0.20F),
                        rot(22, -1.45F, -0.75F, 0.25F)),             // drawn to the cheek
                JointTrack.of(Joint.BODY,
                        rot(0, 0.0F, -0.55F, 0.0F),
                        rot(22, 0.0F, -0.62F, 0.0F)));
    }

    // ------------------------------------------------------------------
    // registry
    // ------------------------------------------------------------------

    private static void clip(String id, float duration, boolean loop,
                             float damageStart, float damageEnd,
                             float cancelAfter, float moveLock, float cameraKick,
                             JointTrack... tracks) {
        CLIPS.put(id, new AnimationClip(id, duration, loop, List.of(tracks),
                damageStart, damageEnd, cancelAfter, moveLock, cameraKick));
    }

    public static AnimationClip get(String id) { return CLIPS.get(id); }

    /** Replaces a clip; used by the studio panel and by the server's animation sync. */
    public static synchronized void put(AnimationClip clip) {
        CLIPS.put(clip.id(), clip);
        revision++;
    }

    public static Map<String, AnimationClip> all() {
        return java.util.Collections.unmodifiableMap(CLIPS);
    }

    public static int revision() { return revision; }
}
