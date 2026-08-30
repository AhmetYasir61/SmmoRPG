package com.smmorpg.party;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.core.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A group of people running the same labyrinth.
 *
 * <p>Sixteen, not four. A dungeon that caps at four is a dungeon you play with the same
 * three people forever; the interesting version is the one where a guild can walk in
 * together and the building has to answer for it.
 */
public class Party {

    public static final int MAX = 16;

    private final UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID leader() { return leader; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public int size() { return members.size(); }
    public boolean full() { return members.size() >= MAX; }
    public boolean has(UUID player) { return members.contains(player); }

    public boolean add(UUID player) {
        if (full()) return false;
        return members.add(player);
    }

    public boolean remove(UUID player) { return members.remove(player); }

    /** The online members, in join order. */
    public List<ServerPlayer> online(net.minecraft.server.MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) out.add(player);
        }
        return out;
    }

    /**
     * How much harder the labyrinth gets for having this many people in it.
     *
     * <p>Every member past the first adds 15% of their own strength to what the dungeon has
     * to answer — deliberately less than the whole of it. Scaling with the full sum would
     * make a group of sixteen exactly as hard as one person and turn co-op into a formality;
     * scaling with nothing would turn it into a walk. Fifteen percent leaves numbers as a
     * real advantage while still making a crowd worth taking seriously.
     */
    public static float pressure(List<ServerPlayer> members) {
        if (members.size() <= 1) return 1.0F;

        float leadPower = 0.0F;
        float rest = 0.0F;
        for (ServerPlayer player : members) {
            float power = powerOf(player);
            if (power > leadPower) {
                rest += leadPower;
                leadPower = power;
            } else {
                rest += power;
            }
        }
        if (leadPower <= 0.0F) return 1.0F;

        return 1.0F + (rest / leadPower) * 0.15F;
    }

    /** One player's contribution: their level and what they have put into their stats. */
    private static float powerOf(ServerPlayer player) {
        PlayerProgress progress = player.getData(ModAttachments.PROGRESS.get());
        return 1.0F + progress.level() * 0.05F
                + (progress.strength() + progress.agility()
                + progress.vitality() + progress.spirit()) * 0.01F;
    }
}
