package com.smmorpg.api;

import com.smmorpg.combat.HitLocation;
import com.smmorpg.combat.ImpactMaterial;
import com.smmorpg.item.WeaponClass;
import com.smmorpg.loot.Affix;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.skill.Skill;
import com.smmorpg.skill.SkillData;
import com.smmorpg.skill.Skills;
import com.smmorpg.wound.Wound;
import com.smmorpg.wound.WoundData;
import com.smmorpg.wound.WoundSystem;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.core.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * The stable surface other mods build against.
 *
 * <p>Nothing here touches internal state directly: every call goes through the same paths
 * the mod uses itself, so an add-on cannot desynchronise a client. This is the only class
 * in the mod whose signatures are treated as frozen — internals may be rearranged freely,
 * this may not.
 *
 * <p>Companion mods should also look at {@link SmmoRPGEvents} for the hooks fired around
 * every blow, wound and level, and at {@link WeaponRegistrationApi} for adding weapons that
 * the server can push to clients without a client-side install.
 */
public final class SmmoRPGApi {

    /** Bumped whenever a signature here changes incompatibly. */
    public static final int API_VERSION = 1;

    private SmmoRPGApi() {}

    // ------------------------------------------------------------------
    // Progression
    // ------------------------------------------------------------------

    public static PlayerProgress progress(Player player) {
        return player.getData(ModAttachments.PROGRESS.get());
    }

    public static void grantExperience(Player player, int amount) {
        PlayerProgress next = progress(player).grantExperience(amount);
        player.setData(ModAttachments.PROGRESS.get(), next);
        com.smmorpg.event.ProgressionEvents.applyClassStats(player, next);
    }

    public static int level(Player player) { return progress(player).level(); }

    // ------------------------------------------------------------------
    // Skills
    // ------------------------------------------------------------------

    /** Registers a skill so it appears in the tree and syncs to clients. */
    public static Skill registerSkill(ResourceLocation id, String category, int maxRank,
                                      int cost, float perRank, ResourceLocation requires) {
        return Skills.add(new Skill(id, category, maxRank, cost, perRank, requires));
    }

    public static Map<ResourceLocation, Skill> allSkills() { return Skills.all(); }

    public static SkillData skills(Player player) {
        return player.getData(ModAttachments.SKILLS.get());
    }

    public static float skillValue(Player player, Skill skill) {
        return skills(player).value(skill);
    }

    // ------------------------------------------------------------------
    // Wounds
    // ------------------------------------------------------------------

    public static WoundData wounds(LivingEntity entity) { return WoundSystem.get(entity); }

    /** Opens a wound from outside the combat engine — a trap, a spell, a custom weapon. */
    public static Wound inflictWound(LivingEntity target, HitLocation location, int woundType,
                                     float u, float v, float angle, float severity, Vec3 worldPos) {
        return WoundSystem.inflict(target, location, woundType, u, v, angle, severity, worldPos);
    }

    public static boolean severLimb(LivingEntity target, HitLocation location) {
        return WoundSystem.trySever(target, location, Float.MAX_VALUE, 1.0F,
                target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
    }

    public static boolean treatWounds(LivingEntity target, float strength) {
        return WoundSystem.treat(target, strength);
    }

    // ------------------------------------------------------------------
    // Loot
    // ------------------------------------------------------------------

    public static GearData gearOf(ItemStack stack) { return LootRoller.of(stack); }

    public static ItemStack applyGear(ItemStack stack, GearData data) {
        return LootRoller.apply(stack, data);
    }

    /** Rolls a fresh piece of loot at the given level. */
    public static GearData rollGear(net.minecraft.util.RandomSource rng, int level, float luck) {
        return com.smmorpg.loot.LootRoller.rollGear(rng, level, luck);
    }

    public static List<Affix> affixes() { return List.of(Affix.values()); }

    // ------------------------------------------------------------------
    // Combat information
    // ------------------------------------------------------------------

    public static HitLocation[] hitLocations() { return HitLocation.values(); }

    public static ImpactMaterial materialOf(LivingEntity target, HitLocation loc, boolean blocked) {
        return ImpactMaterial.resolve(target, loc, blocked);
    }

    public static WeaponClass weaponClassOf(ItemStack stack) {
        return com.smmorpg.item.RpgWeaponItem.classOf(stack);
    }
}
