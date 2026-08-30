package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the server to open a training arena.
 *
 * <p>Carries no difficulty: the level the player has earned decides that, and a number
 * the client could put in this packet would be a number the client could cheat.
 */
public record C2SStartTraining() implements CustomPacketPayload {
    public static final Type<C2SStartTraining> TYPE = new Type<>(SmmoRPG.id("start_training"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStartTraining> CODEC =
            StreamCodec.unit(new C2SStartTraining());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
