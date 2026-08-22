package com.smmorpg.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * The bones an animation can drive.
 *
 * <p>Deliberately mapped onto the vanilla humanoid rig rather than a custom armature: every
 * mob in the game already has these six parts, so one animation plays on a player, a
 * zombie and a piglin without anybody authoring a per-entity skeleton.
 */
public enum Joint {
    ROOT("root"),
    HEAD("head"),
    BODY("body"),
    RIGHT_ARM("right_arm"),
    LEFT_ARM("left_arm"),
    RIGHT_LEG("right_leg"),
    LEFT_LEG("left_leg");

    private final String key;

    Joint(String key) { this.key = key; }

    public String key() { return key; }

    public static Joint byKey(String k) {
        for (Joint j : values()) if (j.key.equals(k)) return j;
        return ROOT;
    }

    /** The model part this joint drives, or null for ROOT (which moves the whole entity). */
    public ModelPart partOf(HumanoidModel<?> model) {
        return switch (this) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case ROOT -> null;
        };
    }
}
