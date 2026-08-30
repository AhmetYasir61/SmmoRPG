package com.smmorpg.coop;

import com.smmorpg.SmmoRPG;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Joining a friend's game through the relay.
 *
 * <p>Minecraft cannot be told "connect to this address, and by the way mention this code",
 * so instead this opens a door on the player's own machine. The game connects to
 * {@code 127.0.0.1}; this end dials the relay, says the code, and joins the two. As far as
 * the game is concerned it is talking to a server on this computer, which is exactly the
 * kind of lie a tunnel is supposed to tell.
 *
 * <p>One connection per join, and the door closes behind it. A listener left open would be
 * a way into somebody's friend's world long after they stopped playing together.
 */
public final class CoopGuest {

    private static volatile CoopGuest active;

    private final ServerSocket door;
    private final String code;
    private volatile boolean running = true;

    private CoopGuest(ServerSocket door, String code) {
        this.door = door;
        this.code = code;
    }

    /**
     * Opens the local door for one join.
     *
     * @return the address to hand to Minecraft, or null if the door could not be opened
     */
    public static String open(String code) {
        close();

        try {
            // Loopback only. Binding this to the network would put a way into a friend's
            // world on the local LAN, which is nobody's idea of a co-op feature.
            ServerSocket door = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            CoopGuest guest = new CoopGuest(door, code.trim().toUpperCase(java.util.Locale.ROOT));
            active = guest;
            guest.accept();
            return "127.0.0.1:" + door.getLocalPort();
        } catch (IOException e) {
            SmmoRPG.LOGGER.warn("Could not open the local co-op door", e);
            return null;
        }
    }

    public static void close() {
        CoopGuest guest = active;
        active = null;
        if (guest == null) return;

        guest.running = false;
        try {
            guest.door.close();
        } catch (IOException ignored) {
            // Closing a closed door is not a problem worth reporting.
        }
    }

    private void accept() {
        Thread thread = new Thread(() -> {
            try (ServerSocket listener = door) {
                Socket fromGame = listener.accept();
                fromGame.setTcpNoDelay(true);

                Socket toRelay = new Socket();
                toRelay.setTcpNoDelay(true);
                toRelay.connect(new InetSocketAddress(
                        RelayEndpoints.host(), RelayEndpoints.port()), 10_000);

                OutputStream out = toRelay.getOutputStream();
                out.write(("{\"role\":\"guest\",\"code\":\"" + code + "\"}\n")
                        .getBytes(StandardCharsets.UTF_8));
                out.flush();

                Pipe.join("guest", fromGame, toRelay);
            } catch (IOException e) {
                if (running) SmmoRPG.LOGGER.warn("Co-op join failed", e);
            } finally {
                running = false;
                if (active == this) active = null;
            }
        }, "smmorpg-coop-guest");

        thread.setDaemon(true);
        thread.start();
    }
}
