package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** "Open my vault." Carries nothing: the server already knows whose vault that is. */
public record C2SOpenVault() implements CustomPacketPayload {

    public static final Type<C2SOpenVault> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SmmoRPG.MOD_ID, "open_vault"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SOpenVault> CODEC =
            StreamCodec.unit(new C2SOpenVault());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
