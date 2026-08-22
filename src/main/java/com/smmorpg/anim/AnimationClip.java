package com.smmorpg.anim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * One complete animation: a set of joint tracks plus the combat timing that belongs to it.
 *
 * <p>The timing lives here rather than in the combat code because the two have to agree.
 * A blade only hurts while it is actually travelling, and the window that says when that is
 * has to move whenever the animation does — including when an operator retimes the swing
 * from the studio panel.
 */
public record AnimationClip(String id,
                            float durationTicks,
                            boolean loop,
                            List<JointTrack> tracks,
                            float damageStart,
                            float damageEnd,
                            float cancelAfter,
                            float moveLock,
                            float cameraKick) {

    public static final Codec<AnimationClip> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(AnimationClip::id),
            Codec.FLOAT.fieldOf("duration").forGetter(AnimationClip::durationTicks),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(AnimationClip::loop),
            JointTrack.CODEC.listOf().fieldOf("tracks").forGetter(AnimationClip::tracks),
            Codec.FLOAT.optionalFieldOf("damage_start", -1.0F).forGetter(AnimationClip::damageStart),
            Codec.FLOAT.optionalFieldOf("damage_end", -1.0F).forGetter(AnimationClip::damageEnd),
            Codec.FLOAT.optionalFieldOf("cancel_after", 1.0F).forGetter(AnimationClip::cancelAfter),
            Codec.FLOAT.optionalFieldOf("move_lock", 0.0F).forGetter(AnimationClip::moveLock),
            Codec.FLOAT.optionalFieldOf("camera_kick", 0.0F).forGetter(AnimationClip::cameraKick)
    ).apply(i, AnimationClip::new));

    /** True while the weapon is live and can connect. */
    public boolean damaging(float time) {
        return damageStart >= 0.0F && time >= damageStart && time <= damageEnd;
    }

    /** True once the next attack in a combo may be buffered in. */
    public boolean cancellable(float time) {
        return time >= cancelAfter * durationTicks;
    }

    /** True while the animation pins the entity in place. */
    public boolean locksMovement(float time) {
        return time <= moveLock;
    }

    public boolean isAttack() { return damageStart >= 0.0F; }

    /** Samples every track at {@code time} into the given pose array, indexed by joint. */
    public void sample(float time, Pose[] out) {
        float t = loop && durationTicks > 0 ? time % durationTicks : time;
        for (JointTrack track : tracks) {
            track.sample(t, out[track.bone().ordinal()]);
        }
    }

    /** Rebuilds this clip with new timing, keeping the joint tracks. */
    public AnimationClip retimed(float duration, float dmgStart, float dmgEnd) {
        return new AnimationClip(id, duration, loop, tracks, dmgStart, dmgEnd,
                cancelAfter, moveLock, cameraKick);
    }
}
