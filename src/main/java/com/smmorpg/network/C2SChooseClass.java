package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SChooseClass(String classKey) implements CustomPacketPayload {
    public static final Type<C2SChooseClass> TYPE = new Type<>(SmmoRPG.id("choose_class"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SChooseClass> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, C2SChooseClass::classKey,
            C2SChooseClass::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
