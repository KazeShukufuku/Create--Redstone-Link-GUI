package com.ggrgg.createredstonelinkgui.compat.emi;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

@EmiEntrypoint
public class AddonEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(RedstoneLinkConfigScreen.class, new RedstoneLinkEmiDragHandler());
        registry.addDragDropHandler(VoidLinkConfigScreen.class, new VoidLinkEmiDragHandler());
        registry.addExclusionArea(RedstoneLinkConfigScreen.class, (screen, consumer) -> {
            if (screen.blockPreviewBounds != null) {
                consumer.accept(toBounds(screen.blockPreviewBounds));
            }
        });
        registry.addExclusionArea(VoidLinkConfigScreen.class, (screen, consumer) -> {
            if (screen.blockPreviewBounds != null) {
                consumer.accept(toBounds(screen.blockPreviewBounds));
            }
        });
    }

    private static Bounds toBounds(net.minecraft.client.renderer.Rect2i rect) {
        return new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }
}
