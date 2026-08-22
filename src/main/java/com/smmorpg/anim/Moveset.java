package com.smmorpg.anim;

import com.smmorpg.item.WeaponClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What a weapon can do.
 *
 * <p>A moveset is the whole reason two weapons feel different: swapping from a dagger to a
 * kanabo does not change a damage number, it changes every animation you have access to and
 * every timing you fight by. The combo is a list because chaining is just walking down it —
 * each hit that lands inside the previous one's cancel window advances the index.
 */
public record Moveset(List<String> lightCombo,
                      List<String> heavyCombo,
                      String guard,
                      String parry,
                      String dodge,
                      String idle,
                      String sprint) {

    private static final Map<WeaponClass, Moveset> BY_WEAPON = new EnumMap<>(WeaponClass.class);

    /** Bare hands, or anything with no moveset of its own. */
    public static final Moveset DEFAULT = new Moveset(
            List.of("sword_1", "sword_2"), List.of("heavy_1"),
            "guard", "parry", "dodge", "idle", "sprint");

    static {
        // Katana and tanto share the drawn-stance family; the tanto just never reaches the
        // third hit, because a short blade has no business attempting an iai finisher.
        BY_WEAPON.put(WeaponClass.KATANA, new Moveset(
                List.of("katana_1", "katana_2", "katana_3"), List.of("heavy_1"),
                "guard", "parry", "dodge", "idle", "sprint"));
        BY_WEAPON.put(WeaponClass.TANTO, new Moveset(
                List.of("katana_1", "katana_2"), List.of("dagger_1"),
                "guard", "parry", "dodge", "idle", "sprint"));

        // The long swords run the full four-hit chain.
        BY_WEAPON.put(WeaponClass.DAO, new Moveset(
                List.of("sword_1", "sword_2", "sword_3", "sword_4"), List.of("heavy_1"),
                "guard", "parry", "dodge", "idle", "sprint"));
        BY_WEAPON.put(WeaponClass.JIAN, new Moveset(
                List.of("sword_2", "sword_3", "sword_1", "sword_4"), List.of("heavy_1"),
                "guard", "parry", "dodge", "idle", "sprint"));

        // Two-handers: no light chain worth the name, but the heavies land like a truck.
        BY_WEAPON.put(WeaponClass.ODACHI, new Moveset(
                List.of("sword_1", "sword_4"), List.of("heavy_1", "heavy_2"),
                "guard", "parry", "dodge", "idle", "sprint"));
        BY_WEAPON.put(WeaponClass.KANABO, new Moveset(
                List.of("heavy_1"), List.of("heavy_2", "heavy_1"),
                "guard", "parry", "dodge", "idle", "sprint"));

        // Daggers alternate hands, which is why the chain is so short and so fast.
        BY_WEAPON.put(WeaponClass.DAGGER, new Moveset(
                List.of("dagger_1", "dagger_2", "dagger_1", "dagger_2"), List.of("sword_1"),
                "guard", "parry", "dodge", "idle", "sprint"));

        // Polearms thrust, then sweep with the butt end.
        BY_WEAPON.put(WeaponClass.SPEAR, new Moveset(
                List.of("spear_1", "spear_1", "spear_2"), List.of("spear_2"),
                "guard", "parry", "dodge", "idle", "sprint"));
        BY_WEAPON.put(WeaponClass.NAGINATA, new Moveset(
                List.of("spear_1", "sword_2", "spear_2"), List.of("heavy_2"),
                "guard", "parry", "dodge", "idle", "sprint"));

        BY_WEAPON.put(WeaponClass.BOW, new Moveset(
                List.of("dagger_1"), List.of("dagger_1"),
                "draw_bow", "parry", "dodge", "idle", "sprint"));
    }

    public static Moveset of(WeaponClass weapon) {
        return weapon == null ? DEFAULT : BY_WEAPON.getOrDefault(weapon, DEFAULT);
    }

    /** The clip for a combo step, wrapping back to the start past the end of the chain. */
    public AnimationClip light(int step) {
        if (lightCombo.isEmpty()) return null;
        return Animations.get(lightCombo.get(Math.floorMod(step, lightCombo.size())));
    }

    public AnimationClip heavy(int step) {
        if (heavyCombo.isEmpty()) return null;
        return Animations.get(heavyCombo.get(Math.floorMod(step, heavyCombo.size())));
    }

    public int lightLength() { return lightCombo.size(); }

    public AnimationClip clip(String which) { return Animations.get(which); }
}
