package com.ggrgg.createredstonelinkgui.common.menu;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;
import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VoidLinkMenu extends AbstractLinkMenu {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, CreateRedstoneLinkGUI.MODID);

    public static final RegistryObject<MenuType<VoidLinkMenu>> TYPE = MENUS.register("void_link_menu",
        () -> IForgeMenuType.create((windowId, inv, data) -> new VoidLinkMenu(windowId, inv, data.readBlockPos()))
    );

    private final Object behaviour;

    public VoidLinkMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory, pos, TYPE.get());
        this.behaviour = VoidLinkHelper.getBehaviour(playerInventory.player.level(), pos);

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
    public Object getBehaviour() {
        return this.behaviour;
    }

    public static void applyFrequencyChangeDirect(Object targetBehaviour, boolean isFirstSlot, ItemStack item) {
        FrequencyHelper.applyFrequencyChangeDirect(targetBehaviour, isFirstSlot, item);
    }
}
