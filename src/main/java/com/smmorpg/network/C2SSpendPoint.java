package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SSpendPoint(String stat) implements CustomPacketPayload {
    public static final Type<C2SSpendPoint> TYPE = new Type<>(SmmoRPG.id("spend_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSpendPoint> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, C2SSpendPoint::stat,
            C2SSpendPoint::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
