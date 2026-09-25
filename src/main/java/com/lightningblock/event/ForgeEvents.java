package com.lightningblock.event;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
}
