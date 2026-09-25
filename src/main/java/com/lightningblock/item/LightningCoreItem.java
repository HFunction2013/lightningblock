package com.lightningblock.item;

import net.minecraft.world.item.Item;

/**
 * Lightning Core — obtained by smelting Lightning Ore for half a MC day.
 * While held in either hand:
 *  - grants immunity to fire, lava, explosion, and lightning damage
 *  - strikes hostile mobs within a 10-block radius with lightning
 * Effects are handled in {@link com.lightningblock.event.LightningCoreEvents}.
 */
public class LightningCoreItem extends Item {
    public LightningCoreItem(Properties properties) {
        super(properties);
    }
}
