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

    public static final KeyMapping PARRY = new KeyMapping(
            "key.smmorpg.parry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping TOGGLE_VIEW = new KeyMapping(
            "key.smmorpg.toggle_view", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F5, CATEGORY);
    public static final KeyMapping OPEN_CHARACTER = new KeyMapping(
            "key.smmorpg.character", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping SHEATHE = new KeyMapping(
            "key.smmorpg.sheathe", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping DASH = new KeyMapping(
            "key.smmorpg.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, CATEGORY);
    public static final KeyMapping SKILLS = new KeyMapping(
            "key.smmorpg.skills", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping STUDIO = new KeyMapping(
            "key.smmorpg.studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DASH);
        event.register(SKILLS);
        event.register(STUDIO);
        event.register(PARRY);
        event.register(TOGGLE_VIEW);
        event.register(OPEN_CHARACTER);
        event.register(SHEATHE);
    }
}
