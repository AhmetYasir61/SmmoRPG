package com.smmorpg.classes;

import net.minecraft.util.StringRepresentable;

/**
 * The eight starting paths, chosen once on first join. Sekiro-flavoured: every path is a
 * different answer to the same question — what do you do the instant a blade comes at you.
 */
public enum PlayerClass implements StringRepresentable {
    /** Deflect-centric. Widest parry window, punishing riposte. */
    SHINOBI("shinobi", 20.0F, 1.00F, 1.35F, 0.95F, 1.20F, "posture_break"),
    /** Heavy odachi. Slowest swing, ignores a slice of armour. */
    RONIN("ronin", 26.0F, 1.30F, 0.80F, 1.15F, 0.85F, "armor_shatter"),
    /** Twin daggers, bleed stacking. */
    ASSASSIN("assassin", 16.0F, 0.85F, 1.55F, 0.80F, 1.45F, "crimson_mark"),
    /** Spear reach, keeps distance, counters charges. */
    LANCER("lancer", 22.0F, 1.10F, 0.95F, 1.05F, 1.00F, "impale"),
    /** Holy path. Damage against undead and cursed things, self-mending wounds. */
    MONK("monk", 24.0F, 1.05F, 1.05F, 1.10F, 0.90F, "sanctify"),
    /** Cursed path. Feeds on the blood it spills, at a cost to its own. */
    ONMYOJI("onmyoji", 18.0F, 0.95F, 1.10F, 0.85F, 1.15F, "blood_pact"),
    /** Bow and crossbow specialist — the FPS path. Steadier aim, tighter recoil. */
    KYUDOKA("kyudoka", 18.0F, 1.00F, 1.15F, 0.85F, 1.30F, "still_breath"),
    /** Shield and short sword. Absorbs, never dodges. */
    BULWARK("bulwark", 30.0F, 0.90F, 0.75F, 1.40F, 0.70F, "immovable");

    private final String key;
    private final float baseHealth;
    private final float damageMul;
    private final float speedMul;
    private final float defenseMul;
    private final float staminaRegenMul;
    private final String signatureAbility;

    PlayerClass(String key, float baseHealth, float damageMul, float speedMul,
                float defenseMul, float staminaRegenMul, String signatureAbility) {
        this.key = key;
        this.baseHealth = baseHealth;
        this.damageMul = damageMul;
        this.speedMul = speedMul;
        this.defenseMul = defenseMul;
        this.staminaRegenMul = staminaRegenMul;
        this.signatureAbility = signatureAbility;
    }

    public String key() { return key; }
    public float baseHealth() { return baseHealth; }
    public float damageMultiplier() { return damageMul; }
    public float attackSpeedMultiplier() { return speedMul; }
    public float defenseMultiplier() { return defenseMul; }
    public float staminaRegenMultiplier() { return staminaRegenMul; }
    public String signatureAbility() { return signatureAbility; }

    public String translationKey() { return "class.smmorpg." + key; }
    public String descriptionKey() { return "class.smmorpg." + key + ".desc"; }

    @Override public String getSerializedName() { return key; }

    public static PlayerClass byKey(String k) {
        for (PlayerClass c : values()) if (c.key.equals(k)) return c;
        return SHINOBI;
    }

    /** Parry window in ticks, before gear. */
    public int parryWindowTicks() {
        return switch (this) {
            case SHINOBI -> 8;
            case BULWARK -> 7;
            case RONIN, LANCER, MONK -> 5;
            default -> 4;
        };
    }
}
