package com.smmorpg.backend;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.ServerConfig;
import com.smmorpg.market.TebexService;
import com.smmorpg.network.Net;
import com.smmorpg.network.S2CAccountSync;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Starts the account service with the server and keeps it fed.
 *
 * <p>Nothing here blocks. Syncing and Tebex polling both run on their own schedules and
 * both are allowed to fail: a server whose tick loop waits on someone else's HTTP endpoint
 * is a server that stutters every time that endpoint has a bad minute.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class ServerLifecycle {

    private static int tick;

    private ServerLifecycle() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        AccountService.start(event.getServer());
        // Announce immediately rather than waiting out the first interval, so a restarted
        // server reappears in the list in seconds instead of a minute.
        ServerHeartbeat.send(event.getServer());

        if (BackendClient.configured() && !ServerHeartbeat.configured()) {
            SmmoRPG.LOGGER.warn("Not listing in the server directory: set publicAddress "
                    + "in smmorpg-server.toml to appear in the in-game server list.");
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        // Last chance to get anything out before the process ends; whatever does not make
        // it stays in the queue on disk for the next boot.
        AccountService.syncTick();
        AccountService.stop();
        SmmoRPG.LOGGER.info("Account service stopped with {} writes still queued.",
                OfflineQueue.size());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        tick++;

        int syncTicks = ServerConfig.CFG.syncIntervalSeconds.get() * 20;
        if (tick % syncTicks == 0) AccountService.syncTick();

        int beatTicks = ServerConfig.CFG.heartbeatSeconds.get() * 20;
        if (tick % beatTicks == 0) ServerHeartbeat.send(event.getServer());

        int tebexTicks = ServerConfig.CFG.tebexPollSeconds.get() * 20;
        if (TebexService.configured() && tick % tebexTicks == 0) {
            TebexService.poll(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AccountService.loadRemote(player);
        // Send what we have now rather than waiting for the fetch: the local copy is
        // already correct, and a player should never stare at an empty profile because a
        // request is in flight.
        Net.sendTo(player, new S2CAccountSync(AccountService.of(player)));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AccountService.put(AccountService.of(player));   // marks dirty so it gets pushed
    }
}
