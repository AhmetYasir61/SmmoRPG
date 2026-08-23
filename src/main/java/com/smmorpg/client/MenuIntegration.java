package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.screen.ModServerListScreen;
import com.smmorpg.client.screen.TrainingScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import com.smmorpg.config.CombatConfig;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Puts the mod's own front page in front of the vanilla title screen, and adds an update
 * entry to the pause menu.
 *
 * <p>The swap happens in {@code ScreenEvent.Opening} rather than by adding widgets to the
 * vanilla screen: the sketch this was built from replaces the page rather than decorating
 * it, and a screen you have replaced is one you no longer have to keep in sync.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class MenuIntegration {

    private MenuIntegration() {}

    /** Swaps the vanilla title screen for ours the first time it is opened. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!CombatConfig.CFG.customMainMenu.get()) return;
        if (!(event.getNewScreen() instanceof TitleScreen)) return;
        // Our own screen falls back to the vanilla one on close, so this must not catch it
        // on the way back or the two would bounce off each other forever.
        if (event.getCurrentScreen() instanceof com.smmorpg.client.menu.MainMenuScreen) return;

        event.setNewScreen(new com.smmorpg.client.menu.MainMenuScreen());
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen pause) {
            // Only appears when there is actually something to apply, so the pause menu
            // stays clean the rest of the time.
            com.smmorpg.update.UpdateService.pending().ifPresent(manifest ->
                    event.addListener(Button.builder(
                                    Component.translatable("update.smmorpg.pending_button"),
                                    b -> net.minecraft.client.Minecraft.getInstance().setScreen(
                                            new com.smmorpg.client.screen.UpdateScreen(manifest, pause)))
                            .bounds(pause.width / 2 - 102, pause.height / 4 + 144, 204, 20)
                            .build()));
        } else if (event.getScreen() instanceof SelectWorldScreen world) {
            // Also reachable from the world list, for a player already mid-session.
            event.addListener(Button.builder(Component.translatable("training.smmorpg.button"),
                            b -> net.minecraft.client.Minecraft.getInstance()
                                    .setScreen(new TrainingScreen()))
                    .bounds(world.width - 92, 6, 86, 20).build());
        }
    }

}
