package com.smmorpg.api;

import com.smmorpg.item.WeaponClass;
import com.smmorpg.sync.WeaponDefinition;
import com.smmorpg.sync.ContentRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Adds weapons that live entirely in server data.
 *
 * <p>A weapon registered here has no client-side class. Its stats, model and texture are
 * shipped to every connecting client by {@link com.smmorpg.sync.ContentSync}, which is why
 * new weapons can be added to a live server without anybody reinstalling anything.
 */
public final class WeaponRegistrationApi {

    private WeaponRegistrationApi() {}

    /**
     * @param id        namespaced id; also the lang key suffix
     * @param base      the archetype whose feel the weapon inherits
     * @param modelJson the item model, as a JSON string
     * @param texturePng the texture, as raw PNG bytes
     */
    public static WeaponDefinition register(ResourceLocation id, WeaponClass base,
                                            String modelJson, byte[] texturePng) {
        WeaponDefinition def = new WeaponDefinition(id.toString(), base.key(),
                base.reach(), base.baseDamage(), base.swingSpeed(),
                base.slashFactor(), base.staminaCost(), base.bleedFactor(),
                modelJson, texturePng);
        ContentRegistry.put(def);
        return def;
    }

    /** Registers a fully specified weapon, ignoring the archetype defaults. */
    public static WeaponDefinition register(WeaponDefinition definition) {
        ContentRegistry.put(definition);
        return definition;
    }

    public static Collection<WeaponDefinition> registered() { return ContentRegistry.all(); }

    public static WeaponDefinition get(ResourceLocation id) { return ContentRegistry.get(id.toString()); }
}
