package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public record RedstoneLinkModeTogglePayload(BlockPos pos) {

    public static void encode(RedstoneLinkModeTogglePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
    }

    public static RedstoneLinkModeTogglePayload decode(FriendlyByteBuf buffer) {
        return new RedstoneLinkModeTogglePayload(buffer.readBlockPos());
    }

    public static void handle(RedstoneLinkModeTogglePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos pos = payload.pos();

            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof RedstoneLinkBlock linkBlock) {
                linkBlock.toggleMode(state, level, pos);
                level.scheduleTick(pos, linkBlock, 1);
            }
        });
        context.setPacketHandled(true);
    }
}
