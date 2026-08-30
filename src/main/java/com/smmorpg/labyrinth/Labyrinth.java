package com.smmorpg.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * The arena, as a place rather than an event.
 *
 * <p>One labyrinth per world, laid out from the origin on a grid of chunk-sized cells and
 * written into the world a cell at a time as somebody walks towards it. Nothing is ever
 * rebuilt: a cell you cleared last week is the cell you walk back into, with the same
 * walls in the same places.
 *
 * <p>The layout is not stored, only derived. Whether two neighbouring cells are joined is
 * a hash of the world seed and the two cells' coordinates, computed the same way from
 * either side — so the maze is identical every time it is asked about, and asking about a
 * cell a thousand away costs nothing.
 *
 * <p>Roughly three in five neighbours are joined. That is deliberately above the point at
 * which a grid of random connections stops being islands and becomes one connected sprawl,
 * so there is always a way onward without any cell being open on every side.
 */
public final class Labyrinth {

    /** One cell is one chunk, so generation lines up with the chunks the game loads. */
    public static final int CELL = 16;

    /** Walls tall enough that leaving is something you do through a door. */
    private static final int WALL_HEIGHT = 14;

    /** Headroom above the walls, for air jumps and wall runs that should not hit a ceiling. */
    private static final int CLEARANCE = 20;

    /** Half-width of a doorway, in blocks either side of the cell centre line. */
    private static final int DOOR_HALF = 3;

    /** Chance that any given pair of neighbouring cells is joined. */
    private static final int JOIN_PERCENT = 62;

    /** One cell in this many is a safe room. */
    private static final int SAFE_EVERY = 7;

    /** Cells waiting to be written into the world, so a walk never stalls the server. */
    private static final Deque<Long> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private Labyrinth() {}

    // --- cell arithmetic ---

    public static long cell(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public static int cellX(long cell) { return (int) (cell >> 32); }

    public static int cellZ(long cell) { return (int) cell; }

    public static long cellAt(Vec3 pos) {
        return cell(Math.floorDiv((int) Math.floor(pos.x), CELL),
                Math.floorDiv((int) Math.floor(pos.z), CELL));
    }

    /** The block you stand on. The training world is flat, so this is the same everywhere. */
    public static int floorY(ServerLevel level) {
        return level.getMinBuildHeight() + 3;
    }

    /** Standing position at the middle of a cell. */
    public static Vec3 centreOf(ServerLevel level, long cell) {
        return new Vec3(cellX(cell) * CELL + CELL / 2.0D,
                floorY(level) + 1.0D,
                cellZ(cell) * CELL + CELL / 2.0D);
    }

    /** Cell (0,0) is the camp: always safe, always where you start and return to. */
    public static long camp() { return cell(0, 0); }

    public static boolean isCamp(long cell) { return cell == camp(); }

    /** How far into the labyrinth a cell is, in cells, measured from the camp. */
    public static int depth(long cell) {
        return Math.max(Math.abs(cellX(cell)), Math.abs(cellZ(cell)));
    }

    // --- the layout, derived rather than stored ---

    public static boolean isSafe(ServerLevel level, long cell) {
        if (isCamp(cell)) return true;
        return Math.floorMod(hash(level.getSeed(), cellX(cell), cellZ(cell), 7717), SAFE_EVERY) == 0;
    }

    /**
     * Whether you can walk from one cell to the one next to it.
     *
     * <p>Asked from either side it hashes the same pair in the same order, so the two cells
     * never disagree about whether there is a doorway between them.
     */
    public static boolean joined(ServerLevel level, long a, long b) {
        int ax = cellX(a), az = cellZ(a), bx = cellX(b), bz = cellZ(b);
        if (Math.abs(ax - bx) + Math.abs(az - bz) != 1) return false;

        int lowX = Math.min(ax, bx), lowZ = Math.min(az, bz);
        int axis = ax == bx ? 1 : 0;

        // The camp must never be sealed in, whatever the hash says about its doorways.
        if (isCamp(a) || isCamp(b)) return true;

        return Math.floorMod(hash(level.getSeed(), lowX, lowZ, 31 + axis), 100) < JOIN_PERCENT;
    }

    private static int hash(long seed, int x, int z, int salt) {
        long h = seed * 6364136223846793005L + salt;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) h;
    }

    // --- writing it into the world ---

