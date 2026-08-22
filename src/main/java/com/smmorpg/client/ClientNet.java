package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The one place client code sends a packet.
 *
 * <p>{@code PacketDistributor.sendToServer} throws if there is no connection, and there is
 * no connection on the title screen, during a disconnect, or in the frame or two around
 * either. A screen that can be opened from the main menu therefore cannot call it directly
 * without eventually crashing someone. Routing every send through here makes that
 * impossible rather than merely unlikely.
 */
public final class ClientNet {

    private ClientNet() {}

    /** Returns true if the packet actually went out. */
    public static boolean sendToServer(CustomPacketPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            SmmoRPG.LOGGER.debug("Dropped {} — not connected to a server.",
                    payload.type().id());
            return false;
        }
        PacketDistributor.sendToServer(payload);
        return true;
    }

    /** True when we are in a world and able to talk to its server. */
    public static boolean connected() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && mc.player != null && mc.level != null;
    }
}
