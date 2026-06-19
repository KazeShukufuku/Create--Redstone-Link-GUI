package com.ggrgg.createredstonelinkgui;

import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateRedstoneLinkGUI.MODID, value = Dist.CLIENT)
public class CreateRedstoneLinkGUIClient {

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RedstoneLinkMoveHandler.clientTick();
        }
    }

    @SubscribeEvent
    static void onInputInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && RedstoneLinkMoveHandler.isActive()) {
            if (RedstoneLinkMoveHandler.onRightClick()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = CreateRedstoneLinkGUI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(RedstoneLinkMenu.TYPE.get(), RedstoneLinkConfigScreen::new);
                MenuScreens.register(VoidLinkMenu.TYPE.get(), VoidLinkConfigScreen::new);
            });
        }
    }
}
