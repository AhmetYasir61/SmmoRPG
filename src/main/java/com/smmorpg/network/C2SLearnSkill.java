package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SLearnSkill(String skillId) implements CustomPacketPayload {
    public static final Type<C2SLearnSkill> TYPE = new Type<>(SmmoRPG.id("learn_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SLearnSkill> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, C2SLearnSkill::skillId,
                    C2SLearnSkill::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
