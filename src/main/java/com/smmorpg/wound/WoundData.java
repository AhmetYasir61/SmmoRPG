package com.smmorpg.wound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.config.CombatConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Every wound currently on one entity, plus which parts have been cut clean off. */
public record WoundData(List<Wound> wounds, List<String> severed, float bloodLoss) {

    public static final WoundData EMPTY = new WoundData(List.of(), List.of(), 0.0F);

    public static final Codec<WoundData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Wound.CODEC.listOf().fieldOf("wounds").forGetter(WoundData::wounds),
            Codec.STRING.listOf().fieldOf("severed").forGetter(WoundData::severed),
            Codec.FLOAT.fieldOf("blood").forGetter(WoundData::bloodLoss)
    ).apply(i, WoundData::new));

    public WoundData add(Wound w) {
        List<Wound> list = new ArrayList<>(wounds);
        int cap = CombatConfig.CFG.maxWoundsPerEntity.get();
        list.add(w);
        // Oldest and most-closed wounds are the ones that fall off the end.
        while (list.size() > cap) {
            int worst = 0;
            for (int i = 1; i < list.size(); i++) {
                if (list.get(i).closure() > list.get(worst).closure()) worst = i;
            }
            list.remove(worst);
        }
        return new WoundData(List.copyOf(list), severed, bloodLoss);
    }

    public WoundData sever(HitLocation loc) {
        if (severed.contains(loc.key())) return this;
        List<String> s = new ArrayList<>(severed);
        s.add(loc.key());
        return new WoundData(wounds, List.copyOf(s), bloodLoss);
    }

    public boolean isSevered(HitLocation loc) { return severed.contains(loc.key()); }

    public Set<HitLocation> severedLocations() {
        Set<HitLocation> set = EnumSet.noneOf(HitLocation.class);
        for (String s : severed) set.add(HitLocation.byKey(s));
        return set;
    }

    /** Total bleed per second across every open wound. */
    public float bleedRate() {
        float sum = 0.0F;
        for (Wound w : wounds) sum += w.bleedRate();
        return sum * CombatConfig.CFG.bleedPerWoundPerSecond.get().floatValue();
    }

    /**
     * Advances healing by one tick. Wounds close gradually, so a regenerating mob visibly
     * knits itself back together instead of snapping from "shredded" to "clean".
     */
    public WoundData tickHealing(float healFraction) {
        if (wounds.isEmpty() && bloodLoss <= 0.0F) return this;
        float step = healFraction / Math.max(1, CombatConfig.CFG.woundCloseTicks.get());
        List<Wound> next = new ArrayList<>(wounds.size());
        for (Wound w : wounds) {
            Wound healed = w.type() == Wound.TYPE_SEVER ? w : w.heal(step);
            if (!healed.closed()) next.add(healed);
        }
        float blood = Math.max(0.0F, bloodLoss - step * 40.0F);
        return new WoundData(List.copyOf(next), severed, blood);
    }

    public WoundData withBloodLoss(float v) {
        return new WoundData(wounds, severed, Math.max(0.0F, v));
    }

    public List<Wound> at(HitLocation loc) {
        List<Wound> out = new ArrayList<>();
        for (Wound w : wounds) if (w.location().equals(loc.key())) out.add(w);
        return out;
    }
}
