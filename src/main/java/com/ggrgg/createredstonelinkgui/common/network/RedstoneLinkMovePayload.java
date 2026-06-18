package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.Config;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record RedstoneLinkMovePayload(BlockPos sourcePos, BlockPos targetPos, Direction clickedFace) {

    public static void encode(RedstoneLinkMovePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.sourcePos);
        buffer.writeBlockPos(payload.targetPos);
        buffer.writeEnum(payload.clickedFace);
    }

    public static RedstoneLinkMovePayload decode(FriendlyByteBuf buffer) {
        return new RedstoneLinkMovePayload(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readEnum(Direction.class));
    }

    public static void handle(RedstoneLinkMovePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos sourcePos = payload.sourcePos();
            BlockPos targetPos = payload.targetPos();
            Direction clickedFace = payload.clickedFace();

            if (!level.isLoaded(targetPos)) return;

            BlockEntity sourceBE = level.getBlockEntity(sourcePos);
            if (sourceBE == null) return;

            LinkBehaviour sourceLink = BlockEntityBehaviour.get(sourceBE, LinkBehaviour.TYPE);
            if (sourceLink == null) return;

            FactoryPanelSupportBehaviour gaugeSupport = BlockEntityBehaviour.get(sourceBE, FactoryPanelSupportBehaviour.TYPE);
            boolean hasGaugeConnection = gaugeSupport != null && !gaugeSupport.getLinkedPanels().isEmpty();

            int maxRange = Config.MOVE_RANGE.get();
            if (hasGaugeConnection) {
                maxRange = Math.min(maxRange, 24);
                for (FactoryPanelPosition gaugePos : gaugeSupport.getLinkedPanels())
                    if (!gaugePos.pos().closerThan(targetPos, 24)) return;
            }

            if (player.distanceToSqr(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()) > maxRange * maxRange) return;
            if (Vec3.atCenterOf(sourcePos).distanceToSqr(Vec3.atCenterOf(targetPos)) > maxRange * maxRange) return;

            BlockState sourceState = sourceBE.getBlockState();
            BlockState targetState = level.getBlockState(targetPos);
            boolean inPlace = sourcePos.equals(targetPos);

            if (!inPlace && !targetState.isAir() && !targetState.canBeReplaced()) return;

            Block block = sourceState.getBlock();
            BlockPlaceContext placeContext = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY, new BlockHitResult(Vec3.atCenterOf(targetPos), clickedFace, targetPos, false));
            BlockState newState = block.getStateForPlacement(placeContext);
            if (newState == null) return;

            newState = copyNonOrientationProperties(newState, sourceState);

            if (hasGaugeConnection) {
                Direction oldFace = sourceState.getValue(BlockStateProperties.FACING);
                Direction newFace = newState.getValue(BlockStateProperties.FACING);
                if (oldFace != newFace) return;
            }

            if (!newState.canSurvive(level, targetPos)) return;

            CompoundTag beTag = sourceBE.saveWithoutMetadata();

            level.setBlock(targetPos, newState, Block.UPDATE_ALL);

            BlockEntity newBE = level.getBlockEntity(targetPos);
            if (newBE != null) {
                newBE.load(beTag);
                newBE.setChanged();
            }

            if (gaugeSupport != null && !inPlace) {
                for (FactoryPanelPosition gaugePos : gaugeSupport.getLinkedPanels()) {
                    FactoryPanelBehaviour panel = FactoryPanelBehaviour.at(level, gaugePos);
                    if (panel != null) {
                        panel.targetedByLinks.remove(sourcePos);
                        panel.targetedByLinks.put(targetPos,
                                new FactoryPanelConnection(new FactoryPanelPosition(targetPos, gaugePos.slot()), 1));
                        panel.blockEntity.notifyUpdate();
                    }
                }
            }

            level.sendBlockUpdated(targetPos, newState, newState, Block.UPDATE_ALL);

            if (!inPlace) {
                level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
                level.sendBlockUpdated(sourcePos, sourceState, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        context.setPacketHandled(true);
    }

    private static BlockState copyNonOrientationProperties(BlockState target, BlockState source) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            String name = property.getName();
            if (name.equals("facing") || name.equals("face") || name.equals("attachment"))
                continue;
            if (result.hasProperty(property)) {
                result = copyPropertyUntyped(result, source, property);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyPropertyUntyped(BlockState target, BlockState source, Property<?> property) {
        Property<T> typed = (Property<T>) property;
        return target.setValue(typed, source.getValue(typed));
    }
}
