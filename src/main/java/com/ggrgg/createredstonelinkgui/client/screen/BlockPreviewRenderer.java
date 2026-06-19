package com.ggrgg.createredstonelinkgui.client.screen;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPreviewRenderer {
    private static final String LINKED_BUTTON_BLOCK = "com.hlysine.create_connected.content.linkedtransmitter.LinkedButtonBlock";
    private static final String LINKED_LEVER_BLOCK = "com.hlysine.create_connected.content.linkedtransmitter.LinkedLeverBlock";
    private static final String LINKED_ANALOG_LEVER_BLOCK = "com.hlysine.create_connected.content.linkedtransmitter.LinkedAnalogLeverBlock";
    private static final int LINKED_PREVIEW_X_OFFSET = 22;
    private static final int LINKED_PREVIEW_Y_OFFSET = 62;
    private static final int LINKED_PREVIEW_Y_ROTATION = 135;

    public static void render(GuiGraphics graphics, BlockState blockState, BlockEntity blockEntity, int x, int y) {
        if (isLinkedTransmitterControl(blockState)) {
            GuiGameElement.of(getLinkedPreviewState(blockState))
                    .rotateBlock(25, LINKED_PREVIEW_Y_ROTATION, 0)
                    .scale(42)
                    .atLocal(-0.5, -0.35, 0)
                    .render(graphics, x + LINKED_PREVIEW_X_OFFSET, y + LINKED_PREVIEW_Y_OFFSET);
            return;
        }

        ItemStack blockStack = new ItemStack(blockState.getBlock().asItem());
        if (!blockStack.isEmpty()) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 10);
            GuiGameElement.of(blockStack)
                    .scale(4)
                    .at(0, 0, -200)
                    .render(graphics, x, y);
            pose.popPose();
        }
    }

    private static boolean isLinkedTransmitterControl(BlockState blockState) {
        String className = blockState.getBlock().getClass().getName();
        return className.equals(LINKED_BUTTON_BLOCK)
                || className.equals(LINKED_LEVER_BLOCK)
                || className.equals(LINKED_ANALOG_LEVER_BLOCK);
    }

    private static BlockState getLinkedPreviewState(BlockState blockState) {
        BlockState previewState = blockState.getBlock().defaultBlockState();
        previewState = setIfPresent(previewState, BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        previewState = setIfPresent(previewState, BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        previewState = setIfPresent(previewState, BlockStateProperties.POWERED, false);
        return setBooleanPropertyIfPresent(previewState, "locked", false);
    }

    private static <T extends Comparable<T>> BlockState setIfPresent(BlockState state, Property<T> property, T value) {
        if (state.hasProperty(property)) {
            return state.setValue(property, value);
        }
        return state;
    }

    private static BlockState setBooleanPropertyIfPresent(BlockState state, String name, boolean value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property.getPossibleValues().contains(value)) {
                return setRaw(state, property, value);
            }
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setRaw(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }

}
