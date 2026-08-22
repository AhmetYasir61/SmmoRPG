package com.smmorpg.sync;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A weapon described entirely as data.
 *
 * <p>Carrying the model and texture inline is what lets the server hand a client a weapon
 * it has never seen. The payload is small — an item model is a few hundred bytes and a
 * texture is a PNG — and it is sent once per session, on join.
 */
public record WeaponDefinition(String id,
                               String archetype,
                               float reach,
                               float baseDamage,
                               float swingSpeed,
                               float slashFactor,
                               float staminaCost,
                               float bleedFactor,
                               String modelJson,
                               byte[] texturePng) {

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(WeaponDefinition::id),
            Codec.STRING.fieldOf("archetype").forGetter(WeaponDefinition::archetype),
            Codec.FLOAT.fieldOf("reach").forGetter(WeaponDefinition::reach),
            Codec.FLOAT.fieldOf("damage").forGetter(WeaponDefinition::baseDamage),
            Codec.FLOAT.fieldOf("swing_speed").forGetter(WeaponDefinition::swingSpeed),
            Codec.FLOAT.fieldOf("slash").forGetter(WeaponDefinition::slashFactor),
            Codec.FLOAT.fieldOf("stamina").forGetter(WeaponDefinition::staminaCost),
            Codec.FLOAT.fieldOf("bleed").forGetter(WeaponDefinition::bleedFactor),
            Codec.STRING.optionalFieldOf("model", "").forGetter(WeaponDefinition::modelJson),
            Codec.BYTE_BUFFER.optionalFieldOf("texture", java.nio.ByteBuffer.allocate(0))
                    .xmap(WeaponDefinition::toArray, java.nio.ByteBuffer::wrap)
                    .forGetter(WeaponDefinition::texturePng)
    ).apply(i, WeaponDefinition::new));

    private static byte[] toArray(java.nio.ByteBuffer buf) {
        byte[] out = new byte[buf.remaining()];
        buf.duplicate().get(out);
        return out;
    }

    public boolean hasTexture() { return texturePng != null && texturePng.length > 0; }
}
