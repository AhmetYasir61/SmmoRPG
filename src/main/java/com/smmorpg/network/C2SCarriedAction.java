package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "Do this with what I am holding on the cursor."
 *
 * <p>The stack itself is not in the packet. The server already knows what the player has
 * picked up inside the open menu, and a client that could name the item would be a client
 * that could name any item.
 */
public record C2SCarriedAction(Action action) implements CustomPacketPayload {

    public enum Action { DEPOSIT, TRASH }

    public static final Type<C2SCarriedAction> TYPE = new Type<>(SmmoRPG.id("carried_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCarriedAction> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.idMapper(i -> Action.values()[i], Enum::ordinal),
                    C2SCarriedAction::action,
                    C2SCarriedAction::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
