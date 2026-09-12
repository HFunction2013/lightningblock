package com.lightningblock.tab;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LightningBlockMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> LIGHTNING_TAB = TABS.register("lightning_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.LIGHTNING_BLOCK.get()))
                    .title(Component.translatable("itemGroup.lightningblock"))
                    .displayItems((params, output) -> output.accept(ModBlocks.LIGHTNING_BLOCK.get()))
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
