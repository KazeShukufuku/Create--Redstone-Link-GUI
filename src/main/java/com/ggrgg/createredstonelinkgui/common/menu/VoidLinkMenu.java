package com.ggrgg.createredstonelinkgui.common.menu;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VoidLinkMenu extends AbstractContainerMenu {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CreateRedstoneLinkGUI.MODID);

    public static final RegistryObject<MenuType<VoidLinkMenu>> TYPE = MENUS.register("void_link_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new VoidLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final BlockPos pos;
    private final Object behaviour;

    public VoidLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TYPE.get(), containerId);
        this.pos = pos;
        this.behaviour = VoidLinkHelper.getBehaviour(playerInventory.player.level(), pos);

        this.addSlot(new GhostRecipeSlot(0, 101, 34, () -> getFrequencyItem(0), this::setFrequencyItem));
        this.addSlot(new GhostRecipeSlot(1, 137, 34, () -> getFrequencyItem(1), this::setFrequencyItem));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 112 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 48 + col * 18, 170));
        }
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Object getBehaviour() {
        return this.behaviour;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 2) {
            var slot = this.getSlot(slotId);
            ItemStack targetStack = ItemStack.EMPTY;

            if (button == 1 || clickType == ClickType.THROW) {
                slot.set(ItemStack.EMPTY);
            } else {
                targetStack = getCarried().copy();
                slot.set(targetStack);
            }

            if (player.level().isClientSide()) {
                CreateRedstoneLinkGUI.NETWORK.sendToServer(new RedstoneLinkFrequencyPayload(this.pos, targetStack, slotId));
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private ItemStack getFrequencyItem(int index) {
        return VoidLinkHelper.getFrequencyStack(this.behaviour, index == 0);
    }

    private void setFrequencyItem(int index, ItemStack stack) {
        applyFrequencyChangeDirect(this.behaviour, index == 0, stack);
    }

    public static void applyFrequencyChangeDirect(Object targetBehaviour, boolean isFirstSlot, ItemStack item) {
        VoidLinkHelper.setFrequency(targetBehaviour, isFirstSlot, item);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
