package com.smmorpg.anim;

/** A mutable joint transform. Reused per sample so animating costs no allocation. */
public class Pose {
    public float xRot, yRot, zRot;
    public float xPos, yPos, zPos;

    public void set(float xr, float yr, float zr, float xp, float yp, float zp) {
        xRot = xr; yRot = yr; zRot = zr;
        xPos = xp; yPos = yp; zPos = zp;
    }

    public void zero() { set(0, 0, 0, 0, 0, 0); }

    /** Blends toward {@code other} by {@code t}; used for cross-fading between clips. */
    public void blend(Pose other, float t) {
        xRot += (other.xRot - xRot) * t;
        yRot += (other.yRot - yRot) * t;
        zRot += (other.zRot - zRot) * t;
        xPos += (other.xPos - xPos) * t;
        yPos += (other.yPos - yPos) * t;
        zPos += (other.zPos - zPos) * t;
    }

    public void copyFrom(Pose other) {
        set(other.xRot, other.yRot, other.zRot, other.xPos, other.yPos, other.zPos);
    }
}
