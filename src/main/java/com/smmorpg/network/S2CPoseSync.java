package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Broadcasts one player's animation state so the third-person rig other players see
 * matches, bone for bone, what that player is doing behind their own first-person camera.
 */
public record S2CPoseSync(int entityId, int animation, int frame,
                          float aimPitch, float leanX, float leanZ,
                          boolean aiming, boolean blocking) implements CustomPacketPayload {
    public static final Type<S2CPoseSync> TYPE = new Type<>(SmmoRPG.id("pose_sync"));

    /**
     * Written by hand rather than with {@code StreamCodec.composite}: that helper tops out
     * at six fields, and a pose carries eight.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPoseSync> CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeVarInt(value.entityId());
                buf.writeVarInt(value.animation());
                buf.writeVarInt(value.frame());
                buf.writeFloat(value.aimPitch());
                buf.writeFloat(value.leanX());
                buf.writeFloat(value.leanZ());
                buf.writeBoolean(value.aiming());
                buf.writeBoolean(value.blocking());
            }, buf -> new S2CPoseSync(
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readBoolean(),
                        buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
