package com.smmorpg.update;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.smmorpg.SmmoRPG;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps the installed jar current against a remote manifest.
 *
 * <p>Deliberately conservative about what it does on its own. It checks, it downloads to a
 * staging file, and it verifies the hash — but it never swaps a jar under a running game
 * and never restarts anything without the player saying yes. Minecraft loads its mods once,
 * at launch; replacing the jar in place mid-session cannot take effect and can corrupt the
 * run, so the applied update is always staged for the next start.
 */
public final class UpdateService {

    /** Where to look. Operators point this at their own endpoint. */
    public static volatile String manifestUrl = "";

    /** How often to re-check while the game is running. */
    public static volatile Duration checkInterval = Duration.ofMinutes(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile UpdateManifest pending;
    private static volatile Path stagedJar;

    private UpdateService() {}

    public static Optional<UpdateManifest> pending() { return Optional.ofNullable(pending); }

    public static boolean isStaged() { return stagedJar != null; }

    /** Fetches the manifest and records it if it describes something newer than us. */
    public static CompletableFuture<Optional<UpdateManifest>> check(String currentVersion) {
        if (manifestUrl.isBlank()) return CompletableFuture.completedFuture(Optional.empty());

        HttpRequest request = HttpRequest.newBuilder(URI.create(manifestUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET().build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        SmmoRPG.LOGGER.warn("Update check returned HTTP {}", response.statusCode());
                        return Optional.<UpdateManifest>empty();
                    }
                    var json = JsonParser.parseString(response.body());
                    var parsed = UpdateManifest.CODEC.parse(JsonOps.INSTANCE, json);
                    Optional<UpdateManifest> manifest = parsed.result();
                    if (manifest.isPresent() && manifest.get().isNewerThan(currentVersion)) {
                        pending = manifest.get();
                        return manifest;
                    }
                    return Optional.<UpdateManifest>empty();
                })
                .exceptionally(t -> {
                    // An unreachable endpoint must never block the game from starting.
                    SmmoRPG.LOGGER.warn("Update check failed: {}", t.toString());
                    return Optional.empty();
                });
    }

    /**
     * Downloads the pending update into the mods folder as a staged file. The running game
     * keeps using the old jar; the new one takes over on the next launch.
     */
    public static CompletableFuture<Boolean> download(Path modsDir) {
        UpdateManifest manifest = pending;
        if (manifest == null) return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path temp = Files.createTempFile("smmorpg-update-", ".jar.part");
                HttpRequest request = HttpRequest.newBuilder(URI.create(manifest.downloadUrl()))
                        .timeout(Duration.ofMinutes(10)).GET().build();

                HttpResponse<InputStream> response =
                        CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    Files.deleteIfExists(temp);
                    return false;
                }
                try (InputStream in = response.body()) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!sha256(temp).equalsIgnoreCase(manifest.sha256())) {
                    SmmoRPG.LOGGER.error("Update hash mismatch; discarding download.");
                    Files.deleteIfExists(temp);
                    return false;
                }

                Files.createDirectories(modsDir);
                Path target = modsDir.resolve("smmorpg-" + manifest.version() + ".jar");
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                stagedJar = target;
                SmmoRPG.LOGGER.info("Update {} staged at {}", manifest.version(), target);
                return true;
            } catch (Exception e) {
                SmmoRPG.LOGGER.error("Update download failed", e);
                return false;
            }
        });
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Removes the old jar so the staged one is the only copy on the next launch. Called
     * during shutdown, once nothing is reading from it any more.
     */
    public static void finalizeOnShutdown(Path oldJar) {
        if (stagedJar == null || oldJar == null) return;
        try {
            if (!Files.isSameFile(stagedJar, oldJar)) Files.deleteIfExists(oldJar);
        } catch (Exception e) {
            SmmoRPG.LOGGER.warn("Could not remove the superseded jar at {}", oldJar, e);
        }
    }
}
