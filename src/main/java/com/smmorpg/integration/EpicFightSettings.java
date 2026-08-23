package com.smmorpg.integration;

import com.smmorpg.SmmoRPG;
import yesman.epicfight.config.ClientConfig;

/**
 * Holds Epic Fight's client settings where this pack needs them.
 *
 * <p>Only the compute-shader path is touched. Epic Fight uses it to skin its armatures on
 * the GPU, and on drivers where it misbehaves the result is a mangled or invisible model
 * rather than a crash — which is a miserable thing to debug, and exactly the sort of thing
 * a pack should decide once instead of leaving to every player.
 *
 * <p>Isolated in its own class so nothing here is loaded until it is actually called.
 */
final class EpicFightSettings {

    private EpicFightSettings() {}

    static void disableComputeShader() {
        if (!ClientConfig.ACTIVATE_COMPUTE_SHADER.get()) {
            // Already off, and writing the config file for nothing would churn it on
            // every world join.
            ClientConfig.activateComputeShader = false;
            return;
        }

        ClientConfig.ACTIVATE_COMPUTE_SHADER.set(false);
        ClientConfig.ACTIVATE_COMPUTE_SHADER.save();
        // Epic Fight reads a cached copy of this at render time, so the spec alone is not
        // enough — the field has to come down with it or the change lands next launch.
        ClientConfig.activateComputeShader = false;

        SmmoRPG.LOGGER.info("Epic Fight compute shader disabled.");
    }
}
