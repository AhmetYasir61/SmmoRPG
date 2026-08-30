package com.smmorpg.core;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.combat.CombatState;
import com.smmorpg.mob.MobData;
import com.smmorpg.skill.SkillData;
import com.smmorpg.wound.WoundData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Persistent per-entity data, attached through NeoForge's attachment system. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmmoRPG.MOD_ID);

    public static final Supplier<AttachmentType<PlayerProgress>> PROGRESS = REGISTRY.register(
            "progress", () -> AttachmentType.builder(() -> PlayerProgress.EMPTY)
                    .serialize(PlayerProgress.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<WoundData>> WOUNDS = REGISTRY.register(
            "wounds", () -> AttachmentType.builder(() -> WoundData.EMPTY)
                    .serialize(WoundData.CODEC)
                    .build());

    /** Live combat state (posture, stamina, parry timers). Not saved — rebuilt each session. */
    public static final Supplier<AttachmentType<CombatState>> COMBAT = REGISTRY.register(
            "combat", () -> AttachmentType.builder(CombatState::new).build());

    /**
     * What a mob has become: its archetype, tier, level and how much it has devoured.
     * Saved, because an evolved Lord that reverts on chunk reload is not an evolution.
     */
    public static final Supplier<AttachmentType<MobData>> MOB = REGISTRY.register(
            "mob", () -> AttachmentType.builder(() -> MobData.NONE)
                    .serialize(MobData.CODEC)
                    .build());

    /**
     * How far the player has climbed in the training arena.
     *
     * <p>Saved and kept through death, because the level is the record of what they have
     * already beaten. Losing a fight costs you the wave, not the climb.
     */
    public static final Supplier<AttachmentType<Integer>> TRAINING_LEVEL = REGISTRY.register(
            "training_level", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.INT)
                    .copyOnDeath()
                    .build());

    /** Unlocked skills and their ranks. Persists and survives death. */
    public static final Supplier<AttachmentType<SkillData>> SKILLS = REGISTRY.register(
            "skills", () -> AttachmentType.builder(() -> SkillData.EMPTY)
                    .serialize(SkillData.CODEC)
                    .copyOnDeath()
                    .build());
}
