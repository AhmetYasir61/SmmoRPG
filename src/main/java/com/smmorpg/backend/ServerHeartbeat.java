package com.smmorpg.backend;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.ServerConfig;
import net.minecraft.server.MinecraftServer;

/**
 * Tells the directory this server exists, and keeps telling it.
 *
 * <p>A list that servers are added to by hand goes stale the first time one moves or dies,
 * and then sends players at an address with nothing behind it. Listing is therefore
 * something a server does continuously rather than once: entries expire, and a server that
 * has actually gone away drops off on its own.
 *
 * <p>Nothing is advertised unless {@code publicAddress} is set. A server cannot reliably
 * work out how the outside world reaches it — behind NAT, a proxy or a shared host it will
 * see something that is right for it and useless to everyone else — so this is one thing
 * the operator has to say out loud.
 */
public final class ServerHeartbeat {

    private ServerHeartbeat() {}

    public static boolean configured() {
        return BackendClient.configured()
                && !ServerConfig.CFG.publicAddress.get().isBlank();
    }

    public static void send(MinecraftServer server) {
        if (!configured()) return;

        String address = ServerConfig.CFG.publicAddress.get().trim();
        String name = ServerConfig.CFG.serverName.get().isBlank()
                ? address : ServerConfig.CFG.serverName.get().trim();

        String body = """
                {"address":"%s","name":"%s","players":%d,"max_players":%d,"version":"%s"}"""
                .formatted(escape(address), escape(name),
                        server.getPlayerCount(), server.getMaxPlayers(),
                        escape(SmmoRPG.MOD_ID + " " + modVersion()));

        // Straight out rather than through the offline queue: a heartbeat that arrives late
        // is worse than one that never arrives, since it would advertise a server as live
        // long after it stopped being live.
        BackendClient.post("/servers/heartbeat", body);
    }

    private static String modVersion() {
        return net.neoforged.fml.ModList.get().getModContainerById(SmmoRPG.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("?");
    }

    /** Minimal JSON escaping — these are operator-set strings, not arbitrary input. */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
