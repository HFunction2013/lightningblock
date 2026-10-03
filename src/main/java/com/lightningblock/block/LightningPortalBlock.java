package com.lightningblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Lightning Portal — looks and behaves exactly like a vanilla Nether Portal,
 * but accepts both Obsidian and Lightning Ore as valid frame blocks.
 *
 * Size constraints match vanilla: interior width 2..21, height 3..21.
 *
 * Ignited by flint & steel on a frame that contains at least one Lightning Ore.
 * Pure-obsidian frames still produce vanilla Nether Portals (handled by vanilla).
 */
public class LightningPortalBlock extends NetherPortalBlock {

    public LightningPortalBlock(Properties properties) {
        super(properties);
    }

    // -----------------------------------------------------------------------
    // Frame validation
    // -----------------------------------------------------------------------

    /** A block counts as portal frame if it is obsidian or lightning ore. */
    public static boolean isFrameBlock(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(ModBlocks.LIGHTNING_ORE.get());
    }

    /** The in-plane horizontal direction for a given portal axis. */
    private static Direction horizontalDir(Direction.Axis portalAxis) {
        // axis=X → portal plane is X-Y, width runs along X (EAST/WEST)
        // axis=Z → portal plane is Z-Y, width runs along Z (SOUTH/NORTH)
        return portalAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * Check whether a Lightning Portal block at {@code pos} is still inside a
     * complete, correctly-sized rectangular frame of obsidian / lightning ore.
     * Used by {@link #updateShape} to break the portal when the frame is damaged.
     */
    public static boolean isValidPortalAt(LevelAccessor level, BlockPos pos, Direction.Axis portalAxis) {
        Direction h = horizontalDir(portalAxis);

        // -- walk left / right / down / up, requiring portal blocks in between --
        int leftDist = walkUntil(level, pos, h.getOpposite(), 22);
        if (leftDist == Integer.MIN_VALUE) return false;
        int rightDist = walkUntil(level, pos, h, 22);
        if (rightDist == Integer.MIN_VALUE) return false;
        int downDist = walkUntil(level, pos, Direction.DOWN, 22);
        if (downDist == Integer.MIN_VALUE) return false;
        int upDist = walkUntil(level, pos, Direction.UP, 22);
        if (upDist == Integer.MIN_VALUE) return false;

        int width = rightDist - leftDist - 1;   // interior block count (horizontal)
        int height = upDist - downDist - 1;     // interior block count (vertical)
        if (width < 2 || width > 21 || height < 3 || height > 21) return false;

        // -- bottom-left frame corner --
        BlockPos bl = pos;
        for (int i = 0; i < -leftDist; i++) bl = bl.relative(h.getOpposite());
        for (int i = 0; i < -downDist; i++) bl = bl.below();

        // -- verify all four edges are frame blocks --
        Direction right = h;
        for (int x = 0; x <= width + 1; x++) {
            if (!isFrameBlock(level.getBlockState(bl.relative(right, x)))) return false;
            if (!isFrameBlock(level.getBlockState(bl.relative(right, x).above(height + 1)))) return false;
        }
        for (int y = 0; y <= height + 1; y++) {
            if (!isFrameBlock(level.getBlockState(bl.above(y)))) return false;
            if (!isFrameBlock(level.getBlockState(bl.relative(right, width + 1).above(y)))) return false;
        }
        return true;
    }

    /**
     * Walk from {@code start} in {@code dir} until a frame block is found.
     * Every block in between must be a Lightning Portal block.
     *
     * @return distance (positive in dir direction) or {@link Integer#MIN_VALUE} on failure.
     */
    private static int walkUntil(LevelAccessor level, BlockPos start, Direction dir, int max) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        int dist = 0;
        while (dist < max) {
            cursor.move(dir);
            dist++;
            BlockState s = level.getBlockState(cursor);
            if (isFrameBlock(s)) return dist;
            if (!s.is(ModBlocks.LIGHTNING_PORTAL.get())) return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    // -----------------------------------------------------------------------
    // Frame discovery (for flint & steel ignition)
    // -----------------------------------------------------------------------

    /** Info about a discovered valid portal frame. */
    public static final class FrameInfo {
        public final BlockPos bottomLeft;
        public final int width;
        public final int height;
        public final boolean hasLightningOre;

        public FrameInfo(BlockPos bottomLeft, int width, int height, boolean hasLightningOre) {
            this.bottomLeft = bottomLeft;
            this.width = width;
            this.height = height;
            this.hasLightningOre = hasLightningOre;
        }
    }

    /**
     * Try to discover a complete rectangular frame starting from an air block
     * {@code pos}, assuming the portal will have the given {@code portalAxis}.
     *
     * @return frame info if valid, empty otherwise.
     */
    public static Optional<FrameInfo> findFrame(LevelAccessor level, BlockPos pos, Direction.Axis portalAxis) {
        Direction h = horizontalDir(portalAxis);

        // Walk left to find left frame column
        int leftDist = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (leftDist > -22) {
            cursor.move(h.getOpposite());
            leftDist--;
            BlockState s = level.getBlockState(cursor);
            if (isFrameBlock(s)) break;
            if (!s.isAir()) return Optional.empty();
        }
        if (leftDist <= -22) return Optional.empty();

        // Walk down to find bottom frame row
        int downDist = 0;
        cursor.set(pos);
        while (downDist > -22) {
            cursor.move(Direction.DOWN);
            downDist--;
            BlockState s = level.getBlockState(cursor);
            if (isFrameBlock(s)) break;
            if (!s.isAir()) return Optional.empty();
        }
        if (downDist <= -22) return Optional.empty();

        // bottom-left corner
        BlockPos bl = pos;
        for (int i = 0; i < -leftDist; i++) bl = bl.relative(h.getOpposite());
        for (int i = 0; i < -downDist; i++) bl = bl.below();

        // Measure bottom row width (count frame blocks from corner rightward)
        int frameWidth = 0;
        cursor.set(bl);
        while (frameWidth < 23) {
            cursor.move(h);
            if (!isFrameBlock(level.getBlockState(cursor))) break;
            frameWidth++;
        }
        // frameWidth = number of additional frame blocks after corner; total bottom row = frameWidth + 1
        int interiorWidth = frameWidth - 1; // subtract right corner
        if (interiorWidth < 2 || interiorWidth > 21) return Optional.empty();

        // Measure left column height
        int frameHeight = 0;
        cursor.set(bl);
        while (frameHeight < 23) {
            cursor.move(Direction.UP);
            if (!isFrameBlock(level.getBlockState(cursor))) break;
            frameHeight++;
        }
        int interiorHeight = frameHeight - 1;
        if (interiorHeight < 3 || interiorHeight > 21) return Optional.empty();

        BlockPos topLeft = bl.above(frameHeight);
        BlockPos bottomRight = bl.relative(h, frameWidth);

        // Validate top edge
        for (int x = 0; x <= frameWidth; x++) {
            if (!isFrameBlock(level.getBlockState(topLeft.relative(h, x)))) return Optional.empty();
        }
        // Validate right edge
        for (int y = 0; y <= frameHeight; y++) {
            if (!isFrameBlock(level.getBlockState(bottomRight.above(y)))) return Optional.empty();
        }

        // Validate interior is air (or already a lightning portal) and check for lightning ore in frame
        boolean hasLightningOre = false;
        for (int y = 0; y <= frameHeight; y++) {
            for (int x = 0; x <= frameWidth; x++) {
                BlockPos p = bl.relative(h, x).above(y);
                boolean isEdge = (x == 0 || x == frameWidth || y == 0 || y == frameHeight);
                BlockState s = level.getBlockState(p);
                if (isEdge) {
                    if (s.is(ModBlocks.LIGHTNING_ORE.get())) hasLightningOre = true;
                } else {
                    if (!s.isAir() && !s.is(ModBlocks.LIGHTNING_PORTAL.get())) return Optional.empty();
                }
            }
        }

        return Optional.of(new FrameInfo(bl, interiorWidth, interiorHeight, hasLightningOre));
    }

    /**
     * Fill the interior of a validated frame with Lightning Portal blocks.
     */
    public static void fillPortal(Level level, FrameInfo info, Direction.Axis portalAxis) {
        Direction h = horizontalDir(portalAxis);
        BlockState portalState = ModBlocks.LIGHTNING_PORTAL.get().defaultBlockState()
                .setValue(NetherPortalBlock.AXIS, portalAxis);
        for (int y = 1; y <= info.height; y++) {
            for (int x = 1; x <= info.width; x++) {
                BlockPos p = info.bottomLeft.relative(h, x).above(y);
                level.setBlock(p, portalState, Block.UPDATE_ALL);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Block behaviour overrides
    // -----------------------------------------------------------------------

    /**
     * Register a NETHER_PORTAL point-of-interest so that vanilla PortalForter
     * can find this portal when looking for a teleport destination.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.getPoiManager().add(pos,
                    ForgeRegistries.POI_TYPES.getHolder(PoiTypes.NETHER_PORTAL).orElseThrow(() -> 
                    new IllegalStateException("NETHER_PORTAL POI doesn't exist!")
                ));
        }
    }

    /**
     * Remove the POI when the portal block is destroyed.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.getPoiManager().remove(pos);
        }
    }

    /**
     * When a neighbour changes, re-validate the frame.  Unlike vanilla
     * NetherPortalBlock (which uses PortalShape, obsidian-only), we use our
     * own validator that accepts both obsidian and lightning ore.
     */
    @Override
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighborState,
    @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        Direction.Axis neighborAxis = direction.getAxis();
        Direction.Axis portalAxis = state.getValue(AXIS);

        // Only re-check when the changed neighbour is perpendicular to the portal plane
        boolean relevant = (neighborAxis != portalAxis) && neighborAxis.isHorizontal();
        if (!relevant || neighborState.is(this)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }

        if (isValidPortalAt(level, pos, portalAxis)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        return Blocks.AIR.defaultBlockState();
    }
}
