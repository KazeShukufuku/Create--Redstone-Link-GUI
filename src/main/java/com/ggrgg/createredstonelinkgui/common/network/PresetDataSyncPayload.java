package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.client.ClientPresetSyncHandler;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public record PresetDataSyncPayload(CompoundTag presets, int revision) {

    public static PresetDataSyncPayload fromPlayer(ServerPlayer player, int revision) {
        return new PresetDataSyncPayload(FrequencyPresetData.get(player).serializeNBT(), revision);
    }

    public static void sendTo(ServerPlayer player) {
        sendTo(player, 0);
    }

    public static void sendTo(ServerPlayer player, int revision) {
        CreateRedstoneLinkGUI.NETWORK.send(PacketDistributor.PLAYER.with(() -> player), fromPlayer(player, revision));
    }

    public static void encode(PresetDataSyncPayload payload, FriendlyByteBuf buffer) {
        buffer.writeNbt(payload.presets);
        buffer.writeInt(payload.revision);
    }

    public static PresetDataSyncPayload decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new PresetDataSyncPayload(tag == null ? new CompoundTag() : tag, buffer.readInt());
    }

    public static void handle(PresetDataSyncPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> ClientPresetSyncHandler.handle(payload.presets.copy(), payload.revision)
        ));
        context.setPacketHandled(true);
    }
}
