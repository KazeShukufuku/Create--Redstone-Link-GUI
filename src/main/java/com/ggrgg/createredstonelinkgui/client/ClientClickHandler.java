package com.ggrgg.createredstonelinkgui.client;

import com.ggrgg.createredstonelinkgui.ClientConfig;
import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.network.OpenLinkMenuPayload;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRedstoneLinkGUI.MODID, value = Dist.CLIENT)
public class ClientClickHandler {

    @SubscribeEvent
    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.isCanceled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hitVec)) return;

        BlockPos pos = hitVec.getBlockPos();
        Level level = mc.level;
        Player player = mc.player;
        Vec3 hitLocation = hitVec.getLocation();

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        LinkBehaviour behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        Object voidLinkBehaviour = VoidLinkHelper.getBehaviour(level, pos);

        boolean hasLinkBehaviour = behaviour != null;
        boolean hasVoidLinkBehaviour = voidLinkBehaviour != null;
        if (!hasLinkBehaviour && !hasVoidLinkBehaviour) return;

        ItemStack heldItem = player.getItemInHand(event.getHand());
        if (!heldItem.isEmpty()) {
            if (player.isShiftKeyDown()) return;
            if (AllItems.LINKED_CONTROLLER.isIn(heldItem) || AllItems.WRENCH.isIn(heldItem)) return;
            if (hasLinkBehaviour && trySetFrequencyWithHeldItem(pos, behaviour, heldItem, hitLocation, event)) {
                return;
            }
        }

        ClientConfig.ClickMode clickMode = ClientConfig.CLICK_MODE.get();
        boolean requiresShift = clickMode != ClientConfig.ClickMode.SLOT;
        boolean hitAnyBlock = clickMode == ClientConfig.ClickMode.SHIFT_BLOCK;

        if (requiresShift && !player.isShiftKeyDown()) return;
        if (!requiresShift && player.isShiftKeyDown()) return;

        boolean hitValid = false;
        if (hasLinkBehaviour) {
            hitValid = hitAnyBlock || behaviour.testHit(true, hitLocation) || behaviour.testHit(false, hitLocation);
        } else if (hasVoidLinkBehaviour) {
            hitValid = hitAnyBlock || VoidLinkHelper.isHitOnAnySlot(voidLinkBehaviour, hitLocation);
        }
        if (!hitValid) return;

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHandItem.isEmpty()) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        CreateRedstoneLinkGUI.NETWORK.sendToServer(new OpenLinkMenuPayload(pos));
    }

    private static boolean trySetFrequencyWithHeldItem(BlockPos pos, LinkBehaviour behaviour, ItemStack heldItem,
            Vec3 hitLocation, InputEvent.InteractionKeyMappingTriggered event) {
        boolean firstHit = behaviour.testHit(true, hitLocation);
        boolean secondHit = behaviour.testHit(false, hitLocation);
        if (!firstHit && !secondHit) return false;

        event.setCanceled(true);
        event.setSwingHand(false);
        CreateRedstoneLinkGUI.NETWORK.sendToServer(
                new RedstoneLinkFrequencyPayload(pos, heldItem.copy(), firstHit ? 0 : 1));
        return true;
    }
}
