package com.smmorpg.sync;

import com.smmorpg.SmmoRPG;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Pushes server-registered content down to clients.
 *
 * <p>Sent once on join and again whenever the registry's revision changes, so a weapon
 * added mid-session appears in every connected player's hands without a reconnect.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class ContentSync {

    /** The whole content set in one payload. */
    public record Payload(int revision, List<WeaponDefinition> weapons) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(SmmoRPG.id("content_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::revision,
                ByteBufCodecs.fromCodec(WeaponDefinition.CODEC.listOf()), Payload::weapons,
                Payload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static int lastBroadcastRevision = -1;

    private ContentSync() {}

    public static Payload snapshot() {
        return new Payload(ContentRegistry.revision(), List.copyOf(ContentRegistry.all()));
    }

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot());
    }

    /** Called from the server tick; broadcasts only when something actually changed. */
    public static void broadcastIfChanged(net.minecraft.server.MinecraftServer server) {
        int rev = ContentRegistry.revision();
        if (rev == lastBroadcastRevision) return;
        lastBroadcastRevision = rev;
        Payload payload = snapshot();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sendTo(sp);
    }
}
