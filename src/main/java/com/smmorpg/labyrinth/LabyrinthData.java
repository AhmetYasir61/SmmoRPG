package com.smmorpg.labyrinth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the labyrinth remembers between sessions.
 *
 * <p>The old arena was rebuilt wherever the player happened to be standing, which is why
 * two of them could overlap and why the last session's stragglers ended up walled into
 * somebody else's floor. This is the fix: one labyrinth per world, laid out from the
 * origin, built once and never moved. What lives here is the part that cannot be
 * recomputed — which cells have actually been written into the world, and where each
 * player last saved.
 */
public class LabyrinthData extends SavedData {

    private static final String ID = "smmorpg_labyrinth";

    private final Set<Long> built = new HashSet<>();

    /** The save each player is currently living on, and the one before it. */
    private final Map<UUID, RunSave> current = new HashMap<>();
    private final Map<UUID, RunSave> previous = new HashMap<>();

    public static LabyrinthData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LabyrinthData::new, LabyrinthData::load), ID);
    }

    public boolean isBuilt(long cell) { return built.contains(cell); }

    public void markBuilt(long cell) {
        if (built.add(cell)) setDirty();
    }

    /** The save this player is living on, or null if they have never reached a safe cell. */
    public RunSave save(UUID player) { return current.get(player); }

    public RunSave priorSave(UUID player) { return previous.get(player); }

    /**
     * Records a new save, keeping the one it replaces.
     *
     * <p>Two are kept rather than one because falling back has to land somewhere: a player
     * who burns through a save's lives goes back to the state they were in when they set
     * it, which is only knowable if the older picture survived the newer one.
     */
    public void addSave(UUID player, RunSave save) {
        RunSave old = current.get(player);
        if (old != null) previous.put(player, old);
        current.put(player, save);
        setDirty();
    }

    /** Steps a player back onto their older save, which becomes the one they live on. */
    public RunSave fallBack(UUID player) {
        RunSave older = previous.remove(player);
        if (older == null) {
            // Nothing older exists, so the first save is also the floor: it is refilled
            // rather than taken away, or a bad run would end with nowhere to stand.
            RunSave now = current.get(player);
            if (now == null) return null;
            RunSave refilled = now.withLives(RunSave.LIVES);
            current.put(player, refilled);
            setDirty();
            return refilled;
        }
        current.put(player, older);
        setDirty();
        return older;
    }

    public void setLives(UUID player, int lives) {
        RunSave save = current.get(player);
        if (save == null) return;
        current.put(player, save.withLives(lives));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("built", built.stream().mapToLong(Long::longValue).toArray());

        tag.put("current", writeSaves(current));
        tag.put("previous", writeSaves(previous));
        return tag;
    }

    private static LabyrinthData load(CompoundTag tag, HolderLookup.Provider registries) {
        LabyrinthData data = new LabyrinthData();
        for (long cell : tag.getLongArray("built")) data.built.add(cell);

        readSaves(tag.getList("current", Tag.TAG_COMPOUND), data.current);
        readSaves(tag.getList("previous", Tag.TAG_COMPOUND), data.previous);
        return data;
    }

    private static ListTag writeSaves(Map<UUID, RunSave> saves) {
        ListTag list = new ListTag();
        saves.forEach((uuid, save) -> {
            CompoundTag entry = save.toTag();
            entry.putUUID("player", uuid);
            list.add(entry);
        });
        return list;
    }

    private static void readSaves(ListTag list, Map<UUID, RunSave> into) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            into.put(entry.getUUID("player"), RunSave.fromTag(entry));
        }
    }
}
