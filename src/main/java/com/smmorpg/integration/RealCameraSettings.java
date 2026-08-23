package com.smmorpg.integration;

import com.smmorpg.SmmoRPG;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

/**
 * Pins Real Camera to a working first-person setup.
 *
 * <p>Only the switches that decide whether you have a body at all are pinned. The camera
 * offsets, the bind targets and the smoothing are left exactly as Real Camera ships them:
 * its author tuned those against the vanilla rig and this pack has no better number to put
 * in their place.
 *
 * <p>What does get held is the set of flags that quietly turn the feature off — sneaking,
 * swimming, crawling. A first-person body that vanishes the moment you crouch is worse than
 * no first-person body, because it teaches you not to trust what you are looking at.
 *
 * <p>Isolated in its own class so Real Camera's classes are never touched when it is absent.
 */
final class RealCameraSettings {

    private RealCameraSettings() {}

    static void pinForFirstPerson() {
        ModConfig config = ConfigFile.config();
        if (config == null) return;

        boolean changed = false;

        if (!config.enabled) { config.enabled = true; changed = true; }
        if (!config.renderModel) { config.renderModel = true; changed = true; }
        if (!config.dynamicCrosshair) { config.dynamicCrosshair = true; changed = true; }

        // Classic mode is a fixed offset; binding mode fastens the camera to the model's
        // head vertices. Binding is the better-looking of the two, but Epic Fight replaces
        // the player renderer, and a bind that fails leaves you with no view at all — so
        // whichever mode the player has chosen is left alone, and only the disable-me
        // flags below are held.
        changed |= pinDisableFlags(config.classic);
        changed |= pinBindingFlags(config.binding);

        if (changed) {
            config.clamp();
            ConfigFile.save();
            SmmoRPG.LOGGER.info("Real Camera pinned to a first-person body setup.");
        }
    }

    private static boolean pinDisableFlags(ModConfig.Classic classic) {
        boolean changed = false;
        if (classic.disableWhenSneaking) { classic.disableWhenSneaking = false; changed = true; }
        if (classic.disableWhenSwimming) { classic.disableWhenSwimming = false; changed = true; }
        return changed;
    }

    private static boolean pinBindingFlags(ModConfig.Binding binding) {
        boolean changed = false;
        if (binding.disableWhenSneaking) { binding.disableWhenSneaking = false; changed = true; }
        if (binding.disableWhenSwimming) { binding.disableWhenSwimming = false; changed = true; }
        if (binding.disableWhenCrawling) { binding.disableWhenCrawling = false; changed = true; }
        // A failed bind is a one-line log entry, not something to interrupt a fight with.
        if (!binding.hideFailureMessage) { binding.hideFailureMessage = true; changed = true; }
        return changed;
    }
}
