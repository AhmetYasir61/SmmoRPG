package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.network.C2SStartTraining;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Gets the player into a training arena from wherever they pressed the button.
 *
 * <p>From inside a world it is one packet. From the title screen there is no server to send
 * it to, so a dedicated singleplayer world is opened first (created on the first visit,
 * reused after) and the request is held until the player is actually in it.
 */
public final class TrainingLauncher {

    /** The dedicated arena world. Kept out of the normal world list by its own id. */
    private static final String LEVEL_ID = "smmorpg_training";
    private static final String LEVEL_NAME = "SmmoRPG Training";

    /** Difficulty waiting to be requested once we are in a world; -1 for nothing pending. */
    private static int pendingDifficulty = -1;
    /** Ticks left to wait for the world before giving up on the pending request. */
    private static int pendingTimeout = 0;

    private TrainingLauncher() {}

    public static void enter(int difficultyPercent) {
        Minecraft mc = Minecraft.getInstance();

        if (ClientNet.connected()) {
            ClientNet.sendToServer(new C2SStartTraining(difficultyPercent));
            return;
        }

        pendingDifficulty = difficultyPercent;
        pendingTimeout = 20 * 60;                 // a minute is plenty to load a world
        openArenaWorld(mc);
    }

    /**
     * Called every client tick. Fires the held request the moment the world is ready —
     * joining is asynchronous, so there is no single callback that reliably means
     * "the player exists and the server will listen".
     */
    public static void tick() {
        if (pendingDifficulty < 0) return;

        if (ClientNet.connected()) {
            int difficulty = pendingDifficulty;
            pendingDifficulty = -1;
            ClientNet.sendToServer(new C2SStartTraining(difficulty));
            return;
        }

        if (--pendingTimeout <= 0) {
            pendingDifficulty = -1;
            SmmoRPG.LOGGER.warn("Gave up waiting for the training world to load.");
        }
    }

    /** Cancels a held request, so backing out of world loading does not fire it later. */
    public static void cancel() { pendingDifficulty = -1; }

    private static void openArenaWorld(Minecraft mc) {
        var flows = mc.createWorldOpenFlows();

        if (mc.getLevelSource().levelExists(LEVEL_ID)) {
            flows.openWorld(LEVEL_ID, TrainingLauncher::cancel);
            return;
        }

        GameRules rules = new GameRules();
        // The arena is for practising fights, not for losing your gear to one.
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, null);

        LevelSettings settings = new LevelSettings(
                LEVEL_NAME, GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                rules, WorldDataConfiguration.DEFAULT);

        flows.createFreshLevel(LEVEL_ID, settings, WorldOptions.defaultWithRandomSeed(),
                TrainingLauncher::normalDimensions, mc.screen);
    }

    private static net.minecraft.world.level.levelgen.WorldDimensions normalDimensions(
            RegistryAccess registries) {
        return WorldPresets.createNormalWorldDimensions(registries);
    }

    public static Component levelName() { return Component.literal(LEVEL_NAME); }
}
