package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * What the arena HUD draws: where you are in the climb and where you are in the wave.
 *
 * <p>Sent rather than worked out client-side because the level and the kill count are the
 * server's to know; a client that computed them would be a client that could be wrong
 * about them at exactly the moment they matter.
 */
public record S2CArenaStatus(boolean active,
                             int level,
                             int percent,
                             int kills,
                             int needed,
                             boolean resting) implements CustomPacketPayload {

    public static final S2CArenaStatus INACTIVE =
            new S2CArenaStatus(false, 0, 0, 0, 0, false);

    public static final Type<S2CArenaStatus> TYPE = new Type<>(SmmoRPG.id("arena_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CArenaStatus> CODEC =
            StreamCodec.of((buf, msg) -> {
                buf.writeBoolean(msg.active());
                buf.writeVarInt(msg.level());
                buf.writeVarInt(msg.percent());
                buf.writeVarInt(msg.kills());
                buf.writeVarInt(msg.needed());
                buf.writeBoolean(msg.resting());
            }, buf -> new S2CArenaStatus(
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
