package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public record RedstoneLinkFrequencyPayload(BlockPos pos, ItemStack selectedItem, int slotIndex) {

    public static void encode(RedstoneLinkFrequencyPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeItem(payload.selectedItem);
        buffer.writeInt(payload.slotIndex);
    }

    public static RedstoneLinkFrequencyPayload decode(FriendlyByteBuf buffer) {
        return new RedstoneLinkFrequencyPayload(buffer.readBlockPos(), buffer.readItem(), buffer.readInt());
    }

    public static void handle(RedstoneLinkFrequencyPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos pos = payload.pos();

            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            var be = level.getBlockEntity(pos);
            if (be != null) {
                LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
                if (behaviour != null) {
                    RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, payload.slotIndex() == 0, payload.selectedItem());

                    be.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
