package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The felt half of a blow: how hard the camera shakes, how far the weapon kicks, and how
 * long the frame freezes. Sent to whoever should feel it.
 */
public record S2CImpactFeedback(float shake, float recoil, int hitStopTicks,
                                String location, String material,
                                boolean parried, boolean severed) implements CustomPacketPayload {
    public static final Type<S2CImpactFeedback> TYPE = new Type<>(SmmoRPG.id("impact_feedback"));

    /**
     * Written by hand rather than with {@code StreamCodec.composite}: that helper tops out
     * at six fields, and a blow carries seven.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CImpactFeedback> CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeFloat(value.shake());
                buf.writeFloat(value.recoil());
                buf.writeVarInt(value.hitStopTicks());
                buf.writeUtf(value.location());
                buf.writeUtf(value.material());
                buf.writeBoolean(value.parried());
                buf.writeBoolean(value.severed());
            }, buf -> new S2CImpactFeedback(
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readVarInt(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readBoolean(),
                        buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
