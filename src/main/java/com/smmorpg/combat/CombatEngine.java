package com.smmorpg.combat;

import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.config.CombatConfig;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.core.ModParticles;
import com.smmorpg.core.ModSounds;
import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import com.smmorpg.loot.Affix;
import com.smmorpg.loot.GearData;
import com.smmorpg.loot.LootRoller;
import com.smmorpg.network.Net;
import com.smmorpg.network.S2CImpactFeedback;
import com.smmorpg.wound.Wound;
import com.smmorpg.wound.WoundSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The single place a melee blow is turned into everything the player feels: a damage
 * number, a wound, one of the 60 impact sounds, blood, and the shake/recoil packet.
 */
public final class CombatEngine {

    private CombatEngine() {}

    /** Everything one blow produced, so callers can react without recomputing it. */
    public record Blow(HitLocation location, ImpactMaterial material, float damage,
                       boolean parried, boolean severed, boolean guardBroken, Vec3 point) {}

    public static Blow strike(LivingEntity attacker, LivingEntity target, float incomingDamage) {
        CombatState attackerState = attacker.getData(ModAttachments.COMBAT.get());
        CombatState targetState = target.getData(ModAttachments.COMBAT.get());

        ItemStack weaponStack = attacker.getMainHandItem();
        WeaponClass wc = RpgWeaponItem.classOf(weaponStack);
        GearData gear = LootRoller.of(weaponStack);

        Vec3 from = attacker.getEyePosition();
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5D, 0).subtract(from);
        float swingAngle = swingAngle(attacker, attackerState);


        // A live parry window on the defender turns the blow aside entirely.
        boolean parried = targetState.parryOpen() && facing(target, attacker);

        // The cut comes from where the blade actually went this swing. Only if the trace
        // has nothing usable (a projectile, a mob with no tracked swing, a laggy tick) do
        // we fall back to a straight ray, so no blow is ever left without a location.
        BladeTrace.Cut cut = SwingTracker.trace(attacker).primary(target).orElse(null);
        HitboxResolver.Result hit = cut != null
                ? new HitboxResolver.Result(cut.location(), cut.entry(), cut.u(), cut.v(), cut.angle())
                : HitboxResolver.resolve(target, from, dir, swingAngle);
        if (cut != null) swingAngle = cut.angle();
        HitLocation loc = parried ? HitLocation.GUARD : hit.location();
        ImpactMaterial material = ImpactMaterial.resolve(target, loc, parried);

        float damage = incomingDamage;
        if (wc != null) damage = Math.max(damage, wc.baseDamage());
        damage *= gear.damageMultiplier();
        damage *= loc.damageMultiplier();

        if (attacker instanceof Player p) {
            PlayerProgress prog = p.getData(ModAttachments.PROGRESS.get());
            damage *= prog.meleeDamageMultiplier();
        }
        if (gear.powerOf(Affix.UNDEAD_BANE) > 0 && target.isInvertedHealAndHarm()) {
            damage *= 1.0F + gear.powerOf(Affix.UNDEAD_BANE);
        }

        boolean guardBroken = false;
        if (parried) {
            damage = 0.0F;
            // A parry costs the attacker posture, not the defender.
            guardBroken = attackerState.addPosture(28.0F);
            playSound(target, ModSounds.PARRY.get(), 1.0F, 1.0F);
            sparks(target, hit.point());
        } else {
            float posture = (wc == null ? 12.0F : wc.staminaCost() * 0.35F);
            guardBroken = targetState.addPosture(posture);
            playSound(target, ModSounds.impact(material, loc), 1.0F, pitchFor(material, damage));
        }

