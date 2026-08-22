package com.smmorpg.sync;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side store of data-driven content.
 *
 * <p>Deliberately a plain map rather than a Minecraft registry: entries can be added,
 * replaced and removed while the server is running, which is the whole point — a new
 * weapon should reach players the moment it is added, not at the next restart.
 */
public final class ContentRegistry {

    private static final Map<String, WeaponDefinition> WEAPONS = new LinkedHashMap<>();
    /** Bumped on every change; clients compare it to know whether they are stale. */
    private static volatile int revision = 0;

    private ContentRegistry() {}

    public static synchronized void put(WeaponDefinition def) {
        WEAPONS.put(def.id(), def);
        revision++;
    }

    public static synchronized void remove(String id) {
        if (WEAPONS.remove(id) != null) revision++;
    }

    public static WeaponDefinition get(String id) { return WEAPONS.get(id); }

    public static Collection<WeaponDefinition> all() {
        return Collections.unmodifiableCollection(WEAPONS.values());
    }

    public static int revision() { return revision; }

    public static synchronized void clear() {
        WEAPONS.clear();
        revision++;
    }
}
