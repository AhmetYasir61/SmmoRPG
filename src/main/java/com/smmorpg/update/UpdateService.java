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
 * <p>The player decides, always. The service checks, downloads to a staging file and
 * verifies the hash on its own, but the last step — apply and restart, or wait — is a
 * button, never something that happens to you mid-fight.
 *
 * <p>Minecraft loads its mods once, at launch, so the staged jar cannot take effect in the
 * running process. "Apply and restart" therefore relaunches the game: it spawns a fresh
 * process from this one's own command line, then shuts this one down.
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

    /**
     * Relaunches the game so the staged jar is picked up, then returns true if the new
     * process actually started.
     *
     * <p>The command line is taken from this very process, so the relaunch inherits the
     * exact JVM arguments, classpath and game arguments the launcher used — there is no
     * second, guessed invocation that might come up wrong.
     *
     * <p>Returns false when the platform will not hand back a full command line (some
     * launchers and some JVM configurations hide it). The caller falls back to asking the
     * player to restart by hand rather than exiting into nothing.
     */
    public static boolean relaunch() {
        try {
            Optional<String[]> command = commandLine();
            if (command.isEmpty()) {
                SmmoRPG.LOGGER.warn("Cannot relaunch: this process does not expose its command line.");
                return false;
            }

            ProcessBuilder builder = new ProcessBuilder(command.get());
            builder.directory(new java.io.File(System.getProperty("user.dir")));
            // Detached: the new game must outlive the one that spawned it.
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.start();

            SmmoRPG.LOGGER.info("Relaunching for update {}", pending == null ? "?" : pending.version());
            return true;
        } catch (Exception e) {
            SmmoRPG.LOGGER.error("Relaunch failed", e);
            return false;
        }
    }

    /** This process's full command line, if the platform exposes it. */
    private static Optional<String[]> commandLine() {
        var info = ProcessHandle.current().info();
        Optional<String> executable = info.command();
        Optional<String[]> arguments = info.arguments();
        if (executable.isEmpty() || arguments.isEmpty()) return Optional.empty();

        String[] argv = arguments.get();
        String[] full = new String[argv.length + 1];
        full[0] = executable.get();
        System.arraycopy(argv, 0, full, 1, argv.length);
        return Optional.of(full);
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
