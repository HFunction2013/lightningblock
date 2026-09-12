package com.lightningblock.menu;

import com.lightningblock.LightningBlockMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, LightningBlockMod.MOD_ID);

    public static final RegistryObject<MenuType<LightningBlockMenu>> LIGHTNING_BLOCK_MENU =
            MENUS.register("lightning_block",
                    () -> IForgeMenuType.create(LightningBlockMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
