package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.combat.ImpactMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 60 blade-impact sounds.
 *
 * <p>56 of them are the full cross product of {@link ImpactMaterial} (8) and the seven
 * flesh-bearing {@link HitLocation}s, so a katana skimming a chainmail shoulder and the
 * same katana splitting a bare skull are genuinely different recordings. The remaining
 * four cover the events that are not a body hit: a parry, a whiff, a decapitation and a
 * dismemberment.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(Registries.SOUND_EVENT, SmmoRPG.MOD_ID);

    /** Locations that can actually take a blade. GUARD is handled by the parry sound. */
    public static final HitLocation[] BODY_LOCATIONS = {
            HitLocation.HEAD, HitLocation.NECK, HitLocation.TORSO,
            HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM,
            HitLocation.LEFT_LEG, HitLocation.RIGHT_LEG
    };

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> IMPACTS = new LinkedHashMap<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = simple("impact.parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIFF = simple("impact.whiff");
    public static final DeferredHolder<SoundEvent, SoundEvent> DECAPITATE = simple("impact.decapitate");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISMEMBER = simple("impact.dismember");

    static {
        for (ImpactMaterial mat : ImpactMaterial.values()) {
            for (HitLocation loc : BODY_LOCATIONS) {
                String key = impactKey(mat, loc);
                IMPACTS.put(key, simple(key));
            }
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> simple(String path) {
        ResourceLocation rl = SmmoRPG.id(path.replace('.', '_'));
        return REGISTRY.register(path.replace('.', '_'), () -> SoundEvent.createVariableRangeEvent(rl));
    }

    public static String impactKey(ImpactMaterial mat, HitLocation loc) {
        return "impact." + mat.key() + "." + loc.key();
    }

    /** Never null: falls back to flesh/torso if an exotic combination is missing. */
    public static SoundEvent impact(ImpactMaterial mat, HitLocation loc) {
        DeferredHolder<SoundEvent, SoundEvent> h = IMPACTS.get(impactKey(mat, loc));
        if (h == null) h = IMPACTS.get(impactKey(ImpactMaterial.FLESH, HitLocation.TORSO));
        return h.get();
    }

    /** 56 body impacts + parry + whiff + decapitate + dismember == 60. */
    public static int impactSoundCount() { return IMPACTS.size() + 4; }
}
