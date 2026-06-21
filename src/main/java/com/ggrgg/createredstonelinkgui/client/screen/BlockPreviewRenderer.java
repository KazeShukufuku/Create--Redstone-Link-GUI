package com.ggrgg.createredstonelinkgui.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockPreviewRenderer {
    private static final String CREATE_CONNECTED = "create_connected";
    private static final String LINKED_ANALOG_LEVER = "linked_analog_lever";
    private static final int PREVIEW_X_OFFSET = 22;
    private static final int PREVIEW_Y_OFFSET = 62;
    private static final int PREVIEW_X_ROTATION = 25;
    private static final int PREVIEW_Y_ROTATION = 135;
    private static final int PREVIEW_SCALE = 42;
    private static final int REDSTONE_LINK_PREVIEW_X_OFFSET = 32;
    private static final int REDSTONE_LINK_PREVIEW_Y_OFFSET = 32;
    private static final int REDSTONE_LINK_PREVIEW_X_ROTATION = 30;
    private static final int REDSTONE_LINK_PREVIEW_Y_ROTATION = 45;
    private static final int REDSTONE_LINK_PREVIEW_SCALE = 40;
    private static final double REDSTONE_LINK_PREVIEW_LOCAL_X = -0.5;
    private static final double REDSTONE_LINK_PREVIEW_LOCAL_Y = 0.5;
    private static final int REDSTONE_LINK_PREVIEW_Z_OFFSET = 10;

    public static void render(GuiGraphics graphics, BlockState blockState, BlockEntity blockEntity, int x, int y) {
        if (blockState.getBlock() instanceof RedstoneLinkBlock) {
            renderRedstoneLinkPreview(graphics, blockState, x, y);
            return;
        }

        if (shouldRenderStatePreview(blockState, blockEntity)) {
            BlockState previewState = getFixedPreviewState(blockState);

            int previewX = x + PREVIEW_X_OFFSET;
            int previewY = y + PREVIEW_Y_OFFSET;
            renderBlockState(graphics, previewState, previewX, previewY,
                    PREVIEW_X_ROTATION, PREVIEW_Y_ROTATION,
                    PREVIEW_SCALE, -0.5, -0.35);

            if (isLinkedAnalogLever(blockState)) {
                renderAnalogLeverParts(graphics, previewState, blockEntity, previewX, previewY);
            }
            return;
        }

        renderItemPreview(graphics, blockState, x, y);
    }

    private static void renderBlockState(GuiGraphics graphics, BlockState previewState, int x, int y,
            int xRotation, int yRotation, int scale, double localX, double localY) {
        GuiGameElement.of(previewState)
                .rotateBlock(xRotation, yRotation, 0)
                .scale(scale)
                .atLocal(localX, localY, 0)
                .render(graphics, x, y);
    }

    private static void renderItemPreview(GuiGraphics graphics, BlockState blockState, int x, int y) {
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

    private static void renderRedstoneLinkPreview(GuiGraphics graphics, BlockState blockState, int x, int y) {
        BlockState previewState = getRedstoneLinkPreviewState(blockState);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, REDSTONE_LINK_PREVIEW_Z_OFFSET);
        renderBlockState(graphics, previewState,
                x + REDSTONE_LINK_PREVIEW_X_OFFSET,
                y + REDSTONE_LINK_PREVIEW_Y_OFFSET,
                REDSTONE_LINK_PREVIEW_X_ROTATION,
                REDSTONE_LINK_PREVIEW_Y_ROTATION,
                REDSTONE_LINK_PREVIEW_SCALE,
                REDSTONE_LINK_PREVIEW_LOCAL_X,
                REDSTONE_LINK_PREVIEW_LOCAL_Y);
        pose.popPose();
    }

    private static boolean shouldRenderStatePreview(BlockState blockState, BlockEntity blockEntity) {
        if (isVoidSeries(blockState, blockEntity)) {
            return false;
        }
        return isCreateConnectedLinkedControl(blockState)
                || hasLinkBehaviour(blockEntity);
    }

    private static boolean hasLinkBehaviour(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return BlockEntityBehaviour.get(blockEntity, LinkBehaviour.TYPE) != null;
    }

    private static boolean isVoidSeries(BlockState blockState, BlockEntity blockEntity) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (id.getPath().contains("void")) {
            return true;
        }
        return blockEntity != null && blockEntity.getClass().getName().toLowerCase().contains("void");
    }

    private static boolean isCreateConnectedLinkedControl(BlockState blockState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (!CREATE_CONNECTED.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return path.equals("linked_lever")
                || path.equals(LINKED_ANALOG_LEVER)
                || path.startsWith("linked_") && path.endsWith("_button");
    }

    private static boolean isLinkedAnalogLever(BlockState blockState) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return CREATE_CONNECTED.equals(id.getNamespace()) && LINKED_ANALOG_LEVER.equals(id.getPath());
    }

    private static void renderAnalogLeverParts(GuiGraphics graphics, BlockState previewState, BlockEntity blockEntity,
            int x, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        preparePreviewMatrix(pose, x, y);

        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        float state = getAnalogLeverState(blockEntity);

        SuperByteBuffer handle = transformAnalogLeverPart(
                CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_HANDLE, previewState), previewState);
        float angle = (float) ((state / 15) * 90 / 180 * Math.PI);
        handle.translate(1 / 2f, 1 / 16f, 1 / 2f)
                .rotate(angle, Direction.EAST)
                .translate(-1 / 2f, -1 / 16f, -1 / 2f)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(pose, vb);

        int color = redstoneColor(state / 15f);
        transformAnalogLeverPart(CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, previewState), previewState)
                .light(LightTexture.FULL_BRIGHT)
                .color(color)
                .renderInto(pose, vb);

        buffer.endBatch();
        pose.popPose();
    }

    private static void preparePreviewMatrix(PoseStack pose, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Lighting.setupFor3DItems();

        pose.translate(x, y, 0);
        pose.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);
        pose.translate(-0.5, -0.35, 0);
        UIRenderHelper.flipForGuiRender(pose);
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees(PREVIEW_Y_ROTATION));
        pose.translate(-0.5, -0.5, -0.5);
    }

    private static float getAnalogLeverState(BlockEntity blockEntity) {
        if (blockEntity instanceof AnalogLeverBlockEntity analogLever) {
            return analogLever.getState();
        }
        return getNumericState(blockEntity);
    }

    private static float getNumericState(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return 0;
        }

        try {
            Object value = blockEntity.getClass().getMethod("getState").invoke(blockEntity);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return 0;
    }

    private static SuperByteBuffer transformAnalogLeverPart(SuperByteBuffer buffer, BlockState leverState) {
        AttachFace face = leverState.getValue(BlockStateProperties.ATTACH_FACE);
        float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        float rY = AngleHelper.horizontalAngle(leverState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        return buffer;
    }

    private static int redstoneColor(float signal) {
        return Color.mixColors(0x2C0300, 0xCD0000, Math.max(0, Math.min(1, signal)));
    }

    private static BlockState getRedstoneLinkPreviewState(BlockState blockState) {
        return setIfPresent(blockState, BlockStateProperties.FACING, Direction.UP);
    }

    private static BlockState getFixedPreviewState(BlockState blockState) {
        BlockState previewState = blockState;
        previewState = setIfPresent(previewState, BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        previewState = setIfPresent(previewState, BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        previewState = setIfPresent(previewState, BlockStateProperties.FACING, Direction.SOUTH);
        previewState = setDirectionPropertyIfPresent(previewState, "facing", Direction.SOUTH);
        return setAttachFacePropertyIfPresent(previewState, "face", AttachFace.FLOOR);
    }

    private static <T extends Comparable<T>> BlockState setIfPresent(BlockState state, Property<T> property, T value) {
        if (state.hasProperty(property) && property.getPossibleValues().contains(value)) {
            return state.setValue(property, value);
        }
        return state;
    }

    private static BlockState setDirectionPropertyIfPresent(BlockState state, String name, Direction value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property.getPossibleValues().contains(value)) {
                return setRaw(state, property, value);
            }
        }
        return state;
    }

    private static BlockState setAttachFacePropertyIfPresent(BlockState state, String name, AttachFace value) {
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
