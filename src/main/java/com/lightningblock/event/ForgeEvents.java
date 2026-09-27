package com.lightningblock.event;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.LightningPortalBlock;
import com.lightningblock.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = LightningBlockMod.MOD_ID)
public class ForgeEvents {

    /**
     * Lightning strikes a dragon egg -> convert the egg into a Lightning Block.
     */
    @SubscribeEvent
    public static void onLightningStrike(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(event.getEntity() instanceof LightningBolt bolt)) return;

        BlockPos strikePos = bolt.blockPosition();

        // Check the strike block and its immediate neighbours for a dragon egg
        for (BlockPos p : BlockPos.betweenClosed(
                strikePos.offset(-1, -1, -1), strikePos.offset(1, 1, 1))) {
            if (level.getBlockState(p).is(Blocks.DRAGON_EGG)) {
                level.setBlock(p, ModBlocks.LIGHTNING_BLOCK.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    /**
     * Flint & steel on an obsidian / lightning-ore frame that contains at least
     * one Lightning Ore block → ignite a Lightning Portal.  Pure-obsidian frames
     * are left to vanilla (normal Nether Portal).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (event.getItemStack().getItem() != Items.FLINT_AND_STEEL) return;

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(Blocks.OBSIDIAN) && !clickedState.is(ModBlocks.LIGHTNING_ORE.get())) return;

        Direction face = event.getFace();
        if (face == null) return;
        BlockPos airPos = clickedPos.relative(face);
        if (!level.getBlockState(airPos).isAir()) return;

        // Try the clicked face's axis first (matches vanilla flint & steel behaviour),
        // then fall back to the other horizontal axis.  This avoids creating a portal
        // with the wrong orientation when both axes happen to match something.
        Direction.Axis faceAxis = face.getAxis();
        List<Direction.Axis> axes = new ArrayList<>();
        if (faceAxis == Direction.Axis.X || faceAxis == Direction.Axis.Z) {
            axes.add(faceAxis);
            axes.add(faceAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        } else {
            axes.add(Direction.Axis.X);
            axes.add(Direction.Axis.Z);
        }

        for (Direction.Axis axis : axes) {
            Optional<LightningPortalBlock.FrameInfo> frame = LightningPortalBlock.findFrame(level, airPos, axis);
            if (frame.isPresent() && frame.get().hasLightningOre) {
                LightningPortalBlock.fillPortal(level, frame.get(), axis);
                level.playSound(null, airPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                Player player = event.getEntity();
                if (player != null) {
                    event.getItemStack().hurtAndBreak(1, player,
                            p -> p.broadcastBreakEvent(event.getHand()));
                }
                event.setCanceled(true);
                return;
            }
        }
    }
}
