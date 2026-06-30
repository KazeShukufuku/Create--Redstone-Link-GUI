package com.ggrgg.createredstonelinkgui;

import org.slf4j.Logger;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.CopyToPresetPayload;
import com.ggrgg.createredstonelinkgui.common.network.OpenLinkMenuPayload;
import com.ggrgg.createredstonelinkgui.common.network.PasteFromPresetPayload;
import com.ggrgg.createredstonelinkgui.common.network.PresetDataSyncPayload;
import com.ggrgg.createredstonelinkgui.common.network.PresetSlotUpdatePayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkModeTogglePayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import com.ggrgg.createredstonelinkgui.common.network.VoidLinkClaimPayload;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(CreateRedstoneLinkGUI.MODID)
public class CreateRedstoneLinkGUI {
    public static final String MODID = "createredstonelinkgui";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String NETWORK_PROTOCOL = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "main"))
            .networkProtocolVersion(() -> NETWORK_PROTOCOL)
            .clientAcceptedVersions(NETWORK_PROTOCOL::equals)
            .serverAcceptedVersions(NETWORK_PROTOCOL::equals)
            .simpleChannel();

    public CreateRedstoneLinkGUI() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        registerPackets();
        RedstoneLinkMenu.MENUS.register(modEventBus);
        VoidLinkMenu.MENUS.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private static void registerPackets() {
        int id = 0;
        NETWORK.registerMessage(id++, RedstoneLinkFrequencyPayload.class,
                RedstoneLinkFrequencyPayload::encode,
                RedstoneLinkFrequencyPayload::decode,
                RedstoneLinkFrequencyPayload::handle);
        NETWORK.registerMessage(id++, RedstoneLinkModeTogglePayload.class,
                RedstoneLinkModeTogglePayload::encode,
                RedstoneLinkModeTogglePayload::decode,
                RedstoneLinkModeTogglePayload::handle);
        NETWORK.registerMessage(id++, RedstoneLinkMovePayload.class,
                RedstoneLinkMovePayload::encode,
                RedstoneLinkMovePayload::decode,
                RedstoneLinkMovePayload::handle);
        NETWORK.registerMessage(id++, VoidLinkClaimPayload.class,
                VoidLinkClaimPayload::encode,
                VoidLinkClaimPayload::decode,
                VoidLinkClaimPayload::handle);
        NETWORK.registerMessage(id++, OpenLinkMenuPayload.class,
                OpenLinkMenuPayload::encode,
                OpenLinkMenuPayload::decode,
                OpenLinkMenuPayload::handle);
        NETWORK.registerMessage(id++, CopyToPresetPayload.class,
                CopyToPresetPayload::encode,
                CopyToPresetPayload::decode,
                CopyToPresetPayload::handle);
        NETWORK.registerMessage(id++, PasteFromPresetPayload.class,
                PasteFromPresetPayload::encode,
                PasteFromPresetPayload::decode,
                PasteFromPresetPayload::handle);
        NETWORK.registerMessage(id++, PresetSlotUpdatePayload.class,
                PresetSlotUpdatePayload::encode,
                PresetSlotUpdatePayload::decode,
                PresetSlotUpdatePayload::handle);
        NETWORK.registerMessage(id++, PresetDataSyncPayload.class,
                PresetDataSyncPayload::encode,
                PresetDataSyncPayload::decode,
                PresetDataSyncPayload::handle);
    }
}
