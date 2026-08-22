package com.smmorpg.combat;

/**
 * Where on a body a blow landed. Resolved geometrically from the attack ray against
 * per-part boxes, not guessed from the damage number.
 */
public enum HitLocation {
    HEAD("head", 2.10F, 1.00F, true),
    NECK("neck", 2.35F, 1.15F, true),
    TORSO("torso", 1.00F, 0.55F, false),
    LEFT_ARM("left_arm", 0.85F, 0.75F, true),
    RIGHT_ARM("right_arm", 0.85F, 0.75F, true),
    LEFT_LEG("left_leg", 0.80F, 0.65F, true),
    RIGHT_LEG("right_leg", 0.80F, 0.65F, true),
    /** Blocked on a shield, parried blade, or armour plate: the blow never reached flesh. */
    GUARD("guard", 0.15F, 0.00F, false);

    private final String key;
    private final float damageMultiplier;
    private final float bleedFactor;
    private final boolean severable;

    HitLocation(String key, float damageMultiplier, float bleedFactor, boolean severable) {
        this.key = key;
        this.damageMultiplier = damageMultiplier;
        this.bleedFactor = bleedFactor;
        this.severable = severable;
    }

    public String key() { return key; }
    public float damageMultiplier() { return damageMultiplier; }
    public float bleedFactor() { return bleedFactor; }

    /** NECK and HEAD sever into a decapitation; limbs sever off. The torso never does. */
    public boolean severable() { return severable; }

    public boolean isLimb() {
        return this == LEFT_ARM || this == RIGHT_ARM || this == LEFT_LEG || this == RIGHT_LEG;
    }

    public static HitLocation byKey(String k) {
        for (HitLocation l : values()) if (l.key.equals(k)) return l;
        return TORSO;
    }
}
