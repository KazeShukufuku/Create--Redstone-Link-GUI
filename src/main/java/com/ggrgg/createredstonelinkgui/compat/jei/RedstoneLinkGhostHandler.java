package com.ggrgg.createredstonelinkgui.compat.jei;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.common.network.PresetSlotUpdatePayload;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedstoneLinkGhostHandler implements IGhostIngredientHandler<RedstoneLinkConfigScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(RedstoneLinkConfigScreen screen, ITypedIngredient<I> typedIngredient, boolean doStart) {
        var itemStackOptional = typedIngredient.getIngredient(VanillaTypes.ITEM_STACK);
        
        // Fast-fail check: Ignore Fluid/Gas/Energy tokens cleanly to prevent garbage heap allocation
        if (itemStackOptional.isEmpty()) {
            return Collections.emptyList();
        }

        ItemStack stack = itemStackOptional.get();

        List<Target<I>> targets = new ArrayList<>(10);

        targets.add(new Target<>() {
            @Override public Rect2i getArea() { return screen.slot1Bounds; }
            @Override public void accept(I ing) { screen.updateFrequencySlot(0, stack); }
        });

        targets.add(new Target<>() {
            @Override public Rect2i getArea() { return screen.slot2Bounds; }
            @Override public void accept(I ing) { screen.updateFrequencySlot(1, stack); }
        });

        if (screen.presetPanel != null) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 2; col++) {
                    final int r = row;
                    final int c = col;
                    Rect2i bounds = screen.presetPanel.getSlotBounds(row, col);
                    if (bounds != null) {
                        targets.add(new Target<>() {
                            @Override public Rect2i getArea() { return bounds; }
                            @Override public void accept(I ing) {
                                screen.presetPanel.getPresetData().setStack(r, c, stack);
                                CreateRedstoneLinkGUI.NETWORK.sendToServer(new PresetSlotUpdatePayload(r, c, stack));
                            }
                        });
                    }
                }
            }
        }

        return targets;
    }

    @Override public void onComplete() {}
}
