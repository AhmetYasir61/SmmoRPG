package com.smmorpg.studio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every animation profile the mod knows, keyed by id.
 *
 * <p>The defaults here are what a fresh install fights with. The studio panel overwrites
 * entries at runtime, and the server pushes its overrides to clients on join, so a tuning
 * change made by an operator reaches every player without anyone updating a jar.
 */
public final class AnimationLibrary {

    private static final Map<String, AnimationProfile> PROFILES = new LinkedHashMap<>();
    private static volatile int revision = 0;

    static {
        def("slash_down",       12, 0.35F, 195.0F, 22.0F,  6.0F, 1.6F, 0.55F);
        def("slash_rising",     14, 0.42F, 175.0F, 18.0F,  8.0F, 1.5F, 0.48F);
        def("slash_horizontal", 12, 0.33F, 210.0F, 34.0F, 10.0F, 1.7F, 0.60F);
        def("thrust",            9, 0.28F,  95.0F, 26.0F,  4.0F, 2.1F, 0.42F);
        def("parry",             8, 0.20F,  70.0F, 12.0F,  3.0F, 2.4F, 0.85F);
        def("guard",            20, 0.50F,  45.0F,  6.0F,  2.0F, 1.0F, 0.05F);
        def("stagger",          40, 0.15F,  60.0F, 30.0F, 18.0F, 0.7F, 0.90F);
        def("draw_bow",         20, 0.80F,  80.0F, 14.0F,  5.0F, 1.2F, 0.30F);
        def("sprint",           20, 0.50F,  55.0F, 12.0F,  7.0F, 1.0F, 0.14F);
        def("dash",              6, 0.20F,  40.0F, 20.0F, 16.0F, 2.0F, 0.65F);
        def("wall_kick",        10, 0.25F,  90.0F, 28.0F, 22.0F, 1.4F, 0.75F);
        def("air_jump",         10, 0.30F,  70.0F, 10.0F,  6.0F, 1.3F, 0.35F);
        def("land_heavy",       14, 0.10F,  50.0F, 16.0F, 14.0F, 1.1F, 1.00F);
    }

    private AnimationLibrary() {}

    private static void def(String id, int duration, float strikeAt, float arm,
                            float twist, float lean, float ease, float kick) {
        PROFILES.put(id, new AnimationProfile(id, duration, strikeAt, arm, twist, lean, ease, kick, Map.of()));
    }

    public static AnimationProfile get(String id) { return PROFILES.get(id); }

    public static synchronized void put(AnimationProfile profile) {
        PROFILES.put(profile.id(), profile);
        revision++;
    }

    public static Map<String, AnimationProfile> all() {
        return java.util.Collections.unmodifiableMap(PROFILES);
    }

    public static int revision() { return revision; }
}
