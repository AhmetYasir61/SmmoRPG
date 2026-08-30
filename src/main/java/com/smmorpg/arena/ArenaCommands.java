package com.smmorpg.arena;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.smmorpg.SmmoRPG;
import com.smmorpg.account.PlayerAccount;
import com.smmorpg.backend.AccountService;
import com.smmorpg.backend.BackendClient;
import com.smmorpg.backend.OfflineQueue;
import com.smmorpg.config.ServerConfig;
import com.smmorpg.rank.EloSystem;
import com.smmorpg.rank.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The queue and the ladder, reachable before the menus exist.
 *
 * <p>Commands first, screens later, on purpose: they let the whole system be played and
 * tested now, and a screen built on top of a flow that already works is a much smaller
 * piece of work than one built at the same time as it.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class ArenaCommands {

    private ArenaCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("smmorpg");

        for (MatchMode mode : MatchMode.values()) {
            root.then(Commands.literal("queue").then(Commands.literal(mode.key())
                    .executes(ctx -> queue(ctx.getSource(), mode))));
        }

        root.then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())));
        root.then(Commands.literal("vault").executes(ctx -> vault(ctx.getSource())));
        root.then(Commands.literal("kit").executes(ctx -> kit(ctx.getSource())));

        root.then(Commands.literal("invite")
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(ctx -> invite(ctx.getSource(),
                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player")))));
        root.then(Commands.literal("accept").executes(ctx -> accept(ctx.getSource())));
        root.then(Commands.literal("party").executes(ctx -> party(ctx.getSource())));
        root.then(Commands.literal("disband").executes(ctx -> disband(ctx.getSource())));
        root.then(Commands.literal("rank").executes(ctx -> rank(ctx.getSource())));
        root.then(Commands.literal("status")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> status(ctx.getSource())));

        event.getDispatcher().register(root);
    }

    private static int queue(CommandSourceStack source, MatchMode mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (!ServerConfig.CFG.rankedEnabled.get()) {
            source.sendFailure(Component.translatable("match.smmorpg.disabled"));
            return 0;
        }
        if (MatchManager.inMatch(player.getUUID())) {
            source.sendFailure(Component.translatable("match.smmorpg.already_in_match"));
            return 0;
        }

        MatchQueue.join(player, mode);
        source.sendSuccess(() -> Component.translatable("match.smmorpg.queued",
                Component.translatable(mode.translationKey()),
                MatchQueue.waitingIn(mode)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MatchQueue.leaveAll(player);
        source.sendSuccess(() -> Component.translatable("match.smmorpg.left"), false);
        return 1;
    }

    private static int vault(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        com.smmorpg.vault.VaultOpener.open(source.getPlayerOrException());
        return 1;
    }

    /** Re-issues whatever the player's class kit is missing — after a death, usually. */
    private static int kit(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.smmorpg.kit.StarterKit.grant(player);
        source.sendSuccess(() -> Component.translatable("kit.smmorpg.granted")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    // --- co-op ---

    private static int invite(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer host = source.getPlayerOrException();
        if (host == target) return 0;

        var party = com.smmorpg.party.PartyManager.ensure(host);
        if (party.full()) {
            source.sendFailure(Component.translatable("party.smmorpg.full",
                    com.smmorpg.party.Party.MAX));
            return 0;
        }

        com.smmorpg.party.PartyManager.invite(host, target);

        target.sendSystemMessage(Component.translatable("party.smmorpg.invited",
                host.getGameProfile().getName()).withStyle(ChatFormatting.AQUA));
        source.sendSuccess(() -> Component.translatable("party.smmorpg.invite_sent",
                target.getGameProfile().getName()), false);
        return 1;
    }

    private static int accept(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        var party = com.smmorpg.party.PartyManager.accept(player, source.getServer());
        if (party == null) {
            source.sendFailure(Component.translatable("party.smmorpg.no_invite"));
            return 0;
        }

        for (ServerPlayer member : party.online(source.getServer())) {
            member.sendSystemMessage(Component.translatable("party.smmorpg.joined",
                    player.getGameProfile().getName(), party.size(),
                    com.smmorpg.party.Party.MAX).withStyle(ChatFormatting.GREEN));
        }

        // Joining a run means joining the run, so the newcomer is put in it rather than
        // left to find the door themselves.
        ServerPlayer leader = source.getServer().getPlayerList().getPlayer(party.leader());
        if (leader != null && com.smmorpg.training.TrainingManager.of(leader) != null) {
            com.smmorpg.training.TrainingManager.join(player, leader);
        }
        return 1;
    }

    private static int party(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var members = com.smmorpg.party.PartyManager.group(player);

        source.sendSuccess(() -> Component.translatable("party.smmorpg.header",
                members.size(), com.smmorpg.party.Party.MAX).withStyle(ChatFormatting.GOLD), false);
        for (ServerPlayer member : members) {
            source.sendSuccess(() -> Component.literal("  " + member.getGameProfile().getName())
                    .withStyle(ChatFormatting.GRAY), false);
        }
        source.sendSuccess(() -> Component.translatable("party.smmorpg.pressure",
                        Math.round((com.smmorpg.party.Party.pressure(members) - 1.0F) * 100.0F))
                .withStyle(ChatFormatting.DARK_GRAY), false);
        return 1;
    }

    private static int disband(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.smmorpg.party.PartyManager.leave(player);
        source.sendSuccess(() -> Component.translatable("party.smmorpg.left"), false);
        return 1;
    }

    private static int rank(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAccount account = AccountService.of(player);
        Rank rank = Rank.of(account.elo(), account.matches());

        source.sendSuccess(() -> Component.translatable(rank.translationKey())
                .withStyle(rank.color())
                .append(Component.literal("  " + account.elo() + " Elo")
                        .withStyle(ChatFormatting.GRAY)), false);

        source.sendSuccess(() -> Component.translatable("match.smmorpg.record",
                account.wins(), account.losses(),
                String.format("%.0f%%", account.winRate() * 100.0F))
                .withStyle(ChatFormatting.DARK_GRAY), false);

        int toNext = rank.toNext(account.elo());
        if (toNext > 0) {
            source.sendSuccess(() -> Component.translatable("match.smmorpg.to_next", toNext)
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }

    /** Operator view of whether the online half is actually working. */
    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Account service: " + (BackendClient.configured()
                        ? (BackendClient.reachable() ? "reachable" : "configured, unreachable")
                        : "local only")), false);
        source.sendSuccess(() -> Component.literal(
                "Queued writes waiting: " + OfflineQueue.size()), false);
        source.sendSuccess(() -> Component.literal(
                "Ranked: " + (ServerConfig.CFG.rankedEnabled.get() ? "on" : "off")
                        + ", active matches: " + MatchManager.activeMatches()), false);
        source.sendSuccess(() -> Component.literal(
                "Tebex: " + (com.smmorpg.market.TebexService.configured()
                        ? "configured" : "not configured")), false);
        return 1;
    }

    /** Exposed for the screens that will replace these commands. */
    public static int previewGain(PlayerAccount account, int opponentElo) {
        return EloSystem.previewGain(account.elo(), opponentElo, account.matches());
    }
}
