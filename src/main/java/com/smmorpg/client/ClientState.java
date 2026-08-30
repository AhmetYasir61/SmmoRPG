package com.smmorpg.client;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.wound.WoundData;

import java.util.HashMap;
import java.util.Map;

/** Everything the client needs to draw, kept out of the render code itself. */
public final class ClientState {
    private ClientState() {}

    public static PlayerProgress progress = PlayerProgress.EMPTY;
    /** How far this player has climbed in the training arena. Server-owned; never set here. */
    public static int trainingLevel = 0;

    public static com.smmorpg.skill.SkillData skills = com.smmorpg.skill.SkillData.EMPTY;
    /** The player's account as the server last reported it. Never fetched by the client. */
    public static com.smmorpg.account.PlayerAccount account =
            com.smmorpg.account.PlayerAccount.fresh("", "");

    /** Wounds for every entity in render range, keyed by entity id. */
    private static final Map<Integer, WoundData> WOUNDS = new HashMap<>();

    public static WoundData wounds(int entityId) {
        return WOUNDS.getOrDefault(entityId, WoundData.EMPTY);
    }

    public static void putWounds(int entityId, WoundData data) {
        if (data.wounds().isEmpty() && data.severed().isEmpty()) WOUNDS.remove(entityId);
        else WOUNDS.put(entityId, data);
    }


    public static void clear() {
        WOUNDS.clear();
        progress = PlayerProgress.EMPTY;
        skills = com.smmorpg.skill.SkillData.EMPTY;
        account = com.smmorpg.account.PlayerAccount.fresh("", "");
    }
}
