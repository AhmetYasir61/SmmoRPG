package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SmmoRPG.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTRY.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.smmorpg.main"))
                    .icon(() -> new ItemStack(ModItems.KATANA.get()))
                    .displayItems((params, output) -> ModItems.ALL.forEach(h -> output.accept(h.get())))
                    .build());
}
