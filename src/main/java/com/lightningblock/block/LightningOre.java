package com.lightningblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;

import java.util.Optional;
/**
 * Lightning Ore — visually and stat-wise identical to obsidian.
 * Can be used as a nether portal frame: when placed (or when a neighbour
 * changes), it briefly masquerades as obsidian so vanilla PortalShape
 * detection picks up the frame, then swaps back to Lightning Ore.
 * Smelting yields a Lightning Core.
 */
public class LightningOre extends Block {

    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE;

    public LightningOre(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        trySpawnPortalWithOre(state, level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // Only re-check portal frame when an obsidian / lightning ore neighbour changes,
        // to avoid spamming detection during world generation.
        BlockState neighbor = level.getBlockState(fromPos);
        if (neighbor.is(Blocks.OBSIDIAN) || neighbor.is(ModBlocks.LIGHTNING_ORE.get())) {
            trySpawnPortalWithOre(state, level, pos);
        }
    }

    /**
     * Temporarily replace this block with obsidian, let vanilla PortalShape
     * detect a valid portal frame (which may include other Lightning Ore blocks),
     * then restore this block.  Any portal blocks generated remain.
     */
    private void trySpawnPortalWithOre(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), SILENT);
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, pos, axis);
            if (shape.isPresent()) {
                shape.get().createPortalBlocks();
                break;
            }
        }
        level.setBlock(pos, state, SILENT);
    }
}
