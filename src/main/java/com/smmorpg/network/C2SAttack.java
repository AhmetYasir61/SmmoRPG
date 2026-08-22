package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "I pressed attack." Not "I hit something" — the server decides that when the animation's
 * damage window comes around, which is what stops a modified client from claiming hits.
 */
public record C2SAttack(boolean heavy) implements CustomPacketPayload {
    public static final Type<C2SAttack> TYPE = new Type<>(SmmoRPG.id("attack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAttack> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, C2SAttack::heavy, C2SAttack::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
