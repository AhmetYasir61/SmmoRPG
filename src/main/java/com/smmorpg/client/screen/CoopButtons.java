package com.smmorpg.client.screen;

import com.smmorpg.SmmoRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The way in to co-op, put where people already look for it.
 *
 * <p>On the pause menu, next to "Open to LAN" — which is the button everyone tries first
 * and the button that does not work from another house.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class CoopButtons {

    private CoopButtons() {}

    @SubscribeEvent
    public static void onPauseMenu(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) return;
        if (Minecraft.getInstance().getSingleplayerServer() == null) return;

        event.addListener(Button.builder(Component.translatable("coop.smmorpg.button"),
                        b -> Minecraft.getInstance().setScreen(new CoopScreen(screen)))
                .bounds(screen.width / 2 - 102, screen.height / 4 + 128 - 16, 204, 20)
                .build());
    }
}
