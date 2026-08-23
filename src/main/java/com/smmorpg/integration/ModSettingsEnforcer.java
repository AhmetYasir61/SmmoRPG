package com.smmorpg.integration;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.CombatConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Holds the settings this pack depends on at the values it depends on.
 *
 * <p>Applied on every world join rather than once at startup: that is what makes it a pin
 * rather than a one-off nudge, and it survives a player changing a setting mid-session and
 * forgetting about it.
 *
 * <p>Every part of it can be switched off in SmmoRPG's own config. Silently rewriting
 * someone's settings forever, with no way to say no, is not something a mod should do —
 * so the enforcement is on by default and the door out is one line away.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class ModSettingsEnforcer {

    private ModSettingsEnforcer() {}

    @SubscribeEvent
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        apply();
    }

    public static void apply() {
        if (!CombatConfig.CFG.enforceModSettings.get()) return;

        if (CombatConfig.CFG.disableEpicFightComputeShader.get()) {
            try {
                EpicFightSettings.disableComputeShader();
            } catch (Throwable t) {
                // A settings preference is never worth taking the client down for.
                SmmoRPG.LOGGER.warn("Could not disable Epic Fight's compute shader", t);
            }
        }

        if (CombatConfig.CFG.pinRealCamera.get() && ModList.get().isLoaded("realcamera")) {
            try {
                RealCameraSettings.pinForFirstPerson();
            } catch (Throwable t) {
                SmmoRPG.LOGGER.warn("Could not pin Real Camera's settings", t);
            }
        }
    }
}
