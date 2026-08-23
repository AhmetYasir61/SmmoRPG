package com.smmorpg.rank;

import net.minecraft.ChatFormatting;

/**
 * The visible face of a rating.
 *
 * <p>Ratings are a number and numbers do not motivate anyone. A rank does — it is a place
 * you can be promoted out of and demoted back into, and the demotion is the part that makes
 * the promotion worth something.
 */
public enum Rank {
    UNRANKED("unranked", ChatFormatting.DARK_GRAY, Integer.MIN_VALUE),
    IRON("iron", ChatFormatting.GRAY, 0),
    BRONZE("bronze", ChatFormatting.GOLD, 800),
    SILVER("silver", ChatFormatting.WHITE, 1000),
    GOLD("gold", ChatFormatting.YELLOW, 1200),
    PLATINUM("platinum", ChatFormatting.AQUA, 1450),
    DIAMOND("diamond", ChatFormatting.BLUE, 1700),
    MASTER("master", ChatFormatting.LIGHT_PURPLE, 1950),
    GRANDMASTER("grandmaster", ChatFormatting.RED, 2200),
    SOVEREIGN("sovereign", ChatFormatting.DARK_RED, 2500);

    /** Matches a player must finish before a rank is shown at all. */
    public static final int PLACEMENT_MATCHES = 5;

    private final String key;
    private final ChatFormatting color;
    private final int floor;

    Rank(String key, ChatFormatting color, int floor) {
        this.key = key;
        this.color = color;
        this.floor = floor;
    }

    public String key() { return key; }
    public ChatFormatting color() { return color; }
    public int floor() { return floor; }
    public String translationKey() { return "rank.smmorpg." + key; }

    public static Rank of(int elo, int matchesPlayed) {
        if (matchesPlayed < PLACEMENT_MATCHES) return UNRANKED;

        Rank best = IRON;
        for (Rank rank : values()) {
            if (rank == UNRANKED) continue;
            if (elo >= rank.floor) best = rank;
        }
        return best;
    }

    /** Rating still needed for the next rank, or 0 at the top. */
    public int toNext(int elo) {
        int i = ordinal() + 1;
        return i < values().length ? Math.max(0, values()[i].floor - elo) : 0;
    }
}
