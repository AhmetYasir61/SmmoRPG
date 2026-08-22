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

    public static TrainingSession start(ServerPlayer player, int difficultyPercent) {
        stop(player);
        TrainingSession session = new TrainingSession(player.getUUID(),
                new Difficulty(difficultyPercent), player.position());
        SESSIONS.put(player.getUUID(), session);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "training.smmorpg.started", difficultyPercent,
                net.minecraft.network.chat.Component.translatable(session.difficulty().tierKey())));
        return session;
    }

    public static void stop(ServerPlayer player) {
        TrainingSession session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.end(level);
    }

    public static TrainingSession of(ServerPlayer player) { return SESSIONS.get(player.getUUID()); }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            TrainingSession session = SESSIONS.get(player.getUUID());
            if (session == null) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;

            // Wander too far and the arena releases you rather than following you home.
            if (player.position().distanceToSqr(session.centre()) > 90.0D * 90.0D) {
                stop(player);
                continue;
            }
            session.tick(level, player);
        }
        com.smmorpg.sync.ContentSync.broadcastIfChanged(event.getServer());
    }
}
