package com.smmorpg.backend;

/**
 * Where the network lives, compiled in.
 *
 * <p>Nobody should have to configure a client. Installing the modpack is the whole setup:
 * the client never contacts this address at all — it asks the server it is playing on — and
 * a server that is part of the network inherits the URL from here rather than from an
 * operator typing it in.
 *
 * <p>The <em>key</em> deliberately is not here. A secret shipped inside a jar that anyone
 * can download is not a secret; it can be read out of the file in about ten seconds. So the
 * public address is built in and the credential stays in each server's SERVER config, where
 * only the operator sees it.
 */
public final class BackendEndpoints {

    /**
     * The network's account service.
     *
     * <p>Empty until there is one to point at. A built-in address that does not answer is
     * worse than none: every server would spend its startup and its sync ticks talking to
     * a host that will never reply, and the logs would fill with failures that look like a
     * bug in the server rather than a service that does not exist yet.
     */
    public static final String DEFAULT_BASE_URL = "https://api.pokewing.com";

    /**
     * The public server list, read by clients from the title screen.
     *
     * <p>Compiled in and unauthenticated, because a client asks for it before it has joined
     * anything and therefore before anyone could have handed it a key. Shipping a key to
     * every client to read a list of public addresses would give away the account service
     * to protect nothing.
     */
    public static final String SERVER_DIRECTORY_URL = DEFAULT_BASE_URL + "/servers";

    private BackendEndpoints() {}

    /**
     * The address to use, or empty for local-only.
     *
     * <p>The key is what decides whether a server is part of a network, not the URL. A
     * server with no key cannot authenticate against the service anyway, so contacting it
     * would only produce 401s — staying local is both correct and quieter.
     */
    public static String resolveBaseUrl(String configuredUrl, String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) return "";
        if (configuredUrl != null && !configuredUrl.isBlank()) return trimTrailingSlashes(configuredUrl.trim());
        return DEFAULT_BASE_URL;
    }

    /**
     * Drops trailing slashes from a base URL.
     *
     * <p>Every path in this package is written with a leading slash, so a base URL that
     * also ends in one produces {@code //servers/heartbeat} — which most web servers
     * answer with a 404 rather than treating as the same route. Typing the address with
     * a trailing slash is the natural thing to do, so it is fixed here instead of being
     * left as a trap in the config file.
     */
    private static String trimTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') end--;
        return url.substring(0, end);
    }
}
