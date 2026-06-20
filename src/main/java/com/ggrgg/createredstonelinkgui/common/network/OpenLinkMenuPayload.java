package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public record OpenLinkMenuPayload(BlockPos pos) {

    public static void encode(OpenLinkMenuPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
    }

    public static OpenLinkMenuPayload decode(FriendlyByteBuf buffer) {
        return new OpenLinkMenuPayload(buffer.readBlockPos());
    }

    public static void handle(OpenLinkMenuPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64.0) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return;

            LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
            if (behaviour != null) {
                be.setChanged();
                level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new RedstoneLinkMenu(id, inv, pos),
                        Component.translatable("container.createredstonelinkgui.redstone_link_menu")
                ), pos);
                return;
            }

            Object voidLinkBehaviour = VoidLinkHelper.getBehaviour(level, pos);
            if (voidLinkBehaviour == null || !VoidLinkHelper.canInteract(voidLinkBehaviour, player)) return;

            be.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (id, inv, p) -> new VoidLinkMenu(id, inv, pos),
                    Component.translatable("container.createredstonelinkgui.void_link_menu")
            ), pos);
        });
        context.setPacketHandled(true);
    }
}
