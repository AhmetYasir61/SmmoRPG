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
        TrainingSession session = new TrainingSession(player.getUUID(), level);
        SESSIONS.put(player.getUUID(), session);

        // Put them where they left off. The labyrinth is written once and kept, so a save
        // from last week is still a real place with the same walls around it.
        if (player.level() instanceof ServerLevel world) {
            long cell = TrainingSession.resumeCell(world, player.getUUID());
            com.smmorpg.labyrinth.Labyrinth.ensureAround(world, 
                    com.smmorpg.labyrinth.Labyrinth.centreOf(world, cell), 2);
            teleportTo(player, world, cell);
        }

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

    /** Drops a player at the middle of a cell, having made sure that cell exists first. */
    public static void teleportTo(ServerPlayer player, ServerLevel world, long cell) {
        // The cell is written now rather than queued: teleporting into a hole and falling
        // through the world while the queue catches up would be worse than a brief pause.
        com.smmorpg.labyrinth.Labyrinth.buildNow(world, cell);

        var centre = com.smmorpg.labyrinth.Labyrinth.centreOf(world, cell);
        player.teleportTo(world, centre.x, centre.y, centre.z, player.getYRot(), player.getXRot());
    }

    /**
     * Puts a player into somebody else's run.
     *
     * <p>They get their own session — their own kit, their own opponents to be counted —
     * but they start standing next to the person who invited them, which is the only part
     * of "joining" anyone actually cares about.
     */
    public static void join(ServerPlayer player, ServerPlayer host) {
        TrainingSession hostSession = SESSIONS.get(host.getUUID());
        if (hostSession == null || !(host.level() instanceof ServerLevel world)) return;

        TrainingSession session = new TrainingSession(player.getUUID(),
                player.getData(com.smmorpg.core.ModAttachments.TRAINING_LEVEL.get()));
        SESSIONS.put(player.getUUID(), session);

        com.smmorpg.kit.StarterKit.grant(player);
        player.teleportTo(world, host.getX(), host.getY(), host.getZ(),
                player.getYRot(), player.getXRot());
    }

    private static void sendStatus(ServerPlayer player, TrainingSession session) {
        com.smmorpg.network.Net.sendTo(player, new com.smmorpg.network.S2CArenaStatus(
                true, session.level(), session.difficulty().percent(),
                session.killsThisWave(), session.killsNeeded(), session.waveCleared(),
                livesOf(player)));
    }

    /** Lives left on the current save, or a full bar for a player who has never saved. */
    private static int livesOf(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return 0;
        var save = com.smmorpg.labyrinth.LabyrinthData.get(world).save(player.getUUID());
        return save == null ? com.smmorpg.labyrinth.RunSave.LIVES : save.lives();
    }

    public static TrainingSession of(ServerPlayer player) { return SESSIONS.get(player.getUUID()); }

    /** Every live session, for the rules that have to know where the arenas are. */
    public static java.util.Collection<TrainingSession> sessions() { return SESSIONS.values(); }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            TrainingSession session = SESSIONS.get(player.getUUID());
            if (session == null) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;

            // The labyrinth has walls; leaving it means leaving the world it is in.
            if (!TrainingArena.isArenaWorld(level)) {
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
