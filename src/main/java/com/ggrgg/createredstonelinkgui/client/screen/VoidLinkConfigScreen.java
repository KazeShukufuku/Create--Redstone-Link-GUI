package com.ggrgg.createredstonelinkgui.client.screen;

import java.util.Objects;
import java.util.UUID;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.VoidLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.VoidLinkClaimPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VoidLinkConfigScreen extends AbstractLinkConfigScreen<VoidLinkMenu> {

    private static final ResourceLocation OVERLAY_TEXTURE =
        new ResourceLocation("createredstonelinkgui", "textures/void_link.png");
    private static final int BLOCK_PREVIEW_X = 222;
    private static final int BLOCK_PREVIEW_Y = 30;

    public VoidLinkConfigScreen(VoidLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    @Override
    protected ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }

    @Override
    protected int getBlockPreviewX() {
        return BLOCK_PREVIEW_X;
    }

    @Override
    protected int getBlockPreviewY() {
        return BLOCK_PREVIEW_Y;
    }

    @Override
    protected void addExtraWidgets(int contentLeft, int contentTop) {
        this.addRenderableWidget(new SkullButton(contentLeft + 79, contentTop + 64, btn -> {
            Object behaviour = this.menu.getBehaviour();
            if (behaviour == null || this.minecraft.player == null) return;

            var owner = VoidLinkHelper.getOwner(behaviour);
            if (owner == null || this.minecraft.player.getUUID().equals(owner.getId())) {
                CreateRedstoneLinkGUI.NETWORK.sendToServer(new VoidLinkClaimPayload(this.menu.getPos()));
            }
        }));
    }

    private class SkullButton extends ImageButton {
        private ItemStack cachedStack = new ItemStack(Items.SKELETON_SKULL);
        private UUID lastOwnerId;

        SkullButton(int x, int y, OnPress onPress) {
            super(x, y, ICON_SIZE, ICON_SIZE, OVERLAY_TEXTURE, 0, 0, Component.empty(), onPress);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Object behaviour = menu.getBehaviour();
            if (behaviour != null) {
                var owner = VoidLinkHelper.getOwner(behaviour);
                UUID ownerId = owner == null ? null : owner.getId();
                if (!Objects.equals(ownerId, lastOwnerId)) {
                    lastOwnerId = ownerId;
                    cachedStack = owner == null ? new ItemStack(Items.SKELETON_SKULL) : ownerHead(owner);
                }
            }
            graphics.renderItem(cachedStack, getX(), getY());
            if (isHovered()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x221500FF);
                boolean owned = behaviour != null && VoidLinkHelper.getOwner(behaviour) != null;
                Component tooltip = owned
                    ? Component.translatable("gui.createredstonelinkgui.forfeit")
                    : Component.translatable("gui.createredstonelinkgui.own");
                graphics.renderTooltip(VoidLinkConfigScreen.this.minecraft.font, tooltip, mouseX, mouseY);
            }
        }

        private ItemStack ownerHead(com.mojang.authlib.GameProfile owner) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            CompoundTag tag = new CompoundTag();
            tag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
            stack.setTag(tag);
            return stack;
        }
    }
}
