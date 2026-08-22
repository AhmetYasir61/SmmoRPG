package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> REGISTRY =
            DeferredRegister.create(Registries.ATTRIBUTE, SmmoRPG.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> BLEED_RESISTANCE = REGISTRY.register(
            "bleed_resistance",
            () -> new RangedAttribute("attribute.smmorpg.bleed_resistance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> POSTURE = REGISTRY.register(
            "posture",
            () -> new RangedAttribute("attribute.smmorpg.posture", 100.0D, 1.0D, 4096.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> WOUND_REGENERATION = REGISTRY.register(
            "wound_regeneration",
            () -> new RangedAttribute("attribute.smmorpg.wound_regeneration", 1.0D, 0.0D, 64.0D).setSyncable(true));
}
