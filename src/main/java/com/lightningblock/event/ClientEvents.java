package com.lightningblock.event;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import com.lightningblock.client.LightningBlockScreen;
import com.lightningblock.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LightningBlockMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.LIGHTNING_BLOCK_MENU.get(), LightningBlockScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHTNING_PORTAL.get(), RenderType.translucent());
        });
    }
}
