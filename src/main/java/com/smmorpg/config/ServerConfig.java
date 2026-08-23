package com.smmorpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The operator's half of the mod.
 *
 * <p>One jar ships to everybody; what makes an install a ranked online server rather than a
 * singleplayer game is this file and nothing else. It is a SERVER config on purpose:
 * NeoForge keeps those on the server and never sends them to a client, which is the only
 * reason it is safe for the credentials below to live here.
 */
public final class ServerConfig {

    public static final ModConfigSpec SPEC;
    public static final ServerConfig CFG;

    // --- account service ---
    public final ModConfigSpec.ConfigValue<String> backendUrl;
    public final ModConfigSpec.ConfigValue<String> backendApiKey;
    public final ModConfigSpec.IntValue syncIntervalSeconds;

    // --- ranked play ---
    public final ModConfigSpec.BooleanValue rankedEnabled;
    public final ModConfigSpec.IntValue queueWidenSeconds;
    public final ModConfigSpec.IntValue matchDurationSeconds;
    public final ModConfigSpec.IntValue coinsPerWin;
    public final ModConfigSpec.IntValue coinsPerLoss;

    // --- store ---
    public final ModConfigSpec.ConfigValue<String> tebexSecret;
    public final ModConfigSpec.ConfigValue<String> tebexStoreUrl;
    public final ModConfigSpec.IntValue tebexPollSeconds;

    private ServerConfig(ModConfigSpec.Builder b) {
        b.push("account_service");
        backendUrl = b.comment("Base URL of the account service.",
                        "Leave empty to use the network's built-in address; set it only to",
                        "point a server at your own service instead.")
                .define("backendUrl", "");
        backendApiKey = b.comment("Shared secret sent as a bearer token.",
                        "This is the one thing that cannot be built into the mod: a secret",
                        "inside a downloadable jar is not a secret. It stays here, in the",
                        "SERVER config, which NeoForge never sends to a client.",
                        "Empty means this server is unranked and runs entirely locally.")
                .define("backendApiKey", "");
        syncIntervalSeconds = b.comment("How often to push dirty accounts and retry the",
                        "offline queue.")
                .defineInRange("syncIntervalSeconds", 30, 5, 3600);
        b.pop();

        b.push("ranked");
        rankedEnabled = b.comment("Turns on queues, Elo and the ladder. Off leaves the mod",
                        "as a singleplayer RPG with no matchmaking at all.")
                .define("rankedEnabled", false);
        queueWidenSeconds = b.comment("Every this many seconds in queue, the acceptable rating",
                        "gap widens. Small servers need this low or nobody ever matches.")
                .defineInRange("queueWidenSeconds", 10, 1, 600);
        matchDurationSeconds = b.comment("Hard time limit on a match. Running out is a draw.")
                .defineInRange("matchDurationSeconds", 300, 30, 3600);
        coinsPerWin = b.defineInRange("coinsPerWin", 100, 0, 1000000);
        coinsPerLoss = b.comment("Losing still pays something, or the ladder becomes a place",
                        "people are afraid to enter.")
                .defineInRange("coinsPerLoss", 25, 0, 1000000);
        b.pop();

        b.push("store");
        tebexSecret = b.comment("Tebex webstore secret. The mod never handles a card number:",
                        "checkout happens on Tebex, and the server only asks Tebex which",
                        "purchases are due and grants those.")
                .define("tebexSecret", "");
        tebexStoreUrl = b.comment("Public store URL players are sent to for checkout.")
                .define("tebexStoreUrl", "");
        tebexPollSeconds = b.comment("How often to ask Tebex for completed purchases.")
                .defineInRange("tebexPollSeconds", 60, 15, 3600);
        b.pop();
    }

    static {
        Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        CFG = pair.getLeft();
        SPEC = pair.getRight();
    }
}
