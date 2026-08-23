package com.smmorpg.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.smmorpg.SmmoRPG;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The only thing in this mod that talks to the outside world — and it runs on the game
 * server, never on a client.
 *
 * <p>That split is deliberate. A client that could reach the account service directly could
 * also lie to it, and every player's machine would need the credential. Instead the client
 * asks the game server, the game server asks the service, and the answer comes back down as
 * a packet. One secret, in one place, held by the operator.
 */
public final class BackendClient {

    /** Base URL of the account service. Empty means everything runs offline. */
    public static volatile String baseUrl = "";
    /** Shared secret. Lives in the server's config and is never sent to a client. */
    public static volatile String apiKey = "";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Flipped by every call, so the sync loop knows whether it is worth draining the queue. */
    private static volatile boolean reachable;

    private BackendClient() {}

    public static boolean configured() { return !baseUrl.isBlank(); }

    public static boolean reachable() { return reachable; }

    public static CompletableFuture<Optional<JsonElement>> get(String path) {
        return send(request(path).GET().build());
    }

    public static CompletableFuture<Optional<JsonElement>> post(String path, String body) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    private static HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path)))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (!apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
        return builder;
    }

    private static CompletableFuture<Optional<JsonElement>> send(HttpRequest request) {
        if (!configured()) return CompletableFuture.completedFuture(Optional.empty());

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    reachable = true;
                    if (response.statusCode() / 100 != 2) {
                        SmmoRPG.LOGGER.warn("Account service returned HTTP {} for {}",
                                response.statusCode(), request.uri().getPath());
                        return Optional.<JsonElement>empty();
                    }
                    String body = response.body();
                    if (body == null || body.isBlank()) return Optional.<JsonElement>empty();
                    return Optional.of(JsonParser.parseString(body));
                })
                .exceptionally(t -> {
                    // Unreachable is a normal state, not an error: the queue exists for it.
                    reachable = false;
                    SmmoRPG.LOGGER.debug("Account service unreachable: {}", t.toString());
                    return Optional.empty();
                });
    }

    /** A cheap liveness probe the sync loop can run without moving any data. */
    public static CompletableFuture<Boolean> ping() {
        return get("/health").thenApply(Optional::isPresent);
    }
}
