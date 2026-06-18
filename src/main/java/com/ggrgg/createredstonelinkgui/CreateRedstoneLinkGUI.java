package com.ggrgg.createredstonelinkgui;

import org.slf4j.Logger;

import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkModeTogglePayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
    }
}
