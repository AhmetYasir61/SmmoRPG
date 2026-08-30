package com.smmorpg.coop;

/**
 * Where the co-op relay lives.
 *
 * <p>Compiled in for the same reason the account service is: nobody should have to type an
 * address to play with a friend. There is no secret here to protect — the relay is a byte
 * pipe that authenticates nothing, and Mojang's session servers do the actual vouching at
 * both ends — so having the address in the jar costs nothing.
 *
 * <p>It can still be overridden at runtime with {@code -Dsmmorpg.relay=host:port}, which
 * is how you point a test client at a relay you are still setting up.
 */
public final class RelayEndpoints {

    /** Change this to your own relay before shipping the pack. */
    public static final String DEFAULT_HOST = "relay.pokewing.com";
    public static final int DEFAULT_PORT = 25599;

    private RelayEndpoints() {}

    public static String host() {
        String override = System.getProperty("smmorpg.relay");
        if (override == null || override.isBlank()) return DEFAULT_HOST;
        int colon = override.lastIndexOf(':');
        return colon < 0 ? override.trim() : override.substring(0, colon).trim();
    }

    public static int port() {
        String override = System.getProperty("smmorpg.relay");
        if (override == null || override.isBlank()) return DEFAULT_PORT;
        int colon = override.lastIndexOf(':');
        if (colon < 0) return DEFAULT_PORT;
        try {
            return Integer.parseInt(override.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    public static boolean configured() {
        return !host().isBlank() && !host().equals("relay.example.com");
    }
}
