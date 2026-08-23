package com.smmorpg.arena;

import com.smmorpg.account.PlayerAccount;
import com.smmorpg.backend.AccountService;
import com.smmorpg.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who is waiting, and who they are willing to fight.
 *
 * <p>The acceptable rating gap starts narrow and widens the longer someone waits. That is
 * the only honest way to run a ladder on a server that might have four people on it: a
 * strict gap gives perfect matches and infinite queues, and no gap at all gives instant
 * matches nobody enjoys. Widening turns the trade-off into something the player can feel
 * happening rather than a number an operator guessed once.
 */
public final class MatchQueue {

    /** Rating either side of you that counts as a fair match at the moment you join. */
    private static final int INITIAL_GAP = 100;
    private static final int GAP_STEP = 120;
    private static final int MAX_GAP = 5000;

    private record Waiting(UUID uuid, int elo, long since) {}

    private static final Map<MatchMode, Map<UUID, Waiting>> QUEUES = new EnumMap<>(MatchMode.class);

    private MatchQueue() {}

    public static synchronized void join(ServerPlayer player, MatchMode mode) {
        PlayerAccount account = AccountService.of(player);
        leaveAll(player);
        QUEUES.computeIfAbsent(mode, m -> new LinkedHashMap<>())
                .put(player.getUUID(), new Waiting(player.getUUID(), account.elo(),
                        System.currentTimeMillis()));
    }

    public static synchronized void leaveAll(ServerPlayer player) {
        for (Map<UUID, Waiting> queue : QUEUES.values()) queue.remove(player.getUUID());
    }

    public static synchronized boolean isQueued(ServerPlayer player) {
        for (Map<UUID, Waiting> queue : QUEUES.values()) {
            if (queue.containsKey(player.getUUID())) return true;
        }
        return false;
    }

    public static synchronized int waitingIn(MatchMode mode) {
        Map<UUID, Waiting> queue = QUEUES.get(mode);
        return queue == null ? 0 : queue.size();
    }

    /**
     * Tries to form one match in each mode. Returns the sides, or an empty list if nothing
     * could be paired this tick.
     */
    public static synchronized List<List<UUID>> tryForm(MatchMode mode) {
        Map<UUID, Waiting> queue = QUEUES.get(mode);
        if (queue == null || queue.size() < mode.totalPlayers()) return List.of();

        List<Waiting> waiting = new ArrayList<>(queue.values());
        // Oldest first: the person who has waited longest gets the widest net and the
        // first shot at a match.
        waiting.sort((a, b) -> Long.compare(a.since, b.since));

        Waiting anchor = waiting.get(0);
        int gap = gapFor(anchor);

        List<Waiting> pool = new ArrayList<>();
        for (Waiting candidate : waiting) {
            if (Math.abs(candidate.elo - anchor.elo) <= gap) pool.add(candidate);
            if (pool.size() == mode.totalPlayers()) break;
        }
        if (pool.size() < mode.totalPlayers()) return List.of();

        // Snake the sorted pool into two sides so the teams are as even as the pool allows.
        pool.sort((a, b) -> Integer.compare(b.elo, a.elo));
        List<UUID> teamA = new ArrayList<>();
        List<UUID> teamB = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            (i % 2 == 0 ? teamA : teamB).add(pool.get(i).uuid);
            queue.remove(pool.get(i).uuid);
        }
        return List.of(teamA, teamB);
    }

    private static int gapFor(Waiting waiting) {
        long seconds = (System.currentTimeMillis() - waiting.since) / 1000L;
        int step = Math.max(1, ServerConfig.CFG.queueWidenSeconds.get());
        return Math.min(MAX_GAP, INITIAL_GAP + (int) (seconds / step) * GAP_STEP);
    }

    /** How long this player has been waiting, in seconds, or -1 if they are not queued. */
    public static synchronized long waitedSeconds(ServerPlayer player) {
        for (Map<UUID, Waiting> queue : QUEUES.values()) {
            Waiting waiting = queue.get(player.getUUID());
            if (waiting != null) return (System.currentTimeMillis() - waiting.since) / 1000L;
        }
        return -1L;
    }
}