        boolean severed = false;
        if (!parried && damage > 0.0F) {
            float slash = wc == null ? 0.8F : wc.slashFactor();
            severed = WoundSystem.trySever(target, loc, damage, slash, hit.point());

            if (!severed) {
                float severity = Math.min(1.0F, damage / Math.max(1.0F, target.getMaxHealth() * 0.5F))
                        * gear.bleedMultiplier()
                        * (wc == null ? 0.5F : wc.bleedFactor())
                        * loc.bleedFactor();
                if (severity > 0.02F) {
                    // Length and depth are the geometry of this particular cut, not a preset:
                    // a glancing pass leaves a scratch, a full follow-through opens a gash.
                    float traceScale = cut == null ? 1.0F : (0.4F + cut.length() * 0.6F) * (0.5F + cut.depth());
                    WoundSystem.inflict(target, loc, wc == null ? Wound.TYPE_SLASH : wc.woundType(),
                            hit.u(), hit.v(), swingAngle, severity * traceScale, hit.point());
                }
            } else {
                playSound(target, loc == HitLocation.HEAD || loc == HitLocation.NECK
                        ? ModSounds.DECAPITATE.get() : ModSounds.DISMEMBER.get(), 1.2F, 0.9F);
            }
        }

        // Cursed gear takes its due, holy gear mends the wielder.
        float cost = gear.lifeCostPerHit();
        if (cost > 0.0F) attacker.hurt(attacker.damageSources().magic(), cost);
        float steal = gear.lifestealFraction();
        if (steal > 0.0F) attacker.heal(damage * steal);
        if (gear.powerOf(Affix.MENDING_LIGHT) > 0.0F) {
            WoundSystem.treat(attacker, gear.powerOf(Affix.MENDING_LIGHT));
        }

        attackerState.sinceSwing = 0;
        attackerState.comboStep = (attackerState.comboStep + 1) % 4;

        Blow blow = new Blow(loc, material, damage, parried, severed, guardBroken, hit.point());
        sendFeedback(attacker, target, blow, wc);
        return blow;
    }

    /** The camera shake and weapon recoil are derived from the blow, not from a preset. */
    private static void sendFeedback(LivingEntity attacker, LivingEntity target, Blow blow, WeaponClass wc) {
        float shakeBase = blow.parried() ? 0.9F : Math.min(2.5F, 0.25F + blow.damage() * 0.07F);
        if (blow.severed()) shakeBase *= 1.8F;
        if (blow.guardBroken()) shakeBase *= 1.4F;

        float recoil = (wc == null ? 0.35F : wc.recoil()) * (blow.parried() ? 2.0F : 1.0F);
        int hitStop = blow.parried() ? 5 : Math.min(9, 2 + Math.round(blow.damage() * 0.25F));

        S2CImpactFeedback toAttacker = new S2CImpactFeedback(
                shakeBase * CombatConfig.CFG.cameraShakeScale.get().floatValue(),
                recoil * CombatConfig.CFG.recoilScale.get().floatValue(),
                Math.round(hitStop * CombatConfig.CFG.hitStopScale.get().floatValue()),
                blow.location().key(), blow.material().key(), blow.parried(), blow.severed());

        if (attacker instanceof ServerPlayer sp) Net.sendTo(sp, toAttacker);

        // The one taking the hit feels it harder than the one landing it.
        if (target instanceof ServerPlayer victim) {
            Net.sendTo(victim, new S2CImpactFeedback(
                    shakeBase * 1.8F * CombatConfig.CFG.cameraShakeScale.get().floatValue(),
                    recoil * 0.5F, hitStop,
                    blow.location().key(), blow.material().key(), blow.parried(), blow.severed()));
        }
    }

    private static float swingAngle(LivingEntity attacker, CombatState state) {
        // The combo step decides whether this is a downward, rising or horizontal cut,
        // which is what makes consecutive wounds cross on the model.
        return switch (state.comboStep) {
            case 0 -> 45.0F;
            case 1 -> 135.0F;
            case 2 -> 0.0F;
            default -> 90.0F;
        } + (attacker.getYRot() % 15.0F) * 0.4F;
    }

    private static boolean facing(LivingEntity defender, LivingEntity attacker) {
        Vec3 look = defender.getLookAngle().normalize();
        Vec3 toAttacker = attacker.position().subtract(defender.position()).normalize();
        return look.dot(toAttacker) > 0.35D;
    }

    private static float pitchFor(ImpactMaterial mat, float damage) {
        float base = switch (mat) {
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

    private static void playSound(LivingEntity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY() + at.getBbHeight() * 0.6D, at.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void sparks(LivingEntity at, Vec3 point) {
        if (at.level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.PARRY_SPARK.get(), point.x, point.y, point.z,
                    18, 0.05D, 0.05D, 0.05D, 0.25D);
        }
    }
}
