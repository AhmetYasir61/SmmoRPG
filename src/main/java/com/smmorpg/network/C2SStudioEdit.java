package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** One field of one animation profile, edited in the studio panel. Operator-only. */
public record C2SStudioEdit(String profileId, String field, float value) implements CustomPacketPayload {
    public static final Type<C2SStudioEdit> TYPE = new Type<>(SmmoRPG.id("studio_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStudioEdit> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, C2SStudioEdit::profileId,
                    ByteBufCodecs.STRING_UTF8, C2SStudioEdit::field,
                    ByteBufCodecs.FLOAT, C2SStudioEdit::value,
                    C2SStudioEdit::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
