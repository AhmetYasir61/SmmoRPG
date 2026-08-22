package com.smmorpg.client;

/**
 * The mod is FPS-first. TPS exists, but it is a toggle off the primary mode, and it is
 * driven by the same pose data — so the third-person rig is never a separate animation set.
 */
public enum ViewMode {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT;

    public boolean isFirstPerson() { return this == FIRST_PERSON; }

    public ViewMode next(boolean allowTps) {
        if (!allowTps) return FIRST_PERSON;
        return switch (this) {
            case FIRST_PERSON -> THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK -> THIRD_PERSON_FRONT;
            case THIRD_PERSON_FRONT -> FIRST_PERSON;
        };
    }

    public net.minecraft.client.CameraType toVanilla() {
        return switch (this) {
            case FIRST_PERSON -> net.minecraft.client.CameraType.FIRST_PERSON;
            case THIRD_PERSON_BACK -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
        };
    }
}
