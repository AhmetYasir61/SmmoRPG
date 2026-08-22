package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import com.smmorpg.loot.GearData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SmmoRPG.MOD_ID);

    public static final Supplier<DataComponentType<GearData>> GEAR = REGISTRY.register(
            "gear", () -> DataComponentType.<GearData>builder().persistent(GearData.CODEC).build());
}
