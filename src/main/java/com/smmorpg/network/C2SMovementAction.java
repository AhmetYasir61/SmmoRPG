package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A traversal action the client asked for. The server re-checks the contact conditions
 * before granting it, so a modified client cannot dash forever.
 */
public record C2SMovementAction(int action, float forward, float strafe) implements CustomPacketPayload {
    public static final int WALL_KICK = 0;
    public static final int AIR_JUMP = 1;
    public static final int DASH = 2;
    public static final int WALL_RUN = 3;

    public static final Type<C2SMovementAction> TYPE = new Type<>(SmmoRPG.id("movement_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMovementAction> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SMovementAction::action,
                    ByteBufCodecs.FLOAT, C2SMovementAction::forward,
                    ByteBufCodecs.FLOAT, C2SMovementAction::strafe,
                    C2SMovementAction::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
