package com.smmorpg.client;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.camera.CameraShake;
import com.smmorpg.client.render.PoseState;
import com.smmorpg.client.screen.ClassSelectScreen;
import com.smmorpg.config.CombatConfig;
import com.smmorpg.item.RpgWeaponItem;
import com.smmorpg.item.WeaponClass;
import com.smmorpg.network.C2SParry;
import com.smmorpg.network.C2SMovementAction;
import com.smmorpg.network.C2SPoseUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/** Per-frame and per-tick client work: camera, pose authoring, keybinds. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static long lastFrameNanos = System.nanoTime();
    private static int poseSendCooldown = 0;

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

        // Lean and aim still ride on PoseState; the limbs come from the animation system.
        PoseState pose = ClientState.pose(player.getId());
        authorLean(player, pose);
        pose.tick();

        // Mirror our own animation state out to everyone else at 10 Hz.
        if (--poseSendCooldown <= 0) {
            poseSendCooldown = 2;
            ClientNet.sendToServer(new C2SPoseUpdate(pose.animation, pose.frame,
                    pose.aimPitch, pose.leanX, pose.leanZ, pose.aiming, pose.blocking));
        }

        if (Keybinds.PARRY.consumeClick()) {
            ClientNet.sendToServer(C2SParry.INSTANCE);
            pose.set(PoseState.ANIM_PARRY, 1, pose.aimPitch, pose.leanX, pose.leanZ, false, true);
        }
        if (Keybinds.TOGGLE_VIEW.consumeClick() && CombatConfig.CFG.allowTpsToggle.get()) {
            ClientState.viewMode = ClientState.viewMode.next(true);
            mc.options.setCameraType(ClientState.viewMode.toVanilla());
        }
        if (Keybinds.OPEN_CHARACTER.consumeClick()) {
            mc.setScreen(new com.smmorpg.client.screen.CharacterScreen());
        }
        if (Keybinds.SKILLS.consumeClick()) {
            mc.setScreen(new com.smmorpg.client.screen.SkillScreen());
        }
        if (Keybinds.STUDIO.consumeClick() && player.hasPermissions(2)) {
            mc.setScreen(new com.smmorpg.client.screen.StudioScreen());
        }
        if (Keybinds.DASH.consumeClick()) {
            ClientNet.sendToServer(new C2SMovementAction(C2SMovementAction.DASH,
                    player.input.forwardImpulse, player.input.leftImpulse));
            CameraShake.addTrauma(0.18F);
        }

        handleTraversal(player);
        handleAttackInput(player, mc);
        applyMotionFeedback(player);

        if (Keybinds.BATTLE_MODE.consumeClick()) {
            var state = com.smmorpg.anim.AnimationHooks.of(player);
            state.setBattleMode(!state.battleMode());
            // Battle mode is an over-the-shoulder stance; mining mode is plain first person.
            ClientState.viewMode = state.battleMode() && CombatConfig.CFG.allowTpsToggle.get()
                    ? ViewMode.THIRD_PERSON_BACK : ViewMode.FIRST_PERSON;
            mc.options.setCameraType(ClientState.viewMode.toVanilla());
        }
    }

    /**
     * Turns the attack buttons into moveset input.
     *
     * <p>The swing starts locally the moment the button goes down, so it feels immediate,
     * and the same request goes to the server, which runs its own copy of the clip and owns
     * every hit that comes out of it. Vanilla's click-resolves-instantly attack is
     * suppressed separately, in the combat events.
     */
    private static void handleAttackInput(LocalPlayer player, Minecraft mc) {
        var state = com.smmorpg.anim.AnimationHooks.of(player);
        if (!state.battleMode()) return;

        boolean light = mc.options.keyAttack.consumeClick();
        boolean heavy = mc.options.keyUse.consumeClick() && player.isCrouching();

        if (!light && !heavy) return;

        var clip = state.attack(heavy);
        ClientNet.sendToServer(new com.smmorpg.network.C2SAttack(heavy));

        if (clip != null) {
            // The kick is authored on the clip, so retuning the swing retunes the feel.
            CameraShake.addRecoil(clip.cameraKick()
                    * CombatConfig.CFG.recoilScale.get().floatValue());
        }
    }

    /**
     * Jumping is reinterpreted: on the ground it is a normal jump, in the air it becomes a
     * wall kick if you are touching anything and an air step otherwise. The request goes to
     * the server, which re-checks the conditions before granting it.
     */
    private static void handleTraversal(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.keyJump.consumeClick()) return;
        if (player.onGround()) return;                 // vanilla already handled this one

        ClientNet.sendToServer(new C2SMovementAction(C2SMovementAction.WALL_KICK, 0.0F, 0.0F));
        ClientNet.sendToServer(new C2SMovementAction(C2SMovementAction.AIR_JUMP, 0.0F, 0.0F));
        CameraShake.addTrauma(0.12F);
        ClientState.pose(player.getId()).set(PoseState.ANIM_IDLE, 1,
                player.getXRot(), 0.0F, 0.0F, false, false);
    }

    /**
     * Movement you can feel. Sprinting, landing and being airborne all feed the same shake
     * channel a hit does, so the whole game reads through one physical language.
     */
    private static void applyMotionFeedback(LocalPlayer player) {
        double speed = player.getDeltaMovement().horizontalDistance();

        if (player.isSprinting() && player.onGround()) {
            // A footfall every few ticks rather than a constant wobble.
            if (player.tickCount % 6 == 0) {
                CameraShake.addTrauma((float) Math.min(0.09D, speed * 0.32D));
            }
        }
        if (player.onGround() && lastFallDistance > 1.5F) {
            // Landing hits proportionally to the drop, and hard landings really land.
            CameraShake.addTrauma(Math.min(0.85F, lastFallDistance * 0.07F));
            CameraShake.addRecoil(Math.min(1.2F, lastFallDistance * 0.05F));
        }
        lastFallDistance = player.fallDistance;
    }

    private static float lastFallDistance = 0.0F;

    /** Body lean from strafe and turn rate. The animation clips own everything else. */
    private static void authorLean(LocalPlayer player, PoseState pose) {
        WeaponClass wc = RpgWeaponItem.classOf(player.getMainHandItem());
        pose.aiming = player.isUsingItem() && wc == WeaponClass.BOW;
        pose.blocking = player.isUsingItem() && wc != null && wc != WeaponClass.BOW;

        float strafe = (float) (player.getDeltaMovement().x * Math.cos(Math.toRadians(player.getYRot()))
                + player.getDeltaMovement().z * Math.sin(Math.toRadians(player.getYRot())));
        pose.leanX = strafe * 12.0F;
        pose.leanZ = (player.getYRot() - player.yRotO) * 0.6F;
        pose.aimPitch = player.getXRot();
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

    /** Aiming pulls the FOV in; that narrowing is most of what "aiming down" feels like. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PoseState pose = ClientState.pose(mc.player.getId());
        if (pose.aiming) event.setNewFovModifier(event.getNewFovModifier() * 0.72F);
    }
}
