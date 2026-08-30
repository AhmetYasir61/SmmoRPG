package com.smmorpg;

import com.mojang.logging.LogUtils;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModAttributes;
import com.smmorpg.core.ModCreativeTabs;
import com.smmorpg.core.ModDataComponents;
import com.smmorpg.core.ModItems;
import com.smmorpg.core.ModParticles;
import com.smmorpg.core.ModSounds;
import com.smmorpg.config.CombatConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * SmmoRPG - "Ultra Realistic Fighting" MMORPG combat overhaul.
 *
 * <p>Everything the user asked for lives under one mod id:
 * <ul>
 *   <li>Sekiro-style class selection on first join, persistent levels and loot.</li>
 *   <li>An FPS-first camera with a real first-person body, mirrored 1:1 onto the
 *       third-person rig so other players see exactly what you do.</li>
 *   <li>Locational hit detection with 60 distinct impact sounds.</li>
 *   <li>Persistent wounds: cuts, severed limbs, bleeding, and gradual healing.</li>
 *   <li>Camera shake and weapon recoil driven by the same impact data.</li>
 * </ul>
 */
@Mod(SmmoRPG.MOD_ID)
public class SmmoRPG {
    public static final String MOD_ID = "smmorpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmmoRPG(IEventBus modBus, ModContainer container) {
        ModSounds.REGISTRY.register(modBus);
        ModItems.REGISTRY.register(modBus);
        ModParticles.REGISTRY.register(modBus);
        ModAttributes.REGISTRY.register(modBus);
        ModAttachments.REGISTRY.register(modBus);
        ModDataComponents.REGISTRY.register(modBus);
        ModCreativeTabs.REGISTRY.register(modBus);
        com.smmorpg.core.ModMenus.REGISTRY.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, CombatConfig.SPEC);
        // SERVER, not COMMON: this one holds credentials and NeoForge keeps it off clients.
        container.registerConfig(ModConfig.Type.SERVER, com.smmorpg.config.ServerConfig.SPEC);

        // Epic Fight is a hard dependency, so it is always here — but registering from a
        // FMLCommonSetupEvent rather than the constructor means its own registries have
        // finished building before we hook them.
        modBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                event -> event.enqueueWork(com.smmorpg.integration.EpicFightBridge::register));

        LOGGER.info("SmmoRPG loaded: {} impact sounds indexed.", ModSounds.impactSoundCount());
    }

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
