package com.smmorpg.vault;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.CombatConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Nothing walks into your bag on its own.
 *
 * <p>Vanilla sweeps loot up the moment you step near it, which turns a fight's aftermath
 * into an accident: you clear a room and find your inventory full of things you never
 * chose. Here a drop sits where it fell until you deliberately take it, so what you are
 * carrying is always something you decided to carry.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class PickupRules {

    private PickupRules() {}

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!CombatConfig.CFG.manualPickup.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        // Crouching is the deliberate act. Walking past something is not.
        if (player.isCrouching()) return;

        // Anything the player threw a moment ago is theirs coming back, not loot; letting
        // that bounce off would make dropping an item feel broken.
        if (event.getItemEntity().getOwner() == player
                && event.getItemEntity().hasPickUpDelay()) {
            return;
        }

        event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
    }

    /** A tossed item keeps its owner briefly so the rule above can recognise it. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        event.getEntity().setPickUpDelay(40);
    }
}
