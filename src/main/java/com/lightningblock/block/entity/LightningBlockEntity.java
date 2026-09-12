package com.lightningblock.block.entity;

import com.lightningblock.menu.LightningBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LightningBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_GOLD_OR_STAR = 0;
    public static final int SLOT_EMERALD = 1;
    public static final int SLOT_DIAMOND = 2;
    public static final int TICKS_PER_SECOND = 20;
    public static final String DEFAULT_FILTER = "@e[type=!player]";

    // remaining strikes: -1 means infinite, 0 means exhausted
    private int remaining = 0;
    // tick interval: <= 0 means never fire
    private int freq = -1;
    // cube side length centered on this block; 0 means no area; capped at 128 (radius 64)
    private int areaSide = 0;
    // cumulative diamond blocks inserted — drives areaSide = min(diamondTotal / 4, 128)
    private int diamondTotal = 0;
    private String filter = DEFAULT_FILTER;
    private int tickCounter = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_GOLD_OR_STAR -> stack.is(Items.GOLD_BLOCK) || stack.is(Items.NETHER_STAR);
                case SLOT_EMERALD -> stack.is(Items.EMERALD_BLOCK);
                case SLOT_DIAMOND -> stack.is(Items.DIAMOND_BLOCK);
                default -> false;
            };
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Refuse nether stars below 64 — only full stacks unlock infinite mode
            if (slot == SLOT_GOLD_OR_STAR && stack.is(Items.NETHER_STAR) && stack.getCount() < 64) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            // Diamond input slot accepts up to 512 (128 side * 4) so max area can be reached
            return slot == SLOT_DIAMOND ? 512 : 64;
        }

        /**
         * Fired by both {@code setStackInSlot} (GUI drag / shift-click) and
         * {@code insertItem} (hoppers / automation).  We apply the configuration
         * effect and immediately consume the input so the slot stays empty.
         */
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) return;

            applyItem(slot, stack);

            // Consume the item — clear the slot directly so we don't re-enter this callback
            this.stacks.set(slot, ItemStack.EMPTY);
        }
    };

    /**
     * Apply the configuration effect of an inserted item.
     * Gold Block  -> remaining += count * 10
     * Nether Star x64 -> remaining = infinite (-1)
     * Emerald Block -> freq = 32 / count * 20 ticks  (count=0 -> never)
     * Diamond Block -> cumulative; area side = min(diamondTotal / 4, 128)
     *                  (cube centred on block, max radius 64)
     */
    private void applyItem(int slot, ItemStack stack) {
        int count = stack.getCount();
        switch (slot) {
            case SLOT_GOLD_OR_STAR -> {
                if (stack.is(Items.GOLD_BLOCK)) {
                    if (remaining != -1) remaining += count * 10;
                } else if (stack.is(Items.NETHER_STAR) && count >= 64) {
                    remaining = -1;
                }
            }
            case SLOT_EMERALD -> {
                // freq = 32 / cnt * ticks_per_sec  =>  (32 * 20) / cnt = 640 / cnt
                freq = count > 0 ? (32 * TICKS_PER_SECOND) / count : -1;
                tickCounter = 0;
            }
            case SLOT_DIAMOND -> {
                diamondTotal += count;
                areaSide = Math.min(diamondTotal / 4, 128);
            }
        }
        setChanged();
    }

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    // Synced integer data: [remaining, freq, areaSide]
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> remaining;
                case 1 -> freq;
                case 2 -> areaSide;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> remaining = value;
                case 1 -> freq = value;
                case 2 -> areaSide = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public LightningBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTNING_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LightningBlockEntity be) {
        if (level.isClientSide) return;
        if (be.freq <= 0) return;
        be.tickCounter++;
        if (be.tickCounter < be.freq) return;
        be.tickCounter = 0;
        if (be.remaining == 0) return;

        be.strikeLightning(level, pos);

        if (be.remaining > 0) {
            be.remaining--;
        }
        be.setChanged();
    }

    private void strikeLightning(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (areaSide <= 0) return;

        AABB area = AABB.ofSize(Vec3.atCenterOf(pos), areaSide, areaSide, areaSide);
        List<? extends Entity> targets = selectEntities(serverLevel, pos, area);

        // Concurrently strike every target in the same tick
        for (Entity entity : targets) {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
            serverLevel.addFreshEntity(bolt);
        }
    }

    private List<? extends Entity> selectEntities(ServerLevel level, BlockPos pos, AABB area) {
        if (filter == null || filter.isBlank()) {
            return fallbackEntities(level, area);
        }
        try {
            net.minecraft.commands.CommandSourceStack source = level.getServer().createCommandSourceStack()
                    .withPosition(Vec3.atCenterOf(pos))
                    .withPermission(2)
                    .withLevel(level);
            net.minecraft.commands.arguments.selector.EntitySelectorParser parser =
                    new net.minecraft.commands.arguments.selector.EntitySelectorParser(
                            new com.mojang.brigadier.StringReader(filter));
            net.minecraft.commands.arguments.selector.EntitySelector selector = parser.parse();
            List<? extends Entity> selected = selector.findEntities(source);
            return selected.stream().filter(e -> area.contains(e.position())).toList();
        } catch (Exception ex) {
            return fallbackEntities(level, area);
        }
    }

    private List<? extends Entity> fallbackEntities(ServerLevel level, AABB area) {
        return level.getEntitiesOfClass(Entity.class, area,
                e -> e.isAlive() && !(e instanceof Player));
    }

    // ---- getters / setters ----
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = (filter == null || filter.isBlank()) ? DEFAULT_FILTER : filter;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getRemaining() {
        return remaining;
    }

    public int getFreq() {
        return freq;
    }

    public int getAreaSide() {
        return areaSide;
    }

    // ---- NBT ----
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("remaining", remaining);
        tag.putInt("freq", freq);
        tag.putInt("areaSide", areaSide);
        tag.putInt("diamondTotal", diamondTotal);
        tag.putString("filter", filter);
        tag.putInt("tickCounter", tickCounter);
        tag.put("inventory", itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remaining = tag.getInt("remaining");
        freq = tag.getInt("freq");
        areaSide = tag.getInt("areaSide");
        diamondTotal = tag.getInt("diamondTotal");
        filter = tag.contains("filter") ? tag.getString("filter") : DEFAULT_FILTER;
        tickCounter = tag.getInt("tickCounter");
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString("filter", filter);
        tag.putInt("remaining", remaining);
        tag.putInt("freq", freq);
        tag.putInt("areaSide", areaSide);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("filter")) filter = tag.getString("filter");
        if (tag.contains("remaining")) remaining = tag.getInt("remaining");
        if (tag.contains("freq")) freq = tag.getInt("freq");
        if (tag.contains("areaSide")) areaSide = tag.getInt("areaSide");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- Capabilities ----
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // ---- Menu ----
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.lightningblock.lightning_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LightningBlockMenu(id, inv, this);
    }
}
