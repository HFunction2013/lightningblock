package com.lightningblock;

import com.lightningblock.block.ModBlocks;
import com.lightningblock.block.entity.ModBlockEntities;
import com.lightningblock.item.ModItems;
import com.lightningblock.menu.ModMenuTypes;
import com.lightningblock.network.ModMessages;
import com.lightningblock.tab.ModCreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LightningBlockMod.MOD_ID)
public class LightningBlockMod {
    public static final String MOD_ID = "lightningblock";

    public LightningBlockMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(bus);
        ModItems.register(bus);
        ModBlockEntities.register(bus);
        ModMenuTypes.register(bus);
        ModCreativeTabs.register(bus);

        ModMessages.register();

        MinecraftForge.EVENT_BUS.register(this);
    }
}
