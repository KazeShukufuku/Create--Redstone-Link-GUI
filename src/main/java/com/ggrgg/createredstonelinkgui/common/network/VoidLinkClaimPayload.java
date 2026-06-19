package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public record VoidLinkClaimPayload(BlockPos pos) {

    public static void encode(VoidLinkClaimPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
    }

    public static VoidLinkClaimPayload decode(FriendlyByteBuf buffer) {
        return new VoidLinkClaimPayload(buffer.readBlockPos());
    }

    public static void handle(VoidLinkClaimPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            Object behaviour = VoidLinkHelper.getBehaviour(level, pos);
            if (behaviour == null) return;

            GameProfile currentOwner = VoidLinkHelper.getOwner(behaviour);
            if (currentOwner == null) {
                VoidLinkHelper.setOwner(behaviour, player.getGameProfile());
            } else if (currentOwner.getId() != null && currentOwner.getId().equals(player.getUUID())) {
                VoidLinkHelper.setOwner(behaviour, null);
            }
        });
        context.setPacketHandled(true);
    }
}
