package com.ggrgg.createredstonelinkgui;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public enum ClickMode {
        SLOT,
        SHIFT_SLOT,
        SHIFT_BLOCK
    }

    public static final ForgeConfigSpec.EnumValue<ClickMode> CLICK_MODE = BUILDER
            .comment("How to open the redstone link frequency menu (Client-side)")
            .translation("createredstonelinkgui.configuration.clickMode")
            .defineEnum("clickMode", ClickMode.SHIFT_SLOT);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
