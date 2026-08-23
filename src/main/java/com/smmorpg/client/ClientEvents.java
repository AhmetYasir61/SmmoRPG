package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.camera.CameraShake;
import com.smmorpg.client.screen.ClassSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The client work SmmoRPG still owns.
 *
 * <p>Combat input, animation and the camera's placement all belong to Epic Fight and Real
 * Camera now. What is left here is the RPG shell — the class screen, the character and
 * skill screens — and the physical feedback layer: the shake and recoil that a blow, a
 * landing or a sprint should put through the view.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static long lastFrameNanos = System.nanoTime();
    private static float lastFallDistance = 0.0F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        TrainingLauncher.tick();

        LocalPlayer player = mc.player;
        if (player == null) {
            CameraShake.reset();
            return;
        }

        CameraShake.tick();

        // First join with no class yet: open the selection screen and keep it open.
        if (!ClientState.progress.classChosen() && mc.screen == null && mc.level != null) {
            mc.setScreen(new ClassSelectScreen());
        }

        applyMotionFeedback(player);

        if (Keybinds.OPEN_CHARACTER.consumeClick()) {
            mc.setScreen(new com.smmorpg.client.screen.CharacterScreen());
        }
        if (Keybinds.SKILLS.consumeClick()) {
            mc.setScreen(new com.smmorpg.client.screen.SkillScreen());
        }
        if (Keybinds.TRAINING.consumeClick()) {
            mc.setScreen(new com.smmorpg.client.screen.TrainingScreen());
        }
    }

    /**
     * Movement you can feel. Sprinting, landing and being hit all feed the same shake
     * channel, so the whole game reads through one physical language rather than through
     * one effect for combat and another for everything else.
     */
    private static void applyMotionFeedback(LocalPlayer player) {
        double speed = player.getDeltaMovement().horizontalDistance();

        if (player.isSprinting() && player.onGround() && player.tickCount % 6 == 0) {
            // A footfall every few ticks rather than a constant wobble.
            CameraShake.addTrauma((float) Math.min(0.09D, speed * 0.32D));
        }
        if (player.onGround() && lastFallDistance > 1.5F) {
            // Landing hits proportionally to the drop, and hard landings really land.
            CameraShake.addTrauma(Math.min(0.85F, lastFallDistance * 0.07F));
            CameraShake.addRecoil(Math.min(1.2F, lastFallDistance * 0.05F));
        }
        lastFallDistance = player.fallDistance;
    }

    /** The actual camera displacement. Applied every frame, independent of tick rate. */
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = now;

        CameraShake.update(dt);

        float time = (float) (now % 1_000_000_000_000L) / 1.0E9F;
        event.setPitch(event.getPitch() + CameraShake.pitchOffset(time));
        event.setYaw(event.getYaw() + CameraShake.yawOffset(time));
        event.setRoll(event.getRoll() + CameraShake.rollOffset(time));
    }
}
