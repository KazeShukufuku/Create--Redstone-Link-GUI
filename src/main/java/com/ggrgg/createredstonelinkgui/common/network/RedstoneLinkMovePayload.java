package com.ggrgg.createredstonelinkgui.common.network;

import java.util.function.Supplier;

import com.ggrgg.createredstonelinkgui.Config;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.simibubi.create.content.kinetics.base.IRotate;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record RedstoneLinkMovePayload(BlockPos sourcePos, BlockPos clickedPos, Vec3 hitLocation, Direction clickedFace) {

    public static void encode(RedstoneLinkMovePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.sourcePos);
        buffer.writeBlockPos(payload.clickedPos);
        buffer.writeDouble(payload.hitLocation.x);
        buffer.writeDouble(payload.hitLocation.y);
        buffer.writeDouble(payload.hitLocation.z);
        buffer.writeEnum(payload.clickedFace);
    }

    public static RedstoneLinkMovePayload decode(FriendlyByteBuf buffer) {
        BlockPos sourcePos = buffer.readBlockPos();
        BlockPos clickedPos = buffer.readBlockPos();
        Vec3 hitLocation = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Direction clickedFace = buffer.readEnum(Direction.class);
        return new RedstoneLinkMovePayload(sourcePos, clickedPos, hitLocation, clickedFace);
    }

    public static void handle(RedstoneLinkMovePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos sourcePos = payload.sourcePos();
            BlockPos clickedPos = payload.clickedPos();
            Vec3 hitLocation = payload.hitLocation();
            Direction clickedFace = payload.clickedFace();

            if (!level.isLoaded(clickedPos)) return;

            BlockEntity sourceBE = level.getBlockEntity(sourcePos);
            if (sourceBE == null) return;

            LinkBehaviour sourceLink = BlockEntityBehaviour.get(sourceBE, LinkBehaviour.TYPE);
            Object sourceVoidLink = VoidLinkHelper.getBehaviour(level, sourcePos);
            if (sourceLink == null && sourceVoidLink == null) return;

            BlockPlaceContext placeContext = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY, new BlockHitResult(hitLocation, clickedFace, clickedPos, false));
            BlockPos targetPos = clickedPos.relative(clickedFace);
            if (!level.isLoaded(targetPos)) return;

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

            if (!inPlace && !targetState.isAir() && !targetState.canBeReplaced(placeContext)) return;

            Block block = sourceState.getBlock();
            BlockState newState = block.getStateForPlacement(placeContext);
            if (newState == null) return;
            newState = orientForClickedFace(newState, clickedFace);

            if (!hasSupportAfterMove(level, sourcePos, targetPos, clickedFace, newState, inPlace)) return;

            newState = copyNonOrientationProperties(newState, sourceState);
            newState = specializeVoidBlockOrientation(level, sourceState, newState, sourcePos, targetPos, player, inPlace);

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

    private static boolean hasSupportAfterMove(Level level, BlockPos sourcePos, BlockPos targetPos, Direction clickedFace, BlockState targetState, boolean inPlace) {
        Direction supportDirection = getSupportDirection(targetState);
        if (supportDirection == null) {
            return true;
        }
        if (supportDirection != clickedFace.getOpposite()) {
            return false;
        }

        BlockPos supportPos = targetPos.relative(supportDirection);
        if (!inPlace && supportPos.equals(sourcePos)) {
            return false;
        }
        return !level.getBlockState(supportPos).canBeReplaced();
    }

    private static Direction getSupportDirection(BlockState state) {
        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
            return switch (face) {
                case CEILING -> Direction.UP;
                case FLOOR -> Direction.DOWN;
                case WALL -> state.hasProperty(BlockStateProperties.FACING)
                        ? state.getValue(BlockStateProperties.FACING).getOpposite()
                        : null;
            };
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING).getOpposite();
        }
        return null;
    }

    private static BlockState specializeVoidBlockOrientation(Level level, BlockState sourceState, BlockState targetState,
            BlockPos sourcePos, BlockPos targetPos, ServerPlayer player, boolean inPlace) {
        if (!isCreateUtilitiesVoidBlock(sourceState)) {
            return targetState;
        }

        if (isVoidMotor(sourceState)) {
            return specializeVoidMotorOrientation(level, targetState, sourcePos, targetPos, player, inPlace);
        }

        DirectionProperty facingProperty = getFacingProperty(targetState);
        if (facingProperty == null) {
            return targetState;
        }

        Direction frequencyFace = faceTowardPlayer(targetPos, player);
        if (!canSet(targetState, facingProperty, frequencyFace)) {
            frequencyFace = horizontalFaceTowardPlayer(targetPos, player);
        }
        if (canSet(targetState, facingProperty, frequencyFace)) {
            return targetState.setValue(facingProperty, frequencyFace);
        }
        return targetState;
    }

    private static BlockState specializeVoidMotorOrientation(Level level, BlockState targetState,
            BlockPos sourcePos, BlockPos targetPos, ServerPlayer player, boolean inPlace) {
        if (!targetState.hasProperty(BlockStateProperties.FACING)) {
            return targetState;
        }

        Direction connectedFace = findConnectableKineticFace(level, targetPos, sourcePos, inPlace, targetState);
        if (connectedFace != null) {
            return targetState.setValue(BlockStateProperties.FACING, connectedFace);
        }

        Direction frequencyFace = faceTowardPlayer(targetPos, player);
        Direction shaftFace = frequencyFace.getOpposite();
        if (canSet(targetState, BlockStateProperties.FACING, shaftFace)) {
            return targetState.setValue(BlockStateProperties.FACING, shaftFace);
        }
        return targetState;
    }

    private static boolean isVoidMotor(BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return blockId.equals("createutilities:void_motor")
                || state.getBlock().getClass().getName()
                        .equals("io.github.jasonsimpart.createutilitiesj.blocks.voidtypes.motor.VoidMotorBlock");
    }

    private static boolean isCreateUtilitiesVoidBlock(BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return blockId.equals("createutilities:void_motor")
                || blockId.equals("createutilities:void_chest")
                || blockId.equals("createutilities:void_battery")
                || isVoidMotor(state);
    }

    private static DirectionProperty getFacingProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("facing") && property instanceof DirectionProperty directionProperty) {
                return directionProperty;
            }
        }
        return null;
    }

    private static Direction findConnectableKineticFace(Level level, BlockPos targetPos, BlockPos sourcePos,
            boolean inPlace, BlockState targetState) {
        for (Direction direction : Direction.values()) {
            if (!canSet(targetState, BlockStateProperties.FACING, direction)) continue;

            BlockPos neighbourPos = targetPos.relative(direction);
            if (!inPlace && neighbourPos.equals(sourcePos)) continue;
            if (!level.isLoaded(neighbourPos)) continue;

            BlockState neighbourState = level.getBlockState(neighbourPos);
            if (neighbourState.getBlock() instanceof IRotate rotate
                    && rotate.hasShaftTowards(level, neighbourPos, neighbourState, direction.getOpposite())) {
                return direction;
            }
        }
        return null;
    }

    private static Direction faceTowardPlayer(BlockPos targetPos, ServerPlayer player) {
        Vec3 fromTarget = player.getEyePosition().subtract(Vec3.atCenterOf(targetPos));
        return Direction.getNearest(fromTarget.x, fromTarget.y, fromTarget.z);
    }

    private static Direction horizontalFaceTowardPlayer(BlockPos targetPos, ServerPlayer player) {
        Vec3 fromTarget = player.position().subtract(Vec3.atCenterOf(targetPos));
        return Direction.getNearest(fromTarget.x, 0, fromTarget.z);
    }

    private static BlockState orientForClickedFace(BlockState state, Direction clickedFace) {
        Direction supportDirection = clickedFace.getOpposite();

        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            AttachFace face = supportDirection == Direction.DOWN
                    ? AttachFace.FLOOR
                    : supportDirection == Direction.UP
                            ? AttachFace.CEILING
                            : AttachFace.WALL;
            state = state.setValue(BlockStateProperties.ATTACH_FACE, face);

            if (face == AttachFace.WALL && state.hasProperty(BlockStateProperties.FACING)
                    && canSet(state, BlockStateProperties.FACING, clickedFace)) {
                state = state.setValue(BlockStateProperties.FACING, clickedFace);
            }
            return state;
        }

        if (state.hasProperty(BlockStateProperties.FACING)
                && canSet(state, BlockStateProperties.FACING, clickedFace)) {
            return state.setValue(BlockStateProperties.FACING, clickedFace);
        }
        return state;
    }

    private static <T extends Comparable<T>> boolean canSet(BlockState state, Property<T> property, T value) {
        return state.hasProperty(property) && property.getPossibleValues().contains(value);
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
