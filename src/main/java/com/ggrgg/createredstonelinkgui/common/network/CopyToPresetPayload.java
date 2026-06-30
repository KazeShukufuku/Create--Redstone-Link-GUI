package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record CopyToPresetPayload(BlockPos pos, int presetIndex, int revision, ItemStack firstFrequency, ItemStack secondFrequency) {

    public static void encode(CopyToPresetPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeInt(payload.presetIndex);
        buffer.writeInt(payload.revision);
        buffer.writeItem(payload.firstFrequency);
        buffer.writeItem(payload.secondFrequency);
    }

    public static CopyToPresetPayload decode(FriendlyByteBuf buffer) {
        return new CopyToPresetPayload(buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readItem(), buffer.readItem());
    }

    public static void handle(CopyToPresetPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) > 64.0) return;
            if (!FrequencyPresetHelper.copyCurrentFrequencies(player, payload.pos, payload.presetIndex)) {
                FrequencyPresetData.get(player).setPreset(payload.presetIndex, payload.firstFrequency, payload.secondFrequency);
            }
            PresetDataSyncPayload.sendTo(player, payload.revision);
        });
        context.setPacketHandled(true);
    }
}
