package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.network.C2SStartTraining;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;
import java.util.Optional;

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
    private static final String LEVEL_NAME = com.smmorpg.training.TrainingArena.LEVEL_NAME;

    /** True while a request is held back waiting for the arena world to finish loading. */
    private static boolean pending;
    /** Ticks left to wait for the world before giving up on the pending request. */
    private static int pendingTimeout = 0;

    private TrainingLauncher() {}

    public static void enter() {
        Minecraft mc = Minecraft.getInstance();

        if (ClientNet.connected()) {
            ClientNet.sendToServer(new C2SStartTraining());
            return;
        }

        pending = true;
        pendingTimeout = 20 * 60;                 // a minute is plenty to load a world
        openArenaWorld(mc);
    }

    /**
     * Called every client tick. Fires the held request the moment the world is ready —
     * joining is asynchronous, so there is no single callback that reliably means
     * "the player exists and the server will listen".
     */
    public static void tick() {
        if (!pending) return;

        if (ClientNet.connected()) {
            pending = false;
            ClientNet.sendToServer(new C2SStartTraining());
            return;
        }

        if (--pendingTimeout <= 0) {
            pending = false;
            SmmoRPG.LOGGER.warn("Gave up waiting for the training world to load.");
        }
    }

    /** Cancels a held request, so backing out of world loading does not fire it later. */
    public static void cancel() { pending = false; }

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
                TrainingLauncher::arenaDimensions, mc.screen);
    }

    /**
     * A superflat overworld: bedrock, a little stone, and a clean surface. No decoration,
     * no structures and — the point — no lakes, so there is no water anywhere to fall into
     * or fight around.
     *
     * <p>Only the overworld is replaced. The nether and end stems come from the normal
     * preset untouched, because a world missing them is a world that breaks the moment
     * something tries to reach one.
     */
    private static WorldDimensions arenaDimensions(RegistryAccess registries) {
        FlatLevelGeneratorSettings flat = new FlatLevelGeneratorSettings(
                Optional.empty(),                                   // no structure sets
                registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS),
                List.of());                                         // no placed features

        List<FlatLayerInfo> layers = flat.getLayersInfo();
        layers.clear();
        layers.add(new FlatLayerInfo(1, Blocks.BEDROCK));
        layers.add(new FlatLayerInfo(2, Blocks.STONE));
        layers.add(new FlatLayerInfo(1, Blocks.SMOOTH_STONE));
        flat.updateLayers();
        // setDecoration() and setAddLakes() are opt-in; not calling them is what keeps the
        // world free of grass, trees and — above all — water.

        return WorldPresets.createNormalWorldDimensions(registries)
                .replaceOverworldGenerator(registries, new FlatLevelSource(flat));
    }

    public static Component levelName() { return Component.literal(LEVEL_NAME); }
}
