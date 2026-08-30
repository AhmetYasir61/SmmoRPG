package com.smmorpg.coop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/** Copies bytes one way between two sockets and closes both when either end stops. */
final class Pipe {

    private static final int BUFFER = 16 * 1024;

    private Pipe() {}

    /** Starts both directions. Returns immediately; the threads own the sockets from here. */
    static void join(String name, Socket a, Socket b) {
        one(name + "-ab", a, b);
        one(name + "-ba", b, a);
    }

    private static void one(String name, Socket from, Socket to) {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER];
            try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (IOException ignored) {
                // Either side hanging up is the normal way this ends.
            } finally {
                close(from);
                close(to);
            }
        }, "smmorpg-relay-" + name);

        thread.setDaemon(true);
        thread.start();
    }

    static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing useful to do about a socket that will not shut.
        }
    }
}
