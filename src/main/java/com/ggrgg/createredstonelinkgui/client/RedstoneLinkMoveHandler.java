package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.Config;
import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneLinkMoveHandler {

    private static final double HIGHLIGHT_INSET = 0.0625; // 1/16 block inset from block edge
    private static final double HIGHLIGHT_DEPTH = 0.0625; // 1/16 block slab thickness

    private static BlockPos sourcePos;
    private static BlockState sourceState;
    private static boolean active;
    private static BlockPos validTarget;
    private static BlockPos validClickedPos;
    private static Vec3 validHitLocation;
    private static Direction validFace;
    private static int moveRange;
    private static String invalidReason;

    public static void startRelocating(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        BlockState state = level.getBlockState(pos);
        sourcePos = pos;
        sourceState = state;
        active = true;
        validTarget = null;
        validClickedPos = null;
        validHitLocation = null;
        validFace = null;
        invalidReason = null;
        moveRange = Config.MOVE_RANGE.get();
    }

    public static void clientTick() {
        if (!active || sourcePos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cancel();
            return;
        }

        // Show face highlight on source block
        Outliner.getInstance()
                .showAABB(sourcePos, getBlockHighlight(mc.level, sourcePos, sourceState))
                .colored(AnimationTickHolder.getTicks() % 16 > 8 ? 0x38b764 : 0xa7f070)
                .lineWidth(1 / 16f);

        mc.player.displayClientMessage(
                Component.translatable("gui.createredstonelinkgui.click_to_relocate"),
                true);

        // Evaluate potential target
        validTarget = null;
        validClickedPos = null;
        validHitLocation = null;
        validFace = null;
        invalidReason = null;

        BlockHitResult worldHit = mc.hitResult instanceof BlockHitResult hit && hit.getType() != Type.MISS
                ? hit
                : null;
        BlockHitResult bhr = getPlacementHit(mc, worldHit);
        if (bhr == null)
            return;

        Direction clickedFace = bhr.getDirection();
        Direction attachFace = clickedFace.getOpposite();
        BlockPos clickedPos = bhr.getBlockPos();
        Vec3 hitLocation = bhr.getLocation();
        BlockPos pos = clickedPos.relative(clickedFace);

        BlockPlaceContext placeContext = new BlockPlaceContext(mc.level, mc.player, InteractionHand.MAIN_HAND,
                ItemStack.EMPTY, bhr);
        BlockState targetState = mc.level.getBlockState(pos);
        boolean inPlace = pos.equals(sourcePos);

        // Check: obstructed
        if (!inPlace && !targetState.isAir() && !targetState.canBeReplaced(placeContext)) {
            invalidReason = "move_fail_obstructed";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: placement logic
        BlockState newState = sourceState.getBlock().getStateForPlacement(placeContext);
        if (newState == null) {
            invalidReason = "move_fail_no_surface";
            showRedOutline(pos, attachFace);
            return;
        }
        newState = orientForClickedFace(newState, clickedFace);

        if (!hasSupportAfterMove(mc.level, sourcePos, pos, clickedFace, newState, inPlace)) {
            invalidReason = "move_fail_no_surface";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: survivability
        if (!newState.canSurvive(mc.level, pos)) {
            invalidReason = "move_fail_cant_survive";
            showRedOutline(pos, attachFace);
            return;
        }

        // Determine effective range (mirrors server logic)
        int effectiveRange = moveRange;

        var be = mc.level.getBlockEntity(sourcePos);
        if (be != null) {
            var gaugeSupport = BlockEntityBehaviour.get(be, FactoryPanelSupportBehaviour.TYPE);
            if (gaugeSupport != null && !gaugeSupport.getLinkedPanels().isEmpty()) {
                effectiveRange = Math.min(effectiveRange, 24);

                // Check: gauge same-surface constraint
                Direction oldFace = sourceState.getValue(BlockStateProperties.FACING);
                Direction newFace = newState.getValue(BlockStateProperties.FACING);
                if (oldFace != newFace) {
                    invalidReason = "move_fail_surface";
                    showRedOutline(pos, attachFace);
                    return;
                }

                // Check: each gauge position within 24 blocks of target
                for (FactoryPanelPosition gaugePos : gaugeSupport.getLinkedPanels()) {
                    if (!gaugePos.pos().closerThan(pos, 24)) {
                        invalidReason = "move_fail_range";
                        showRedOutline(pos, attachFace);
                        return;
                    }
                }
            }
        }

        // Check: player proximity to source (uses same range as server)
        if (mc.player.distanceToSqr(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()) > effectiveRange * effectiveRange) {
            invalidReason = "move_fail_range";
            showRedOutline(pos, attachFace);
            return;
        }

        // Check: source-to-target distance
        if (Vec3.atCenterOf(sourcePos).distanceToSqr(Vec3.atCenterOf(pos)) > effectiveRange * effectiveRange) {
            invalidReason = "move_fail_range";
            showRedOutline(pos, attachFace);
            return;
        }

        // All checks passed
        validTarget = pos;
        validClickedPos = bhr.getBlockPos();
        validHitLocation = hitLocation;
        validFace = clickedFace;

        Outliner.getInstance()
                .showAABB("target", getFaceHighlight(pos, attachFace))
                .colored(0xeeeeee)
                .disableLineNormals()
                .lineWidth(1 / 16f);
    }

    private static void showRedOutline(BlockPos pos, Direction face) {
        Outliner.getInstance()
                .showAABB("target", getFaceHighlight(pos, face))
                .colored(0xff4444)
                .lineWidth(1 / 16f);
    }

    private static BlockHitResult getPlacementHit(Minecraft mc, BlockHitResult worldHit) {
        if (worldHit != null && placesAtSource(worldHit)) {
            return worldHit;
        }

        BlockHitResult sourceHit = getSourceCubeHit(mc);
        if (sourceHit == null) {
            return worldHit;
        }
        if (worldHit == null) {
            return sourceHit;
        }

        Vec3 eye = mc.player.getEyePosition(1.0F);
        double sourceDistance = eye.distanceToSqr(sourceHit.getLocation());
        double worldDistance = eye.distanceToSqr(worldHit.getLocation());
        return sourceDistance <= worldDistance + 1.0E-7 ? sourceHit : worldHit;
    }

    private static boolean placesAtSource(BlockHitResult hit) {
        return hit.getBlockPos().relative(hit.getDirection()).equals(sourcePos);
    }

    private static BlockHitResult getSourceCubeHit(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(Math.max(moveRange + 1.0, 6.0)));
        RayBoxHit hit = clipUnitCube(new AABB(sourcePos), eye, end);
        if (hit == null) {
            return null;
        }

        Direction sourceFace = hit.face();
        BlockPos supportPos = sourcePos.relative(sourceFace);
        Direction clickedFace = sourceFace.getOpposite();
        return new BlockHitResult(hit.location(), clickedFace, supportPos, false);
    }

    private static RayBoxHit clipUnitCube(AABB box, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double tEnter = 0.0;
        double tExit = 1.0;
        Direction enterFace = null;
        Direction exitFace = null;

        SlabResult x = clipSlab(start.x, delta.x, box.minX, box.maxX, Direction.WEST, Direction.EAST);
        if (x == null) return null;
        if (x.enterT() > tEnter) {
            tEnter = x.enterT();
            enterFace = x.enterFace();
        }
        if (x.exitT() < tExit) {
            tExit = x.exitT();
            exitFace = x.exitFace();
        }

        SlabResult y = clipSlab(start.y, delta.y, box.minY, box.maxY, Direction.DOWN, Direction.UP);
        if (y == null) return null;
        if (y.enterT() > tEnter) {
            tEnter = y.enterT();
            enterFace = y.enterFace();
        }
        if (y.exitT() < tExit) {
            tExit = y.exitT();
            exitFace = y.exitFace();
        }

        SlabResult z = clipSlab(start.z, delta.z, box.minZ, box.maxZ, Direction.NORTH, Direction.SOUTH);
        if (z == null) return null;
        if (z.enterT() > tEnter) {
            tEnter = z.enterT();
            enterFace = z.enterFace();
        }
        if (z.exitT() < tExit) {
            tExit = z.exitT();
            exitFace = z.exitFace();
        }

        if (tEnter > tExit || tExit < 0.0 || tEnter > 1.0) {
            return null;
        }

        Direction face = enterFace != null ? enterFace : exitFace;
        if (face == null) {
            return null;
        }

        double t = enterFace != null ? Math.max(tEnter, 0.0) : tExit;
        return new RayBoxHit(start.add(delta.scale(t)), face);
    }

    private static SlabResult clipSlab(double start, double delta, double min, double max, Direction minFace, Direction maxFace) {
        if (Math.abs(delta) < 1.0E-7) {
            return start >= min && start <= max
                    ? new SlabResult(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null, null)
                    : null;
        }

        double t1 = (min - start) / delta;
        double t2 = (max - start) / delta;
        Direction face1 = minFace;
        Direction face2 = maxFace;
        if (t1 > t2) {
            double t = t1;
            t1 = t2;
            t2 = t;
            Direction face = face1;
            face1 = face2;
            face2 = face;
        }
        return new SlabResult(t1, t2, face1, face2);
    }

    private static AABB getBlockHighlight(Level level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            return new AABB(pos);
        }
        return shape.bounds().move(pos);
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

    private static AABB getFaceHighlight(BlockPos pos, Direction face) {
        Vec3 c = Vec3.atCenterOf(pos);
        double minX = c.x - 0.5 + HIGHLIGHT_INSET;
        double minY = c.y - 0.5 + HIGHLIGHT_INSET;
        double minZ = c.z - 0.5 + HIGHLIGHT_INSET;
        double maxX = c.x + 0.5 - HIGHLIGHT_INSET;
        double maxY = c.y + 0.5 - HIGHLIGHT_INSET;
        double maxZ = c.z + 0.5 - HIGHLIGHT_INSET;

        switch (face) {
            case DOWN:  maxY = c.y - 0.5 + HIGHLIGHT_DEPTH; break;
            case UP:    minY = c.y + 0.5 - HIGHLIGHT_DEPTH; break;
            case NORTH: maxZ = c.z - 0.5 + HIGHLIGHT_DEPTH; break;
            case SOUTH: minZ = c.z + 0.5 - HIGHLIGHT_DEPTH; break;
            case WEST:  maxX = c.x - 0.5 + HIGHLIGHT_DEPTH; break;
            case EAST:  minX = c.x + 0.5 - HIGHLIGHT_DEPTH; break;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean onRightClick() {
        if (!active || sourcePos == null) return false;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player.isShiftKeyDown()) {
            validTarget = null;
            validFace = null;
            invalidReason = null;
        }

        if (validTarget != null && validClickedPos != null && validHitLocation != null && validFace != null) {
            CreateRedstoneLinkGUI.NETWORK.sendToServer(new RedstoneLinkMovePayload(sourcePos, validClickedPos, validHitLocation, validFace));
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.link_relocated"), true);
        } else if (invalidReason != null) {
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui." + invalidReason), true);
        } else {
            mc.player.displayClientMessage(
                    Component.translatable("gui.createredstonelinkgui.relocation_aborted"), true);
        }

        cancel();
        return true;
    }

    public static void cancel() {
        active = false;
        sourcePos = null;
        sourceState = null;
        validTarget = null;
        validClickedPos = null;
        validHitLocation = null;
        validFace = null;
        invalidReason = null;
        moveRange = 0;
    }

    public static boolean isActive() {
        return active;
    }

    private record RayBoxHit(Vec3 location, Direction face) {}

    private record SlabResult(double enterT, double exitT, Direction enterFace, Direction exitFace) {}
}
