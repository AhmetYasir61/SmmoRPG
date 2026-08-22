package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells every client watching an entity which clip it just started.
 *
 * <p>Only the start is sent, never the running time: both sides advance the same clip at
 * the same rate, so a per-tick position would be bandwidth spent on something both ends
 * already know.
 */
public record S2CPlayAnimation(int entityId, String clipId, float blendTicks)
        implements CustomPacketPayload {

    public static final Type<S2CPlayAnimation> TYPE = new Type<>(SmmoRPG.id("play_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayAnimation> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, S2CPlayAnimation::entityId,
                    ByteBufCodecs.STRING_UTF8, S2CPlayAnimation::clipId,
                    ByteBufCodecs.FLOAT, S2CPlayAnimation::blendTicks,
                    S2CPlayAnimation::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
