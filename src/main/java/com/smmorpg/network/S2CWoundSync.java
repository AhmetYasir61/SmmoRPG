package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.wound.WoundData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells nearby clients exactly which cuts to draw on an entity. */
public record S2CWoundSync(int entityId, WoundData data) implements CustomPacketPayload {
    public static final Type<S2CWoundSync> TYPE = new Type<>(SmmoRPG.id("wound_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CWoundSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CWoundSync::entityId,
            ByteBufCodecs.fromCodec(WoundData.CODEC), S2CWoundSync::data,
            S2CWoundSync::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
