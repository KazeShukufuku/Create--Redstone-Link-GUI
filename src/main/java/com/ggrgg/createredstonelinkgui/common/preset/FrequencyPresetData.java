package com.ggrgg.createredstonelinkgui.common.preset;

import java.util.ArrayList;
import java.util.List;

import com.ggrgg.createredstonelinkgui.CreateRedstoneLinkGUI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Stores four pairs of frequency preset ItemStacks on the player.
 */
public class FrequencyPresetData {

    public static final int PRESET_COUNT = 4;

    private static final String ROOT_KEY = CreateRedstoneLinkGUI.MODID + ".frequency_presets";

    private final List<Preset> presets;
    private Player owner;

    public FrequencyPresetData() {
        this.presets = new ArrayList<>(PRESET_COUNT);
        for (int i = 0; i < PRESET_COUNT; i++) {
            presets.add(new Preset());
        }
    }

    public static FrequencyPresetData get(Player player) {
        FrequencyPresetData data = new FrequencyPresetData();
        data.owner = player;
        if (player != null && player.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            data.deserializeNBT(player.getPersistentData().getCompound(ROOT_KEY));
        }
        return data;
    }

    public static void copyOnClone(Player original, Player clone) {
        if (original.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            clone.getPersistentData().put(ROOT_KEY, original.getPersistentData().getCompound(ROOT_KEY).copy());
        }
    }

    public static void applyToPlayer(Player player, CompoundTag tag) {
        if (player != null) {
            player.getPersistentData().put(ROOT_KEY, tag.copy());
        }
    }

    public ItemStack getStack(int presetIndex, int slotIndex) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return ItemStack.EMPTY;
        return slotIndex == 0 ? presets.get(presetIndex).stack0 : presets.get(presetIndex).stack1;
    }

    public boolean hasPreset(int presetIndex) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return false;
        return presets.get(presetIndex).saved;
    }

    public void setPreset(int presetIndex, ItemStack first, ItemStack last) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return;
        Preset preset = presets.get(presetIndex);
        preset.stack0 = normalizeStack(first);
        preset.stack1 = normalizeStack(last);
        preset.saved = true;
        saveToOwner();
    }

    public void setStack(int presetIndex, int slotIndex, ItemStack stack) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return;
        ItemStack copy = normalizeStack(stack);
        if (slotIndex == 0) {
            presets.get(presetIndex).stack0 = copy;
        } else {
            presets.get(presetIndex).stack1 = copy;
        }
        presets.get(presetIndex).saved = true;
        saveToOwner();
    }

    /**
     * Set both stacks for a preset from a ClipboardCloneable-style tag.
     */
    public void setFromTag(int presetIndex, CompoundTag tag) {
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return;
        Preset p = presets.get(presetIndex);
        p.stack0 = tag.contains("First", Tag.TAG_COMPOUND)
            ? normalizeStack(ItemStack.of(tag.getCompound("First")))
            : ItemStack.EMPTY;
        p.stack1 = tag.contains("Last", Tag.TAG_COMPOUND)
            ? normalizeStack(ItemStack.of(tag.getCompound("Last")))
            : ItemStack.EMPTY;
        p.saved = true;
        saveToOwner();
    }

    /**
     * Write both stacks of a preset into the format LinkBehaviour expects.
     */
    public CompoundTag getAsTag(int presetIndex) {
        CompoundTag tag = new CompoundTag();
        if (presetIndex < 0 || presetIndex >= PRESET_COUNT) return tag;
        Preset p = presets.get(presetIndex);
        tag.put("First", saveStack(p.stack0));
        tag.put("Last", saveStack(p.stack1));
        return tag;
    }

    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < PRESET_COUNT; i++) {
            CompoundTag entry = new CompoundTag();
            Preset p = presets.get(i);
            if (p.saved) entry.putBoolean("Saved", true);
            if (!p.stack0.isEmpty()) entry.put("0", p.stack0.save(new CompoundTag()));
            if (!p.stack1.isEmpty()) entry.put("1", p.stack1.save(new CompoundTag()));
            list.add(entry);
        }
        root.put("presets", list);
        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        for (Preset preset : presets) {
            preset.stack0 = ItemStack.EMPTY;
            preset.stack1 = ItemStack.EMPTY;
            preset.saved = false;
        }
        ListTag list = root.getList("presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), PRESET_COUNT); i++) {
            CompoundTag entry = list.getCompound(i);
            Preset p = presets.get(i);
            p.stack0 = entry.contains("0", Tag.TAG_COMPOUND)
                ? normalizeStack(ItemStack.of(entry.getCompound("0")))
                : ItemStack.EMPTY;
            p.stack1 = entry.contains("1", Tag.TAG_COMPOUND)
                ? normalizeStack(ItemStack.of(entry.getCompound("1")))
                : ItemStack.EMPTY;
            p.saved = entry.getBoolean("Saved") || !p.stack0.isEmpty() || !p.stack1.isEmpty();
        }
    }

    public List<CompoundTag> toTagList() {
        List<CompoundTag> list = new ArrayList<>(PRESET_COUNT);
        for (int i = 0; i < PRESET_COUNT; i++) {
            list.add(getAsTag(i));
        }
        return list;
    }

    public void fromTagList(List<CompoundTag> list) {
        for (int i = 0; i < Math.min(list.size(), PRESET_COUNT); i++) {
            setFromTag(i, list.get(i));
        }
    }

    private static CompoundTag saveStack(ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }

    private static ItemStack normalizeStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        return copy;
    }

    private void saveToOwner() {
        if (owner != null) {
            owner.getPersistentData().put(ROOT_KEY, serializeNBT());
        }
    }

    private static class Preset {
        ItemStack stack0 = ItemStack.EMPTY;
        ItemStack stack1 = ItemStack.EMPTY;
        boolean saved = false;
    }
}
