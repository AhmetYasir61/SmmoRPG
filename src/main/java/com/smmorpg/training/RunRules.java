package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import com.smmorpg.labyrinth.Labyrinth;
import com.smmorpg.labyrinth.LabyrinthData;
import com.smmorpg.labyrinth.RunSave;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * What a run costs when it goes wrong.
 *
 * <p>Three rules, and they are the same rule from three angles: what you find between two
 * saves is not yours yet.
 *
 * <ul>
 *   <li>Dying costs a life, not your bag. Five lives come with every save.
 *   <li>Spending the fifth one drops you back to the save before it, carrying exactly what
 *       you were carrying when you set it.
 *   <li>Logging out mid-run puts you back to your last save too. Quitting the moment
 *       something good drops is otherwise the safest way to play, and a dungeon where
 *       quitting is the optimal move is a broken dungeon.
 * </ul>
 *
 * <p>The way to keep something is to walk it to a safe cell and put it in the vault. That
 * is the whole loop, and it is why the vault sits where it does.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class RunRules {

    private RunRules() {}

    /** Nothing is dropped on death inside the labyrinth: the save decides what you keep. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TrainingManager.of(player) == null) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TrainingManager.of(player) == null) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        LabyrinthData data = LabyrinthData.get(world);
        RunSave save = data.save(player.getUUID());

        if (save == null) {
            // Never saved anywhere: the camp is the only floor there is.
            TrainingManager.teleportTo(player, world, Labyrinth.camp());
            com.smmorpg.kit.StarterKit.grant(player);
            return;
        }

        int left = save.lives() - 1;

        if (left > 0) {
            data.setLives(player.getUUID(), left);
            save.restore(player);
            TrainingManager.teleportTo(player, world, save.cell());

            player.sendSystemMessage(Component.translatable("training.smmorpg.life_lost", left)
                    .withStyle(ChatFormatting.RED));
            return;
        }

        RunSave older = data.fallBack(player.getUUID());
        if (older == null) older = save;

        older.restore(player);
        TrainingManager.teleportTo(player, world, older.cell());

        player.sendSystemMessage(Component.translatable("training.smmorpg.fell_back",
                        Labyrinth.cellX(older.cell()), Labyrinth.cellZ(older.cell()))
                .withStyle(ChatFormatting.DARK_RED));
    }

    /** Leaving mid-run rolls you back to your last save rather than banking the loot. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TrainingManager.of(player) == null) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        RunSave save = LabyrinthData.get(world).save(player.getUUID());
        if (save != null) save.restore(player);

        TrainingManager.stop(player);
    }
}
