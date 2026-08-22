package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Asks the server to open a training arena at the requested difficulty. */
public record C2SStartTraining(int difficultyPercent) implements CustomPacketPayload {
    public static final Type<C2SStartTraining> TYPE = new Type<>(SmmoRPG.id("start_training"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStartTraining> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, C2SStartTraining::difficultyPercent,
                    C2SStartTraining::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
