package com.smmorpg.network;

import com.smmorpg.SmmoRPG;
import com.smmorpg.capability.PlayerProgress;
import com.smmorpg.classes.PlayerClass;
import com.smmorpg.combat.CombatState;
import com.smmorpg.core.ModAttachments;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Packet registration and the server-side handlers. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class Net {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");

        r.playToClient(S2CWoundSync.TYPE, S2CWoundSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onWoundSync(payload)));
        r.playToClient(S2CProgressSync.TYPE, S2CProgressSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onProgressSync(payload)));
        r.playToClient(S2CImpactFeedback.TYPE, S2CImpactFeedback.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onImpact(payload)));
        r.playToClient(S2CPoseSync.TYPE, S2CPoseSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onPoseSync(payload)));

        r.playToServer(C2SChooseClass.TYPE, C2SChooseClass.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> chooseClass(ctx.player(), payload)));
        r.playToServer(C2SSpendPoint.TYPE, C2SSpendPoint.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> spendPoint(ctx.player(), payload)));
        r.playToServer(C2SPoseUpdate.TYPE, C2SPoseUpdate.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> relayPose(ctx.player(), payload)));
        r.playToServer(C2SParry.TYPE, C2SParry.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> parry(ctx.player())));
        r.playToServer(C2SStartTraining.TYPE, C2SStartTraining.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> startTraining(ctx.player(), payload)));
        r.playToServer(C2SMovementAction.TYPE, C2SMovementAction.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> movement(ctx.player(), payload)));
        r.playToServer(C2SLearnSkill.TYPE, C2SLearnSkill.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> learnSkill(ctx.player(), payload)));
        r.playToServer(C2SAttack.TYPE, C2SAttack.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> attack(ctx.player(), payload)));
        r.playToClient(S2CPlayAnimation.TYPE, S2CPlayAnimation.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onPlayAnimation(payload)));

        r.playToServer(C2SStudioEdit.TYPE, C2SStudioEdit.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> studioEdit(ctx.player(), payload)));

        r.playToClient(S2CSkillSync.TYPE, S2CSkillSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onSkillSync(payload)));
        r.playToClient(S2CAnimationSync.TYPE, S2CAnimationSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onAnimationSync(payload)));
        r.playToClient(com.smmorpg.sync.ContentSync.Payload.TYPE,
                com.smmorpg.sync.ContentSync.Payload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.sync.ClientContentCache.accept(payload)));
    }

    // --- server handlers ---

    private static void chooseClass(net.minecraft.world.entity.player.Player player, C2SChooseClass payload) {
        PlayerProgress progress = player.getData(ModAttachments.PROGRESS.get());
        // The class is a one-time decision; a second packet cannot re-roll it.
        if (progress.classChosen()) return;
        PlayerProgress next = progress.withClass(PlayerClass.byKey(payload.classKey()));
        player.setData(ModAttachments.PROGRESS.get(), next);
        com.smmorpg.event.ProgressionEvents.applyClassStats(player, next);
        if (player instanceof ServerPlayer sp) sendTo(sp, new S2CProgressSync(next));
    }

    private static void spendPoint(net.minecraft.world.entity.player.Player player, C2SSpendPoint payload) {
        PlayerProgress next = player.getData(ModAttachments.PROGRESS.get()).spendPoint(payload.stat());
        player.setData(ModAttachments.PROGRESS.get(), next);
        com.smmorpg.event.ProgressionEvents.applyClassStats(player, next);
        if (player instanceof ServerPlayer sp) sendTo(sp, new S2CProgressSync(next));
    }

    private static void relayPose(net.minecraft.world.entity.player.Player player, C2SPoseUpdate payload) {
        if (!(player instanceof ServerPlayer sp)) return;
        S2CPoseSync sync = new S2CPoseSync(sp.getId(), payload.animation(), payload.frame(),
                payload.aimPitch(), payload.leanX(), payload.leanZ(),
                payload.aiming(), payload.blocking());
        // Everyone who can see this player gets the same pose the player sees from inside.
        PacketDistributor.sendToPlayersTrackingEntity(sp, sync);
    }

    private static void parry(net.minecraft.world.entity.player.Player player) {
        CombatState state = player.getData(ModAttachments.COMBAT.get());
        if (state.staggered()) return;
        PlayerProgress progress = player.getData(ModAttachments.PROGRESS.get());
        state.openParryWindow(progress.playerClass().parryWindowTicks());
    }

    private static void startTraining(net.minecraft.world.entity.player.Player player,
                                      C2SStartTraining payload) {
        if (player instanceof ServerPlayer sp) {
            com.smmorpg.training.TrainingManager.start(sp, payload.difficultyPercent());
        }
    }

    /**
     * Movement is requested by the client but granted by the server: every action re-runs
     * its own contact and cooldown checks here, so the authority never leaves this side.
     */
    private static void movement(net.minecraft.world.entity.player.Player player,
                                 C2SMovementAction payload) {
        switch (payload.action()) {
            case C2SMovementAction.WALL_KICK -> com.smmorpg.movement.MovementSystem.tryWallKick(player);
            case C2SMovementAction.AIR_JUMP -> com.smmorpg.movement.MovementSystem.tryAirJump(player);
            case C2SMovementAction.DASH -> com.smmorpg.movement.MovementSystem.tryDash(
                    player, payload.forward(), payload.strafe(), !player.onGround());
            case C2SMovementAction.WALL_RUN -> com.smmorpg.movement.MovementSystem.tryWallRun(player);
            default -> { }
        }
    }

    private static void learnSkill(net.minecraft.world.entity.player.Player player,
                                   C2SLearnSkill payload) {
        var skill = com.smmorpg.skill.Skills.get(
                net.minecraft.resources.ResourceLocation.parse(payload.skillId()));
        if (skill == null) return;
        var data = player.getData(ModAttachments.SKILLS.get());
        var next = data.learn(skill);
        if (next == data) return;                 // not affordable, capped, or missing a parent
        player.setData(ModAttachments.SKILLS.get(), next);
        if (player instanceof ServerPlayer sp) sendTo(sp, new S2CSkillSync(next));
    }

    /** Studio edits are operator-only, and are rejected outright for anyone else. */
    private static void studioEdit(net.minecraft.world.entity.player.Player player,
                                   C2SStudioEdit payload) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.hasPermissions(2)) return;

        var profile = com.smmorpg.studio.AnimationLibrary.get(payload.profileId());
        if (profile == null) return;
        com.smmorpg.studio.AnimationLibrary.put(profile.with(payload.field(), payload.value()));

        var sync = new S2CAnimationSync(com.smmorpg.studio.AnimationLibrary.revision(),
                List.copyOf(com.smmorpg.studio.AnimationLibrary.all().values()));
        for (ServerPlayer other : sp.server.getPlayerList().getPlayers()) sendTo(other, sync);
    }

    /**
     * Starts a swing. The server owns the animation clock from here: the damage window and
     * every hit come off the clip it starts, so the client can only ask to swing, never
     * claim to have connected.
     */
    private static void attack(net.minecraft.world.entity.player.Player player, C2SAttack payload) {
        if (!(player instanceof ServerPlayer sp)) return;

        var state = com.smmorpg.anim.AnimationHooks.of(sp);
        if (state.animator.clip() != null && !state.animator.cancellable()) {
            // Still committed to the previous swing; the state machine buffers it for us.
            state.attack(payload.heavy());
            return;
        }

        var clip = state.attack(payload.heavy());
        if (clip == null) return;
        broadcastAnimation(sp, clip.id(), 2.0F);
    }

    /** Sends a clip start to everyone who can see the entity, the entity itself included. */
    public static void broadcastAnimation(net.minecraft.world.entity.LivingEntity entity,
                                          String clipId, float blendTicks) {
        var payload = new S2CPlayAnimation(entity.getId(), clipId, blendTicks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    // --- helpers ---

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTracking(ServerLevel level, Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }
}
