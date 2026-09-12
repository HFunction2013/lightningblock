package com.lightningblock.item;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LightningBlockMod.MOD_ID);

    public static final RegistryObject<Item> LIGHTNING_BLOCK = ITEMS.register("lightning_block",
            () -> new BlockItem(ModBlocks.LIGHTNING_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
