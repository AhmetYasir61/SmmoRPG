package com.smmorpg.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smmorpg.classes.PlayerClass;
import com.smmorpg.config.CombatConfig;

/**
 * A player's persistent MMORPG state: chosen class, level, experience and the stat points
 * they have spent. Immutable — every mutation returns a new record, which keeps the
 * network sync trivially correct.
 */
public record PlayerProgress(String classKey,
                             boolean classChosen,
                             int level,
                             int experience,
                             int unspentPoints,
                             int strength,
                             int agility,
                             int vitality,
                             int spirit) {

    public static final PlayerProgress EMPTY =
            new PlayerProgress(PlayerClass.SHINOBI.key(), false, 1, 0, 0, 5, 5, 5, 5);

    public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("class").forGetter(PlayerProgress::classKey),
            Codec.BOOL.fieldOf("chosen").forGetter(PlayerProgress::classChosen),
            Codec.INT.fieldOf("level").forGetter(PlayerProgress::level),
            Codec.INT.fieldOf("xp").forGetter(PlayerProgress::experience),
            Codec.INT.fieldOf("points").forGetter(PlayerProgress::unspentPoints),
            Codec.INT.fieldOf("str").forGetter(PlayerProgress::strength),
            Codec.INT.fieldOf("agi").forGetter(PlayerProgress::agility),
            Codec.INT.fieldOf("vit").forGetter(PlayerProgress::vitality),
            Codec.INT.fieldOf("spi").forGetter(PlayerProgress::spirit)
    ).apply(i, PlayerProgress::new));

    public PlayerClass playerClass() { return PlayerClass.byKey(classKey); }

    public PlayerProgress withClass(PlayerClass c) {
        return new PlayerProgress(c.key(), true, level, experience, unspentPoints,
                strength, agility, vitality, spirit);
    }

    /** XP needed to go from {@code level} to {@code level + 1}. */
    public static int xpForNext(int level) {
        int base = CombatConfig.CFG.baseXpPerLevel.get();
        double exp = CombatConfig.CFG.xpCurveExponent.get();
        return (int) Math.round(base * Math.pow(level, exp - 1.0) * level);
    }

    public int xpForNext() { return xpForNext(level); }

    /** Adds experience and rolls over as many levels as it covers. */
    public PlayerProgress grantExperience(int amount) {
        if (amount <= 0) return this;
        int max = CombatConfig.CFG.maxLevel.get();
        int lvl = level;
        int xp = experience + amount;
        int pts = unspentPoints;
        while (lvl < max) {
            int need = xpForNext(lvl);
            if (xp < need) break;
            xp -= need;
            lvl++;
            pts += 3;
        }
        if (lvl >= max) xp = 0;
        return new PlayerProgress(classKey, classChosen, lvl, xp, pts, strength, agility, vitality, spirit);
    }

    public PlayerProgress spendPoint(String stat) {
        if (unspentPoints <= 0) return this;
        return switch (stat) {
            case "strength" -> new PlayerProgress(classKey, classChosen, level, experience,
                    unspentPoints - 1, strength + 1, agility, vitality, spirit);
            case "agility" -> new PlayerProgress(classKey, classChosen, level, experience,
                    unspentPoints - 1, strength, agility + 1, vitality, spirit);
            case "vitality" -> new PlayerProgress(classKey, classChosen, level, experience,
                    unspentPoints - 1, strength, agility, vitality + 1, spirit);
            case "spirit" -> new PlayerProgress(classKey, classChosen, level, experience,
                    unspentPoints - 1, strength, agility, vitality, spirit + 1);
            default -> this;
        };
    }

    public float meleeDamageMultiplier() {
        return playerClass().damageMultiplier() * (1.0F + strength * 0.018F + level * 0.004F);
    }

    public float maxHealth() {
        return playerClass().baseHealth() + vitality * 0.8F + level * 0.35F;
    }

    public float staminaMax() { return 100.0F + spirit * 4.0F + agility * 1.5F; }

    public float staminaRegen() {
        return playerClass().staminaRegenMultiplier() * (1.2F + spirit * 0.05F);
    }
}
