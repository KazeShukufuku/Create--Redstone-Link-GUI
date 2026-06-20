package com.ggrgg.createredstonelinkgui;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MOVE_RANGE = BUILDER
            .comment("Maximum distance in blocks a redstone link can be moved")
            .translation("createredstonelinkgui.configuration.moveRange")
            .defineInRange("moveRange", 24, 1, 256);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
