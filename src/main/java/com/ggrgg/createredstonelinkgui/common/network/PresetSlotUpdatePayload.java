package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record PresetSlotUpdatePayload(int presetIndex, int slotIndex, ItemStack stack) {

    public static void encode(PresetSlotUpdatePayload payload, FriendlyByteBuf buffer) {
        buffer.writeInt(payload.presetIndex);
        buffer.writeInt(payload.slotIndex);
        buffer.writeItem(payload.stack);
    }

    public static PresetSlotUpdatePayload decode(FriendlyByteBuf buffer) {
        return new PresetSlotUpdatePayload(buffer.readInt(), buffer.readInt(), buffer.readItem());
    }

    public static void handle(PresetSlotUpdatePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            FrequencyPresetData.get(player).setStack(payload.presetIndex, payload.slotIndex, payload.stack);
        });
        context.setPacketHandled(true);
    }
}
