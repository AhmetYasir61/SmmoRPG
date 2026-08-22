package com.smmorpg.combat;

import com.smmorpg.anim.AnimatedCollider;
import com.smmorpg.anim.AnimationHooks;
import com.smmorpg.anim.AnimationState;
import com.smmorpg.core.ModSounds;
import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Applies a blow when the blade actually arrives, not when the button was pressed.
 *
 * <p>Vanilla resolves an attack the instant you click. That is why vanilla combat has no
 * weight: the wind-up is decoration, and the hit has already happened before the arm has
 * moved. Here the swing runs on the animation's timeline and this sweeps the blade's swept
 * volume against nearby entities for as long as the clip says the weapon is live — so a
 * heavy weapon really can be walked out of, and a fast one really does land first.
 *
 * <p>Each swing may only touch a given target once, tracked per attacker so two entities
 * swinging at the same victim never interfere.
 */
public final class AttackSweeper {

    /** Targets already struck by the swing in flight, per attacker. */
    private static final WeakHashMap<LivingEntity, Set<Integer>> HIT_THIS_SWING = new WeakHashMap<>();

    private AttackSweeper() {}

    public static void tick(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        AnimationState state = AnimationHooks.of(attacker);

        if (!state.animator.damaging()) {
            // The window has closed; the next swing starts with a clean slate.
            if (state.animator.finished()) HIT_THIS_SWING.remove(attacker);
            return;
        }

        AnimatedCollider.Blade blade = AnimatedCollider.resolve(attacker, 0.0F);
        Set<Integer> already = HIT_THIS_SWING.computeIfAbsent(attacker, k -> new java.util.HashSet<>());

        WeaponClass weapon = RpgWeaponItem.classOf(attacker.getMainHandItem());
        float reach = weapon == null ? 2.5F : weapon.reach();

        // Search a box around the blade rather than around the attacker: a spear tip four
        // blocks out should hit what is next to the tip, not what is next to the wielder.
        AABB search = new AABB(blade.base(), blade.tip()).inflate(1.0D);

        List<LivingEntity> candidates = new ArrayList<>(
                level.getEntitiesOfClass(LivingEntity.class, search,
                        e -> e != attacker && e.isAlive() && e.isPickable()));

        boolean connected = false;
        for (LivingEntity target : candidates) {
            if (already.contains(target.getId())) continue;
            if (!crosses(blade, target)) continue;
            if (attacker.distanceTo(target) > reach + target.getBbWidth()) continue;

            already.add(target.getId());
            connected = true;

            // The engine reads the same blade trace to place the wound, so the cut lands
            // exactly where this sweep found the target.
            float base = weapon == null ? 4.0F : weapon.baseDamage();
            target.hurt(level.damageSources().mobAttack(attacker), base);
        }

        if (connected) state.animator.consumeDamage();
    }

    /** Does the blade segment pass through the target's bounding box this tick? */
    private static boolean crosses(AnimatedCollider.Blade blade, Entity target) {
        AABB box = target.getBoundingBox().inflate(0.12D);
        if (box.contains(blade.tip()) || box.contains(blade.base())) return true;
        return box.clip(blade.base(), blade.tip()).isPresent();
    }

    /** Called when a swing ends having touched nothing, so the miss is audible. */
    public static void reportWhiff(LivingEntity attacker) {
        attacker.level().playSound(null, attacker.getX(), attacker.getEyeY(), attacker.getZ(),
                ModSounds.WHIFF.get(), SoundSource.PLAYERS, 0.55F, 0.95F + attacker.getRandom().nextFloat() * 0.15F);
    }

    public static void forget(LivingEntity attacker) { HIT_THIS_SWING.remove(attacker); }

    /** The blade's current position, for anything that needs to draw or reason about it. */
    public static Vec3 tipOf(LivingEntity attacker) {
        return AnimatedCollider.resolve(attacker, 0.0F).tip();
    }
}
