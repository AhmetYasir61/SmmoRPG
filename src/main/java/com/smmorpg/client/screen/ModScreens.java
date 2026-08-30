package com.smmorpg.client.screen;

import com.smmorpg.SmmoRPG;
import com.smmorpg.core.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Binds this mod's menus to the screens that draw them. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModScreens {

    private ModScreens() {}

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.VAULT.get(), VaultScreen::new);
    }
}
