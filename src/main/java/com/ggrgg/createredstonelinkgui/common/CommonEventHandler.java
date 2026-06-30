package com.ggrgg.createredstonelinkgui.common;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRedstoneLinkGUI.MODID)
public class CommonEventHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            FrequencyPresetData.copyOnClone(event.getOriginal(), event.getEntity());
        }
    }
}
