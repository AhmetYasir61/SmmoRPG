package com.smmorpg.anim;

import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Where the weapon is, derived from the animation rather than from the look vector.
 *
 * <p>This is what ties the cut to the swing. The arm's animated rotation is rebuilt as a
 * transform in the entity's local space, the weapon is hung off the end of it, and the
 * result is fed to the blade trace. Retime or reshape an animation and the hitbox follows
 * it exactly, with nothing to keep in sync by hand.
 */
public final class AnimatedCollider {

    /** Where the shoulder sits relative to the entity's feet, in blocks. */
    private static final float SHOULDER_HEIGHT = 1.35F;
    private static final float SHOULDER_OFFSET = 0.32F;

    private AnimatedCollider() {}

    public record Blade(Vec3 base, Vec3 tip) {}

    /** The weapon's base and tip in world space, for the pose the animator is holding. */
    public static Blade resolve(LivingEntity entity, float partialTick) {
        AnimationState state = AnimationHooks.of(entity);
        Pose[] pose = state.animator.samplePose(partialTick);

        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        Pose arm = pose[(rightHanded ? Joint.RIGHT_ARM : Joint.LEFT_ARM).ordinal()];
        Pose body = pose[Joint.BODY.ordinal()];

        WeaponClass weapon = RpgWeaponItem.classOf(entity.getMainHandItem());
        float length = weapon == null ? 1.0F : weapon.reach() * 0.42F;

        // Local space: +Z forward, +Y up, +X to the entity's right.
        Matrix4f transform = new Matrix4f();
        transform.rotateY(-body.yRot);
        transform.rotateX(body.xRot);
        transform.translate(rightHanded ? SHOULDER_OFFSET : -SHOULDER_OFFSET, SHOULDER_HEIGHT, 0.0F);
        // The vanilla rig applies Z, then Y, then X; matching that order is what makes the
        // collider agree with what the renderer draws.
        transform.rotateZ(arm.zRot);
        transform.rotateY(-arm.yRot);
        transform.rotateX(arm.xRot);

        // The arm hangs down its own -Y, and the weapon continues past the hand.
        Vector4f hand = transform.transform(new Vector4f(0.0F, -0.45F, 0.0F, 1.0F));
        Vector4f tip = transform.transform(new Vector4f(0.0F, -0.45F - length, 0.0F, 1.0F));

        float yaw = -entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 origin = new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));

        return new Blade(toWorld(origin, hand, yaw), toWorld(origin, tip, yaw));
    }

    private static Vec3 toWorld(Vec3 origin, Vector4f local, float yaw) {
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        // Rotate the local offset around the entity's own vertical axis, then translate.
        double x = local.x * cos - local.z * sin;
        double z = local.x * sin + local.z * cos;
        return origin.add(x, local.y, z);
    }
}
