package com.lightningblock.network;

import com.lightningblock.block.entity.LightningBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FilterUpdatePacket {
    private final BlockPos pos;
    private final String filter;

    public FilterUpdatePacket(BlockPos pos, String filter) {
        this.pos = pos;
        this.filter = filter;
    }

    public static void encode(FilterUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.filter);
    }

    public static FilterUpdatePacket decode(FriendlyByteBuf buf) {
        return new FilterUpdatePacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(FilterUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null
                    && player.level().getBlockEntity(msg.pos) instanceof LightningBlockEntity be) {
                be.setFilter(msg.filter);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
