package com.smmorpg.combat;

/**
 * Transient per-entity combat state. Deliberately mutable and un-serialised: posture and
 * stamina should reset on relog, the way they do in the games this borrows from.
 */
public class CombatState {
    public float stamina = 100.0F;
    public float staminaMax = 100.0F;
    public float posture = 0.0F;
    public float postureMax = 100.0F;

    /** Ticks left in the active parry window opened by a block press. */
    public int parryTicks = 0;
    /** Ticks the entity is locked out after a broken guard. */
    public int staggerTicks = 0;
    /** Ticks of hit-stop still to burn off. */
    public int hitStopTicks = 0;
    /** Ticks since the last swing; drives combo chaining. */
    public int sinceSwing = 200;
    public int comboStep = 0;

    public boolean parryOpen() { return parryTicks > 0; }
    public boolean staggered() { return staggerTicks > 0; }

    public void tick(float staminaRegen, float postureRecovery) {
        if (parryTicks > 0) parryTicks--;
        if (staggerTicks > 0) staggerTicks--;
        if (hitStopTicks > 0) hitStopTicks--;
        if (sinceSwing < 200) sinceSwing++;
        if (sinceSwing > 40) comboStep = 0;

        if (!staggered()) {
            stamina = Math.min(staminaMax, stamina + staminaRegen * 0.05F);
            posture = Math.max(0.0F, posture - postureRecovery * 0.05F);
        }
    }

    public boolean spendStamina(float cost) {
        if (stamina < cost) return false;
        stamina -= cost;
        return true;
    }

    /** Returns true if this hit broke the guard. */
    public boolean addPosture(float amount) {
        posture += amount;
        if (posture >= postureMax) {
            posture = 0.0F;
            staggerTicks = 40;
            return true;
        }
        return false;
    }

    public void openParryWindow(int ticks) { parryTicks = Math.max(parryTicks, ticks); }
}
