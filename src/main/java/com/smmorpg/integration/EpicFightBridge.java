package com.smmorpg.integration;

import com.smmorpg.SmmoRPG;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.combat.HitboxResolver;
import com.smmorpg.combat.ImpactMaterial;
import com.smmorpg.config.CombatConfig;
import com.smmorpg.core.ModSounds;
import com.smmorpg.item.WeaponClass;
import com.smmorpg.loot.Affix;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.network.Net;
import com.smmorpg.network.S2CImpactFeedback;
import com.smmorpg.wound.Wound;
import com.smmorpg.wound.WoundSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.entity.DealDamageEvent;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * Where SmmoRPG attaches to Epic Fight.
 *
 * <p>Epic Fight owns the fight itself — the animations, the movesets, the colliders that
 * decide when a blade connects. This mod owns what a connection leaves behind: a wound in a
 * particular place, the right one of sixty impact sounds, blood, and the shake you feel.
 *
 * <p>The important part is {@link EpicFightDamageSource#getInitialPosition()}. Epic Fight
 * already knows exactly where in the world its collider met the target, so the wound goes
 * where the blade actually arrived — the same idea the mod used to implement itself, except
 * now the position comes from the system that really swung the weapon.
 */
public final class EpicFightBridge {

    private EpicFightBridge() {}

    public static void register() {
        // Post rather than Pre: by then Epic Fight has settled the damage number, and a
        // wound should describe the blow that actually landed.
        EpicFightEventHooks.Entity.DELIVER_DAMAGE_POST.registerEvent(
                EpicFightBridge::onDealDamage, "smmorpg:wounds");

        SmmoRPG.LOGGER.info("Hooked into Epic Fight: wounds, impact sounds and hit feedback.");
    }

    private static void onDealDamage(DealDamageEvent.Post event) {
        LivingEntity attacker = event.getEntityPatch().getOriginal();
        LivingEntity target = event.getTarget();
        if (attacker == null || target == null) return;
        if (target.level().isClientSide) return;

        float damage = event.getModifiedDamage();
        if (damage <= 0.0F) return;

        // Epic Fight hands us the collider's own contact point when it has one. When it
        // does not — a projectile, a skill with no physical collider — fall back to the
        // ray, so no blow is ever left without a location.
        Vec3 hitPos = contactPoint(event.getDamageSource(), attacker, target);
        HitboxResolver.Result hit = HitboxResolver.at(target, hitPos);
        HitLocation location = hit.location();

        ItemStack weapon = weaponOf(event.getDamageSource(), attacker);
        GearData gear = LootRoller.of(weapon);
        WeaponClass weaponClass = com.smmorpg.item.RpgWeaponItem.classOf(weapon);

        ImpactMaterial material = ImpactMaterial.resolve(target, location, false);
        playImpact(target, material, location, damage);

        boolean severed = openWound(attacker, target, location, hit, damage, gear, weaponClass, hitPos);
        payCursedToll(attacker, gear, damage);
        sendFeedback(attacker, target, location, material, damage, severed, weaponClass);
    }

    /** The collider's contact point, or the centre of the target if Epic Fight had none. */
    private static Vec3 contactPoint(net.minecraft.world.damagesource.DamageSource source,
                                     LivingEntity attacker, LivingEntity target) {
        if (source instanceof EpicFightDamageSource efSource) {
            Vec3 initial = efSource.getInitialPosition();
            if (initial != null) return initial;
        }
        // Nothing better available: aim from the attacker's eyes at the target's middle.
        Vec3 from = attacker.getEyePosition();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        return HitboxResolver.resolve(target, from, to.subtract(from), 0.0F).point();
    }

    private static ItemStack weaponOf(net.minecraft.world.damagesource.DamageSource source,
                                      LivingEntity attacker) {
        if (source instanceof EpicFightDamageSource efSource) {
            ItemStack used = efSource.getUsedItem();
            if (used != null && !used.isEmpty()) return used;
        }
        return attacker.getMainHandItem();
    }

    /** Opens the cut, or takes the limb off when the blow was severe enough to. */
    private static boolean openWound(LivingEntity attacker, LivingEntity target,
                                     HitLocation location, HitboxResolver.Result hit,
                                     float damage, GearData gear, WeaponClass weaponClass,
                                     Vec3 hitPos) {
        float slash = weaponClass == null ? 0.85F : weaponClass.slashFactor();
        if (WoundSystem.trySever(target, location, damage, slash, hitPos)) {
            target.level().playSound(null, target.getX(), target.getEyeY(), target.getZ(),
                    location == HitLocation.HEAD || location == HitLocation.NECK
                            ? ModSounds.DECAPITATE.get() : ModSounds.DISMEMBER.get(),
                    SoundSource.PLAYERS, 1.2F, 0.9F);
            return true;
        }

        float severity = Math.min(1.0F, damage / Math.max(1.0F, target.getMaxHealth() * 0.5F))
                * gear.bleedMultiplier()
                * (weaponClass == null ? 0.6F : weaponClass.bleedFactor())
                * location.bleedFactor();

        if (severity > 0.02F) {
            int type = weaponClass == null ? Wound.TYPE_SLASH : weaponClass.woundType();
            // The swing angle is taken from where the blade met the body relative to its
            // centre, so a cut across a shoulder and one up through it look different.
            float angle = (float) Math.toDegrees(Math.atan2(
                    hitPos.y - target.getY() - target.getBbHeight() * 0.5D,
                    hitPos.x - target.getX()));
            WoundSystem.inflict(target, location, type, hit.u(), hit.v(), angle, severity, hitPos);
        }
        return false;
    }

    /** Cursed gear takes its due from the wielder; holy gear mends them. */
    private static void payCursedToll(LivingEntity attacker, GearData gear, float damage) {
        float cost = gear.lifeCostPerHit();
        if (cost > 0.0F) attacker.hurt(attacker.damageSources().magic(), cost);

        float steal = gear.lifestealFraction();
        if (steal > 0.0F) attacker.heal(damage * steal);

        float mending = gear.powerOf(Affix.MENDING_LIGHT);
        if (mending > 0.0F) WoundSystem.treat(attacker, mending);
    }

    private static void playImpact(LivingEntity target, ImpactMaterial material,
                                   HitLocation location, float damage) {
        if (location == HitLocation.GUARD) return;
        target.level().playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.6D,
                target.getZ(), ModSounds.impact(material, location), SoundSource.PLAYERS,
                1.0F, pitchFor(material, damage));
    }

    private static float pitchFor(ImpactMaterial material, float damage) {
        float base = switch (material) {
            case BONE -> 1.10F;
            case PLATE -> 0.95F;
            case CHAIN -> 1.05F;
            case STONE -> 0.85F;
            case UNDEAD -> 0.90F;
            default -> 1.00F;
        };
        // Heavier blows ring lower.
        return Math.max(0.6F, base - Math.min(0.25F, damage * 0.012F));
    }

    /** The felt half: shake for whoever swung, harder shake for whoever was hit. */
    private static void sendFeedback(LivingEntity attacker, LivingEntity target,
                                     HitLocation location, ImpactMaterial material,
                                     float damage, boolean severed, WeaponClass weaponClass) {
        float shake = Math.min(2.5F, 0.25F + damage * 0.07F) * (severed ? 1.8F : 1.0F);
        float recoil = weaponClass == null ? 0.35F : weaponClass.recoil();
        int hitStop = Math.min(9, 2 + Math.round(damage * 0.25F));

        float shakeScale = CombatConfig.CFG.cameraShakeScale.get().floatValue();
        float recoilScale = CombatConfig.CFG.recoilScale.get().floatValue();
        int stopTicks = Math.round(hitStop * CombatConfig.CFG.hitStopScale.get().floatValue());

        if (attacker instanceof ServerPlayer sp) {
            Net.sendTo(sp, new S2CImpactFeedback(shake * shakeScale, recoil * recoilScale,
                    stopTicks, location.key(), material.key(), false, severed));
        }
        if (target instanceof ServerPlayer victim) {
            Net.sendTo(victim, new S2CImpactFeedback(shake * 1.8F * shakeScale,
                    recoil * 0.5F * recoilScale, stopTicks,
                    location.key(), material.key(), false, severed));
        }
    }
}
