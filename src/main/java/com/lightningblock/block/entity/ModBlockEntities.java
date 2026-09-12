package com.lightningblock.block.entity;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LightningBlockMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<LightningBlockEntity>> LIGHTNING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("lightning_block",
                    () -> BlockEntityType.Builder.of(LightningBlockEntity::new,
                                    ModBlocks.LIGHTNING_BLOCK.get())
                            .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
