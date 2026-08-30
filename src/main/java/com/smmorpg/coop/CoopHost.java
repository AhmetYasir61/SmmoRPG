package com.smmorpg.coop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smmorpg.SmmoRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Opening your own game to friends, without opening a port.
 *
 * <p>Minecraft can already publish a singleplayer world to the local network. What it
 * cannot do is make that reachable from anyone else's house, and on most home connections
 * no amount of router configuration will help — behind carrier-grade NAT there is no port
 * to forward.
 *
 * <p>So the game keeps listening only on this machine, and this dials out to the relay and
 * holds one connection open. When a friend turns up with the code, the relay says so, and
 * this dials out a second time and staples that socket to the local one. Every byte of the
 * game protocol crosses untouched; the relay never learns what it is carrying.
 */
public final class CoopHost {

    private static volatile CoopHost active;

    private final Socket control;
    private final int localPort;
    private volatile String code;
    private volatile boolean running = true;

    private CoopHost(Socket control, int localPort) {
        this.control = control;
        this.localPort = localPort;
    }

    public static CoopHost active() { return active; }

    public static String code() {
        CoopHost host = active;
        return host == null ? null : host.code;
    }

    public static boolean hosting() {
        CoopHost host = active;
        return host != null && host.running && host.code != null;
    }

    /**
     * Publishes the open world to the local machine and starts the tunnel.
     *
     * @return the port the game is now listening on, or -1 if it refused to publish
     */
    public static int start() {
        stop();

        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return -1;

        int port = server.getPort();
        if (port <= 0) {
            // publishServer is what turns a singleplayer world into one that accepts
            // connections at all; without it there is nothing for the tunnel to reach.
            if (!server.publishServer(GameType.SURVIVAL, false, 0)) return -1;
            port = server.getPort();
        }
        if (port <= 0) return -1;

        try {
            Socket socket = dial();
            CoopHost host = new CoopHost(socket, port);
            active = host;
            host.startControlLoop();
            return port;
        } catch (IOException e) {
            SmmoRPG.LOGGER.warn("Could not reach the co-op relay", e);
            return -1;
        }
    }

    public static void stop() {
        CoopHost host = active;
        active = null;
        if (host == null) return;

        host.running = false;
        Pipe.close(host.control);
    }

    private static Socket dial() throws IOException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(RelayEndpoints.host(), RelayEndpoints.port()), 10_000);
        return socket;
    }

    private void startControlLoop() {
        Thread thread = new Thread(() -> {
            try {
                send(control, "{\"role\":\"host\",\"name\":\""
                        + Minecraft.getInstance().getUser().getName().replace('"', '\'') + "\"}");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(control.getInputStream(), StandardCharsets.UTF_8));

                String line;
                while (running && (line = reader.readLine()) != null) {
                    JsonObject message = JsonParser.parseString(line).getAsJsonObject();
                    String op = message.has("op") ? message.get("op").getAsString() : "";

                    switch (op) {
                        case "ready" -> {
                            code = message.get("code").getAsString();
                            SmmoRPG.LOGGER.info("Co-op session open. Code: {}", code);
                        }
                        case "connect" -> dialBack(message.get("id").getAsString());
                        case "ping" -> send(control, "{\"op\":\"pong\"}");
                        default -> { }
                    }
                }
            } catch (Exception e) {
                if (running) SmmoRPG.LOGGER.warn("Co-op relay connection dropped", e);
            } finally {
                running = false;
                code = null;
                if (active == this) active = null;
                Pipe.close(control);
            }
        }, "smmorpg-coop-host");

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * A friend is waiting at the relay: open a socket to each side and join them.
     *
     * <p>Done on its own thread because both connects can block, and the control loop has
     * to stay free to hear about the next friend.
     */
    private void dialBack(String id) {
        Thread thread = new Thread(() -> {
            Socket toRelay = null;
            Socket toGame = null;
            try {
                toRelay = dial();
                send(toRelay, "{\"role\":\"hostdata\",\"id\":\"" + id.replace('"', ' ') + "\"}");

                toGame = new Socket();
                toGame.setTcpNoDelay(true);
                toGame.connect(new InetSocketAddress("127.0.0.1", localPort), 10_000);

                Pipe.join("host", toRelay, toGame);
            } catch (IOException e) {
                SmmoRPG.LOGGER.warn("Could not join a guest to the local game", e);
                if (toRelay != null) Pipe.close(toRelay);
                if (toGame != null) Pipe.close(toGame);
            }
        }, "smmorpg-coop-dialback");

        thread.setDaemon(true);
        thread.start();
    }

    private static void send(Socket socket, String json) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
