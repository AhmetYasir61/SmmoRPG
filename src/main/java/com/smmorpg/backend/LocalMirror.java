package com.smmorpg.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * The copy on disk.
 *
 * <p>This is what makes "the service went away" survivable rather than catastrophic. The
 * server keeps a full local mirror of every account it has touched; if the service is down
 * at boot, play continues from the mirror and the queue catches the service up later. The
 * only way a player loses anything is if this file is deleted while the service is
 * unreachable — which is exactly the situation the queue and the mirror together are meant
 * to make rare.
 */
final class LocalMirror {

    private LocalMirror() {}

    static void load(Path file, Map<UUID, PlayerAccount> into) {
        if (file == null || !Files.exists(file)) return;
        try {
            JsonArray array = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
            int loaded = 0;
            for (var element : array) {
                var account = AccountService.decode(element);
                if (account.isPresent()) {
                    into.put(UUID.fromString(account.get().uuid()), account.get());
                    loaded++;
                }
            }
            SmmoRPG.LOGGER.info("Loaded {} accounts from the local mirror.", loaded);
        } catch (Exception e) {
            SmmoRPG.LOGGER.error("Could not read the account mirror at {}", file, e);
        }
    }

    static void save(Path file, Map<UUID, PlayerAccount> accounts) {
        if (file == null) return;
        try {
            JsonArray array = new JsonArray();
            for (PlayerAccount account : accounts.values()) {
                String json = AccountService.encode(account);
                if (json != null) array.add(JsonParser.parseString(json).getAsJsonObject());
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, array.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            SmmoRPG.LOGGER.error("Could not write the account mirror to {}", file, e);
        }
    }

    static JsonObject empty() { return new JsonObject(); }
}
