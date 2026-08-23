package com.smmorpg.client.menu;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The last account the client was shown, kept on disk.
 *
 * <p>The hub opens before you have joined anything, and at that point there is no server to
 * ask. Rather than showing an empty page until you connect, the client remembers what the
 * server last told it and displays that, clearly marked as of when. Nothing here is
 * authoritative and nothing here is trusted — the moment you join a server it sends the
 * real account down and this is overwritten.
 *
 * <p>Which also means it is safe that a player can edit this file. Changing a number in it
 * changes what their own title screen says and nothing else; the server has never read it.
 */
public final class ProfileCache {

    private static PlayerAccount cached = PlayerAccount.fresh("", "");
    private static long updatedAt;
    private static boolean loaded;

    private ProfileCache() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("smmorpg").resolve("profile-cache.json");
    }

    public static PlayerAccount get() {
        if (!loaded) load();
        return cached;
    }

    /** True when this is remembered data rather than something a server just sent. */
    public static boolean stale() { return updatedAt == 0L || !onlineThisSession; }

    public static long updatedAt() {
        if (!loaded) load();
        return updatedAt;
    }

    private static boolean onlineThisSession;

    /** Called when a server pushes the real thing. */
    public static void accept(PlayerAccount account) {
        cached = account;
        updatedAt = System.currentTimeMillis();
        onlineThisSession = true;
        save();
    }

    public static void markOffline() { onlineThisSession = false; }

    private static void load() {
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            var json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            var root = json.getAsJsonObject();
            updatedAt = root.has("updated_at") ? root.get("updated_at").getAsLong() : 0L;
            PlayerAccount.CODEC.parse(JsonOps.INSTANCE, root.get("account"))
                    .result().ifPresent(a -> cached = a);
        } catch (Exception e) {
            // A cache that will not parse is a cache we simply do not have.
            SmmoRPG.LOGGER.debug("Could not read the profile cache: {}", e.toString());
        }
    }

    private static void save() {
        try {
            var account = PlayerAccount.CODEC.encodeStart(JsonOps.INSTANCE, cached).result();
            if (account.isEmpty()) return;

            var root = new com.google.gson.JsonObject();
            root.addProperty("updated_at", updatedAt);
            root.add("account", account.get());

            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            SmmoRPG.LOGGER.debug("Could not write the profile cache: {}", e.toString());
        }
    }
}
