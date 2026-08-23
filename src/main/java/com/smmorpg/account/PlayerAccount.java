package com.smmorpg.account;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about a player that belongs to the account rather than to a world.
 *
 * <p>This is the record the online service owns. A world can be deleted, a server can be
 * reinstalled, and none of it should cost anyone their rating or their vault — which is
 * why nothing here lives in level data.
 */
public record PlayerAccount(String uuid,
                            String name,
                            int elo,
                            int wins,
                            int losses,
                            long coins,
                            long premium,
                            List<VaultItem> vault,
                            long revision) {

    /** Where a new account starts. 1000 is the usual convention and it reads as "average". */
    public static final int STARTING_ELO = 1000;

    public static final Codec<PlayerAccount> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("uuid").forGetter(PlayerAccount::uuid),
            Codec.STRING.fieldOf("name").forGetter(PlayerAccount::name),
            Codec.INT.fieldOf("elo").forGetter(PlayerAccount::elo),
            Codec.INT.fieldOf("wins").forGetter(PlayerAccount::wins),
            Codec.INT.fieldOf("losses").forGetter(PlayerAccount::losses),
            Codec.LONG.fieldOf("coins").forGetter(PlayerAccount::coins),
            Codec.LONG.optionalFieldOf("premium", 0L).forGetter(PlayerAccount::premium),
            VaultItem.CODEC.listOf().fieldOf("vault").forGetter(PlayerAccount::vault),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(PlayerAccount::revision)
    ).apply(i, PlayerAccount::new));

    public static PlayerAccount fresh(String uuid, String name) {
        return new PlayerAccount(uuid, name, STARTING_ELO, 0, 0, 0L, 0L, List.of(), 0L);
    }

    public int matches() { return wins + losses; }

    public float winRate() { return matches() == 0 ? 0.0F : (float) wins / matches(); }

    /** Every mutation bumps the revision, which is what lets the sync resolve conflicts. */
    private PlayerAccount bumped(int elo, int wins, int losses, long coins, long premium,
                                 List<VaultItem> vault) {
        return new PlayerAccount(uuid, name, elo, wins, losses, coins, premium,
                vault, revision + 1);
    }

    public PlayerAccount withResult(int newElo, boolean won) {
        return bumped(newElo, wins + (won ? 1 : 0), losses + (won ? 0 : 1),
                coins, premium, vault);
    }

    public PlayerAccount withCoins(long delta) {
        return bumped(elo, wins, losses, Math.max(0L, coins + delta), premium, vault);
    }

    public PlayerAccount withPremium(long delta) {
        return bumped(elo, wins, losses, coins, Math.max(0L, premium + delta), vault);
    }

    /** Adds one stack, merging into an existing entry when the two are truly identical. */
    public PlayerAccount deposit(VaultItem item) {
        List<VaultItem> next = new ArrayList<>(vault);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).stacksWith(item)) {
                next.set(i, next.get(i).withCount(next.get(i).count() + item.count()));
                return bumped(elo, wins, losses, coins, premium, List.copyOf(next));
            }
        }
        next.add(item);
        return bumped(elo, wins, losses, coins, premium, List.copyOf(next));
    }

    /** Removes up to {@code amount} from the entry at {@code index}. */
    public PlayerAccount withdraw(int index, int amount) {
        if (index < 0 || index >= vault.size()) return this;
        List<VaultItem> next = new ArrayList<>(vault);
        VaultItem entry = next.get(index);

        int taken = Math.min(amount, entry.count());
        if (taken >= entry.count()) next.remove(index);
        else next.set(index, entry.withCount(entry.count() - taken));

        return bumped(elo, wins, losses, coins, premium, List.copyOf(next));
    }

    public PlayerAccount renamed(String newName) {
        return new PlayerAccount(uuid, newName, elo, wins, losses, coins, premium,
                vault, revision + 1);
    }
}
