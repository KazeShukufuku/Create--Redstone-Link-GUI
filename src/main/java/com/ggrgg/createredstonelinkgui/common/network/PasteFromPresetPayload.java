package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PasteFromPresetPayload(BlockPos pos, int presetIndex) {

    public static void encode(PasteFromPresetPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeInt(payload.presetIndex);
    }

    public static PasteFromPresetPayload decode(FriendlyByteBuf buffer) {
        return new PasteFromPresetPayload(buffer.readBlockPos(), buffer.readInt());
    }

    public static void handle(PasteFromPresetPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) > 64.0) return;
            FrequencyPresetHelper.pasteToLink(player, payload.pos, payload.presetIndex);
        });
        context.setPacketHandled(true);
    }
}
