package com.smmorpg.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** An {@link Affix} with the magnitude it actually rolled at. */
public record RolledAffix(String affix, float power) {
    public static final Codec<RolledAffix> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("affix").forGetter(RolledAffix::affix),
            Codec.FLOAT.fieldOf("power").forGetter(RolledAffix::power)
    ).apply(i, RolledAffix::new));

    public Affix type() { return Affix.byKey(affix); }

    /** Rolled magnitude as a percentage, for the tooltip. */
    public int percent() { return Math.round(power * 100.0F); }
}
