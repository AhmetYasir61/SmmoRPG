package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.skill.SkillData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CSkillSync(SkillData data) implements CustomPacketPayload {
    public static final Type<S2CSkillSync> TYPE = new Type<>(SmmoRPG.id("skill_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSkillSync> CODEC =
            StreamCodec.composite(ByteBufCodecs.fromCodec(SkillData.CODEC), S2CSkillSync::data,
                    S2CSkillSync::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
