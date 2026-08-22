package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.screen.ModServerListScreen;
import com.smmorpg.client.screen.TrainingScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Adds the mod's entry points to the vanilla menus: a square Training button beside
 * Singleplayer, and a server browser beside Multiplayer.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class MenuIntegration {

    private MenuIntegration() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen title) {
            addTitleButtons(event, title);
        } else if (event.getScreen() instanceof SelectWorldScreen world) {
            // Also reachable from the world list, for a player already mid-session.
            event.addListener(Button.builder(Component.translatable("training.smmorpg.button"),
                            b -> net.minecraft.client.Minecraft.getInstance()
                                    .setScreen(new TrainingScreen()))
                    .bounds(world.width - 92, 6, 86, 20).build());
        }
    }

    private static void addTitleButtons(ScreenEvent.Init.Post event, TitleScreen title) {
        // Find the Singleplayer button so the square Training button sits right beside it.
        int anchorX = title.width / 2 - 100;
        int anchorY = title.height / 4 + 48;

        for (var child : title.children()) {
            if (child instanceof Button button
                    && button.getMessage().getString()
                        .equals(Component.translatable("menu.singleplayer").getString())) {
                anchorX = button.getX();
                anchorY = button.getY();
                break;
            }
        }

        // A square button, deliberately the same height as the row it sits on.
        event.addListener(Button.builder(Component.translatable("training.smmorpg.button_short"),
                        b -> net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new TrainingScreen()))
                .bounds(anchorX + 204, anchorY, 20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("training.smmorpg.button_tooltip")))
                .build());

        event.addListener(Button.builder(Component.translatable("servers.smmorpg.button"),
                        b -> net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new ModServerListScreen(title)))
                .bounds(anchorX + 204, anchorY + 24, 20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("servers.smmorpg.button_tooltip")))
                .build());
    }
}
