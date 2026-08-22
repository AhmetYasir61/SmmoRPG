package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.studio.AnimationProfile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Pushes studio-edited animation profiles to clients, live. */
public record S2CAnimationSync(int revision, List<AnimationProfile> profiles) implements CustomPacketPayload {
    public static final Type<S2CAnimationSync> TYPE = new Type<>(SmmoRPG.id("animation_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAnimationSync> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, S2CAnimationSync::revision,
                    ByteBufCodecs.fromCodec(AnimationProfile.CODEC.listOf()), S2CAnimationSync::profiles,
                    S2CAnimationSync::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
