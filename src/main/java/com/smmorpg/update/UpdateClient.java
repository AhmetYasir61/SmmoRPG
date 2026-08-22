package com.smmorpg.update;

import com.smmorpg.SmmoRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The always-on update loop, client side.
 *
 * <p>Checks on launch and then on an interval. When something newer exists, the player is
 * told once — in chat if they are already in a world, on the title screen otherwise — and
 * the decision to apply it is always theirs. Nothing downloads without a yes; nothing
 * restarts without a second yes.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class UpdateClient {

    private static int cooldown = 200;      // first check shortly after launch
    private static boolean notified = false;

    private UpdateClient() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (--cooldown > 0) return;
        cooldown = (int) (UpdateService.checkInterval.toSeconds() * 20L);

        String current = currentVersion();
        UpdateService.check(current).thenAccept(found -> {
            if (found.isEmpty() || notified) return;
            notified = true;
            Minecraft.getInstance().execute(() -> announce(found.get()));
        });
    }

    private static void announce(UpdateManifest manifest) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Already playing: a note, not an interruption. The player finishes their fight.
            mc.player.sendSystemMessage(Component.translatable(
                    "update.smmorpg.available_ingame", manifest.version())
                    .withStyle(ChatFormatting.GOLD));
            if (manifest.mandatory()) {
                mc.player.sendSystemMessage(Component.translatable("update.smmorpg.mandatory")
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            mc.setScreen(new com.smmorpg.client.screen.UpdateScreen(manifest, mc.screen));
        }
    }

    public static String currentVersion() {
        return net.neoforged.fml.ModList.get().getModContainerById(SmmoRPG.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("0.0.0");
    }

    /** Marks the notice as unseen again, so a later check can raise it once more. */
    public static void resetNotice() { notified = false; }
}
