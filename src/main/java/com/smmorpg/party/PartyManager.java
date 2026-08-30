package com.smmorpg.party;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is in whose party, and who has been asked.
 *
 * <p>Invitations are held rather than pushed: being dragged into somebody's dungeon by a
 * click you did not make is the thing every invite system exists to prevent.
 */
public final class PartyManager {

    /** An invitation goes stale rather than waiting forever in somebody's queue. */
    private static final long INVITE_TTL_MS = 120_000L;

    private static final Map<UUID, Party> BY_MEMBER = new ConcurrentHashMap<>();
    private static final Map<UUID, Invite> INVITES = new ConcurrentHashMap<>();

    private record Invite(UUID from, long expiresAt) {}

    private PartyManager() {}

    public static Party of(ServerPlayer player) { return BY_MEMBER.get(player.getUUID()); }

    /** The party this player leads or belongs to, creating one for them if they have none. */
    public static Party ensure(ServerPlayer player) {
        return BY_MEMBER.computeIfAbsent(player.getUUID(), Party::new);
    }

    public static void invite(ServerPlayer from, ServerPlayer to) {
        INVITES.put(to.getUUID(), new Invite(from.getUUID(), System.currentTimeMillis() + INVITE_TTL_MS));
    }

    /** The party the invitation is for, or null when there is nothing valid to accept. */
    public static Party accept(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        Invite invite = INVITES.remove(player.getUUID());
        if (invite == null || invite.expiresAt() < System.currentTimeMillis()) return null;

        ServerPlayer host = server.getPlayerList().getPlayer(invite.from());
        if (host == null) return null;

        Party party = ensure(host);
        if (party.full()) return null;

        leave(player);
        if (!party.add(player.getUUID())) return null;

        BY_MEMBER.put(player.getUUID(), party);
        return party;
    }

    public static void leave(ServerPlayer player) {
        Party party = BY_MEMBER.remove(player.getUUID());
        if (party == null) return;

        party.remove(player.getUUID());
        // A party of one is just a person, and keeping it around would leave the leader
        // unable to be invited anywhere else.
        if (party.size() <= 1) {
            for (UUID id : party.members()) BY_MEMBER.remove(id);
        }
    }

    /** Everyone in the same party as this player, including them. Never empty. */
    public static java.util.List<ServerPlayer> group(ServerPlayer player) {
        Party party = of(player);
        if (party == null || player.getServer() == null) return java.util.List.of(player);

        var online = party.online(player.getServer());
        return online.isEmpty() ? java.util.List.of(player) : online;
    }

    public static Map<UUID, Party> all() { return new HashMap<>(BY_MEMBER); }
}
