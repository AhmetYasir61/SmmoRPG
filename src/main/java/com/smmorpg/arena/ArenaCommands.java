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
