package com.smmorpg.arena;

/** The queues a player can stand in. */
public enum MatchMode {
    DUEL("duel", 1),
    DOUBLES("doubles", 2),
    /** Not rated. For fighting someone you know without either of you risking anything. */
    FRIENDLY("friendly", 1);

    private final String key;
    private final int teamSize;

    MatchMode(String key, int teamSize) {
        this.key = key;
        this.teamSize = teamSize;
    }

    public String key() { return key; }
    public int teamSize() { return teamSize; }
    public int totalPlayers() { return teamSize * 2; }
    public boolean rated() { return this != FRIENDLY; }
    public String translationKey() { return "mode.smmorpg." + key; }

    public static MatchMode byKey(String key) {
        for (MatchMode mode : values()) if (mode.key.equals(key)) return mode;
        return DUEL;
    }
}
