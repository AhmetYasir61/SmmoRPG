package com.smmorpg.client;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.client.render.PoseState;
import com.smmorpg.wound.WoundData;

import java.util.HashMap;
import java.util.Map;

/** Everything the client needs to draw, kept out of the render code itself. */
public final class ClientState {
    private ClientState() {}

    public static ViewMode viewMode = ViewMode.FIRST_PERSON;
    public static PlayerProgress progress = PlayerProgress.EMPTY;
    public static com.smmorpg.skill.SkillData skills = com.smmorpg.skill.SkillData.EMPTY;

    /** Wounds for every entity in render range, keyed by entity id. */
    private static final Map<Integer, WoundData> WOUNDS = new HashMap<>();
    /** Animation state for every player in render range — the FPS/TPS mirror. */
    private static final Map<Integer, PoseState> POSES = new HashMap<>();

    public static WoundData wounds(int entityId) {
        return WOUNDS.getOrDefault(entityId, WoundData.EMPTY);
    }

    public static void putWounds(int entityId, WoundData data) {
        if (data.wounds().isEmpty() && data.severed().isEmpty()) WOUNDS.remove(entityId);
        else WOUNDS.put(entityId, data);
    }

    public static PoseState pose(int entityId) {
        return POSES.computeIfAbsent(entityId, id -> new PoseState());
    }

    public static void clear() {
        WOUNDS.clear();
        POSES.clear();
        progress = PlayerProgress.EMPTY;
        skills = com.smmorpg.skill.SkillData.EMPTY;
    }
}
