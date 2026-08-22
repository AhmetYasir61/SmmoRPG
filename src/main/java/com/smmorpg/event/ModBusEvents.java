package com.smmorpg.event;

import com.smmorpg.SmmoRPG;
import com.smmorpg.core.ModAttributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/** Mod-bus setup: the custom attributes have to be attached to every living entity type. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, ModAttributes.POSTURE.getDelegate());
            event.add(type, ModAttributes.BLEED_RESISTANCE.getDelegate());
            event.add(type, ModAttributes.WOUND_REGENERATION.getDelegate());
        });
    }
}
