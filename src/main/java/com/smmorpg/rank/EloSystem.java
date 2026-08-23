package com.smmorpg.rank;

/**
 * Standard Elo, with the two adjustments a game needs that chess does not.
 *
 * <p>K falls as a player settles: a newcomer's rating should find its level in a handful of
 * matches, and a Grandmaster's should not swing on one bad night. And there is a floor, so
 * a losing streak cannot bury someone below the point where the ladder still has opponents
 * for them.
 */
public final class EloSystem {

    /** Nobody drops below this, however the night goes. */
    public static final int FLOOR = 100;

    private EloSystem() {}

    /** How much a single result can move a player. */
    public static int kFactor(int elo, int matchesPlayed) {
        if (matchesPlayed < Rank.PLACEMENT_MATCHES) return 64;   // find your level fast
        if (elo >= 2200) return 16;                              // and then stop swinging
        if (elo >= 1700) return 24;
        return 32;
    }

    /** Probability that {@code elo} beats {@code opponentElo}, by the usual logistic curve. */
    public static double expectedScore(int elo, int opponentElo) {
        return 1.0D / (1.0D + Math.pow(10.0D, (opponentElo - elo) / 400.0D));
    }

    /**
     * The new rating after one result.
     *
     * @param score 1 for a win, 0 for a loss, 0.5 for a draw
     */
    public static int rate(int elo, int opponentElo, double score, int matchesPlayed) {
        double expected = expectedScore(elo, opponentElo);
        int k = kFactor(elo, matchesPlayed);
        return Math.max(FLOOR, (int) Math.round(elo + k * (score - expected)));
    }

    /**
     * Team ratings for 2v2. Each player is rated against the opposing team's average, which
     * keeps a strong player carrying a weak one from farming rating off it.
     */
    public static int rateTeam(int elo, int[] opposingTeam, double score, int matchesPlayed) {
        if (opposingTeam.length == 0) return elo;
        int sum = 0;
        for (int rating : opposingTeam) sum += rating;
        return rate(elo, sum / opposingTeam.length, score, matchesPlayed);
    }

    /** What a win would be worth, for showing before a player commits to a match. */
    public static int previewGain(int elo, int opponentElo, int matchesPlayed) {
        return rate(elo, opponentElo, 1.0D, matchesPlayed) - elo;
    }

    public static int previewLoss(int elo, int opponentElo, int matchesPlayed) {
        return elo - rate(elo, opponentElo, 0.0D, matchesPlayed);
    }
}
