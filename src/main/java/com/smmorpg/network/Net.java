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

        r.playToServer(C2SChooseClass.TYPE, C2SChooseClass.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> chooseClass(ctx.player(), payload)));
        r.playToServer(C2SSpendPoint.TYPE, C2SSpendPoint.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> spendPoint(ctx.player(), payload)));
        r.playToServer(C2SStartTraining.TYPE, C2SStartTraining.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> startTraining(ctx.player(), payload)));
        r.playToServer(C2SOpenVault.TYPE, C2SOpenVault.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        com.smmorpg.vault.VaultOpener.open(sp);
                    }
                }));
        r.playToServer(C2SCarriedAction.TYPE, C2SCarriedAction.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) carriedAction(sp, payload);
                }));
        r.playToServer(C2SLearnSkill.TYPE, C2SLearnSkill.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> learnSkill(ctx.player(), payload)));

        r.playToClient(S2CSkillSync.TYPE, S2CSkillSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onSkillSync(payload)));
        r.playToClient(S2CTrainingLevel.TYPE, S2CTrainingLevel.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientState.trainingLevel = payload.level()));
        r.playToClient(S2CAccountSync.TYPE, S2CAccountSync.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.smmorpg.client.ClientPacketHandler.onAccountSync(payload)));

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
        if (player instanceof ServerPlayer sp) {
            // Picking a path is the moment you become that class, so it is also the moment
            // you get its weapon; making players find one first would leave the first
            // minutes of every character playing identically.
            com.smmorpg.kit.StarterKit.grant(sp);
            sendTo(sp, new S2CProgressSync(next));
        }
    }

    private static void spendPoint(net.minecraft.world.entity.player.Player player, C2SSpendPoint payload) {
        PlayerProgress next = player.getData(ModAttachments.PROGRESS.get()).spendPoint(payload.stat());
        player.setData(ModAttachments.PROGRESS.get(), next);
        com.smmorpg.event.ProgressionEvents.applyClassStats(player, next);
        if (player instanceof ServerPlayer sp) sendTo(sp, new S2CProgressSync(next));
    }

    private static void startTraining(net.minecraft.world.entity.player.Player player,
                                      C2SStartTraining payload) {
        if (player instanceof ServerPlayer sp) {
            com.smmorpg.training.TrainingManager.start(sp);
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

    /**
     * Sends what the player is holding on the cursor to the vault, or destroys it.
     *
     * <p>Reading the carried stack from the open menu rather than from the packet is what
     * makes this safe: the only thing a client can ask for is "the thing I already picked
     * up", which the server watched them pick up.
     */
    private static void carriedAction(ServerPlayer player, C2SCarriedAction payload) {
        var menu = player.containerMenu;
        if (menu == null) return;

        net.minecraft.world.item.ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;

        if (payload.action() == C2SCarriedAction.Action.DEPOSIT) {
            var account = com.smmorpg.backend.AccountService.of(player);
            com.smmorpg.backend.AccountService.put(
                    account.deposit(com.smmorpg.account.VaultItem.of(carried)));
        }

        menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
        menu.broadcastChanges();
    }

    // --- helpers ---

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTracking(ServerLevel level, Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }
}
