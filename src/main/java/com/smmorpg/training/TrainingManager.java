package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns every live {@link TrainingSession} and ticks them. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class TrainingManager {

    private static final Map<UUID, TrainingSession> SESSIONS = new HashMap<>();

    private TrainingManager() {}

    /**
     * Starts a session at whatever level the player has already earned.
     *
     * <p>There is no difficulty argument on purpose. The percentage is the record of what
     * they have beaten, so the only thing that can raise it is beating the next wave.
     */
    public static TrainingSession start(ServerPlayer player) {
        stop(player);
        int level = player.getData(com.smmorpg.core.ModAttachments.TRAINING_LEVEL.get());
        TrainingSession session = new TrainingSession(player.getUUID(), level, player.position());
        SESSIONS.put(player.getUUID(), session);

        // Nobody fights a divine-tier arena with their fists. The kit tops up what is
        // missing rather than replacing what the player brought, so walking in with your
        // own gear still means walking in with your own gear.
        com.smmorpg.kit.StarterKit.grant(player);
        Difficulty difficulty = session.difficulty();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "training.smmorpg.started", level, difficulty.percent(),
                net.minecraft.network.chat.Component.translatable(difficulty.tierKey())));
        return session;
    }

    public static void stop(ServerPlayer player) {
        TrainingSession session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.end(level);
        if (session != null) {
            com.smmorpg.network.Net.sendTo(player, com.smmorpg.network.S2CArenaStatus.INACTIVE);
        }
    }

    private static void sendStatus(ServerPlayer player, TrainingSession session) {
        com.smmorpg.network.Net.sendTo(player, new com.smmorpg.network.S2CArenaStatus(
                true, session.level(), session.difficulty().percent(),
                session.killsThisWave(), session.killsNeeded(), session.resting()));
    }

    public static TrainingSession of(ServerPlayer player) { return SESSIONS.get(player.getUUID()); }

    /** Every live session, for the rules that have to know where the arenas are. */
    public static java.util.Collection<TrainingSession> sessions() { return SESSIONS.values(); }

    /**
     * Dying in the arena is meant to cost you the fight, not the session. A player who
     * respawns mid-session and finds themselves empty-handed has to walk out and come
     * back in just to be armed again.
     */
    @SubscribeEvent
    public static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SESSIONS.containsKey(player.getUUID())) {
            com.smmorpg.kit.StarterKit.grant(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            TrainingSession session = SESSIONS.get(player.getUUID());
            if (session == null) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;

            // Step outside the walls and the arena releases you rather than following you
            // home. The threshold sits just beyond the 32x32 floor, so walking out is an
            // unambiguous way to end a session without a command.
            if (player.position().distanceToSqr(session.centre()) > 26.0D * 26.0D) {
                stop(player);
                continue;
            }
            session.tick(level, player);

            // Half a second is often enough for a kill counter and cheap enough to ignore.
            if (player.tickCount % 10 == 0) sendStatus(player, session);
        }
        com.smmorpg.sync.ContentSync.broadcastIfChanged(event.getServer());
    }
}
