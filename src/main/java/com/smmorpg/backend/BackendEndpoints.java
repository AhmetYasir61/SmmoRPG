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

    /** The network's account service. A server may override it in its own config. */
    public static final String DEFAULT_BASE_URL = "https://api.smmorpg.net";

    private BackendEndpoints() {}

    /** The config value if the operator set one, otherwise the built-in address. */
    public static String resolveBaseUrl(String configured) {
        return configured == null || configured.isBlank() ? DEFAULT_BASE_URL : configured.trim();
    }
}
