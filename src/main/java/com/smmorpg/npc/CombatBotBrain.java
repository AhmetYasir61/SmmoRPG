package com.smmorpg.npc;

import com.smmorpg.core.ModAttachments;
import com.smmorpg.movement.MovementSystem;
import com.smmorpg.training.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The behaviour that makes a training opponent worth fighting.
 *
 * <p>This is a decision layer on top of whatever mob it is attached to, not a new entity
 * type: it reads distance, the target's recovery frames and its own style, then drives
 * movement and attacks directly. It uses the same {@link MovementSystem} the player does,
 * so a bot can wall-kick, dash and air-jump exactly the way you can — there is no separate
 * set of moves that only the AI is allowed.
 */
public class CombatBotBrain {

    private final FightingStyle style;
    private final Difficulty difficulty;
    private final net.minecraft.util.RandomSource rng;

    private int reactionTimer;
    private int commitTimer;
    private int circleDirection = 1;
    private int repositionTimer;

    public CombatBotBrain(FightingStyle style, Difficulty difficulty,
                          net.minecraft.util.RandomSource rng) {
        this.style = style;
        this.difficulty = difficulty;
        this.rng = rng;
        this.reactionTimer = difficulty.reactionTicks();
    }

    public FightingStyle style() { return style; }

    public void tick(Mob self, LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        MovementSystem.tick(self);

        if (reactionTimer > 0) { reactionTimer--; return; }
        reactionTimer = difficulty.reactionTicks();

        double dist = self.distanceTo(target);
        float aggression = Math.min(0.99F, style.aggression() * difficulty.aggression() * 1.4F);
        float acrobatics = Math.min(1.0F, style.acrobatics() * difficulty.acrobatics() * 1.5F);

        self.getLookControl().setLookAt(target, 60.0F, 60.0F);

        // --- decide the approach ---
        if (dist > style.preferredRange() * 1.4D) {
            close(self, target, acrobatics);
        } else if (dist < style.preferredRange() * 0.55D && rng.nextFloat() > aggression) {
            disengage(self, target, acrobatics);
        } else {
            circle(self, target, acrobatics);
        }

        // --- decide the attack ---
        if (commitTimer > 0) { commitTimer--; return; }

        boolean targetRecovering = target.swinging && target.swingTime > 2;
        float attackChance = aggression * (targetRecovering ? 1.6F : 1.0F);
        if (dist <= style.preferredRange() * 1.1D && rng.nextFloat() < attackChance) {
            self.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            self.doHurtTarget(target);
            commitTimer = Math.max(2, difficulty.reactionTicks() * 2);
        }

        // --- decide whether to guard ---
        if (rng.nextFloat() < difficulty.parryChance() * style.patience()) {
            self.getData(ModAttachments.COMBAT.get()).openParryWindow(4);
        }
    }

    /** Closes distance, using vertical movement when the style and difficulty allow it. */
    private void close(Mob self, LivingEntity target, float acrobatics) {
        Vec3 to = target.position().subtract(self.position());
        self.getNavigation().moveTo(target, difficulty.speedMultiplier());

        if (rng.nextFloat() < acrobatics * 0.5F) {
            // A dash straight down the approach line; this is what makes a high-difficulty
            // opponent cover ten blocks before you have finished a swing.
            MovementSystem.tryDash(self, 1.0F, 0.0F, !self.onGround());
        }
        if (!self.onGround() && rng.nextFloat() < acrobatics * 0.4F) {
            if (!MovementSystem.tryWallKick(self)) MovementSystem.tryAirJump(self);
        }
        if (to.y > 1.5D && rng.nextFloat() < acrobatics * 0.6F) {
            // Target is above: go up the wall rather than around.
            MovementSystem.tryWallRun(self);
            MovementSystem.tryAirJump(self);
        }
    }

    /** Backs off, preferring a dash out over walking backwards. */
    private void disengage(Mob self, LivingEntity target, float acrobatics) {
        if (rng.nextFloat() < acrobatics) {
            MovementSystem.tryDash(self, -1.0F, rng.nextBoolean() ? 1.0F : -1.0F, !self.onGround());
            return;
        }
        Vec3 away = self.position().subtract(target.position()).normalize().scale(4.0D);
        self.getNavigation().moveTo(self.getX() + away.x, self.getY(), self.getZ() + away.z,
                difficulty.speedMultiplier());
    }

    /** Strafes around the target, changing direction on its own schedule. */
    private void circle(Mob self, LivingEntity target, float acrobatics) {
        if (--repositionTimer <= 0) {
            repositionTimer = 20 + rng.nextInt(30);
            circleDirection = rng.nextBoolean() ? 1 : -1;
        }
        Vec3 to = target.position().subtract(self.position()).normalize();
        Vec3 side = new Vec3(-to.z, 0.0D, to.x).scale(circleDirection * 2.5D);
        Vec3 goal = self.position().add(side);
        self.getNavigation().moveTo(goal.x, goal.y, goal.z, difficulty.speedMultiplier());

        if (rng.nextFloat() < acrobatics * 0.25F) {
            MovementSystem.tryDash(self, 0.2F, circleDirection, !self.onGround());
        }
    }
}
