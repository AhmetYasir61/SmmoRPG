package com.smmorpg.sync;

import com.smmorpg.SmmoRPG;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * What the client currently knows about server-pushed content.
 *
 * <p>Textures arrive as PNG bytes and are uploaded into a dynamic texture at runtime, so
 * they never touch disk and never need a resource pack reload.
 */
public final class ClientContentCache {

    private static final Map<String, WeaponDefinition> WEAPONS = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static int revision = -1;

    private ClientContentCache() {}

    public static int revision() { return revision; }

    public static void accept(ContentSync.Payload payload) {
        if (payload.revision() == revision) return;
        revision = payload.revision();
        WEAPONS.clear();
        for (WeaponDefinition def : payload.weapons()) {
            WEAPONS.put(def.id(), def);
            if (def.hasTexture()) uploadTexture(def);
        }
        SmmoRPG.LOGGER.info("Received {} server-defined weapons (revision {}).",
                WEAPONS.size(), revision);
    }

    public static WeaponDefinition get(String id) { return WEAPONS.get(id); }

    public static ResourceLocation texture(String id) { return TEXTURES.get(id); }

    private static void uploadTexture(WeaponDefinition def) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var image = com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(def.texturePng()));
            var dynamic = new net.minecraft.client.renderer.texture.DynamicTexture(image);
            ResourceLocation rl = mc.getTextureManager().register(
                    "smmorpg_synced_" + def.id().replace(':', '_'), dynamic);
            TEXTURES.put(def.id(), rl);
        } catch (Exception e) {
            // A bad texture must never take the client down; the weapon just falls back
            // to its archetype's icon.
            SmmoRPG.LOGGER.warn("Could not decode synced texture for {}", def.id(), e);
        }
    }
}
