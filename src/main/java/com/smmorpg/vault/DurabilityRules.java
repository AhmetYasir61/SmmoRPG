package com.smmorpg.vault;

import com.smmorpg.SmmoRPG;
import com.smmorpg.config.CombatConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

/**
 * Wear that means something.
 *
 * <p>Two rules. Damage survives the vault — a blade put away at half durability comes back
 * at half durability, or repairing anything would be pointless. And a broken item is
 * genuinely gone rather than politely vanishing from your hotbar, so the material cost of
 * keeping a good weapon alive is a real part of owning one.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class DurabilityRules {

    /** Durability restored per unit of repair material. */
    private static final float REPAIR_PER_MATERIAL = 0.25F;

    private DurabilityRules() {}

    /** How worn a stack is, 0 fresh to 1 about to break. */
    public static float wear(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 0.0F;
        return (float) stack.getDamageValue() / stack.getMaxDamage();
    }

    public static boolean nearlyBroken(ItemStack stack) { return wear(stack) >= 0.9F; }

    /** What repairs this item, if anything does. */
    public static Ingredient repairMaterial(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered) {
            return tiered.getTier().getRepairIngredient();
        }
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
            return armor.getMaterial().value().repairIngredient().get();
        }
        // Everything the mod adds is kept alive with iron unless it says otherwise.
        return Ingredient.of(Items.IRON_INGOT);
    }

    /**
     * Repairs a stack with a quantity of material, returning how many units were consumed.
     * Never over-repairs: handing over a stack of iron to fix one scratch wastes the rest.
     */
    public static int repair(ItemStack stack, int available) {
        if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) return 0;

        int perUnit = Math.max(1, Math.round(stack.getMaxDamage() * REPAIR_PER_MATERIAL));
        int needed = Math.min(available, (stack.getDamageValue() + perUnit - 1) / perUnit);

        stack.setDamageValue(Math.max(0, stack.getDamageValue() - needed * perUnit));
        return needed;
    }

    /** Anvil repairs keep the rolled affixes, which vanilla would happily discard. */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!CombatConfig.CFG.preserveGearOnRepair.get()) return;

        ItemStack left = event.getLeft();
        ItemStack output = event.getOutput();
        if (left.isEmpty() || output.isEmpty()) return;

        var gear = left.get(com.smmorpg.core.ModDataComponents.GEAR.get());
        if (gear != null && output.get(com.smmorpg.core.ModDataComponents.GEAR.get()) == null) {
            output.set(com.smmorpg.core.ModDataComponents.GEAR.get(), gear);
            SmmoRPG.LOGGER.debug("Carried rolled gear through an anvil repair.");
        }
    }
}
