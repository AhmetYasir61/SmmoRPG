package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the client how far it has climbed, so the training screen can say so. */
public record S2CTrainingLevel(int level) implements CustomPacketPayload {

    public static final Type<S2CTrainingLevel> TYPE = new Type<>(SmmoRPG.id("training_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTrainingLevel> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, S2CTrainingLevel::level,
                    S2CTrainingLevel::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
