package com.smmorpg.backend;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import com.smmorpg.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accounts, held in memory and mirrored to the service.
 *
 * <p>The in-memory copy is authoritative for the running server. Reads never block on the
 * network — a player who logs in while the service is down gets whatever the server last
 * knew, plays normally, and their writes go to the queue. Nothing waits, and nothing is
 * lost.
 */
public final class AccountService {

    private static final Map<UUID, PlayerAccount> ACCOUNTS = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

    private static Path localMirror;

    private AccountService() {}

    public static void start(MinecraftServer server) {
        BackendClient.baseUrl = BackendEndpoints.resolveBaseUrl(ServerConfig.CFG.backendUrl.get());
        BackendClient.apiKey = ServerConfig.CFG.backendApiKey.get();

        Path dir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("smmorpg");
        localMirror = dir.resolve("accounts.json");
        OfflineQueue.load(dir.resolve("pending-writes.json"));
        LocalMirror.load(localMirror, ACCOUNTS);

        SmmoRPG.LOGGER.info("Account service {}.",
                BackendClient.configured() ? "online at " + BackendClient.baseUrl : "running locally");
    }

    public static void stop() {
        LocalMirror.save(localMirror, ACCOUNTS);
        OfflineQueue.drain();
    }

    public static PlayerAccount of(ServerPlayer player) {
        return ACCOUNTS.computeIfAbsent(player.getUUID(),
                id -> PlayerAccount.fresh(id.toString(), player.getGameProfile().getName()));
    }

    public static PlayerAccount of(UUID uuid) {
        return ACCOUNTS.get(uuid);
    }

    /** Replaces an account and marks it for the next push. */
    public static void put(PlayerAccount account) {
        UUID uuid = UUID.fromString(account.uuid());
        ACCOUNTS.put(uuid, account);
        DIRTY.add(uuid);
    }

    /** Pulls the authoritative copy on join, if the service has one and is up. */
    public static void loadRemote(ServerPlayer player) {
        if (!BackendClient.configured()) return;

        UUID uuid = player.getUUID();
        BackendClient.get("/accounts/" + uuid).thenAccept(json ->
                json.flatMap(AccountService::decode).ifPresent(remote -> {
                    PlayerAccount local = ACCOUNTS.get(uuid);
                    // The higher revision wins. A server that played on while the service
                    // was down is ahead of it, and its work must not be overwritten by a
                    // stale copy the moment the service comes back.
                    if (local == null || remote.revision() >= local.revision()) {
                        ACCOUNTS.put(uuid, remote.renamed(player.getGameProfile().getName()));
                    } else {
                        DIRTY.add(uuid);
                    }
                }));
    }

    /** Pushes everything dirty. Called on a timer and again at shutdown. */
    public static void syncTick() {
        OfflineQueue.drain();

        if (DIRTY.isEmpty()) return;
        for (UUID uuid : java.util.Set.copyOf(DIRTY)) {
            PlayerAccount account = ACCOUNTS.get(uuid);
            if (account == null) { DIRTY.remove(uuid); continue; }

            String body = encode(account);
            if (body == null) { DIRTY.remove(uuid); continue; }

            // Straight into the queue rather than sent directly: the queue is what makes a
            // write survive the service being down, and routing everything through it means
            // there is only one code path to get right.
            OfflineQueue.enqueue("/accounts/" + uuid, body, uuid + ":" + account.revision());
            DIRTY.remove(uuid);
        }
        LocalMirror.save(localMirror, ACCOUNTS);
    }

    static java.util.Optional<PlayerAccount> decode(JsonElement json) {
        return PlayerAccount.CODEC.parse(JsonOps.INSTANCE, json).result();
    }

    static String encode(PlayerAccount account) {
        return PlayerAccount.CODEC.encodeStart(JsonOps.INSTANCE, account)
                .result().map(Object::toString).orElse(null);
    }

    public static Map<UUID, PlayerAccount> all() { return Map.copyOf(ACCOUNTS); }
}
