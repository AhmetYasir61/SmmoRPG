package com.smmorpg.item;

/** Weapon archetypes. Reach, arc and swing weight are what make them feel different. */
public enum WeaponClass {
    KATANA("katana", 3.2F, 7.0F, 0.85F, 1.15F, 55.0F, 0.62F),
    ODACHI("odachi", 3.9F, 10.5F, 0.60F, 1.05F, 80.0F, 0.80F),
    DAO("dao", 3.1F, 7.5F, 0.90F, 1.10F, 50.0F, 0.60F),
    JIAN("jian", 3.0F, 6.5F, 1.05F, 1.00F, 42.0F, 0.50F),
    DAGGER("dagger", 2.1F, 4.0F, 1.75F, 0.85F, 22.0F, 0.35F),
    TANTO("tanto", 2.0F, 4.5F, 1.60F, 0.85F, 24.0F, 0.40F),
    SPEAR("spear", 4.6F, 8.0F, 0.75F, 0.90F, 60.0F, 0.30F),
    NAGINATA("naginata", 4.2F, 9.0F, 0.70F, 1.10F, 70.0F, 0.72F),
    KANABO("kanabo", 3.0F, 12.0F, 0.50F, 0.60F, 95.0F, 0.10F),
    BOW("bow", 24.0F, 9.0F, 1.00F, 0.70F, 30.0F, 0.20F);

    private final String key;
    private final float reach;
    private final float baseDamage;
    private final float swingSpeed;
    private final float slashFactor;
    private final float staminaCost;
    private final float bleedFactor;

    WeaponClass(String key, float reach, float baseDamage, float swingSpeed,
                float slashFactor, float staminaCost, float bleedFactor) {
        this.key = key; this.reach = reach; this.baseDamage = baseDamage;
        this.swingSpeed = swingSpeed; this.slashFactor = slashFactor;
        this.staminaCost = staminaCost; this.bleedFactor = bleedFactor;
    }

    public String key() { return key; }
    public float reach() { return reach; }
    public float baseDamage() { return baseDamage; }
    public float swingSpeed() { return swingSpeed; }
    /** How well the weapon cuts rather than crushes; drives sever chance. */
    public float slashFactor() { return slashFactor; }
    public float staminaCost() { return staminaCost; }
    public float bleedFactor() { return bleedFactor; }

    public int woundType() {
        return switch (this) {
            case SPEAR, BOW, TANTO -> com.smmorpg.wound.Wound.TYPE_PIERCE;
            case KANABO -> com.smmorpg.wound.Wound.TYPE_BLUNT;
            default -> com.smmorpg.wound.Wound.TYPE_SLASH;
        };
    }

    /** Kick applied to the camera when this weapon lands or is loosed. */
    public float recoil() { return staminaCost * 0.012F + baseDamage * 0.02F; }
}
