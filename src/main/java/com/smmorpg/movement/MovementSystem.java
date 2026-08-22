package com.smmorpg.movement;

import com.smmorpg.core.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Traversal: wall kicks, tree kicks, air jumps, dashes and wall runs.
 *
 * <p>All of it is contact-driven and resolved the instant it is asked for — there is no
 * scripted sequence to wait out. Push off a trunk and you leave the trunk on that tick,
 * in the direction you were actually facing.
 */
public final class MovementSystem {

    /** No cap for now: the ceiling is a tuning value, not a design decision. */
    public static int maxAirJumps = Integer.MAX_VALUE;

    private MovementSystem() {}

    public static MovementState state(LivingEntity e) {
        return e.getData(ModAttachments.MOVEMENT.get());
    }

    public static void tick(LivingEntity e) {
        MovementState s = state(e);
        s.tick();
        if (e.onGround()) s.onGround();

        if (s.dashing()) {
            // A dash overrides normal acceleration entirely while it lasts.
            e.setDeltaMovement(s.dashX, s.dashY, s.dashZ);
            e.hurtMarked = true;
            // Bleeding the dash off over its duration keeps the exit from snapping.
            double decay = 0.82D;
            s.dashX *= decay; s.dashY *= decay; s.dashZ *= decay;
        }

        if (s.wallRunTicks > 0 && !e.onGround()) {
            // While running a wall, gravity is mostly cancelled.
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, Math.max(v.y, -0.04D), v.z);
        }
    }

    /**
     * Attempts a jump off whatever the entity is touching. Works on any solid face,
     * including a tree trunk, and throws the entity away from the surface and forward.
     */
    public static boolean tryWallKick(LivingEntity e) {
        MovementState s = state(e);
        if (e.onGround() || s.wallKickCooldown > 0) return false;

        Contact contact = findContact(e);
        if (contact == null) return false;

        int hash = contact.pos.hashCode() * 31 + contact.face.ordinal();
        if (hash == s.lastWallHash && s.wallKickCooldown > 0) return false;

        Vec3 away = new Vec3(contact.face.getStepX(), 0.0D, contact.face.getStepZ()).normalize();
        Vec3 look = e.getLookAngle().normalize();
        // Away from the wall, plus wherever you were looking, plus a solid vertical kick.
        Vec3 push = away.scale(0.52D)
                .add(look.x * 0.35D, 0.0D, look.z * 0.35D)
                .add(0.0D, 0.62D + Math.max(0.0D, look.y) * 0.25D, 0.0D);

        e.setDeltaMovement(push);
        e.hurtMarked = true;
        e.fallDistance = 0.0F;

        s.wallKickCooldown = 6;
        s.lastWallHash = hash;
        s.airJumps = 0;   // a wall contact refreshes the air jumps, the way a ledge would
        return true;
    }

    /** A mid-air jump. Uncapped for now; each one is slightly weaker than the last. */
    public static boolean tryAirJump(LivingEntity e) {
        MovementState s = state(e);
        if (e.onGround()) return false;
        if (s.airJumps >= maxAirJumps) return false;

        s.airJumps++;
        double falloff = Math.max(0.45D, 1.0D - s.airJumps * 0.08D);
        Vec3 look = e.getLookAngle();
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x + look.x * 0.12D, 0.52D * falloff, v.z + look.z * 0.12D);
        e.hurtMarked = true;
        e.fallDistance = 0.0F;
        return true;
    }

    /**
     * A directional dash. {@code forward}/{@code strafe} are the entity's current input, so
     * a dash goes where the player is actually steering, including straight up in the air.
     */
    public static boolean tryDash(LivingEntity e, float forward, float strafe, boolean airborne) {
        MovementState s = state(e);
        if (s.dashCooldown > 0) return false;

        Vec3 look = e.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

        Vec3 dir;
        if (Math.abs(forward) < 0.05F && Math.abs(strafe) < 0.05F) {
            dir = look;                                  // no input: dash where you look
        } else {
            dir = look.scale(forward).add(right.scale(-strafe)).normalize();
        }
        if (airborne) dir = dir.add(0.0D, 0.18D, 0.0D).normalize();

        double power = 1.35D;
        s.dashX = dir.x * power;
        s.dashY = dir.y * power * 0.6D;
        s.dashZ = dir.z * power;
        s.dashTicks = 6;
        s.dashCooldown = 14;
        e.fallDistance = 0.0F;
        return true;
    }

    /** Starts or extends a wall run while moving along a surface. */
    public static boolean tryWallRun(LivingEntity e) {
        MovementState s = state(e);
        if (e.onGround()) return false;
        if (findContact(e) == null) return false;
        s.wallRunTicks = 10;
        e.fallDistance = 0.0F;
        return true;
    }

    private record Contact(BlockPos pos, Direction face) {}

    /** Finds a solid surface within a hair of the entity's sides. */
    private static Contact findContact(LivingEntity e) {
        Level level = e.level();
        double reach = e.getBbWidth() * 0.5D + 0.12D;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = BlockPos.containing(
                    e.getX() + dir.getStepX() * reach,
                    e.getY() + e.getBbHeight() * 0.6D,
                    e.getZ() + dir.getStepZ() * reach);
            BlockState state = level.getBlockState(pos);
            // Any solid face works — stone, planks, or the side of a tree.
            if (state.isFaceSturdy(level, pos, dir.getOpposite())
                    || state.isCollisionShapeFullBlock(level, pos)) {
                return new Contact(pos, dir.getOpposite());
            }
        }
        return null;
    }
}
