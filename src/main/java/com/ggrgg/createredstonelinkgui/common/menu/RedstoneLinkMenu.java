package com.ggrgg.createredstonelinkgui.common.menu;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class RedstoneLinkMenu extends AbstractLinkMenu {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, CreateRedstoneLinkGUI.MODID);

    public static final RegistryObject<MenuType<RedstoneLinkMenu>> TYPE = MENUS.register("redstone_link_menu",
        () -> IForgeMenuType.create((windowId, inv, data) -> new RedstoneLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final LinkBehaviour behaviour;
    private final boolean redstoneLink;
    private final boolean receiverMode;

    public RedstoneLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory, pos, TYPE.get());

        var level = playerInventory.player.level();
        var state = level.getBlockState(pos);
        this.redstoneLink = state.getBlock() instanceof RedstoneLinkBlock;
        this.receiverMode = this.redstoneLink && state.getValue(RedstoneLinkBlock.RECEIVER);

        var be = level.getBlockEntity(pos);
        this.behaviour = be == null ? null : BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);

        this.addSlot(new GhostRecipeSlot(0, 101, 34,
            () -> FrequencyHelper.getFrequencyItem(this.behaviour, 0),
            (id, stack) -> FrequencyHelper.setFrequencyItem(this.behaviour, id, stack)));
        this.addSlot(new GhostRecipeSlot(1, 137, 34,
            () -> FrequencyHelper.getFrequencyItem(this.behaviour, 1),
            (id, stack) -> FrequencyHelper.setFrequencyItem(this.behaviour, id, stack)));

        addPresetSlots(playerInventory);
        addPlayerInventorySlots(playerInventory);
    }

    @Override
    public LinkBehaviour getBehaviour() {
        return this.behaviour;
    }

    public boolean isRedstoneLink() {
        return this.redstoneLink;
    }

    public boolean isReceiverMode() {
        return this.receiverMode;
    }

    public static void applyFrequencyChangeDirect(LinkBehaviour targetBehaviour, boolean isFirstSlot, ItemStack item) {
        FrequencyHelper.applyFrequencyChangeDirect(targetBehaviour, isFirstSlot, item);
    }
}
