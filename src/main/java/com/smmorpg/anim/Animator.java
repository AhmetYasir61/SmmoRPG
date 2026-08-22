package com.smmorpg.anim;

/**
 * Plays clips for one entity and cross-fades between them.
 *
 * <p>Runs on both sides. The client uses it to pose the model; the server uses the very same
 * timeline to decide when a swing is live and where the blade is, which is what keeps what
 * you see and what actually hits from drifting apart.
 */
public class Animator {

    private static final int JOINTS = Joint.values().length;

    private AnimationClip current;
    private AnimationClip previous;

    private float time;
    private float previousTime;
    /** Ticks remaining in the cross-fade out of the previous clip. */
    private float blendLeft;
    private float blendLength;

    /** Set once per attack so one swing cannot register two hits. */
    private boolean damageConsumed;

    private final Pose[] pose = new Pose[JOINTS];
    private final Pose[] scratch = new Pose[JOINTS];

    public Animator() {
        for (int i = 0; i < JOINTS; i++) {
            pose[i] = new Pose();
            scratch[i] = new Pose();
        }
    }

    public AnimationClip clip() { return current; }
    public float time() { return time; }
    public boolean damageConsumed() { return damageConsumed; }
    public void consumeDamage() { damageConsumed = true; }

    /** Starts a clip, cross-fading out of whatever is playing. */
    public void play(AnimationClip clip, float blendTicks) {
        if (clip == null) return;
        if (current == clip && clip.loop()) return;   // already looping this one

        previous = current;
        previousTime = time;
        blendLength = Math.max(0.001F, blendTicks);
        blendLeft = current == null ? 0.0F : blendLength;

        current = clip;
        time = 0.0F;
        damageConsumed = false;
    }

    public void tick(float delta) {
        time += delta;
        if (blendLeft > 0.0F) {
            blendLeft = Math.max(0.0F, blendLeft - delta);
            previousTime += delta;
        }
        if (current != null && !current.loop() && time > current.durationTicks()) {
            time = current.durationTicks();
        }
    }

    public boolean finished() {
        return current == null || (!current.loop() && time >= current.durationTicks());
    }

    public boolean damaging() {
        return current != null && !damageConsumed && current.damaging(time);
    }

    public boolean cancellable() {
        return current == null || current.cancellable(time);
    }

    public boolean locksMovement() {
        return current != null && current.locksMovement(time);
    }

    /**
     * Samples the current pose, blended with the outgoing clip. {@code partialTick} lets
     * the render thread interpolate between ticks without touching the animator's own state.
     */
    public Pose[] samplePose(float partialTick) {
        for (Pose p : pose) p.zero();
        if (current == null) return pose;

        current.sample(time + partialTick, pose);

        if (blendLeft > 0.0F && previous != null) {
            for (Pose p : scratch) p.zero();
            previous.sample(previousTime + partialTick, scratch);
            // blendLeft counts down, so this weight starts at 1 (all previous) and falls to 0.
            float weight = blendLeft / blendLength;
            for (int i = 0; i < JOINTS; i++) {
                Pose blended = new Pose();
                blended.copyFrom(scratch[i]);
                blended.blend(pose[i], 1.0F - weight);
                pose[i].copyFrom(blended);
            }
        }
        return pose;
    }

    public void stop() {
        current = null;
        previous = null;
        time = 0.0F;
        blendLeft = 0.0F;
    }
}
