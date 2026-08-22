package com.smmorpg.client;

import com.smmorpg.client.camera.CameraShake;
import com.smmorpg.client.render.PoseState;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.network.S2CImpactFeedback;
import com.smmorpg.network.S2CPoseSync;
import com.smmorpg.network.S2CProgressSync;
import com.smmorpg.network.S2CWoundSync;
import net.minecraft.client.Minecraft;

/** Client-side landing zone for every server packet. Runs on the render thread. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onWoundSync(S2CWoundSync payload) {
        ClientState.putWounds(payload.entityId(), payload.data());
    }

    public static void onProgressSync(S2CProgressSync payload) {
        ClientState.progress = payload.progress();
    }

    public static void onPoseSync(S2CPoseSync payload) {
        Minecraft mc = Minecraft.getInstance();
        // The local player already drives its own pose from raw input; taking the server
        // echo back would add a round-trip of latency to your own arms.
        if (mc.player != null && payload.entityId() == mc.player.getId()) return;

        PoseState pose = ClientState.pose(payload.entityId());
        pose.set(payload.animation(), payload.frame(), payload.aimPitch(),
                payload.leanX(), payload.leanZ(), payload.aiming(), payload.blocking());
    }

    public static void onSkillSync(com.smmorpg.network.S2CSkillSync payload) {
        ClientState.skills = payload.data();
    }

    public static void onAnimationSync(com.smmorpg.network.S2CAnimationSync payload) {
        // A studio edit lands here; the very next frame already animates with the new values.
        payload.profiles().forEach(com.smmorpg.studio.AnimationLibrary::put);
    }

    public static void onImpact(S2CImpactFeedback payload) {
        CameraShake.addTrauma(payload.shake());
        CameraShake.addRecoil(payload.recoil());
        CameraShake.setHitStop(payload.hitStopTicks());

        HitLocation loc = HitLocation.byKey(payload.location());
        // A hit on the head snaps the view harder than one on a leg.
        if (loc == HitLocation.HEAD || loc == HitLocation.NECK) {
            CameraShake.addRecoil(payload.recoil() * 1.6F);
        }
        if (payload.severed()) CameraShake.addTrauma(0.4F);
        if (payload.parried()) CameraShake.addRecoil(payload.recoil() * 0.8F);
    }
}
