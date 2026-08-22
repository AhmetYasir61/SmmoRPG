package com.smmorpg.movement;

/**
 * Live traversal state. Deliberately without a stamina cost for now — the limits are
 * cooldowns and contact conditions only, so the movement can be tuned on feel first and
 * priced later.
 */
public class MovementState {
    /** Air jumps used since the last ground or wall contact. */
    public int airJumps = 0;
    /** Ticks until another dash is allowed. */
    public int dashCooldown = 0;
    /** Ticks until another wall kick is allowed off the *same* surface. */
    public int wallKickCooldown = 0;
    /** Normal of the last surface kicked, so you cannot ladder up one flat wall forever. */
    public int lastWallHash = 0;
    /** Ticks left of an active wall-run. */
    public int wallRunTicks = 0;
    /** Ticks left of dash i-frames and speed. */
    public int dashTicks = 0;

    public double dashX, dashY, dashZ;

    public void tick() {
        if (dashCooldown > 0) dashCooldown--;
        if (wallKickCooldown > 0) wallKickCooldown--;
        if (dashTicks > 0) dashTicks--;
        if (wallRunTicks > 0) wallRunTicks--;
    }

    public void onGround() {
        airJumps = 0;
        wallRunTicks = 0;
        lastWallHash = 0;
    }

    public boolean dashing() { return dashTicks > 0; }
}
