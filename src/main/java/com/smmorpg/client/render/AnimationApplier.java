package com.smmorpg.client.render;

import com.smmorpg.anim.AnimationHooks;
import com.smmorpg.anim.AnimationState;
import com.smmorpg.anim.Joint;
import com.smmorpg.anim.Pose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Writes an animated pose onto a humanoid model.
 *
 * <p>Additive over what vanilla already did, not a replacement: walking, head tracking and
 * riding still set the base pose, and the animation is layered on top of it. That is what
 * lets a player swing mid-stride and have both read at once, instead of the swing snapping
 * the legs to a standstill.
 */
public final class AnimationApplier {

    private AnimationApplier() {}

    /**
     * @param weight 0 leaves the model alone, 1 is the full animated pose. Used to fade the
     *               animation out over the last frames rather than popping back to vanilla.
     */
    public static void apply(LivingEntity entity, HumanoidModel<?> model, float partialTick,
                             float weight) {
        if (weight <= 0.001F) return;

        AnimationState state = AnimationHooks.of(entity);
        if (state.animator.clip() == null) return;

        Pose[] pose = state.animator.samplePose(partialTick);

        for (Joint joint : Joint.values()) {
            ModelPart part = joint.partOf(model);
            if (part == null) continue;

            Pose p = pose[joint.ordinal()];
            // A joint with no track samples to zero, and adding zero is the correct no-op —
            // which is why a clip only has to author the limbs it actually moves.
            part.xRot += p.xRot * weight;
            part.yRot += p.yRot * weight;
            part.zRot += p.zRot * weight;
            part.x += p.xPos * weight;
            part.y += p.yPos * weight;
            part.z += p.zPos * weight;
        }
    }

    /** Full weight while a clip is playing, easing off over its final three ticks. */
    public static float weightFor(LivingEntity entity, float partialTick) {
        AnimationState state = AnimationHooks.of(entity);
        var clip = state.animator.clip();
        if (clip == null) return 0.0F;
        if (clip.loop()) return 1.0F;

        float remaining = clip.durationTicks() - (state.animator.time() + partialTick);
        return Math.max(0.0F, Math.min(1.0F, remaining / 3.0F));
    }
}
