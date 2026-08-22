package com.smmorpg.anim;

import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import net.minecraft.world.entity.LivingEntity;

/**
 * Per-entity animation and combo state.
 *
 * <p>Lives on both sides. Combo progress is decided here rather than in the input handler
 * so a mob walking its own moveset and a player mashing attack go through identical code.
 */
public class AnimationState {

    public final Animator animator = new Animator();

    private int comboStep = 0;
    /** Ticks left in which the next input still counts as a continuation. */
    private int comboWindow = 0;
    /** An attack pressed during the current one, replayed the moment it can be. */
    private boolean buffered;
    private boolean bufferedHeavy;

    private WeaponClass weapon;
    private boolean battleMode = true;

    public int comboStep() { return comboStep; }
    public boolean battleMode() { return battleMode; }
    public void setBattleMode(boolean value) { battleMode = value; }

    public Moveset moveset() { return Moveset.of(weapon); }

    public void tick(LivingEntity entity) {
        WeaponClass held = RpgWeaponItem.classOf(entity.getMainHandItem());
        if (held != weapon) {
            // Changing weapon mid-chain drops the chain: the new moveset is a different
            // sequence and continuing at step 3 of it would be meaningless.
            weapon = held;
            comboStep = 0;
            comboWindow = 0;
        }

        animator.tick(1.0F);
        if (comboWindow > 0) comboWindow--; else comboStep = 0;

        if (buffered && animator.cancellable()) {
            boolean heavy = bufferedHeavy;
            buffered = false;
            bufferedHeavy = false;
            attack(heavy);
        }

        if (animator.finished()) {
            AnimationClip idle = moveset().clip(moveset().idle());
            if (animator.clip() == null || !animator.clip().loop()) {
                animator.play(idle, 4.0F);
            }
        }
    }

    /**
     * Starts the next attack, or buffers it if the current one is still committed.
     * Returns the clip that will play, or null when the input was only buffered.
     */
    public AnimationClip attack(boolean heavy) {
        if (!animator.cancellable()) {
            buffered = true;
            bufferedHeavy = heavy;
            return null;
        }

        Moveset moveset = moveset();
        AnimationClip clip = heavy ? moveset.heavy(comboStep) : moveset.light(comboStep);
        if (clip == null) return null;

        animator.play(clip, 2.0F);
        comboStep++;
        // The window is the clip plus a little grace, so a chain survives a dropped frame.
        comboWindow = (int) clip.durationTicks() + 8;
        return clip;
    }

    public void play(String clipId, float blend) {
        AnimationClip clip = Animations.get(clipId);
        if (clip != null) animator.play(clip, blend);
    }

    /** Interrupts everything — a guard break, a hit that goes through. */
    public void interrupt(String clipId) {
        comboStep = 0;
        comboWindow = 0;
        buffered = false;
        play(clipId, 1.0F);
    }

    public boolean attacking() {
        AnimationClip clip = animator.clip();
        return clip != null && clip.isAttack() && !animator.finished();
    }
}
