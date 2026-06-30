package com.ggrgg.createredstonelinkgui.common.preset;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Helper to interact with link behaviours via the ClipboardCloneable interface.
 * Mirrors the pattern used in ClipboardValueSettingsHandler.interact().
 */
public class FrequencyPresetHelper {

    /**
     * Copy the live frequency stacks from the current behaviour at the position.
     * This avoids trusting a still-open GUI's cached behaviour after a mode toggle.
     */
    public static boolean copyCurrentFrequencies(Player player, BlockPos pos, int presetIndex) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        Object behaviour = BlockEntityBehaviour.get(be, LinkBehaviour.TYPE);
        if (behaviour == null) {
            behaviour = VoidLinkHelper.getBehaviour(level, pos);
            if (behaviour != null && !VoidLinkHelper.canInteract(behaviour, player)) {
                return false;
            }
        }
        if (behaviour == null) return false;

        ItemStack first = FrequencyHelper.getFrequencyItem(behaviour, 0);
        ItemStack second = FrequencyHelper.getFrequencyItem(behaviour, 1);
        FrequencyPresetData.get(player).setPreset(presetIndex, first, second);
        return true;
    }

    /**
     * Copy frequencies from the link at pos into the preset under the given index.
     * Uses the ClipboardCloneable interface on the behaviour (same as clipboard item copy).
     * Returns true if any data was copied.
     */
    public static boolean copyFromLink(Player player, BlockPos pos, int presetIndex) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        CompoundTag copiedTag = new CompoundTag();
        boolean anySuccess = false;

        // Try behaviours (LinkBehaviour, VoidLinkBehaviour both implement ClipboardCloneable)
        if (be instanceof net.minecraft.world.level.block.entity.BlockEntity) {
            // Use the same pattern as ClipboardValueSettingsHandler: iterate all behaviours
            if (be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smartBE) {
                for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
                    if (behaviour instanceof ClipboardCloneable cc) {
                        CompoundTag tag = new CompoundTag();
                        boolean success = cc.writeToClipboard(tag, Direction.NORTH);
                        if (success) {
                            copiedTag.put(cc.getClipboardKey(), tag);
                            anySuccess = true;
                        }
                    }
                }
                // Also check if the BE itself is ClipboardCloneable
                if (smartBE instanceof ClipboardCloneable ccbe) {
                    CompoundTag tag = new CompoundTag();
                    boolean success = ccbe.writeToClipboard(tag, Direction.NORTH);
                    if (success) {
                        copiedTag.put(ccbe.getClipboardKey(), tag);
                        anySuccess = true;
                    }
                }
            }
        }

        if (!anySuccess) return false;

        // Store in player attachment
        FrequencyPresetData data = FrequencyPresetData.get(player);
        // Use the tag stored under "Frequencies" key (the clipboard key used by both LinkBehaviour and VoidLinkBehaviour)
        if (copiedTag.contains("Frequencies")) {
            data.setFromTag(presetIndex, copiedTag.getCompound("Frequencies"));
        } else {
            // Fallback: use whatever we got
            for (String key : copiedTag.getAllKeys()) {
                data.setFromTag(presetIndex, copiedTag.getCompound(key));
                break;
            }
        }
        return true;
    }

    /**
     * Paste frequencies from the preset into the link at pos.
     * Uses ClipboardCloneable paste logic. For void links, preserve the current
     * clipboard data and only overlay the frequency fields so owner data is not
     * affected by presets.
     * Returns true if the paste succeeded.
     */
    public static boolean pasteToLink(Player player, BlockPos pos, int presetIndex) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        FrequencyPresetData data = FrequencyPresetData.get(player);
        if (!data.hasPreset(presetIndex)) return false;
        CompoundTag presetTag = data.getAsTag(presetIndex);

        Object voidBehaviour = VoidLinkHelper.getBehaviour(level, pos);
        boolean isVoidLink = voidBehaviour != null && BlockEntityBehaviour.get(be, LinkBehaviour.TYPE) == null;
        if (isVoidLink && !VoidLinkHelper.canInteract(voidBehaviour, player)) {
            return false;
        }

        boolean anySuccess = false;

        if (be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smartBE) {
            for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
                if (behaviour instanceof ClipboardCloneable cc) {
                    boolean success = pasteToClipboardCloneable(cc, presetTag, player, isVoidLink);
                    anySuccess |= success;
                }
            }
            if (smartBE instanceof ClipboardCloneable ccbe) {
                boolean success = pasteToClipboardCloneable(ccbe, presetTag, player, isVoidLink);
                anySuccess |= success;
            }
        }

        if (anySuccess) {
            be.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        }
        return anySuccess;
    }

    private static boolean pasteToClipboardCloneable(ClipboardCloneable clipboard, CompoundTag presetTag,
                                                     Player player, boolean preserveNonFrequencyData) {
        CompoundTag tagToPaste = presetTag;
        if (preserveNonFrequencyData) {
            CompoundTag currentTag = new CompoundTag();
            if (!clipboard.writeToClipboard(currentTag, Direction.NORTH) || !hasFrequencyData(currentTag)) {
                return false;
            }
            tagToPaste = currentTag;
            overlayFrequencyData(tagToPaste, presetTag);
        }
        return clipboard.readFromClipboard(tagToPaste, player, Direction.NORTH, false);
    }

    private static boolean hasFrequencyData(CompoundTag tag) {
        return tag.contains("First") || tag.contains("Last");
    }

    private static void overlayFrequencyData(CompoundTag target, CompoundTag source) {
        target.put("First", source.getCompound("First").copy());
        target.put("Last", source.getCompound("Last").copy());
    }
}
