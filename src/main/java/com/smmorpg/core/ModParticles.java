package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(Registries.PARTICLE_TYPE, SmmoRPG.MOD_ID);

    /** Arterial spray at the moment of the cut. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_SPRAY =
            REGISTRY.register("blood_spray", () -> new SimpleParticleType(false));
    /** The slow drip that follows while a wound is still open. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_DRIP =
            REGISTRY.register("blood_drip", () -> new SimpleParticleType(false));
    /** Sparks when a blade skates off plate. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PARRY_SPARK =
            REGISTRY.register("parry_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BONE_CHIP =
            REGISTRY.register("bone_chip", () -> new SimpleParticleType(false));
}
