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
            .comment("How to open the redstone link frequency menu (Client-side)",
                     "SLOT - Right-click frequency slot with empty hand",
                     "SHIFT_SLOT - Shift + right-click frequency slot",
                     "SHIFT_BLOCK - Shift + right-click anywhere on the block")
            .defineEnum("clickMode", ClickMode.SHIFT_SLOT);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
