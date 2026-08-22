package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Opens the parry window. The server decides whether it was in time. */
public record C2SParry() implements CustomPacketPayload {
    public static final Type<C2SParry> TYPE = new Type<>(SmmoRPG.id("parry"));
    public static final C2SParry INSTANCE = new C2SParry();

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SParry> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
