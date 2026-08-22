package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client tells the server what its first-person body is doing; the server relays it. */
public record C2SPoseUpdate(int animation, int frame, float aimPitch,
                            float leanX, float leanZ,
                            boolean aiming, boolean blocking) implements CustomPacketPayload {
    public static final Type<C2SPoseUpdate> TYPE = new Type<>(SmmoRPG.id("pose_update"));

    /**
     * Written by hand rather than with {@code StreamCodec.composite}: that helper tops out
     * at six fields, and a pose carries seven.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPoseUpdate> CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeVarInt(value.animation());
                buf.writeVarInt(value.frame());
                buf.writeFloat(value.aimPitch());
                buf.writeFloat(value.leanX());
                buf.writeFloat(value.leanZ());
                buf.writeBoolean(value.aiming());
                buf.writeBoolean(value.blocking());
            }, buf -> new C2SPoseUpdate(
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readBoolean(),
                        buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
