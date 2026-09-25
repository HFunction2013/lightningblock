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

    public static final RegistryObject<Item> LIGHTNING_ORE = ITEMS.register("lightning_ore",
            () -> new BlockItem(ModBlocks.LIGHTNING_ORE.get(), new Item.Properties()));

    // Lightning Core — stack up to 9, grants fire/explosion/lightning immunity
    // and strikes nearby hostile mobs with lightning while held.
    public static final RegistryObject<Item> LIGHTNING_CORE = ITEMS.register("lightning_core",
            () -> new LightningCoreItem(new Item.Properties().stacksTo(9)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
