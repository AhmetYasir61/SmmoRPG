package com.smmorpg.update;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What the update endpoint returns.
 *
 * <p>The hash is mandatory: a downloaded jar is only ever installed once its SHA-256
 * matches what the manifest promised, so a truncated or tampered download is discarded
 * rather than swapped into the mods folder.
 */
public record UpdateManifest(String version,
                             String downloadUrl,
                             String sha256,
                             long sizeBytes,
                             boolean mandatory,
                             String notes) {

    public static final Codec<UpdateManifest> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("version").forGetter(UpdateManifest::version),
            Codec.STRING.fieldOf("url").forGetter(UpdateManifest::downloadUrl),
            Codec.STRING.fieldOf("sha256").forGetter(UpdateManifest::sha256),
            Codec.LONG.fieldOf("size").forGetter(UpdateManifest::sizeBytes),
            Codec.BOOL.optionalFieldOf("mandatory", false).forGetter(UpdateManifest::mandatory),
            Codec.STRING.optionalFieldOf("notes", "").forGetter(UpdateManifest::notes)
    ).apply(i, UpdateManifest::new));

    /** Simple dotted-numeric comparison; anything unparsable sorts as older. */
    public boolean isNewerThan(String current) {
        String[] a = version.split("[.-]");
        String[] b = current.split("[.-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = parse(i < a.length ? a[i] : "0");
            int y = parse(i < b.length ? b[i] : "0");
            if (x != y) return x > y;
        }
        return false;
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }
}
