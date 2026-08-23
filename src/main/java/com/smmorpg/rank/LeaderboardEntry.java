package com.smmorpg.rank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One row of the ladder, as the online service returns it. */
public record LeaderboardEntry(int position, String uuid, String name,
                               int elo, int wins, int losses) {

    public static final Codec<LeaderboardEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("position").forGetter(LeaderboardEntry::position),
            Codec.STRING.fieldOf("uuid").forGetter(LeaderboardEntry::uuid),
            Codec.STRING.fieldOf("name").forGetter(LeaderboardEntry::name),
            Codec.INT.fieldOf("elo").forGetter(LeaderboardEntry::elo),
            Codec.INT.fieldOf("wins").forGetter(LeaderboardEntry::wins),
            Codec.INT.fieldOf("losses").forGetter(LeaderboardEntry::losses)
    ).apply(i, LeaderboardEntry::new));

    public Rank rank() { return Rank.of(elo, wins + losses); }

    public float winRate() {
        int total = wins + losses;
        return total == 0 ? 0.0F : (float) wins / total;
    }
}
