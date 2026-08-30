package com.smmorpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.smmorpg.SmmoRPG;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class Keybinds {
    private static final String CATEGORY = "key.categories.smmorpg";

    public static final KeyMapping OPEN_CHARACTER = new KeyMapping(
            "key.smmorpg.character", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    public static final KeyMapping SKILLS = new KeyMapping(
            "key.smmorpg.skills", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping TRAINING = new KeyMapping(
            "key.smmorpg.training", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, CATEGORY);

    public static final KeyMapping VAULT = new KeyMapping(
            "key.smmorpg.vault", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SKILLS);
        event.register(TRAINING);
        event.register(OPEN_CHARACTER);
        event.register(VAULT);
    }
}
