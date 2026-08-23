package com.smmorpg.npc;

import com.smmorpg.training.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The layer that makes a training opponent worth fighting.
 *
 * <p>Epic Fight already gives every mob a real moveset and the combat goals to use it, so
 * this deliberately does not swing anything. What it adds is the part Epic Fight leaves to
 * the mob's own AI: <em>where</em> to stand and <em>when</em> to press. A counter-fighter
 * hangs at the edge of your reach and waits for a whiff; a flanker never fights from where
 * you last saw it; a skirmisher only closes while you are recovering.
 *
 * <p>Attacks are handed to Epic Fight by keeping the mob in range and aimed. That is what
 * keeps a bot honest: its wind-ups are the same animations yours are, so they are readable,
 * and its damage lands on the frame yours would.
 */
public class CombatBotBrain {

    private final FightingStyle style;
    private final Difficulty difficulty;
    private final net.minecraft.util.RandomSource rng;

    private int reactionTimer;
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

        if (reactionTimer > 0) { reactionTimer--; return; }
        reactionTimer = difficulty.reactionTicks();

        self.setTarget(target);
        self.getLookControl().setLookAt(target, 60.0F, 60.0F);

        double dist = self.distanceTo(target);
        double preferred = style.preferredRange();
        float aggression = Math.min(0.99F, style.aggression() * difficulty.aggression() * 1.4F);

        // The one read that matters: a target mid-swing is a target that cannot answer.
        boolean targetRecovering = target.swinging && target.swingTime > 2;
        if (targetRecovering) aggression = Math.min(0.99F, aggression * 1.6F);

        if (dist > preferred * 1.4D) {
            close(self, target);
        } else if (dist < preferred * 0.55D && rng.nextFloat() > aggression) {
            disengage(self, target);
        } else {
            circle(self, target);
        }
    }

    private void close(Mob self, LivingEntity target) {
        self.getNavigation().moveTo(target, difficulty.speedMultiplier());
    }

    private void disengage(Mob self, LivingEntity target) {
        Vec3 away = self.position().subtract(target.position()).normalize().scale(4.0D);
        self.getNavigation().moveTo(self.getX() + away.x, self.getY(), self.getZ() + away.z,
                difficulty.speedMultiplier());
    }

    /** Strafes around the target, changing direction on its own schedule rather than yours. */
    private void circle(Mob self, LivingEntity target) {
        if (--repositionTimer <= 0) {
            repositionTimer = 20 + rng.nextInt(30);
            circleDirection = rng.nextBoolean() ? 1 : -1;
        }
        Vec3 to = target.position().subtract(self.position()).normalize();
        Vec3 side = new Vec3(-to.z, 0.0D, to.x).scale(circleDirection * 2.5D);
        Vec3 goal = self.position().add(side);
        self.getNavigation().moveTo(goal.x, goal.y, goal.z, difficulty.speedMultiplier());
    }
}
