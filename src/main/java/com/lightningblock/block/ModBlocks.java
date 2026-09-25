package com.lightningblock.block;

import com.lightningblock.LightningBlockMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LightningBlockMod.MOD_ID);

    // Obsidian hardness = 50, blast resistance = 1200.
    // Lightning Block hardness = 100 (2x obsidian), blast resistance kept at 1200.
    public static final RegistryObject<Block> LIGHTNING_BLOCK = BLOCKS.register("lightning_block",
            () -> new LightningBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(100.0F, 1200.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // Lightning Ore — identical stats to obsidian, can frame nether portals
    public static final RegistryObject<Block> LIGHTNING_ORE = BLOCKS.register("lightning_ore",
            () -> new LightningOre(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
