package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CProgressSync(PlayerProgress progress) implements CustomPacketPayload {
    public static final Type<S2CProgressSync> TYPE = new Type<>(SmmoRPG.id("progress_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CProgressSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(PlayerProgress.CODEC), S2CProgressSync::progress,
            S2CProgressSync::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