    /** Queues every unbuilt cell within {@code radius} cells of a position. */
    public static void ensureAround(ServerLevel level, Vec3 pos, int radius) {
        LabyrinthData data = LabyrinthData.get(level);
        long here = cellAt(pos);
        int cx = cellX(here), cz = cellZ(here);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long cell = cell(cx + dx, cz + dz);
                if (data.isBuilt(cell) || QUEUED.contains(cell)) continue;
                QUEUE.add(cell);
                QUEUED.add(cell);
            }
        }
    }

    /**
     * Writes at most one queued cell per call.
     *
     * <p>A cell is several thousand block changes. Doing a whole neighbourhood in one tick
     * is what turns walking into a stutter, so the queue drains at walking pace instead.
     */
    public static void tickBuild(ServerLevel level) {
        Long cell = QUEUE.poll();
        if (cell == null) return;
        QUEUED.remove(cell);

        LabyrinthData data = LabyrinthData.get(level);
        if (data.isBuilt(cell)) return;

        build(level, cell);
        data.markBuilt(cell);
    }

    /** Writes one cell straight away, skipping the queue. Used before a teleport. */
    public static void buildNow(ServerLevel level, long cell) {
        LabyrinthData data = LabyrinthData.get(level);
        if (data.isBuilt(cell)) return;
        build(level, cell);
        data.markBuilt(cell);
    }

    private static void build(ServerLevel level, long cell) {
        int baseX = cellX(cell) * CELL;
        int baseZ = cellZ(cell) * CELL;
        int floorY = floorY(level);

        boolean safe = isSafe(level, cell);
        BlockState floor = safe
                ? Blocks.POLISHED_DIORITE.defaultBlockState()
                : Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState wall = safe
                ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < CELL; x++) {
            for (int z = 0; z < CELL; z++) {
                pos.set(baseX + x, floorY, baseZ + z);
                set(level, pos, floor);

                boolean isWall = wallAt(level, cell, x, z);
                for (int dy = 1; dy <= CLEARANCE; dy++) {
                    pos.set(baseX + x, floorY + dy, baseZ + z);
                    set(level, pos, isWall && dy <= WALL_HEIGHT
                            ? wall
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }

        light(level, cell, safe);
        if (safe) placeCheckpoint(level, cell);
    }

    /**
     * Whether this block of the cell is part of its wall.
     *
     * <p>The wall runs along the cell's own edge, with a doorway punched through the middle
     * of any edge whose neighbour is joined to this one.
     */
    private static boolean wallAt(ServerLevel level, long cell, int x, int z) {
        int cx = cellX(cell), cz = cellZ(cell);
        int mid = CELL / 2;

        if (z == 0) {
            return !(joined(level, cell, cell(cx, cz - 1)) && Math.abs(x - mid) <= DOOR_HALF);
        }
        if (z == CELL - 1) {
            return !(joined(level, cell, cell(cx, cz + 1)) && Math.abs(x - mid) <= DOOR_HALF);
        }
        if (x == 0) {
            return !(joined(level, cell, cell(cx - 1, cz)) && Math.abs(z - mid) <= DOOR_HALF);
        }
        if (x == CELL - 1) {
            return !(joined(level, cell, cell(cx + 1, cz)) && Math.abs(z - mid) <= DOOR_HALF);
        }
        return false;
    }

    /** Safe cells are lit like rooms; the rest get just enough to fight in. */
    private static void light(ServerLevel level, long cell, boolean safe) {
        int baseX = cellX(cell) * CELL;
        int baseZ = cellZ(cell) * CELL;
        int y = floorY(level) + WALL_HEIGHT;

        int[][] spots = safe
                ? new int[][]{{4, 4}, {11, 4}, {4, 11}, {11, 11}, {8, 8}}
                : new int[][]{{8, 8}};

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] spot : spots) {
            pos.set(baseX + spot[0], y, baseZ + spot[1]);
            set(level, pos, safe
                    ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.SHROOMLIGHT.defaultBlockState());
        }
    }

    /**
     * The stone you touch to save.
     *
     * <p>A lodestone, because it already means "a place you can come back to" and needs no
     * explaining. In the camp it sends you back to wherever you last saved; anywhere else
     * it saves where you are and sends you to the camp.
     */
    private static void placeCheckpoint(ServerLevel level, long cell) {
        BlockPos pos = new BlockPos(cellX(cell) * CELL + CELL / 2,
                floorY(level) + 1,
                cellZ(cell) * CELL + CELL / 2);
        set(level, pos, Blocks.LODESTONE.defaultBlockState());
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) == state) return;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
