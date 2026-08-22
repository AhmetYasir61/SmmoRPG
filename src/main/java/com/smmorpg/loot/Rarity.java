package com.smmorpg.loot;

import net.minecraft.ChatFormatting;

/** Loot tiers. Weight is the raw roll weight before level scaling. */
public enum Rarity {
    COMMON("common", ChatFormatting.GRAY, 1000, 0, 1.00F),
    UNCOMMON("uncommon", ChatFormatting.GREEN, 420, 1, 1.10F),
    RARE("rare", ChatFormatting.BLUE, 160, 2, 1.25F),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 55, 3, 1.45F),
    LEGENDARY("legendary", ChatFormatting.GOLD, 14, 4, 1.75F),
    HOLY("holy", ChatFormatting.YELLOW, 5, 4, 1.90F),
    CURSED("cursed", ChatFormatting.DARK_RED, 5, 4, 2.10F),
    MYTHIC("mythic", ChatFormatting.LIGHT_PURPLE, 2, 5, 2.40F);

    private final String key;
    private final ChatFormatting color;
    private final int weight;
    private final int affixSlots;
    private final float statScale;

    Rarity(String key, ChatFormatting color, int weight, int affixSlots, float statScale) {
        this.key = key; this.color = color; this.weight = weight;
        this.affixSlots = affixSlots; this.statScale = statScale;
    }

    public String key() { return key; }
    public ChatFormatting color() { return color; }
    public int weight() { return weight; }
    public int affixSlots() { return affixSlots; }
    public float statScale() { return statScale; }
    public String translationKey() { return "rarity.smmorpg." + key; }

    public static Rarity byKey(String k) {
        for (Rarity r : values()) if (r.key.equals(k)) return r;
        return COMMON;
    }
}
