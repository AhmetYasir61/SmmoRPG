package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The player's own account, pushed down.
 *
 * <p>This is the only way a client ever learns its rating, coins or vault. The client has
 * no route to the account service at all — it asks the server it is on, which is what keeps
 * the credential in one place and the numbers out of reach of the machine they are shown on.
 */
public record S2CAccountSync(PlayerAccount account) implements CustomPacketPayload {

    public static final Type<S2CAccountSync> TYPE = new Type<>(SmmoRPG.id("account_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAccountSync> CODEC =
            StreamCodec.composite(ByteBufCodecs.fromCodec(PlayerAccount.CODEC),
                    S2CAccountSync::account, S2CAccountSync::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
