package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import com.smmorpg.labyrinth.Labyrinth;
import com.smmorpg.labyrinth.LabyrinthData;
import com.smmorpg.labyrinth.RunSave;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Saving, and getting back.
 *
 * <p>A labyrinth you have to walk out of is a labyrinth nobody explores. Every safe cell
 * has a lodestone in the middle of it: touch one out in the maze and it remembers where
 * you are and sends you to the camp; touch the camp's own and it sends you back to
 * whatever you last remembered. So going deep costs a walk once, not every time.
 */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID)
public final class Checkpoints {

    private Checkpoints() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        if (!TrainingArena.isArenaWorld(world)) return;
        if (!world.getBlockState(event.getPos()).is(Blocks.LODESTONE)) return;

        event.setCanceled(true);

        long here = Labyrinth.cellAt(player.position());
        if (!Labyrinth.isSafe(world, here)) return;

        LabyrinthData data = LabyrinthData.get(world);

        if (Labyrinth.isCamp(here)) {
            RunSave save = data.save(player.getUUID());
            if (save == null || Labyrinth.isCamp(save.cell())) {
                say(player, "training.smmorpg.no_checkpoint", ChatFormatting.GRAY);
                return;
            }
            TrainingManager.teleportTo(player, world, save.cell());
            player.sendSystemMessage(Component.translatable("training.smmorpg.resumed",
                            Labyrinth.cellX(save.cell()), Labyrinth.cellZ(save.cell()), save.lives())
                    .withStyle(ChatFormatting.AQUA));
            return;
        }

        // The save is the picture, not the pin. What you are carrying now is what you get
        // back if the next stretch goes badly — and what you put in the vault first is
        // what no stretch can take.
        data.addSave(player.getUUID(), RunSave.of(player, here));

        TrainingManager.teleportTo(player, world, Labyrinth.camp());
        player.sendSystemMessage(Component.translatable("training.smmorpg.saved",
                        Labyrinth.cellX(here), Labyrinth.cellZ(here), RunSave.LIVES)
                .withStyle(ChatFormatting.GREEN));
    }

    private static void say(ServerPlayer player, String key, ChatFormatting colour) {
        player.sendSystemMessage(Component.translatable(key).withStyle(colour));
    }
}
