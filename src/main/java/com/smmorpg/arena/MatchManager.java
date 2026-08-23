package com.smmorpg.arena;

import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import com.smmorpg.backend.AccountService;
import com.smmorpg.config.ServerConfig;
import com.smmorpg.rank.EloSystem;
import com.smmorpg.rank.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs matches from the moment two sides are paired to the moment ratings move.
 *
 * <p>A rated result is written once and only once. Everything that can end a match — a
 * knockout, a disconnect, the clock — funnels through {@link #conclude}, because a ladder
 * where leaving before you lose costs nothing is a ladder nobody trusts.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class MatchManager {

    private static final List<Match> ACTIVE = new ArrayList<>();
    private static int tick;

    private MatchManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ServerConfig.CFG.rankedEnabled.get()) return;
        MinecraftServer server = event.getServer();

        // Matchmaking once a second is plenty and keeps the pairing work off the hot path.
        if (++tick % 20 == 0) {
            for (MatchMode mode : MatchMode.values()) {
                if (mode == MatchMode.FRIENDLY) continue;
                List<List<UUID>> sides = MatchQueue.tryForm(mode);
                if (sides.size() == 2) begin(server, mode, sides.get(0), sides.get(1));
            }
        }

        long limit = ServerConfig.CFG.matchDurationSeconds.get();
        for (Match match : List.copyOf(ACTIVE)) {
            if (match.elapsedSeconds() >= limit) {
                match.timeOut();
                conclude(server, match);
            }
        }
    }

    public static void begin(MinecraftServer server, MatchMode mode,
                             List<UUID> teamA, List<UUID> teamB) {
        Match match = new Match(mode, teamA, teamB);
        ACTIVE.add(match);

        for (UUID uuid : match.everyone()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;
            player.sendSystemMessage(Component.translatable("match.smmorpg.found",
                    Component.translatable(mode.translationKey())).withStyle(ChatFormatting.GOLD));
        }
        SmmoRPG.LOGGER.info("Match started: {} {}v{}", mode.key(), mode.teamSize(), mode.teamSize());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Match match = matchOf(player.getUUID());
        if (match == null) return;

        match.markDefeated(player.getUUID());
        if (match.finished()) conclude(player.server, match);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MatchQueue.leaveAll(player);

        Match match = matchOf(player.getUUID());
        if (match == null) return;

        // Leaving mid-match is a loss. Anything else and the ladder rewards rage-quitting.
        match.forfeit(player.getUUID());
        conclude(player.server, match);
    }

    /** Applies the result: ratings, coins, and a line telling everyone what happened. */
    private static void conclude(MinecraftServer server, Match match) {
        if (!ACTIVE.remove(match)) return;      // already concluded by another path

        for (UUID uuid : match.everyone()) {
            PlayerAccount account = AccountService.of(uuid);
            if (account == null) continue;

            double score = match.scoreFor(uuid);
            int newElo = account.elo();

            if (match.mode().rated()) {
                int[] opponents = match.opponentsOf(uuid).stream()
                        .map(AccountService::of)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(PlayerAccount::elo)
                        .toArray();
                newElo = EloSystem.rateTeam(account.elo(), opponents, score, account.matches());
            }

            boolean won = score > 0.5D;
            long coins = won ? ServerConfig.CFG.coinsPerWin.get()
                    : ServerConfig.CFG.coinsPerLoss.get();

            PlayerAccount updated = match.mode().rated()
                    ? account.withResult(newElo, won).withCoins(coins)
                    : account.withCoins(coins);
            AccountService.put(updated);

            report(server, uuid, account, updated, score);
        }
    }

    private static void report(MinecraftServer server, UUID uuid,
                               PlayerAccount before, PlayerAccount after, double score) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;

        int delta = after.elo() - before.elo();
        String key = score > 0.5D ? "match.smmorpg.win"
                : score < 0.5D ? "match.smmorpg.loss" : "match.smmorpg.draw";

        player.sendSystemMessage(Component.translatable(key,
                (delta >= 0 ? "+" : "") + delta, after.elo())
                .withStyle(delta >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));

        Rank was = Rank.of(before.elo(), before.matches());
        Rank now = Rank.of(after.elo(), after.matches());
        if (was != now) {
            player.sendSystemMessage(Component.translatable(
                    now.ordinal() > was.ordinal() ? "match.smmorpg.promoted" : "match.smmorpg.demoted",
                    Component.translatable(now.translationKey())).withStyle(now.color()));
        }
    }

    public static Match matchOf(UUID uuid) {
        for (Match match : ACTIVE) if (match.contains(uuid)) return match;
        return null;
    }

    public static boolean inMatch(UUID uuid) { return matchOf(uuid) != null; }

    public static int activeMatches() { return ACTIVE.size(); }
}
