package com.smmorpg.client.hud;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.client.ClientState;
import com.smmorpg.combat.CombatState;
import com.smmorpg.core.ModAttachments;
import com.smmorpg.wound.WoundData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Stamina, posture, level and a blood-loss vignette that darkens as you bleed out. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CombatHud {

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SmmoRPG.id("combat_hud"), CombatHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        CombatState state = mc.player.getData(ModAttachments.COMBAT.get());
        PlayerProgress progress = ClientState.progress;
        WoundData wounds = ClientState.wounds(mc.player.getId());

        int w = g.guiWidth(), h = g.guiHeight();

        // Blood loss darkens and reddens the edges of the screen.
        float bleed = Math.min(1.0F, wounds.bloodLoss() / Math.max(1.0F, mc.player.getMaxHealth() * 1.5F));
        if (bleed > 0.02F) {
            int alpha = (int) (bleed * 140.0F) << 24;
            g.fillGradient(0, 0, w, h / 3, alpha | 0x400000, 0x00000000);
            g.fillGradient(0, h - h / 3, w, h, 0x00000000, alpha | 0x400000);
        }

        int barX = w / 2 - 91;
        int barY = h - 52;

        // Stamina
        float stamina = state.staminaMax <= 0 ? 0 : state.stamina / state.staminaMax;
        g.fill(barX, barY, barX + 182, barY + 4, 0xB0101010);
        g.fill(barX, barY, barX + (int) (182 * stamina), barY + 4, 0xFF3FA34D);

        // Posture — fills toward a guard break, so it is drawn as a threat, not a resource.
        float posture = state.postureMax <= 0 ? 0 : state.posture / state.postureMax;
        g.fill(barX, barY - 6, barX + 182, barY - 2, 0xB0101010);
        g.fill(barX, barY - 6, barX + (int) (182 * posture), barY - 2,
                posture > 0.8F ? 0xFFE04040 : 0xFFD9B44A);

        if (state.staggered()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.smmorpg.stagger"),
                    w / 2, h / 2 - 30, 0xFFE04040);
        }
        if (state.parryOpen()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.smmorpg.parry_window"),
                    w / 2, h / 2 - 42, 0xFF9DE7FF);
        }

        // Level and experience, kept small and out of the way.
        String lvl = "Lv " + progress.level();
        g.drawString(mc.font, lvl, barX, barY - 18, 0xFFFFD760);
        int need = progress.xpForNext();
        float xpFrac = need <= 0 ? 1.0F : (float) progress.experience() / need;
        g.fill(barX + 26, barY - 16, barX + 156, barY - 13, 0xB0101010);
        g.fill(barX + 26, barY - 16, barX + 26 + (int) (130 * xpFrac), barY - 13, 0xFF7FC4FF);

        if (!wounds.severed().isEmpty()) {
            g.drawString(mc.font, Component.translatable("hud.smmorpg.severed", wounds.severed().size()),
                    barX, barY - 30, 0xFFE04040);
        }
    }
}
