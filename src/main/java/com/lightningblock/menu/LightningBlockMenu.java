package com.lightningblock.menu;

import com.lightningblock.block.entity.LightningBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class LightningBlockMenu extends AbstractContainerMenu {

    private final LightningBlockEntity blockEntity;
    private final ContainerData data;

    // Server-side constructor
    public LightningBlockMenu(int id, Inventory inv, LightningBlockEntity be) {
        this(id, inv, be, be.getDataAccess());
    }

    // Client-side constructor (called from network buffer)
    public LightningBlockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (LightningBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    private LightningBlockMenu(int id, Inventory inv, LightningBlockEntity be, ContainerData data) {
        super(ModMenuTypes.LIGHTNING_BLOCK_MENU.get(), id);
        this.blockEntity = be;
        this.data = data;
        checkContainerDataCount(data, 3);
        this.addDataSlots(data);

        IItemHandler handler = be.getItemHandler();
        // Input slots
        this.addSlot(new FilteredSlot(handler, LightningBlockEntity.SLOT_GOLD_OR_STAR, 40, 32));
        this.addSlot(new FilteredSlot(handler, LightningBlockEntity.SLOT_EMERALD, 40, 70));
        this.addSlot(new FilteredSlot(handler, LightningBlockEntity.SLOT_DIAMOND, 40, 108));

        // Player inventory (3 rows) — centred in the wider GUI
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 34 + col * 18, 140 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 34 + col * 18, 198));
        }
    }

    public LightningBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getRemaining() {
        return data.get(0);
    }

    public int getFreq() {
        return data.get(1);
    }

    public int getAreaSide() {
        return data.get(2);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 3) {
                // from input slots to player inventory
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // from player inventory to matching input slot
                boolean moved = false;
                if (stack.is(Items.GOLD_BLOCK) || stack.is(Items.NETHER_STAR)) {
                    moved = this.moveItemStackTo(stack, 0, 1, false);
                } else if (stack.is(Items.EMERALD_BLOCK)) {
                    moved = this.moveItemStackTo(stack, 1, 2, false);
                } else if (stack.is(Items.DIAMOND_BLOCK)) {
                    moved = this.moveItemStackTo(stack, 2, 3, false);
                }
                if (!moved) {
                    if (index < 30) {
                        if (!this.moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
                    } else if (index < 39) {
                        if (!this.moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
                    }
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /**
     * Slot that may briefly hold a stack before the BE consumes it.
     * Only the diamond input slot allows up to 512 (128 side * 4);
     * player inventory slots are untouched.
     */
    private static class FilteredSlot extends SlotItemHandler {
        public FilteredSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return this.getSlotIndex() == LightningBlockEntity.SLOT_DIAMOND ? 512 : 64;
        }
    }
}
