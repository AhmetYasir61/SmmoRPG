package com.smmorpg.loot;

/**
 * One rolled property on a weapon or accessory.
 *
 * <p>Holy and cursed affixes are deliberately two-sided: a cursed blade hits far harder
 * than anything you can find, and it takes the difference out of you.
 */
public enum Affix {
    // --- ordinary ---
    KEEN("keen", Alignment.NEUTRAL, 0.05F, 0.20F),
    HEAVY("heavy", Alignment.NEUTRAL, 0.08F, 0.30F),
    SWIFT("swift", Alignment.NEUTRAL, 0.04F, 0.18F),
    SERRATED("serrated", Alignment.NEUTRAL, 0.10F, 0.45F),
    TEMPERED("tempered", Alignment.NEUTRAL, 0.03F, 0.15F),
    VAMPIRIC("vampiric", Alignment.NEUTRAL, 0.02F, 0.12F),
    ARMOR_PIERCING("armor_piercing", Alignment.NEUTRAL, 0.06F, 0.25F),
    STEADY("steady", Alignment.NEUTRAL, 0.05F, 0.22F),

    // --- holy ---
    SANCTIFIED("sanctified", Alignment.HOLY, 0.12F, 0.40F),
    RADIANT("radiant", Alignment.HOLY, 0.10F, 0.35F),
    PURIFYING("purifying", Alignment.HOLY, 0.08F, 0.30F),
    MENDING_LIGHT("mending_light", Alignment.HOLY, 0.06F, 0.25F),
    UNDEAD_BANE("undead_bane", Alignment.HOLY, 0.25F, 0.80F),

    // --- cursed ---
    BLOODTHIRSTY("bloodthirsty", Alignment.CURSED, 0.18F, 0.60F),
    SOUL_EATING("soul_eating", Alignment.CURSED, 0.14F, 0.50F),
    ROTTING("rotting", Alignment.CURSED, 0.12F, 0.45F),
    HEMORRHAGE("hemorrhage", Alignment.CURSED, 0.20F, 0.75F),
    LIFE_TITHE("life_tithe", Alignment.CURSED, 0.22F, 0.90F);

    public enum Alignment { NEUTRAL, HOLY, CURSED }

    private final String key;
    private final Alignment alignment;
    private final float minPower;
    private final float maxPower;

    Affix(String key, Alignment alignment, float minPower, float maxPower) {
        this.key = key; this.alignment = alignment;
        this.minPower = minPower; this.maxPower = maxPower;
    }

    public String key() { return key; }
    public Alignment alignment() { return alignment; }
    public float minPower() { return minPower; }
    public float maxPower() { return maxPower; }
    public String translationKey() { return "affix.smmorpg." + key; }

    public static Affix byKey(String k) {
        for (Affix a : values()) if (a.key.equals(k)) return a;
        return KEEN;
    }
}
