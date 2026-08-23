package com.smmorpg.training;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Lays down the arena itself.
 *
 * <p>The training world is already flat, so this is not about levelling ground — it is
 * about making the fight legible. A 32x32 floor you can read your own footwork against, a
 * wall that tells you where the arena ends before a bot backs you into it, and light, so a
 * session started at midnight is not fought blind.
 *
 * <p>32 across is deliberate: small enough that a fight stays a fight rather than a chase,
 * and still wide enough for a dash or a wall run to mean something.
 *
 * <p>Built once per session and only in the dedicated training world. Pressing the button
 * inside your own survival world must never rearrange it.
 */
public final class TrainingArena {

    /** The name {@code TrainingLauncher} gives the dedicated world. */
    public static final String LEVEL_NAME = "SmmoRPG Training";

    /** Side length of the fighting surface, in blocks. */
    private static final int SIZE = 32;
    /** Half-open bounds, so the floor is exactly SIZE across rather than SIZE + 1. */
    private static final int MIN = -SIZE / 2;
    private static final int MAX = SIZE / 2 - 1;

    private static final int WALL_HEIGHT = 4;
    private static final int CLEARANCE = 24;       // headroom for air jumps and wall runs

    private TrainingArena() {}

    /** True only in the world SmmoRPG created for training. */
    public static boolean isArenaWorld(ServerLevel level) {
        return level.getServer() != null
                && LEVEL_NAME.equals(level.getServer().getWorldData().getLevelName());
    }

    public static void build(ServerLevel level, Vec3 centre) {
        if (!isArenaWorld(level)) return;

        BlockPos origin = BlockPos.containing(centre.x, centre.y, centre.z);
        int floorY = origin.getY() - 1;

        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState grid = Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState();
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState cap = Blocks.STONE_BRICK_SLAB.defaultBlockState();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = MIN; dx <= MAX; dx++) {
            for (int dz = MIN; dz <= MAX; dz++) {
                boolean edge = dx == MIN || dx == MAX || dz == MIN || dz == MAX;

                // A grid every eight blocks. Without it a flat floor gives the eye nothing
                // to judge distance against, and spacing is most of what a fight is.
                boolean gridLine = !edge && (dx % 8 == 0 || dz % 8 == 0);

                pos.set(origin.getX() + dx, floorY, origin.getZ() + dz);
                setIfChanged(level, pos, gridLine ? grid : floor);

                clearColumn(level, pos, origin.getX() + dx, floorY, origin.getZ() + dz);

                if (edge) buildWall(level, pos, origin.getX() + dx, floorY, origin.getZ() + dz, wall, cap);
            }
        }

        lightCorners(level, origin, floorY);
        // Frozen daylight means whatever hour the session opened in is the hour it keeps.
        level.setDayTime(6000L);
    }

    private static void clearColumn(ServerLevel level, BlockPos.MutableBlockPos pos,
                                    int x, int floorY, int z) {
        for (int dy = 1; dy <= CLEARANCE; dy++) {
            pos.set(x, floorY + dy, z);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void buildWall(ServerLevel level, BlockPos.MutableBlockPos pos,
                                  int x, int floorY, int z, BlockState wall, BlockState cap) {
        for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
            pos.set(x, floorY + dy, z);
            setIfChanged(level, pos, dy == WALL_HEIGHT ? cap : wall);
        }
    }

    /** Lanterns on the corners and midpoints, high enough not to be swung through. */
    private static void lightCorners(ServerLevel level, BlockPos origin, int floorY) {
        int[][] spots = {
                {MIN, MIN}, {MAX, MIN}, {MIN, MAX}, {MAX, MAX},
                {0, MIN}, {0, MAX}, {MIN, 0}, {MAX, 0},
        };
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] spot : spots) {
            for (int dy = 1; dy <= WALL_HEIGHT + 1; dy++) {
                pos.set(origin.getX() + spot[0], floorY + dy, origin.getZ() + spot[1]);
                // The lantern sits one course above the wall so it lights the floor rather
                // than the top of the bricks.
                setIfChanged(level, pos, dy > WALL_HEIGHT
                        ? Blocks.SEA_LANTERN.defaultBlockState()
                        : Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            }
        }
    }

    /** Skips the block update entirely when the block is already what we want. */
    private static void setIfChanged(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) == state) return;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
