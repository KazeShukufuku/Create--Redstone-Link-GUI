package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.client.screen.AbstractLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;
import com.ggrgg.createredstonelinkgui.common.preset.PresetSyncRevision;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class ClientPresetSyncHandler {

    public static void handle(CompoundTag tag, int revision) {
        if (!PresetSyncRevision.shouldApplyServerSync(revision)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            FrequencyPresetData.applyToPlayer(minecraft.player, tag);
        }
        if (minecraft.screen instanceof AbstractLinkConfigScreen<?> screen && screen.presetPanel != null) {
            screen.getMenu().getPresetData().deserializeNBT(tag);
        }
    }
}
