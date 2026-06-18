package com.ggrgg.createredstonelinkgui.client.screen;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.client.RedstoneLinkMoveHandler;
import com.ggrgg.createredstonelinkgui.client.screen.widget.RedstoneLinkToggleWidget;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RedstoneLinkConfigScreen extends AbstractContainerScreen<RedstoneLinkMenu> {

    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");

    public Rect2i slot1Bounds;
    public Rect2i slot2Bounds;

    public RedstoneLinkConfigScreen(RedstoneLinkMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.slot1Bounds = new Rect2i(this.leftPos + 53, this.topPos + 25, 18, 18);
        this.slot2Bounds = new Rect2i(this.leftPos + 107, this.topPos + 25, 18, 18);

        if (this.menu.isRedstoneLink()) {
            this.addRenderableWidget(new RedstoneLinkToggleWidget(
                    this.leftPos + 14, this.topPos + 58,
                    this.menu.getPos(),
                    () -> {
                        var level = this.minecraft.level;
                        if (level != null) {
                            var state = level.getBlockState(this.menu.getPos());
                            if (state.getBlock() instanceof RedstoneLinkBlock) {
                                return state.getValue(RedstoneLinkBlock.RECEIVER);
                            }
                        }
                        return false;
                    }
            ));
        }

        IconButton relocateButton = new IconButton(this.leftPos + 80, this.topPos + 58, AllIcons.I_MOVE_GAUGE);
        relocateButton.withCallback(() -> {
            RedstoneLinkMoveHandler.startRelocating(this.menu.getPos());
            this.minecraft.setScreen(null);
        });
        relocateButton.setToolTip(Component.translatable("gui.createredstonelinkgui.relocate"));
        this.addRenderableWidget(relocateButton);
    }

    public void updateFrequencySlot(int slotIndex, ItemStack stack) {
        RedstoneLinkMenu customMenu = this.menu;
        var behaviour = customMenu.getBehaviour();
        if (behaviour != null) {
            RedstoneLinkMenu.applyFrequencyChangeDirect(behaviour, slotIndex == 0, stack);
        }
        CreateRedstoneLinkGUI.NETWORK.sendToServer(new RedstoneLinkFrequencyPayload(customMenu.getPos(), stack, slotIndex));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(BASE_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        graphics.fill(x + 7, y + 7, x + 169, y + 82, 0xFFC6C6C6);

        int s1x = this.slot1Bounds.getX();
        int s1y = this.slot1Bounds.getY();
        drawSlot(graphics, s1x, s1y);
        graphics.fill(s1x - 4, s1y - 4, s1x + 22, s1y, 0xFF7D2D3B);
        graphics.fill(s1x - 4, s1y + 18, s1x + 22, s1y + 22, 0xFF7D2D3B);
        graphics.fill(s1x - 4, s1y, s1x, s1y + 18, 0xFF7D2D3B);
        graphics.fill(s1x + 18, s1y, s1x + 22, s1y + 18, 0xFF7D2D3B);

        int s2x = this.slot2Bounds.getX();
        int s2y = this.slot2Bounds.getY();
        drawSlot(graphics, s2x, s2y);
        graphics.fill(s2x - 4, s2y - 4, s2x + 22, s2y, 0xFF5059AB);
        graphics.fill(s2x - 4, s2y + 18, s2x + 22, s2y + 22, 0xFF5059AB);
        graphics.fill(s2x - 4, s2y, s2x, s2y + 18, 0xFF5059AB);
        graphics.fill(s2x + 18, s2y, s2x + 22, s2y + 18, 0xFF5059AB);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 18, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + 18, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + 17, y + 17, 0xFFC6C6C6);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }
}
