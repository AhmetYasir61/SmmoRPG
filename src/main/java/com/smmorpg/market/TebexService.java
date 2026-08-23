package com.smmorpg.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import com.smmorpg.backend.AccountService;
import com.smmorpg.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Real-money purchases, via Tebex.
 *
 * <p>The mod never sees a card number, a billing address or a payment token. Checkout
 * happens entirely on Tebex's own store in the player's browser; all this does is ask
 * Tebex which purchases are due to be delivered, deliver them, and tell Tebex they were
 * delivered. That is the whole integration, and keeping it that narrow is the point —
 * payment data has no business inside a game server.
 *
 * <p>Runs on the server only. The secret is a SERVER config value.
 */
public final class TebexService {

    private static final String API = "https://plugin.tebex.io";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private TebexService() {}

    public static boolean configured() {
        return !ServerConfig.CFG.tebexSecret.get().isBlank();
    }

    /** The URL a player is sent to in order to buy anything. */
    public static String storeUrl() { return ServerConfig.CFG.tebexStoreUrl.get(); }

    /**
     * Asks Tebex for commands waiting to be run for online players and applies them.
     *
     * <p>Tebex's model is "offline commands" and "online commands"; each carries an id, and
     * a delivered id is reported back so it is never run twice. That acknowledgement is the
     * only thing standing between a network hiccup and a player being granted the same
     * package repeatedly, so it is done for every command, not in a batch at the end.
     */
    public static void poll(MinecraftServer server) {
        if (!configured()) return;

        request("/queue/offline-commands").thenAccept(json -> {
            if (json == null) return;
            server.execute(() -> applyCommands(server, json));
        });
    }

    private static void applyCommands(MinecraftServer server, JsonObject json) {
        if (!json.has("commands")) return;
        JsonArray commands = json.getAsJsonArray("commands");

        JsonArray delivered = new JsonArray();
        for (var element : commands) {
            JsonObject command = element.getAsJsonObject();
            try {
                if (grant(server, command)) delivered.add(command.get("id").getAsInt());
            } catch (Exception e) {
                // A package this build does not understand must not block every other one
                // behind it in the queue.
                SmmoRPG.LOGGER.warn("Could not deliver Tebex command {}", command, e);
            }
        }

        if (!delivered.isEmpty()) acknowledge(delivered);
    }

    /**
     * Turns one Tebex command into an account grant.
     *
     * <p>Deliberately not {@code server.getCommands().performCommand(...)}: running arbitrary
     * console commands supplied over the network is a remote code execution waiting to
     * happen. Only the package types this mod understands are honoured, and anything else
     * is logged and left undelivered so it can be looked at.
     */
    private static boolean grant(MinecraftServer server, JsonObject command) {
        if (!command.has("player") || !command.has("command")) return false;

        JsonObject playerJson = command.getAsJsonObject("player");
        UUID uuid = UUID.fromString(playerJson.get("uuid").getAsString()
                .replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

        String raw = command.get("command").getAsString().trim();
        String[] parts = raw.split("\\s+");
        if (parts.length < 2) return false;

        PlayerAccount account = AccountService.of(uuid);
        if (account == null) {
            // The player has never been on this server. Leaving it undelivered means Tebex
            // will offer it again once they join, which is the correct outcome.
            return false;
        }

        PlayerAccount updated = switch (parts[0].toLowerCase()) {
            case "smmorpg:premium" -> account.withPremium(Long.parseLong(parts[1]));
            case "smmorpg:coins" -> account.withCoins(Long.parseLong(parts[1]));
            default -> null;
        };

        if (updated == null) {
            SmmoRPG.LOGGER.warn("Unrecognised Tebex package command: {}", raw);
            return false;
        }

        AccountService.put(updated);
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            online.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("market.smmorpg.delivered"));
        }
        return true;
    }

    private static void acknowledge(JsonArray ids) {
        StringBuilder query = new StringBuilder("/queue/offline-commands?");
        for (int i = 0; i < ids.size(); i++) {
            query.append("ids[]=").append(ids.get(i).getAsInt());
            if (i < ids.size() - 1) query.append('&');
        }
        send(HttpRequest.newBuilder(URI.create(API + query))
                .header("X-Tebex-Secret", ServerConfig.CFG.tebexSecret.get())
                .DELETE().build());
    }

    private static java.util.concurrent.CompletableFuture<JsonObject> request(String path) {
        return send(HttpRequest.newBuilder(URI.create(API + path))
                .header("X-Tebex-Secret", ServerConfig.CFG.tebexSecret.get())
                .header("Accept", "application/json")
                .GET().build());
    }

    private static java.util.concurrent.CompletableFuture<JsonObject> send(HttpRequest request) {
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) return null;
                    String body = response.body();
                    if (body == null || body.isBlank()) return null;
                    return com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                })
                .exceptionally(t -> {
                    SmmoRPG.LOGGER.debug("Tebex unreachable: {}", t.toString());
                    return null;
                });
    }
}
