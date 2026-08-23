package com.smmorpg.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smmorpg.SmmoRPG;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * What could not be sent yet.
 *
 * <p>The rule this exists to keep: nothing a player earns is lost because the account
 * service happened to be down. Every write goes here first, is flushed to disk immediately,
 * and is only dropped once the service has acknowledged it. A server that is restarted
 * mid-outage picks the queue back up on the next boot and keeps trying.
 *
 * <p>Writes are therefore expected to be idempotent on the service's side — each carries an
 * id, and replaying one that already landed must be a no-op rather than a double grant.
 */
public final class OfflineQueue {

    private static final Deque<JsonObject> PENDING = new ArrayDeque<>();
    private static Path file;
    private static volatile boolean dirty;

    /** Beyond this the oldest entries are dropped rather than growing without bound. */
    private static final int MAX_ENTRIES = 20_000;

    private OfflineQueue() {}

    public static synchronized void load(Path storage) {
        file = storage;
        PENDING.clear();
        if (file == null || !Files.exists(file)) return;

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (var element : array) PENDING.add(element.getAsJsonObject());
            SmmoRPG.LOGGER.info("Restored {} unsent account writes from disk.", PENDING.size());
        } catch (Exception e) {
            SmmoRPG.LOGGER.error("Could not read the offline queue at {}", file, e);
        }
    }

    public static synchronized void enqueue(String path, String body, String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("path", path);
        entry.addProperty("body", body);
        entry.addProperty("queued_at", System.currentTimeMillis());

        PENDING.addLast(entry);
        while (PENDING.size() > MAX_ENTRIES) PENDING.pollFirst();

        dirty = true;
        flush();
    }

    /**
     * Sends what it can, oldest first, and stops at the first failure.
     *
     * <p>Order matters: these are account mutations, and applying a later one before an
     * earlier one would leave the account in a state that never actually existed.
     */
    public static synchronized void drain() {
        if (PENDING.isEmpty() || !BackendClient.configured()) return;

        Iterator<JsonObject> it = PENDING.iterator();
        int sent = 0;

        while (it.hasNext()) {
            JsonObject entry = it.next();
            boolean ok;
            try {
                ok = BackendClient.post(entry.get("path").getAsString(),
                                entry.get("body").getAsString())
                        .join().isPresent();
            } catch (Exception e) {
                ok = false;
            }
            if (!ok) break;

            it.remove();
            sent++;
            dirty = true;
        }

        if (sent > 0) {
            SmmoRPG.LOGGER.info("Flushed {} queued account writes ({} still waiting).",
                    sent, PENDING.size());
            flush();
        }
    }

    public static synchronized int size() { return PENDING.size(); }

    private static void flush() {
        if (!dirty || file == null) return;
        try {
            JsonArray array = new JsonArray();
            PENDING.forEach(array::add);
            Files.createDirectories(file.getParent());
            Files.writeString(file, array.toString(), StandardCharsets.UTF_8);
            dirty = false;
        } catch (Exception e) {
            SmmoRPG.LOGGER.error("Could not persist the offline queue to {}", file, e);
        }
    }
}
