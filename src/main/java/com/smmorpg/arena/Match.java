package com.smmorpg.arena;

import java.util.List;
import java.util.UUID;

/** One match in progress. */
public final class Match {

    public enum Result { TEAM_A, TEAM_B, DRAW }

    private final MatchMode mode;
    private final List<UUID> teamA;
    private final List<UUID> teamB;
    private final long startedAt = System.currentTimeMillis();

    private final java.util.Set<UUID> defeated = new java.util.HashSet<>();
    private Result result;

    public Match(MatchMode mode, List<UUID> teamA, List<UUID> teamB) {
        this.mode = mode;
        this.teamA = List.copyOf(teamA);
        this.teamB = List.copyOf(teamB);
    }

    public MatchMode mode() { return mode; }
    public List<UUID> teamA() { return teamA; }
    public List<UUID> teamB() { return teamB; }
    public Result result() { return result; }
    public boolean finished() { return result != null; }

    public List<UUID> everyone() {
        List<UUID> all = new java.util.ArrayList<>(teamA);
        all.addAll(teamB);
        return all;
    }

    public boolean contains(UUID uuid) { return teamA.contains(uuid) || teamB.contains(uuid); }

    public List<UUID> teamOf(UUID uuid) { return teamA.contains(uuid) ? teamA : teamB; }

    public List<UUID> opponentsOf(UUID uuid) { return teamA.contains(uuid) ? teamB : teamA; }

    public long elapsedSeconds() { return (System.currentTimeMillis() - startedAt) / 1000L; }

    /**
     * Records a player as out. A side loses when every one of its members is down, which is
     * what makes 2v2 a team fight rather than two duels sharing a floor.
     */
    public void markDefeated(UUID uuid) {
        if (finished()) return;
        defeated.add(uuid);

        if (defeated.containsAll(teamA)) result = Result.TEAM_B;
        else if (defeated.containsAll(teamB)) result = Result.TEAM_A;
    }

    /** Someone left. Their side forfeits rather than the match hanging forever. */
    public void forfeit(UUID uuid) {
        if (finished()) return;
        result = teamA.contains(uuid) ? Result.TEAM_B : Result.TEAM_A;
    }

    public void timeOut() {
        if (!finished()) result = Result.DRAW;
    }

    /** The score for one player, in Elo's terms: 1 won, 0 lost, 0.5 drew. */
    public double scoreFor(UUID uuid) {
        if (result == Result.DRAW) return 0.5D;
        boolean onA = teamA.contains(uuid);
        return (result == Result.TEAM_A) == onA ? 1.0D : 0.0D;
    }
}
